package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.LoadState
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
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
    }
  }

  @Test
  fun `test refresh error`() = runTest {
    val paging = testPagingError(errorPage = 1)
    paging.stateFlow.test {
      paging.refresh()

      // initial
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refresh error
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals("error", (refreshLoadState as LoadState.Error).error.message)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
    }
  }

  @Test
  fun `test refresh no more data`() = runTest {
    val paging = testPagingNoMoreData(noMoreDataPage = 1)
    paging.stateFlow.test {
      paging.refresh()

      // initial
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(true, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
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
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // initial
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
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
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
      // refreshing
      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // appending
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(LoadState.Loading, appendLoadState)
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refreshing
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      // refresh success
      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
    }
  }
}