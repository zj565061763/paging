package com.sd.demo.paging

import com.sd.lib.paging.LoadState
import com.sd.lib.paging.PagingState
import org.junit.Assert.assertEquals

fun <T : Any> PagingState<T>.testInitial() {
  testItemsEmpty()
  refreshLoadState.testInComplete()
  appendLoadState.testInComplete()
}

fun <T : Any> PagingState<T>.testInitialRefreshing() {
  testItemsEmpty()
  refreshLoadState.testLoading()
  appendLoadState.testInComplete()
}

fun PagingState<*>.testItemsEmpty() = assertEquals(true, items.isEmpty())
fun LoadState.testLoading() = assertEquals(LoadState.Loading, this)
fun LoadState.testError(message: String) = assertEquals(message, (this as LoadState.Error).error.message)
fun LoadState.testComplete() = assertEquals(true, (this as LoadState.NotLoading).endOfPaginationReached)
fun LoadState.testInComplete() = assertEquals(false, (this as LoadState.NotLoading).endOfPaginationReached)