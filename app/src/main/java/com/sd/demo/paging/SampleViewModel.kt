package com.sd.demo.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.lib.paging.FPaging
import com.sd.lib.paging.PagingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SampleViewModel : ViewModel() {
  private val _paging: FPaging<String> = FPaging()

  val stateFlow: StateFlow<PagingState<String>>
    get() = _paging.stateFlow

  /** 刷新 */
  fun refresh() {
    viewModelScope.launch {
      _paging.refresh { page ->
        logMsg { "refresh $page" }
        delay(1_000)
        newList()
      }
    }
  }

  /** 加载更多 */
  fun append() {
    viewModelScope.launch {
      _paging.append { page ->
        logMsg { "append $page" }
        delay(1_000)
        when (page) {
          3 -> loadOrThrow()
          4 -> emptyList()
          else -> newList()
        }
      }
    }
  }

  init {
    refresh()
  }
}

private fun loadOrThrow(): List<String> {
  return if (listOf(true, false).random()) {
    newList()
  } else {
    error("load error")
  }
}

private fun newList(): List<String> {
  return List(20) { (it + 1).toString() }
}