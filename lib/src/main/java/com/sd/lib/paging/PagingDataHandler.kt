package com.sd.lib.paging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class PagingDataHandler<Key : Any, Value : Any> {
  /**
   * 处理第每页的数据，并返回总数据，如果返回null表示不处理，[handlePageData]不会并发
   */
  abstract suspend fun handlePageData(
    params: LoadParams<Key>,
    result: LoadResult.Page<Key, Value>,
  ): List<Value>?

  companion object {
    fun <Key : Any, Value : Any> default(): PagingDataHandler<Key, Value> {
      return DefaultPagingDataHandler()
    }
  }
}

private class DefaultPagingDataHandler<Key : Any, Value : Any> : PagingDataHandler<Key, Value>() {
  private val _list = mutableListOf<Value>()

  override suspend fun handlePageData(
    params: LoadParams<Key>,
    result: LoadResult.Page<Key, Value>,
  ): List<Value> {
    return withContext(Dispatchers.Default) {
      _list.apply {
        if (params is LoadParams.Refresh) clear()
        addAll(result.data)
      }.toList()
    }
  }
}