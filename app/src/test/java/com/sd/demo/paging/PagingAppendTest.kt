package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagingAppendTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `test append success`() = runTest {
    val paging = testPaging()
    paging.refresh()

    paging.stateFlow.test {
      // initial
      awaitItem().testInitialRefreshSuccess()

      paging.append()

      // appending
      awaitItem().testInitialAppending()

      // append success
      with(awaitItem()) {
        assertEquals(listOf("1", "2"), items)
        appendLoadState.testInComplete()
        refreshLoadState.testComplete()
      }
    }
  }

  @Test
  fun `test append error`() = runTest {
    val paging = testPagingError(errorPage = 2)
    paging.refresh()

    paging.stateFlow.test {
      // initial
      awaitItem().testInitialRefreshSuccess()

      paging.append()

      // appending
      awaitItem().testInitialAppending()

      // append error
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        appendLoadState.testError("error")
        refreshLoadState.testComplete()
      }
    }
  }

  @Test
  fun `test append no more data`() = runTest {
    val paging = testPagingNoMoreData(noMoreDataPage = 2)
    paging.refresh()

    paging.stateFlow.test {
      // initial
      awaitItem().testInitialRefreshSuccess()

      paging.append()

      // appending
      awaitItem().testInitialAppending()

      // append success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        appendLoadState.testComplete()
        refreshLoadState.testComplete()
      }
    }
  }

  @Test
  fun `test append when appending`() = runTest {
    val paging = testPaging()
    paging.refresh()

    paging.stateFlow.test {
      // initial
      awaitItem().testInitialRefreshSuccess()

      launch { paging.append() }.also { runCurrent() }
      runCatching { paging.append() }.also {
        assertEquals(true, it.exceptionOrNull() is CancellationException)
      }

      // appending
      awaitItem().testInitialAppending()

      // append success
      with(awaitItem()) {
        assertEquals(listOf("1", "2"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }
    }
  }

  @Test
  fun `test append when refreshing`() = runTest {
    val paging = testPaging()
    launch { paging.refresh() }.also { runCurrent() }

    paging.stateFlow.test {
      runCatching { paging.append() }.also {
        assertEquals(true, it.exceptionOrNull() is CancellationException)
      }

      // refreshing
      with(awaitItem()) {
        testItemsEmpty()
        refreshLoadState.testLoading()
        appendLoadState.testInComplete()
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }
    }
  }

  @Test
  fun `test append to refresh`() = runTest {
    val paging = testPaging()

    paging.stateFlow.test {
      paging.append()

      // initial
      with(awaitItem()) {
        testItemsEmpty()
        refreshLoadState.testInComplete()
        appendLoadState.testInComplete()
      }

      // refreshing
      with(awaitItem()) {
        testItemsEmpty()
        refreshLoadState.testLoading()
        appendLoadState.testInComplete()
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }
    }
  }
}

fun <T : Any> PagingState<T>.testInitialRefreshSuccess() {
  assertEquals(listOf("1"), items)
  refreshLoadState.testComplete()
  appendLoadState.testInComplete()
}

fun <T : Any> PagingState<T>.testInitialAppending() {
  assertEquals(listOf("1"), items)
  appendLoadState.testLoading()
  refreshLoadState.testComplete()
}