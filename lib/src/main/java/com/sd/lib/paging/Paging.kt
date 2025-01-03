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
   * 刷新数据，如果当前正在刷新或者正在加载更多，会取消正在进行的加载，然后再触发[onLoad]
   * @param onLoad 加载回调，主线程触发
   */
  suspend fun refresh(
    onLoad: suspend LoadScope<T>.(page: Int) -> List<T>,
  )

  /**
   * 加载更多数据，如果当前正在刷新或者正在加载更多，会抛出[CancellationException]，取消本次调用，
   * 如果调用时数据为空，会转发到[refresh]
   *
   * @param onLoad 加载回调，主线程触发
   */
  suspend fun append(
    onLoad: suspend LoadScope<T>.(page: Int) -> List<T>,
  )

  /** 取消加载 */
  suspend fun cancelLoad()

  interface LoadScope<T> {
    /** 当前状态 */
    val pagingState: PagingState<T>
  }
}

/**
 * 创建[FPaging]
 *
 * @param refreshPage 刷新数据的页码，如果数据源页码从1开始，那么[refreshPage]就传1
 * @param dataHandler [PagingDataHandler]
 */
fun <T> FPaging(
  refreshPage: Int = 1,
  dataHandler: PagingDataHandler<T> = DefaultPagingDataHandler(),
): FPaging<T> {
  return PagingImpl(
    refreshPage = refreshPage,
    dataHandler = dataHandler,
  )
}

//-------------------- impl --------------------

private class PagingImpl<T>(
  refreshPage: Int,
  private val dataHandler: PagingDataHandler<T>,
) : FPaging<T>, FPaging.LoadScope<T> {

  private val _mutator = MutatorMutex()
  private var _currentAppendPage = refreshPage

  private val _stateFlow = MutableStateFlow(
    PagingState(
      data = emptyList<T>(),
      refreshPage = refreshPage,
    )
  )

  init {
    dataHandler.getPagingState = { state }
  }

  override val state: PagingState<T>
    get() = _stateFlow.value

  override val stateFlow: StateFlow<PagingState<T>>
    get() = _stateFlow.asStateFlow()

  override val pagingState: PagingState<T>
    get() = state

  override suspend fun refresh(
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ) = withContext(Dispatchers.Main) {
    _mutator.mutate {
      val loadPage = state.refreshPage
      val oldLoadState = state.refreshLoadState
      _stateFlow.update { it.copy(refreshLoadState = LoadState.Loading) }
      load(
        page = loadPage,
        onSuccess = { _, totalData ->
          _currentAppendPage = loadPage
          _stateFlow.update {
            it.copy(
              data = totalData ?: it.data,
              refreshLoadState = LoadState.NotLoading.Complete,
            )
          }
        },
        onFailure = { error ->
          if (error is CancellationException) {
            _stateFlow.update { it.copy(refreshLoadState = oldLoadState) }
            throw error
          } else {
            _stateFlow.update { it.copy(refreshLoadState = LoadState.Error(error)) }
          }
        },
        onLoad = onLoad,
      )
    }
  }

  override suspend fun append(
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ) = withContext(Dispatchers.Main) {
    if (_mutator.isMutating) {
      // 如果正在加载中，抛出异常，取消当前协程
      throw CancellationException()
    }

    if (state.data.isEmpty()) {
      // 如果数据为空，触发刷新
      refresh(onLoad)
      return@withContext
    }

    _mutator.mutate {
      val loadPage = _currentAppendPage + 1
      val oldLoadState = state.appendLoadState
      _stateFlow.update { it.copy(appendLoadState = LoadState.Loading) }
      load(
        page = loadPage,
        onSuccess = { pageData, totalData ->
          if (pageData.isNotEmpty()) {
            _currentAppendPage = loadPage
          }
          _stateFlow.update {
            it.copy(
              data = totalData ?: it.data,
              appendLoadState = if (pageData.isEmpty()) LoadState.NotLoading.Complete else LoadState.NotLoading.Incomplete,
            )
          }
        },
        onFailure = { error ->
          if (error is CancellationException) {
            _stateFlow.update { it.copy(appendLoadState = oldLoadState) }
            throw error
          } else {
            _stateFlow.update { it.copy(appendLoadState = LoadState.Error(error)) }
          }
        },
        onLoad = onLoad,
      )
    }
  }

  override suspend fun cancelLoad() {
    _mutator.cancelAndJoin()
  }

  private suspend fun load(
    page: Int,
    onSuccess: (pageData: List<T>, totalData: List<T>?) -> Unit,
    onFailure: (Throwable) -> Unit,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ) {
    runCatching {
      val pageData = onLoad(page)
      val totalData = handlePageData(page, pageData)
      pageData to totalData
    }.onSuccess {
      onSuccess(it.first, it.second)
    }.onFailure {
      onFailure(it)
    }
  }

  private suspend fun handlePageData(page: Int, data: List<T>): List<T>? {
    currentCoroutineContext().ensureActive()
    return dataHandler.handlePageData(page, data).also {
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

  suspend fun cancelAndJoin() {
    while (true) {
      val mutator = currentMutator.get() ?: return
      mutator.cancel()
      try {
        mutator.job.join()
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