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
    notifyLoading: Boolean,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>> {
    return load(
      page = state.refreshPage,
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
      onLoad = onLoad,
    )
  }

  override suspend fun append(
    notifyLoading: Boolean,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>> {
    if (_mutator.isMutating) throw AppendCancellationException()
    return load(
      page = getAppendPage(),
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
      onLoad = onLoad,
    )
  }

  override suspend fun cancelLoad() {
    _mutator.cancelAndJoin()
  }

  private fun getAppendPage(): Int {
    with(state) {
      if (data.isEmpty()) return refreshPage
      val successPage = successPage ?: return refreshPage
      return if (successPage.size > 0) successPage.page + 1 else successPage.page
    }
  }

  private suspend fun load(
    page: Int,
    onStart: suspend () -> Unit,
    onFinish: () -> Unit,
    onLoad: suspend FPaging.LoadScope<T>.(page: Int) -> List<T>,
  ): Result<List<T>> {
    return _mutator.mutate {
      try {
        onStart().also { currentCoroutineContext().ensureActive() }
        onLoad(page)
          .also { handlePageData(page, it) }
          .let { Result.success(it) }
      } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure<List<T>>(e).also {
          currentCoroutineContext().ensureActive()
          _stateFlow.update {
            it.copy(
              loadPage = page,
              loadResult = Result.failure(e)
            )
          }
        }
      } finally {
        onFinish()
      }
    }
  }

  private suspend fun handlePageData(page: Int, data: List<T>) {
    currentCoroutineContext().ensureActive()
    val totalData = dataHandler.handlePageData(page, data)

    currentCoroutineContext().ensureActive()
    _stateFlow.update { state ->
      state.copy(
        data = totalData ?: state.data,
        loadPage = page,
        loadResult = Result.success(Unit),
        successPage = SuccessPage(page = page, size = data.size),
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