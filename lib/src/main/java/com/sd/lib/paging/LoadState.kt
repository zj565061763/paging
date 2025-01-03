package com.sd.lib.paging

sealed interface LoadState {
  data class NotLoading(
    /** 是否已到达末页 */
    val endOfPaginationReached: Boolean,
  ) : LoadState {
    companion object {
      /** 未到达末页 */
      val Incomplete = NotLoading(endOfPaginationReached = false)
      /** 已到达末页 */
      val Complete = NotLoading(endOfPaginationReached = true)
    }
  }

  /** 加载中 */
  data object Loading : LoadState

  /** 加载失败 */
  data class Error(
    val error: Throwable,
  ) : LoadState
}