package com.sd.lib.paging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 分页数据处理
 */
abstract class PagingDataHandler<T> {
  internal lateinit var getPagingState: () -> PagingState<T>

  protected val pagingState: PagingState<T>
    get() = getPagingState()

  /**
   * 处理第[page]页数据[data]并返回总数据，如果返回null则总数据不变。
   * 此方法总是串行，不会并发。
   */
  abstract suspend fun handlePageData(page: Int, data: List<T>): List<T>?
}

class DefaultPagingDataHandler<T> : PagingDataHandler<T>() {
  private val _list = mutableListOf<T>()

  override suspend fun handlePageData(page: Int, data: List<T>): List<T> {
    return withContext(Dispatchers.Default) {
      _list.apply {
        if (page == pagingState.refreshPage) clear()
        addAll(data)
      }.toList()
    }
  }
}