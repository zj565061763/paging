package com.sd.lib.paging

abstract class IntPagingSource<Value : Any> : PagingSource<Int, Value>() {
  final override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
    val key = params.key
    val data = loadImpl(key) ?: return LoadResult.None()
    return LoadResult.Page(
      data = data,
      nextKey = if (data.isEmpty()) null else key + 1,
    )
  }

  protected abstract suspend fun loadImpl(key: Int): List<Value>?
}

abstract class PagingSource<Key : Any, Value : Any> {
  /**
   * 根据参数[params]加载数据，并返回结果[LoadResult]
   */
  abstract suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value>
}

/** 加载参数 */
sealed interface LoadParams<Key : Any> {
  val key: Key

  data class Refresh<Key : Any>(
    override val key: Key,
  ) : LoadParams<Key>

  data class Append<Key : Any>(
    override val key: Key,
  ) : LoadParams<Key>
}

/** 加载结果 */
sealed interface LoadResult<Key : Any, Value : Any> {
  data class Page<Key : Any, Value : Any>(
    val data: List<Value>,
    val nextKey: Key?,
  ) : LoadResult<Key, Value>

  class None<Key : Any, Value : Any> : LoadResult<Key, Value>
}