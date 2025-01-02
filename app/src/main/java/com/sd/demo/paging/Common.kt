package com.sd.demo.paging

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope

@Composable
fun LazyListState.FAppend(
  append: suspend CoroutineScope.() -> Unit,
) {
  if (reachedAppend()) {
    val appendUpdated by rememberUpdatedState(append)
    LaunchedEffect(Unit) {
      appendUpdated()
    }
  }
}

@Composable
private fun LazyListState.reachedAppend(): Boolean {
  return remember(this) {
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