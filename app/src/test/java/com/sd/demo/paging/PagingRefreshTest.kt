package com.sd.demo.paging

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagingRefreshTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `test refresh success`() = runTest {
    val paging = testPaging()

    paging.stateFlow.test {
      paging.refresh()

      // initial
      awaitItem().testInitial()

      // refreshing
      awaitItem().testInitialRefreshing()

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }
    }
  }

  @Test
  fun `test refresh error`() = runTest {
    val paging = testPagingError(errorPage = 1)
    paging.stateFlow.test {
      paging.refresh()

      // initial
      awaitItem().testInitial()

      // refreshing
      awaitItem().testInitialRefreshing()

      // refresh error
      with(awaitItem()) {
        testItemsEmpty()
        refreshLoadState.testError("error")
        appendLoadState.testInComplete()
      }
    }
  }

  @Test
  fun `test refresh no more data`() = runTest {
    val paging = testPagingNoMoreData(noMoreDataPage = 1)
    paging.stateFlow.test {
      paging.refresh()

      // initial
      awaitItem().testInitial()

      // refreshing
      awaitItem().testInitialRefreshing()

      // refresh success
      with(awaitItem()) {
        testItemsEmpty()
        refreshLoadState.testComplete()
        appendLoadState.testComplete()
      }
    }
  }

  @Test
  fun `test refresh when refreshing`() = runTest {
    val paging = testPaging()

    paging.stateFlow.test {
      launch { paging.refresh() }.also { runCurrent() }
      paging.refresh()

      // initial
      awaitItem().testInitial()

      // refreshing
      awaitItem().testInitialRefreshing()

      // initial
      awaitItem().testInitial()

      // refreshing
      awaitItem().testInitialRefreshing()

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }
    }
  }

  @Test
  fun `test refresh when appending`() = runTest {
    val paging = testPaging()

    paging.stateFlow.test {
      paging.refresh()
      launch { paging.append() }.also { runCurrent() }
      paging.refresh()

      // initial
      awaitItem().testInitial()
      // refreshing
      awaitItem().testInitialRefreshing()
      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }

      // appending
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testLoading()
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        refreshLoadState.testComplete()
        appendLoadState.testInComplete()
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
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