package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.FPaging
import com.sd.lib.paging.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PagingRefreshTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `test refresh success`() = runTest {
    val paging = FPaging<Int>()

    paging.refresh { page ->
      assertEquals(1, page)
      listOf(1, 2)
    }

    with(paging.state) {
      assertEquals(listOf(1, 2), data)
      assertEquals(LoadState.NotLoading.Complete, refreshLoadState)
    }
  }

  @Test
  fun `test refresh failure`() = runTest {
    val paging = FPaging<Int>()

    paging.refresh {
      error("refresh failure")
    }

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      val loadState = refreshLoadState as LoadState.Error
      assertEquals("refresh failure", loadState.error.message)
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

    paging.cancelLoad()

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      assertEquals(LoadState.NotLoading.Incomplete, refreshLoadState)
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
      assertEquals(LoadState.NotLoading.Complete, refreshLoadState)
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
      assertEquals(LoadState.NotLoading.Complete, refreshLoadState)
    }
  }

  @Test
  fun `test refresh flow`() = runTest {
    val paging = FPaging<Int>()

    paging.stateFlow.test {
      with(awaitItem()) {
        assertEquals(emptyList<Int>(), data)
        assertEquals(LoadState.NotLoading.Incomplete, refreshLoadState)
      }

      paging.refresh { listOf(3, 4) }

      with(awaitItem()) {
        assertEquals(emptyList<Int>(), data)
        assertEquals(LoadState.Loading, refreshLoadState)
      }
      with(awaitItem()) {
        assertEquals(listOf(3, 4), data)
        assertEquals(LoadState.NotLoading.Complete, refreshLoadState)
      }
    }
  }
}