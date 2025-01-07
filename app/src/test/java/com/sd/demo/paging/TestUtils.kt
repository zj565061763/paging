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

fun testErrorPaging(): FPaging<String> {
  return FPaging(
    refreshKey = 1,
    pagingSource = TestErrorPagingSource(),
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