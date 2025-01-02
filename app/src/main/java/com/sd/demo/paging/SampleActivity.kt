package com.sd.demo.paging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.paging.theme.AppTheme
import com.sd.lib.paging.FPaging
import com.sd.lib.paging.PagingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
  val state by vm.stateFlow.collectAsStateWithLifecycle()

  val lazyListState = rememberLazyListState()
  lazyListState.FAppend { vm.append() }

  PullToRefreshBox(
    modifier = modifier.fillMaxSize(),
    isRefreshing = state.isRefreshing,
    onRefresh = { vm.refresh() },
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = lazyListState,
    ) {
      items(state.data) { item ->
        Button(
          modifier = Modifier.fillParentMaxWidth(),
          onClick = {}
        ) {
          Text(text = item)
        }
      }

      if (state.data.isNotEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillParentMaxWidth()
              .heightIn(100.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (state.isAppending) {
              CircularProgressIndicator()
            }
          }
        }
      }
    }
  }
}

internal class SampleViewModel : ViewModel() {
  private val _list = mutableListOf<String>()
  private val _paging: FPaging<String> = FPaging { page, pageData ->
    _list.apply {
      if (page == refreshPage) clear()
      addAll(pageData)
    }.toList()
  }

  val stateFlow: StateFlow<PagingState<String>>
    get() = _paging.stateFlow

  /** 刷新 */
  fun refresh() {
    logMsg { "refresh" }
    viewModelScope.launch {
      _paging.refresh {
        delay(1_000)
        List(10) { (it + 1).toString() }
      }
    }
  }

  /** 加载更多 */
  fun append() {
    logMsg { "append" }
    viewModelScope.launch {
      _paging.append {
        delay(1_000)
        List(10) { (it + 1).toString() }
      }
    }
  }

  init {
    refresh()
  }
}