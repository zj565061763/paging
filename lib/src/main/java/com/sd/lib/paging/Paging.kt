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

interface FPaging<T> {
  /** 状态 */
  val state: PagingState<T>

  /** 状态流 */
  val stateFlow: StateFlow<PagingState<T>>

  /**
   * 刷新数据，如果当前正在刷新或者加载更多，会先取消加载
   */
  suspend fun refresh()

  /**
   * 加载更多数据，如果当前正在刷新或者加载更多，会抛出[CancellationException]取消本次调用，
   * 如果调用时数据为空，会转发到[refresh]
   */
  suspend fun append()
}

/**
 * 创建[FPaging]
 *
 * @param refreshKey 刷新数据的页码
 * @param dataHandler [PagingDataHandler]
 */
fun <Key : Any, Value : Any> FPaging(
  refreshKey: Key,
  dataHandler: PagingDataHandler<Key, Value> = PagingDataHandler.default(),
  pagingSource: PagingSource<Key, Value>,
): FPaging<Value> {
  return PagingImpl(
    refreshKey = refreshKey,
    dataHandler = dataHandler,
    pagingSource = pagingSource,
  )
}

//-------------------- impl --------------------

private class PagingImpl<Key : Any, Value : Any>(
  private val refreshKey: Key,
  private val dataHandler: PagingDataHandler<Key, Value>,
  private val pagingSource: PagingSource<Key, Value>,
) : FPaging<Value> {

  private val _mutator = MutatorMutex()
  private val _stateFlow = MutableStateFlow(PagingState<Value>())

  private var _nextKey: Key? = null

  override val state: PagingState<Value>
    get() = _stateFlow.value

  override val stateFlow: StateFlow<PagingState<Value>>
    get() = _stateFlow.asStateFlow()

  override suspend fun refresh(): Unit = withContext(Dispatchers.Main) {
    _mutator.mutate {
      val oldLoadState = state.refreshLoadState
      _stateFlow.update { it.copy(refreshLoadState = LoadState.Loading) }
      loadData(LoadParams.Refresh(refreshKey))
        .onSuccess { data ->
          val (loadResult, totalData) = data
          when (loadResult) {
            is LoadResult.None -> check(totalData == null)
            is LoadResult.Page -> {
              _nextKey = loadResult.nextKey
              _stateFlow.update {
                it.copy(
                  data = totalData ?: it.data,
                  refreshLoadState = LoadState.NotLoading.Complete,
                )
              }
            }
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
    if (_mutator.isMutating) {
      // 如果正在加载中，抛出异常，取消当前协程
      throw CancellationException()
    }

    if (state.data.isEmpty()) {
      // 如果数据为空，触发刷新
      refresh()
      return@withContext
    }

    val appendKey = _nextKey ?: return@withContext

    _mutator.mutate {
      val oldLoadState = state.appendLoadState
      _stateFlow.update { it.copy(appendLoadState = LoadState.Loading) }
      loadData(LoadParams.Append(appendKey))
        .onSuccess { data ->
          val (loadResult, totalData) = data
          when (loadResult) {
            is LoadResult.None -> check(totalData == null)
            is LoadResult.Page -> {
              loadResult.nextKey?.also { _nextKey = it }
              _stateFlow.update {
                it.copy(
                  data = totalData ?: it.data,
                  appendLoadState = if (loadResult.nextKey == null) LoadState.NotLoading.Complete else LoadState.NotLoading.Incomplete,
                )
              }
            }
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

  /** 加载数据 */
  private suspend fun loadData(loadParams: LoadParams<Key>): Result<Pair<LoadResult<Key, Value>, List<Value>?>> {
    return runCatching {
      val loadResult = pagingSource.load(loadParams)
      val totalData = handleResult(loadParams, loadResult)
      loadResult to totalData
    }
  }

  /** 处理加载结果，并返回总数据 */
  private suspend fun handleResult(
    loadParams: LoadParams<Key>,
    loadResult: LoadResult<Key, Value>,
  ): List<Value>? {
    return when (loadResult) {
      is LoadResult.None -> null
      is LoadResult.Page -> {
        currentCoroutineContext().ensureActive()
        dataHandler.handlePageData(
          params = loadParams,
          result = loadResult,
        )
      }
    }.also {
      currentCoroutineContext().ensureActive()
    }
  }
}

//-------------------- MutatorMutex --------------------

private class MutatorMutex {
  private class Mutator(val priority: Int, val job: Job) {
    fun canInterrupt(other: Mutator) = priority >= other.priority

    fun cancel() = job.cancel(MutationInterruptedException())
  }

  private val currentMutator = AtomicReference<Mutator?>(null)
  private val mutex = Mutex()

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

  val isMutating: Boolean
    get() = mutex.isLocked
}

private class MutationInterruptedException : CancellationException("Mutation interrupted") {
  override fun fillInStackTrace(): Throwable {
    // Avoid null.clone() on Android <= 6.0 when accessing stackTrace
    stackTrace = emptyArray()
    return this
  }
}