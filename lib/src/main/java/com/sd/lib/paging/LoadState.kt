package com.sd.lib.paging

sealed interface LoadState {
  data class NotLoading(
    val endOfPaginationReached: Boolean,
  ) : LoadState {
    companion object {
      val Incomplete = NotLoading(endOfPaginationReached = false)
      val Complete = NotLoading(endOfPaginationReached = true)
    }
  }

  data object Loading : LoadState

  data class Error(
    val error: Throwable,
  ) : LoadState
}