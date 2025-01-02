package com.sd.demo.paging

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
internal fun LazyListState.reachedBottom(): Boolean {
  return remember {
    derivedStateOf {
      val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
      if (lastVisibleItem == null) {
        false
      } else {
        lastVisibleItem.index != 0 && lastVisibleItem.index == layoutInfo.totalItemsCount - 1
      }
    }
  }.value
}