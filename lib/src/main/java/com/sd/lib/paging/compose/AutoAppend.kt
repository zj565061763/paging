package com.sd.lib.paging.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sd.lib.paging.LoadState

@Composable
internal fun <T : Any> AppendIfLastItem(paging: PagingPresenter<T>, item: T) {
  if (item !== paging.items.lastOrNull()) return
  when (paging.refreshLoadState) {
    LoadState.NotLoading.Complete -> AppendIfInComplete(paging)
    is LoadState.Error -> AppendIfInComplete(paging)
    else -> {}
  }
}

@Composable
private fun AppendIfInComplete(paging: PagingPresenter<*>) {
  LaunchedEffect(paging) {
    if (paging.appendLoadState == LoadState.NotLoading.Incomplete) {
      paging.append()
    }
  }
}