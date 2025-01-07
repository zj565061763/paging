package com.sd.demo.paging

import com.sd.lib.paging.FPaging
import com.sd.lib.paging.KeyIntPagingSource
import com.sd.lib.paging.LoadParams
import kotlinx.coroutines.delay

fun testPaging(): FPaging<String> {
  return FPaging(
    refreshKey = 1,
    pagingSource = TestPagingSource(),
  )
}

fun testPagingError(): FPaging<String> {
  return FPaging(
    refreshKey = 1,
    pagingSource = TestErrorPagingSource(),
  )
}

fun testPagingNoMoreData(noMoreDataPage: Int): FPaging<String> {
  require(noMoreDataPage > 0)
  return FPaging(
    refreshKey = 1,
    pagingSource = TestNoMoreDataPagingSource(noMoreDataPage = noMoreDataPage),
  )
}

private class TestPagingSource : KeyIntPagingSource<String>() {
  override suspend fun loadImpl(params: LoadParams<Int>): List<String> {
    delay(5_000)
    return listOf(params.key.toString())
  }
}

private class TestErrorPagingSource : KeyIntPagingSource<String>() {
  override suspend fun loadImpl(params: LoadParams<Int>): List<String> {
    delay(5_000)
    error("error")
  }
}

private class TestNoMoreDataPagingSource(
  private val noMoreDataPage: Int,
) : KeyIntPagingSource<String>() {
  override suspend fun loadImpl(params: LoadParams<Int>): List<String> {
    delay(5_000)
    return if (params.key >= noMoreDataPage) {
      emptyList()
    } else {
      listOf(params.key.toString())
    }
  }
}