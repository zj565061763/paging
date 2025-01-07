package com.sd.demo.paging

import app.cash.turbine.test
import com.sd.lib.paging.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      with(awaitItem()) {
        assertEquals(listOf("1"), items)
        assertEquals(true, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
    }
  }

  @Test
  fun `test refresh error`() = runTest {
    val paging = testPaging(pagingSource = TestErrorPagingSource())
    paging.stateFlow.test {
      paging.refresh()

      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(false, (refreshLoadState as LoadState.NotLoading).endOfPaginationReached)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals(LoadState.Loading, refreshLoadState)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }

      with(awaitItem()) {
        assertEquals(emptyList<String>(), items)
        assertEquals("error", (refreshLoadState as LoadState.Error).error.message)
        assertEquals(false, (appendLoadState as LoadState.NotLoading).endOfPaginationReached)
      }
    }
  }
}