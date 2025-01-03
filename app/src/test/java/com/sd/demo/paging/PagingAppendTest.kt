package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.FPaging
import com.sd.lib.paging.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PagingAppendTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `test append success`() = runTest {
    val paging = FPaging { page ->
      when (page) {
        1 -> listOf(1, 2)
        2 -> listOf(3, 4)
        else -> emptyList()
      }
    }

    // 1
    paging.append().also {
      with(paging.state) {
        assertEquals(listOf(1, 2), data)
        assertEquals(LoadState.NotLoading.Incomplete, appendLoadState)
      }
    }

    // 2
    paging.append().also {
      with(paging.state) {
        assertEquals(listOf(1, 2, 3, 4), data)
        assertEquals(LoadState.NotLoading.Incomplete, appendLoadState)
      }
    }

    // 3
    paging.append().also {
      with(paging.state) {
        assertEquals(listOf(1, 2, 3, 4), data)
        assertEquals(LoadState.NotLoading.Complete, appendLoadState)
      }
    }
  }

  @Test
  fun `test append failure`() = runTest {
    val paging = FPaging<Int> {
      error("append failure")
    }

    paging.append()

    with(paging.state) {
      assertEquals(emptyList<Int>(), data)
      val loadState = appendLoadState as LoadState.Error
      assertEquals("append failure", loadState.error.message)
    }
  }

//  @Test
//  fun `test append when appending`() = runTest {
//    val paging = FPaging<Int>()
//
//    val loadJob = launch {
//      paging.append {
//        delay(5_000)
//        listOf(1, 2)
//      }
//    }.also { runCurrent() }
//
//    runCatching {
//      paging.append { listOf(3, 4) }
//    }.also { result ->
//      assertEquals(true, result.exceptionOrNull()!! is CancellationException)
//    }
//
//    loadJob.join()
//    with(paging.state) {
//      assertEquals(listOf(1, 2), data)
//      assertEquals(LoadState.NotLoading.Complete, appendLoadState)
//    }
//  }
//
//  @Test
//  fun `test append when refreshing`() = runTest {
//    val paging = FPaging<Int>()
//
//    val loadJob = launch {
//      paging.refresh {
//        delay(5_000)
//        listOf(1, 2)
//      }
//    }.also { runCurrent() }
//
//    runCatching {
//      paging.append { listOf(3, 4) }
//    }.also { result ->
//      assertEquals(true, result.exceptionOrNull()!! is CancellationException)
//    }
//
//    loadJob.join()
//    with(paging.state) {
//      assertEquals(listOf(1, 2), data)
//      assertEquals(LoadState.NotLoading.Complete, refreshLoadState)
//    }
//  }
//
//  @Test
//  fun `test append flow`() = runTest {
//    val paging = FPaging<Int>()
//
//    paging.stateFlow.test {
//      with(awaitItem()) {
//        assertEquals(emptyList<Int>(), data)
//        assertEquals(LoadState.NotLoading.Incomplete, refreshLoadState)
//      }
//
//      paging.append { listOf(3, 4) }
//
//      with(awaitItem()) {
//        assertEquals(emptyList<Int>(), data)
//        assertEquals(LoadState.Loading, refreshLoadState)
//      }
//      with(awaitItem()) {
//        assertEquals(listOf(3, 4), data)
//        assertEquals(LoadState.NotLoading.Complete, refreshLoadState)
//      }
//    }
//  }
}