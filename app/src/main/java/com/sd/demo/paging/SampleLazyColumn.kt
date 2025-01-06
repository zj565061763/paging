package com.sd.demo.paging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sd.demo.paging.theme.AppTheme
import com.sd.lib.paging.FPaging
import com.sd.lib.paging.compose.PagingPresenter
import com.sd.lib.paging.compose.UiRefreshSlot
import com.sd.lib.paging.compose.pagingItemAppend
import com.sd.lib.paging.compose.pagingItems
import com.sd.lib.paging.compose.presenter
import com.sd.lib.paging.modifier
import com.sd.lib.paging.replaceAll
import kotlinx.coroutines.launch

class SampleLazyColumn : ComponentActivity() {
  private val _paging = FPaging(
    refreshKey = 1,
    pagingSource = StringPagingSource(),
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Content(
          paging = _paging.presenter(),
          onClickItem = { modifyItem(it) },
        )
      }
    }
  }

  private fun modifyItem(item: String) {
    lifecycleScope.launch {
      _paging.modifier().replaceAll(oldItem = item, newItem = "modify")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
  modifier: Modifier = Modifier,
  paging: PagingPresenter<String>,
  onClickItem: (String) -> Unit,
) {
  LaunchedEffect(paging) {
    paging.refresh()
  }

  PullToRefreshBox(
    modifier = modifier.fillMaxSize(),
    isRefreshing = paging.isRefreshing,
    onRefresh = { paging.refresh() },
    contentAlignment = Alignment.Center,
  ) {
    LazyView(
      modifier = Modifier.fillMaxSize(),
      paging = paging,
      onClickItem = onClickItem,
    )
    paging.UiRefreshSlot(
      stateError = { Text(text = "加载失败：$it") },
      stateEmpty = { Text(text = "暂无数据") },
    )
  }
}

@Composable
private fun LazyView(
  modifier: Modifier = Modifier,
  paging: PagingPresenter<String>,
  onClickItem: (String) -> Unit,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(8.dp),
  ) {
    pagingItems(paging) { item ->
      ItemView(
        text = item,
        modifier = Modifier.clickable { onClickItem(item) },
      )
    }
    pagingItemAppend(paging)
  }
}