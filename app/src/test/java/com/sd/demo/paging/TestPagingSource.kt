package com.sd.demo.paging

import com.sd.lib.paging.FPaging
import com.sd.lib.paging.KeyIntPagingSource
import com.sd.lib.paging.LoadParams
import com.sd.lib.paging.PagingSource
import kotlinx.coroutines.delay

fun testPaging(
  pagingSource: PagingSource<Int, String> = TestPagingSource(),
): FPaging<String> {
  return FPaging(
    refreshKey = 1,
    pagingSource = pagingSource,
  )
}

class TestPagingSource : KeyIntPagingSource<String>() {
  override suspend fun loadImpl(params: LoadParams<Int>): List<String> {
    delay(5_000)
    return listOf(params.key.toString())
  }
}

class TestErrorPagingSource : KeyIntPagingSource<String>() {
  override suspend fun loadImpl(params: LoadParams<Int>): List<String> {
    delay(5_000)
    error("error")
  }
}