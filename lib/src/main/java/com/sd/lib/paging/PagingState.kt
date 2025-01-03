package com.sd.lib.paging

data class PagingState<T>(
  /** 刷新数据的页码，即数据源开始的页码 */
  val refreshPage: Int,

  /** 总数据 */
  val data: List<T> = emptyList(),

  /** 刷新状态 */
  val refreshLoadState: LoadState = LoadState.NotLoading.Incomplete,

  /** 加载更多状态 */
  val appendLoadState: LoadState = LoadState.NotLoading.Incomplete,
)

/** 是否显示加载数据为空 */
val PagingState<*>.showLoadEmpty: Boolean
  get() = data.isEmpty()
    && refreshLoadState is LoadState.NotLoading && refreshLoadState.endOfPaginationReached

/** 是否显示加载数据失败 */
val PagingState<*>.showLoadFailure: Boolean
  get() = data.isEmpty() && refreshLoadState is LoadState.Error