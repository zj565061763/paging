package com.sd.lib.paging

data class PagingState<T>(
  /** 刷新数据的页码，即数据源开始的页码 */
  val refreshPage: Int,

  /** 总数据 */
  val data: List<T> = emptyList(),

  /** 最后一次加载的结果 */
  val result: Result<Unit>? = null,

  /** 最后一次加载成功的页 */
  val successPage: SuccessPage? = null,

  /** 是否正在刷新 */
  val isRefreshing: Boolean = false,

  /** 是否正在加载更多 */
  val isAppending: Boolean = false,
)

data class SuccessPage(
  /** 页码 */
  val page: Int,
  /** 该页的数据个数 */
  val size: Int,
)

/** 是否显示加载数据为空 */
val PagingState<*>.showLoadEmpty: Boolean
  get() = data.isEmpty() && result?.isSuccess == true

/** 是否显示加载数据失败 */
val PagingState<*>.showLoadFailure: Boolean
  get() = data.isEmpty() && result?.isFailure == true

/** 是否显示加载更多没有数据了 */
val PagingState<*>.showAppendNoMoreData: Boolean
  get() = data.isNotEmpty() && successPage?.size == 0

/** 是否显示加载更多失败 */
val PagingState<*>.showAppendFailure: Boolean
  get() = data.isNotEmpty()
    && result?.isFailure == true
    && (successPage != null && successPage.page > refreshPage)