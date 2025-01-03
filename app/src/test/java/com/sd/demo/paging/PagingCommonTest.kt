package com.sd.demo.paging

import com.sd.lib.paging.FPaging
import org.junit.Assert.assertEquals
import org.junit.Test

class PagingCommonTest {
  @Test
  fun `test default state`() {
    val paging = FPaging<Int>()
    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      assertEquals(1, refreshPage)
      assertEquals(null, loadResult)
      assertEquals(null, loadPage)
      assertEquals(null, loadSize)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }
}