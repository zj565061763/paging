package com.sd.demo.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.lib.paging.PagingState
import com.sd.lib.paging.showAppendFailure
import com.sd.lib.paging.showAppendNoMoreData
import kotlinx.coroutines.CoroutineScope

@Composable
fun AppendItem(
  modifier: Modifier = Modifier,
  state: PagingState<*>,
  onLoading: @Composable () -> Unit = {
    CircularProgressIndicator()
  },
  onNoMoreData: @Composable () -> Unit = {
    Text(text = "没有更多数据了")
  },
  onFailure: @Composable () -> Unit = {
    Text(text = "加载失败")
  },
) {
  Box(
    modifier = modifier.heightIn(100.dp),
    contentAlignment = Alignment.Center,
  ) {
    when {
      state.isAppending -> onLoading()
      state.showAppendNoMoreData -> onNoMoreData()
      state.showAppendFailure -> onFailure()
    }
  }
}

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