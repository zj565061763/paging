package com.sd.lib.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

interface Paging<T : Any>

interface FPaging<T : Any> : Paging<T> {
  /** 状态 */
  val state: PagingState<T>

  /** 状态流 */
  val stateFlow: StateFlow<PagingState<T>>

  /**
   * 刷新数据，如果当前正在[refresh]或者[append]，会先取消再刷新
   */
  suspend fun refresh()

  /**
   * 加载尾部数据，如果当前正在[refresh]或者[append]，会抛出[CancellationException]，
   * 如果调用时数据为空，会转发到[refresh]
   */
  suspend fun append()

  /**
   * 修改数据，[block]在主线程触发，
   * 注意：[block]中不允许再调用[modify]，否则会抛异常[NestModifyException]
   */
  suspend fun modify(block: suspend (List<T>) -> List<T>)
}

/**
 * 创建[FPaging]
 *
 * @param refreshKey 刷新数据的页码
 * @param pagingDataHandler [PagingDataHandler]
 */
fun <Key : Any, Value : Any> FPaging(
  refreshKey: Key,
  pagingSource: PagingSource<Key, Value>,
  pagingDataHandler: PagingDataHandler<Key, Value> = DefaultPagingDataHandler(),
): FPaging<Value> {
  return PagingImpl(
    refreshKey = refreshKey,
    pagingSource = pagingSource,
    pagingDataHandler = pagingDataHandler,
  )
}

//-------------------- impl --------------------

private class PagingImpl<Key : Any, Value : Any>(
  private val refreshKey: Key,
  private val pagingSource: PagingSource<Key, Value>,
  private val pagingDataHandler: PagingDataHandler<Key, Value>,
) : FPaging<Value> {

  private val _mutator = Mutator()
  private val _stateFlow = MutableStateFlow(PagingState<Value>())

  private var _nextKey: Key? = null

  override val state: PagingState<Value> get() = _stateFlow.value
  override val stateFlow: StateFlow<PagingState<Value>> get() = _stateFlow.asStateFlow()

  override suspend fun refresh(): Unit = withContext(Dispatchers.Main) {
    _mutator.mutate {
      val oldLoadState = state.refreshLoadState.also { check(it !is LoadState.Loading) }
      _stateFlow.update { it.copy(refreshLoadState = LoadState.Loading) }
      loadAndHandle(LoadParams.Refresh(refreshKey))
        .onSuccess { data ->
          val (loadResult, items) = data
          _nextKey = loadResult.nextKey
          _stateFlow.update {
            it.copy(
              items = items,
              refreshLoadState = LoadState.NotLoading.Complete,
              appendLoadState = LoadState.NotLoading.Incomplete,
            )
          }
        }
        .onFailure { error ->
          if (error is CancellationException) {
            _stateFlow.update { it.copy(refreshLoadState = oldLoadState) }
            throw error
          } else {
            _stateFlow.update { it.copy(refreshLoadState = LoadState.Error(error)) }
          }
        }
    }
  }

  override suspend fun append(): Unit = withContext(Dispatchers.Main) {
    if (_mutator.mutex.isLocked) {
      // 如果正在加载中，抛出异常，取消当前协程
      throw CancellationException()
    }

    if (state.items.isEmpty()) {
      // 如果数据为空，触发刷新
      refresh()
      return@withContext
    }

    val appendKey = _nextKey ?: return@withContext

    _mutator.mutate {
      val oldLoadState = state.appendLoadState.also { check(it !is LoadState.Loading) }
      _stateFlow.update { it.copy(appendLoadState = LoadState.Loading) }
      loadAndHandle(LoadParams.Append(appendKey))
        .onSuccess { data ->
          val (loadResult, items) = data
          loadResult.nextKey?.also { _nextKey = it }
          _stateFlow.update {
            it.copy(
              items = items,
              appendLoadState = if (loadResult.nextKey == null) LoadState.NotLoading.Complete else LoadState.NotLoading.Incomplete,
            )
          }
        }
        .onFailure { error ->
          if (error is CancellationException) {
            _stateFlow.update { it.copy(appendLoadState = oldLoadState) }
            throw error
          } else {
            _stateFlow.update { it.copy(appendLoadState = LoadState.Error(error)) }
          }
        }
    }
  }

  override suspend fun modify(block: suspend (List<Value>) -> List<Value>) {
    if (currentCoroutineContext()[ModifyElement]?.tag === this@PagingImpl) {
      throw NestModifyException("Can not call modify in the modify block.")
    }
    withContext(Dispatchers.Main + ModifyElement(this@PagingImpl)) {
      _mutator.mutex.withLock {
        val newItems = block(state.items)
        _stateFlow.update {
          it.copy(items = newItems)
        }
      }
    }
  }

  /** 加载分页数据，并返回总数据 */
  private suspend fun loadAndHandle(loadParams: LoadParams<Key>): Result<Pair<LoadResult.Page<Key, Value>, List<Value>>> {
    return runCatching {
      val loadResult = pagingSource.load(loadParams).also { currentCoroutineContext().ensureActive() }
      when (loadResult) {
        is LoadResult.None -> throw CancellationException()
        is LoadResult.Page -> {
          val items = pagingDataHandler.handlePageData(
            totalData = state.items,
            params = loadParams,
            pageData = loadResult.data,
          ).also { currentCoroutineContext().ensureActive() }
          loadResult to items
        }
      }
    }
  }
}

private class ModifyElement(
  val tag: FPaging<*>,
) : AbstractCoroutineContextElement(ModifyElement) {
  companion object Key : CoroutineContext.Key<ModifyElement>
}

/** 嵌套修改异常 */
private class NestModifyException(message: String) : IllegalStateException(message)

//-------------------- Mutator --------------------

private class Mutator {
  private class Mutator(val priority: Int, val job: Job) {
    fun canInterrupt(other: Mutator) = priority >= other.priority

    fun cancel() = job.cancel(MutationInterruptedException())
  }

  private val currentMutator = AtomicReference<Mutator?>(null)
  val mutex = Mutex()

  private fun tryMutateOrCancel(mutator: Mutator) {
    while (true) {
      val oldMutator = currentMutator.get()
      if (oldMutator == null || mutator.canInterrupt(oldMutator)) {
        if (currentMutator.compareAndSet(oldMutator, mutator)) {
          oldMutator?.cancel()
          break
        }
      } else throw CancellationException("Current mutation had a higher priority")
    }
  }

  suspend fun <R> mutate(
    priority: Int = 0,
    block: suspend () -> R,
  ) = coroutineScope {
    val mutator = Mutator(priority, coroutineContext[Job]!!)

    tryMutateOrCancel(mutator)

    mutex.withLock {
      try {
        block()
      } finally {
        currentMutator.compareAndSet(mutator, null)
      }
    }
  }
}

private class MutationInterruptedException : CancellationException("Mutation interrupted") {
  override fun fillInStackTrace(): Throwable {
    // Avoid null.clone() on Android <= 6.0 when accessing stackTrace
    stackTrace = emptyArray()
    return this
  }
}