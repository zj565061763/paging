package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.FPaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class PagingAppendTest {
  @Test
  fun `test append success`() = runTest {
    val paging = FPaging<Int>()

    // 1
    paging.append { page ->
      assertEquals(1, page)
      assertEquals(false, paging.state.isRefreshing)
      assertEquals(true, paging.state.isAppending)
      listOf(1, 2)
    }.also { append ->
      assertEquals(listOf(1, 2), append.getOrThrow())
      with(paging.state) {
        assertEquals(listOf(1, 2), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(refreshPage, successPage?.page)
        assertEquals(2, successPage?.size)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }
    }

    // 2
    paging.append { page ->
      assertEquals(2, page)
      listOf(3, 4)
    }.also { append ->
      assertEquals(listOf(3, 4), append.getOrThrow())
      with(paging.state) {
        assertEquals(listOf(1, 2, 3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(2, successPage?.page)
        assertEquals(2, successPage?.size)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }
    }

    // 3 空数据
    paging.append { page ->
      assertEquals(3, page)
      emptyList()
    }.also { append ->
      assertEquals(emptyList<Int>(), append.getOrThrow())
      with(paging.state) {
        assertEquals(listOf(1, 2, 3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(3, successPage?.page)
        assertEquals(0, successPage?.size)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }
    }

    // 4
    paging.append { page ->
      // 由于上一次加载数据为空，所以本次page和上一次一样
      assertEquals(3, page)
      emptyList()
    }.also { append ->
      assertEquals(emptyList<Int>(), append.getOrThrow())
      with(paging.state) {
        assertEquals(listOf(1, 2, 3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(3, successPage?.page)
        assertEquals(0, successPage?.size)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }
    }
  }

  @Test
  fun `test append failure`() = runTest {
    val paging = FPaging<Int>()

    paging.append {
      error("append failure")
    }.also { append ->
      assertEquals("append failure", append.exceptionOrNull()!!.message)
    }

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      assertEquals("append failure", loadResult!!.exceptionOrNull()!!.message)
      assertEquals(null, successPage)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test append cancel`() = runTest {
    val paging = FPaging<Int>()

    launch {
      paging.append {
        delay(5_000)
        listOf(1, 2)
      }
    }.also { runCurrent() }

    paging.cancelLoad()

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      assertEquals(null, loadResult)
      assertEquals(null, successPage)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test append when appending`() = runTest {
    val paging = FPaging<Int>()

    val loadJob = launch {
      paging.append {
        delay(5_000)
        listOf(1, 2)
      }
    }.also { runCurrent() }

    try {
      paging.append { listOf(3, 4) }
    } catch (e: CancellationException) {
      Result.failure(e)
    }.also { append ->
      assertEquals(true, append.exceptionOrNull()!! is CancellationException)
    }

    loadJob.join()
    with(paging.state) {
      assertEquals(listOf(1, 2), data)
      assertEquals(true, loadResult?.isSuccess)
      assertEquals(refreshPage, successPage?.page)
      assertEquals(2, successPage?.size)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test append when refreshing`() = runTest {
    val paging = FPaging<Int>()

    val loadJob = launch {
      paging.refresh {
        delay(5_000)
        listOf(1, 2)
      }
    }.also { runCurrent() }

    try {
      paging.append { listOf(3, 4) }
    } catch (e: CancellationException) {
      Result.failure(e)
    }.also { append ->
      assertEquals(true, append.exceptionOrNull()!! is CancellationException)
    }

    loadJob.join()
    with(paging.state) {
      assertEquals(listOf(1, 2), data)
      assertEquals(true, loadResult?.isSuccess)
      assertEquals(refreshPage, successPage?.page)
      assertEquals(2, successPage?.size)
      assertEquals(false, isRefreshing)
      assertEquals(false, isAppending)
    }
  }

  @Test
  fun `test append notify loading`() = runTest {
    val paging = FPaging<Int>()
    assertEquals(false, paging.state.isAppending)

    launch {
      paging.append(notifyLoading = false) {
        delay(5_000)
        listOf(1, 2)
      }
    }

    runCurrent()
    assertEquals(false, paging.state.isAppending)

    advanceUntilIdle()
    assertEquals(false, paging.state.isAppending)
  }

  @Test
  fun `test append flow`() = runTest {
    val paging = FPaging<Int>()

    paging.stateFlow.test {
      with(awaitItem()) {
        assertEquals(emptyList<Int>(), data)
        assertEquals(null, loadResult)
        assertEquals(null, successPage)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }

      paging.append { listOf(3, 4) }

      with(awaitItem()) {
        assertEquals(emptyList<Int>(), data)
        assertEquals(null, loadResult)
        assertEquals(null, successPage)
        assertEquals(false, isRefreshing)
        assertEquals(true, isAppending)
      }
      with(awaitItem()) {
        assertEquals(listOf(3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(refreshPage, successPage?.page)
        assertEquals(2, successPage?.size)
        assertEquals(false, isRefreshing)
        assertEquals(true, isAppending)
      }
      with(awaitItem()) {
        assertEquals(listOf(3, 4), data)
        assertEquals(true, loadResult?.isSuccess)
        assertEquals(refreshPage, successPage?.page)
        assertEquals(2, successPage?.size)
        assertEquals(false, isRefreshing)
        assertEquals(false, isAppending)
      }
    }
  }
}