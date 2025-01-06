package com.sd.lib.paging

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PagingDataHandler<Key : Any, Value : Any> {
  /**
   * 处理分页数据并返回总数据，主线程触发
   *
   * @param totalData 总数据
   * @param params 分页加载参数
   * @param pageData 分页加载数据
   */
  suspend fun handlePageData(
    totalData: List<Value>,
    params: LoadParams<Key>,
    pageData: List<Value>,
  ): List<Value>
}

open class DefaultPagingDataHandler<Key : Any, Value : Any>(
  private val refreshDispatcher: CoroutineDispatcher? = null,
  private val appendDispatcher: CoroutineDispatcher? = Dispatchers.IO,
) : PagingDataHandler<Key, Value> {

  final override suspend fun handlePageData(
    totalData: List<Value>,
    params: LoadParams<Key>,
    pageData: List<Value>,
  ): List<Value> = when (params) {
    is LoadParams.Refresh -> {
      if (refreshDispatcher != null) {
        withContext(refreshDispatcher) { refreshData(totalData, params, pageData) }
      } else {
        refreshData(totalData, params, pageData)
      }
    }
    is LoadParams.Append -> {
      if (appendDispatcher != null) {
        withContext(appendDispatcher) { appendData(totalData, params, pageData) }
      } else {
        appendData(totalData, params, pageData)
      }
    }
  }

  /** 刷新数据，在[refreshDispatcher]上执行 */
  protected open suspend fun refreshData(
    totalData: List<Value>,
    params: LoadParams<Key>,
    pageData: List<Value>,
  ): List<Value> = pageData

  /** 添加尾部数据，在[appendDispatcher]上执行 */
  protected open suspend fun appendData(
    totalData: List<Value>,
    params: LoadParams<Key>,
    pageData: List<Value>,
  ): List<Value> = if (pageData.isNotEmpty()) totalData + pageData else totalData
}