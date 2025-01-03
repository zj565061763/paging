package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.FPaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class PagingRefreshTest {
  @Test
  fun `test refresh success`() = runTest {
    val paging = FPaging<Int>()

    paging.refresh { page ->
      assertEquals(refreshPage, page)
      assertEquals(refreshPage, 1)
      assertEquals(true, paging.state.isRefreshing)
      assertEquals(false, paging.state.isAppending)
      listOf(1, 2)
    }.also { result ->
      assertEquals(listOf(1, 2), result.getOrThrow())
    }

    with(paging.state) {
      assertEquals(listOf(1, 2), data)
      assertEquals(true, loadResult?.isSuccess)
      assertEquals(refreshPage, loadPage)
      assertEquals(2, loadSize)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test refresh failure`() = runTest {
    val paging = FPaging<Int>()

    paging.refresh {
      error("refresh failure")
    }.also { result ->
      assertEquals("refresh failure", result.exceptionOrNull()!!.message)
    }

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      assertEquals("refresh failure", loadResult!!.exceptionOrNull()!!.message)
      assertEquals(null, loadPage)
      assertEquals(null, loadSize)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test refresh cancel`() = runTest {
    val paging = FPaging<Int>()

    launch {
      paging.refresh {
        delay(5_000)
        listOf(1, 2)
      }
    }.also { runCurrent() }

    paging.cancelRefresh()

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      assertEquals(null, loadResult)
      assertEquals(null, loadPage)
      assertEquals(null, loadSize)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test refresh when refreshing`() = runTest {
    val paging = FPaging<Int>()

    launch {
      paging.refresh {
        delay(5_000)
        listOf(1, 2)
      }
    }.also { runCurrent() }

    paging.refresh { listOf(3, 4) }

    with(paging.state) {
      assertEquals(listOf(3, 4), data)
      assertEquals(true, loadResult?.isSuccess)
      assertEquals(refreshPage, loadPage)
      assertEquals(2, loadSize)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test refresh when appending`() = runTest {
    val paging = FPaging<Int>()

    launch {
      paging.append {
        delay(5_000)
        listOf(1, 2)
      }
    }.also { runCurrent() }

    paging.refresh { listOf(3, 4) }

    with(paging.state) {
      assertEquals(listOf(3, 4), data)
      assertEquals(true, loadResult?.isSuccess)
      assertEquals(refreshPage, loadPage)
      assertEquals(2, loadSize)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test refresh notify loading`() = runTest {
    val paging = FPaging<Int>()
    assertEquals(false, paging.state.isRefreshing)

    launch {
      paging.refresh(notifyLoading = false) {
        delay(5_000)
        listOf(1, 2)
      }
    }

    runCurrent()
    assertEquals(false, paging.state.isRefreshing)

    advanceUntilIdle()
    assertEquals(false, paging.state.isRefreshing)
  }

  @Test
  fun `test refresh flow`() = runTest {
    val paging = FPaging<Int>()

    paging.stateFlow.test {
      with(awaitItem()) {
        assertEquals(emptyList<Int>(), data)
        assertEquals(null, loadResult)
        assertEquals(null, loadPage)
        assertEquals(null, loadSize)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }

      paging.refresh { listOf(3, 4) }

      with(awaitItem()) {
        assertEquals(emptyList<Int>(), data)
        assertEquals(null, loadResult)
        assertEquals(null, loadPage)
        assertEquals(null, loadSize)
        assertEquals(true, isRefreshing)
        assertEquals(false, isAppending)
      }
      with(awaitItem()) {
        assertEquals(listOf(3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(refreshPage, loadPage)
        assertEquals(2, loadSize)
        assertEquals(true, isRefreshing)
        assertEquals(false, isAppending)
      }
      with(awaitItem()) {
        assertEquals(listOf(3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(refreshPage, loadPage)
        assertEquals(2, loadSize)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }
    }
  }
}