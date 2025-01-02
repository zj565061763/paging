package com.sd.lib.paging

data class PagingState<T>(
  /** 总数据 */
  val data: List<T> = emptyList(),
  /** 刷新数据的页码，例如数据源页码从1开始，那么[refreshPage]就为1 */
  val refreshPage: Int = 1,

  /** 最后一次加载的结果 */
  val loadResult: Result<Unit>? = null,
  /** 最后一次加载的页码 */
  val loadPage: Int? = null,
  /** 最后一次加载的数据个数 */
  val loadSize: Int? = null,

  /** 是否正在刷新 */
  val isRefreshing: Boolean = false,
  /** 是否正在加载更多 */
  val isAppending: Boolean = false,
)

/** 是否显示加载更多没有数据了 */
val PagingState<*>.showAppendNoMoreData: Boolean
  get() = data.isNotEmpty() && loadSize == 0

/** 是否显示加载更多失败 */
val PagingState<*>.showAppendFailure: Boolean
  get() = data.isNotEmpty()
    && (loadPage != null && loadPage > refreshPage)
    && loadResult?.isFailure == true

/** 是否显示加载数据为空 */
val PagingState<*>.showLoadEmpty: Boolean
  get() = data.isEmpty() && loadResult?.isSuccess == true

/** 是否显示加载数据失败 */
val PagingState<*>.showLoadFailure: Boolean
  get() = data.isEmpty() && loadResult?.isFailure == true