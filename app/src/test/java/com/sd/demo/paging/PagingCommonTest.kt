package com.sd.demo.paging

import com.sd.lib.paging.FPaging
import com.sd.lib.paging.LoadState
import org.junit.Assert.assertEquals
import org.junit.Test

class PagingCommonTest {
  @Test
  fun `test default state`() {
    val paging = FPaging { listOf(1) }
    with(paging.state) {
      assertEquals(1, refreshPage)
      assertEquals(emptyList<Int>(), data)
      assertEquals(LoadState.NotLoading.Incomplete, refreshLoadState)
      assertEquals(LoadState.NotLoading.Incomplete, appendLoadState)
    }
  }
}