package com.sd.demo.paging

import com.sd.lib.paging.FPaging
import org.junit.Assert.assertEquals
import org.junit.Test

class PagingCommonTest {
  @Test
  fun `test default state`() {
    val paging = FPaging<Int>()
    with(paging.state) {
      assertEquals(1, refreshPage)
      assertEquals(emptyList<Int>(), data)
      assertEquals(null, result)
      assertEquals(null, successPage)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }
}