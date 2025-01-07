package com.sd.lib.paging.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sd.lib.paging.LoadState

@Composable
internal fun <T : Any> AppendIfLastItem(paging: PagingPresenter<T>, item: T) {
  if (item !== paging.items.lastOrNull()) return

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