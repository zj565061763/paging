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
  val pagingState by vm.stateFlow.collectAsStateWithLifecycle()

  val lazyListState = rememberLazyListState()
  lazyListState.FAppend { vm.append() }

  PullToRefreshBox(
    modifier = modifier.fillMaxSize(),
    isRefreshing = pagingState.isRefreshing,
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
    viewModelScope.launch {
      _paging.refresh { page ->
        logMsg { "append $page" }
        delay(1_000)
        List(10) { (it + 1).toString() }
      }
    }
  }

  /** 加载更多 */
  fun append() {

    viewModelScope.launch {
      _paging.append { page ->
        logMsg { "append $page" }
        delay(1_000)
        when (page) {
          4 -> loadOrThrow()
          5 -> emptyList()
          else -> List(10) { (it + 1).toString() }
        }
      }
    }
  }

  init {
    refresh()
  }
}

private fun loadOrThrow(): List<String> {
  return if (listOf(true, false).random()) {
    List(10) { (it + 1).toString() }
  } else {
    error("load error")
  }
}