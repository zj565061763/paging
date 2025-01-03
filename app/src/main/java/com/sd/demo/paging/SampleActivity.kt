package com.sd.demo.paging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.paging.theme.AppTheme
import com.sd.lib.paging.LoadState

class SampleActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Content()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
  modifier: Modifier = Modifier,
  vm: SampleViewModel = viewModel(),
) {
  val pagingState by vm.stateFlow.collectAsStateWithLifecycle()

  val lazyListState = rememberLazyListState()
  lazyListState.FAppend { vm.append() }

  PullToRefreshBox(
    modifier = modifier.fillMaxSize(),
    isRefreshing = pagingState.refreshLoadState is LoadState.Loading,
    onRefresh = { vm.refresh() },
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = lazyListState,
    ) {
      items(pagingState.data) { item ->
        Button(
          modifier = Modifier.fillParentMaxWidth(),
          onClick = {}
        ) {
          Text(text = item)
        }
      }

      if (pagingState.data.isNotEmpty()) {
        item {
          AppendItem(
            modifier = Modifier.fillParentMaxWidth(),
            state = pagingState,
          )
        }
      }
    }
  }
}