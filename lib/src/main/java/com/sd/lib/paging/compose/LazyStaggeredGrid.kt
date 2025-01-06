package com.sd.lib.paging.compose

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable

fun LazyStaggeredGridScope.pagingItemAppend(
  paging: PagingPresenter<*>,
  key: Any? = "paging append ui state",
  contentType: Any? = "paging append ui state",
  span: StaggeredGridItemSpan? = StaggeredGridItemSpan.FullLine,
  content: @Composable LazyStaggeredGridItemScope.() -> Unit = { paging.UiAppend() },
) {
  if (paging.isEmpty) return
  item(
    key = key,
    contentType = contentType,
    span = span,
    content = content,
  )
}

fun <T : Any> LazyStaggeredGridScope.pagingItems(
  paging: PagingPresenter<T>,
  key: ((item: T) -> Any)? = null,
  contentType: (item: T) -> Any? = { null },
  span: ((item: T) -> StaggeredGridItemSpan)? = null,
  itemContent: @Composable LazyStaggeredGridItemScope.(item: T) -> Unit,
) {
  items(
    items = paging.items,
    key = key,
    contentType = contentType,
    span = span,
  ) { item ->
    itemContent(item)
    AppendIfLastItem(paging, item)
  }
}

fun <T : Any> LazyStaggeredGridScope.pagingItemsIndexed(
  paging: PagingPresenter<T>,
  key: ((index: Int, item: T) -> Any)? = null,
  contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
  span: ((index: Int, item: T) -> StaggeredGridItemSpan)? = null,
  itemContent: @Composable LazyStaggeredGridItemScope.(index: Int, item: T) -> Unit,
) {
  itemsIndexed(
    items = paging.items,
    key = key,
    contentType = contentType,
    span = span,
  ) { index, item ->
    itemContent(index, item)
    AppendIfLastItem(paging, item)
  }
}