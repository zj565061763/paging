package com.sd.lib.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface FPaging<T> {
  /** 状态 */
  val state: PagingState<T>

  /** 状态流 */
  val stateFlow: StateFlow<PagingState<T>>

  /**
   * 刷新，如果当前正在刷新或者正在加载更多，会取消正在进行的加载
   *
   * @param notifyLoading 是否通知[PagingState.isRefreshing]
   * @param onLoad 加载回调
   */
  suspend fun refresh(
    notifyLoading: Boolean = true,
    onLoad: suspend LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>>

  /**
   * 加载更多，如果当前正在刷新或者正在加载更多，会抛出[CancellationException]异常，取消本次调用
   *
   * @param notifyLoading 是否通知[PagingState.isAppending]
   * @param onLoad 加载回调
   */
  suspend fun append(
    notifyLoading: Boolean = true,
    onLoad: suspend LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>>

  /** 取消刷新 */
  suspend fun cancelRefresh()

  /** 取消加载更多 */
  suspend fun cancelAppend()

  interface LoadScope<T> {
    /** 当前状态 */
    val pagingState: PagingState<T>

    /** 刷新数据的页码，例如数据源页码从1开始，那么[refreshPage]就为1 */
    val refreshPage: Int
      get() = pagingState.refreshPage
  }
}

/**
 * [FPaging]
 *
 * @param initial 初始值
 * @param refreshPage 刷新数据的页码，例如数据源页码从1开始，那么[refreshPage]就为1
 * @param dataHandler 处理每页的数据，并返回总的数据，返回null则总数据不变
 */
fun <T> FPaging(
  initial: List<T> = emptyList(),
  refreshPage: Int = 1,
  dataHandler: PagingDataHandler<T> = DefaultPagingDataHandler(),
): FPaging<T> {
  return PagingImpl(
    initial = initial,
    refreshPage = refreshPage,
    dataHandler = dataHandler,
  )
}

//-------------------- impl --------------------

private class PagingImpl<T>(
  initial: List<T>,
  refreshPage: Int,
  private val dataHandler: PagingDataHandler<T>,
) : FPaging<T>, FPaging.LoadScope<T> {

  private val _refreshMutator = MutatorMutex()
  private val _appendMutator = MutatorMutex()

  private val _stateFlow = MutableStateFlow(
    PagingState(
      data = initial,
      refreshPage = refreshPage,
    )
  )

  init {
    dataHandler.getPagingState = { _stateFlow.value }
  }

  override val state: PagingState<T>
    get() = _stateFlow.value

  override val stateFlow: StateFlow<PagingState<T>>
    get() = _stateFlow.asStateFlow()

  override val pagingState: PagingState<T>
    get() = _stateFlow.value

  override suspend fun refresh(
    notifyLoading: Boolean,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>> {
    // 刷新之前，取消加载更多
    cancelAppend()
    return load(
      mutator = _refreshMutator,
      onStart = {
        if (notifyLoading) {
          _stateFlow.update { it.copy(isRefreshing = true) }
        }
      },
      onFinish = {
        if (notifyLoading) {
          _stateFlow.update { it.copy(isRefreshing = false) }
        }
      },
      getPage = { state.refreshPage },
      onLoad = onLoad,
    )
  }

  override suspend fun append(
    notifyLoading: Boolean,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>> {
    if (state.isRefreshing || state.isAppending) {
      throw AppendCancellationException()
    }
    return load(
      mutator = _appendMutator,
      onStart = {
        if (notifyLoading) {
          _stateFlow.update { it.copy(isAppending = true) }
        }
      },
      onFinish = {
        if (notifyLoading) {
          _stateFlow.update { it.copy(isAppending = false) }
        }
      },
      getPage = { getAppendPage() },
      onLoad = onLoad,
    )
  }

  private suspend fun load(
    mutator: MutatorMutex,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    getPage: () -> Int,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>> {
    return mutator.mutate {
      val page = getPage()
      try {
        onStart()
        onLoad(page)
          .also { handleLoadData(page, it) }
          .let { Result.success(it) }
      } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure<List<T>>(e).also {
          currentCoroutineContext().ensureActive()
          _stateFlow.update { it.copy(loadResult = Result.failure(e)) }
        }
      } finally {
        onFinish()
      }
    }
  }

  override suspend fun cancelRefresh() {
    _refreshMutator.cancelAndJoin()
  }

  override suspend fun cancelAppend() {
    _appendMutator.cancelAndJoin()
  }

  private fun getAppendPage(): Int {
    if (state.data.isEmpty()) return state.refreshPage
    val loadPage = state.loadPage ?: return state.refreshPage
    return if (state.loadSize!! <= 0) loadPage else loadPage + 1
  }

  private suspend fun handleLoadData(page: Int, data: List<T>) {
    currentCoroutineContext().ensureActive()
    val totalData = dataHandler.handlePageData(page, data)

    currentCoroutineContext().ensureActive()
    _stateFlow.update { state ->
      state.copy(
        data = totalData ?: state.data,
        loadResult = Result.success(Unit),
        loadPage = page,
        loadSize = data.size,
      )
    }
  }
}

private class AppendCancellationException : CancellationException("Append cancellation") {
  override fun fillInStackTrace(): Throwable {
    stackTrace = emptyArray()
    return this
  }
}