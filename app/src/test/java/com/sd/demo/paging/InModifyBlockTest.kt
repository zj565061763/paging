package com.sd.demo.paging

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InModifyBlockTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `test modify in modify block`() = runTest {
    val paging = testPaging()
    paging.modify {
      runCatching {
        paging.modify { emptyList() }
      }.also {
        assertEquals("Can not call modify in the modify block.", it.exceptionOrNull()!!.message)
      }
      emptyList()
    }
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
      emptyList()
    }
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
      emptyList()
    }
  }
}