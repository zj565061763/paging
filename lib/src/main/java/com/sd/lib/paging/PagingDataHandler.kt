package com.sd.lib.paging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PagingDataHandler<Key : Any, Value : Any> {
  /**
   * 处理分页数据并返回总数据，主线程执行
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

open class DefaultPagingDataHandler<Key : Any, Value : Any> : PagingDataHandler<Key, Value> {

  final override suspend fun handlePageData(
    totalData: List<Value>,
    params: LoadParams<Key>,
    pageData: List<Value>,
  ): List<Value> = when (params) {
    is LoadParams.Refresh -> handleRefreshData(totalData, params, pageData)
    is LoadParams.Append -> handleAppendData(totalData, params, pageData)
  }

  /** 处理[LoadParams.Refresh]的数据 */
  protected open suspend fun handleRefreshData(
    totalData: List<Value>,
    params: LoadParams.Refresh<Key>,
    pageData: List<Value>,
  ): List<Value> = pageData

  /** 处理[LoadParams.Append]的数据 */
  protected open suspend fun handleAppendData(
    totalData: List<Value>,
    params: LoadParams.Append<Key>,
    pageData: List<Value>,
  ): List<Value> = withContext(Dispatchers.IO) {
    if (pageData.isNotEmpty()) totalData + pageData else totalData
  }
}