package com.sd.demo.paging

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PagingInModifyBlockTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `test modify`() = runTest {
    val paging = testPaging()
    paging.modify { listOf("a") }
    assertEquals(listOf("a"), paging.state.items)
  }

  @Test
  fun `test modify in modify block`() = runTest {
    val paging = testPaging()
    paging.modify {
      runCatching {
        paging.modify { listOf("b") }
      }.also {
        assertEquals("Can not call modify in the modify block.", it.exceptionOrNull()!!.message)
      }
      listOf("a")
    }
    assertEquals(listOf("a"), paging.state.items)
  }

  @Test
  fun `test refresh in modify block`() = runTest {
    val paging = testPaging()
    paging.modify {
      runCatching {
        paging.refresh()
      }.also {
        assertEquals("Can not call refresh in the modify block.", it.exceptionOrNull()!!.message)
      }
      listOf("a")
    }
    assertEquals(listOf("a"), paging.state.items)
  }

  @Test
  fun `test append in modify block`() = runTest {
    val paging = testPaging()
    paging.modify {
      runCatching {
        paging.append()
      }.also {
        assertEquals("Can not call append in the modify block.", it.exceptionOrNull()!!.message)
      }
      listOf("a")
    }
    assertEquals(listOf("a"), paging.state.items)
  }
}