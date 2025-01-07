package com.sd.lib.paging.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sd.lib.paging.LoadState

@Composable
internal fun PagingAppendIfLastIndex(paging: PagingPresenter<*>, index: Int) {
  paging.items.lastIndex.also { lastIndex ->
    if (lastIndex < 0) return
    if (lastIndex != index) return
  }

  val isRefreshReady = with(paging) {
    refreshLoadState == LoadState.NotLoading.Complete
      || refreshLoadState is LoadState.Error
  }

  if (isRefreshReady) {
    LaunchedEffect(paging) {
      if (paging.appendLoadState == LoadState.NotLoading.Incomplete) {
        paging.append()
      }
    }
  }
}