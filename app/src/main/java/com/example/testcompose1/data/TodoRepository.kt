package com.example.testcompose1.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

// 数据仓库
class TodoRepository(private val dao: TodoDao) {

    companion object {
        /** 每页条数：上拉每次加载的数量 */
        const val PAGE_SIZE = 9

        /**
         * 模拟网络/慢查询：每次分页 `load` 前暂停（毫秒）。
         * 设为 0 即关闭模拟耗时。
         */
        const val SIMULATED_PAGE_LOAD_DELAY_MS = 300L
    }

    fun getAllTodos(): Flow<List<TodoEntity>> = dao.getAllTodos()

    fun getTodosPaged(query: String = ""): Flow<PagingData<TodoEntity>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
//            initialLoadSize = PAGE_SIZE,
//            prefetchDistance = 1,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            // 模拟延迟，为了能看到加载动画
//            DelayingPagingSource(
//                delegate = dao.getTodosPaged(),
//                delayMs = SIMULATED_PAGE_LOAD_DELAY_MS
//            )
            dao.getTodosPaged(query)
        }
    ).flow

    suspend fun addTodo(title: String) {
        dao.insert(TodoEntity(title = title))
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        dao.delete(todo)
    }

    suspend fun updateTodo(todo: TodoEntity) {
        dao.update(todo)
    }

    suspend fun getTodoById(id: Int): TodoEntity? = dao.getTodoById(id)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    fun getCompletedCount(): Flow<Int> = dao.getCompletedCount()
}

/**
 * 在 Room 的 [PagingSource] 外包一层，每次真正加载前 [delay]，便于观察 Loading UI。
 * 并把 delegate 的失效转发出来，保证数据库变更后仍能刷新列表。
 */
private class DelayingPagingSource(
    private val delegate: PagingSource<Int, TodoEntity>,
    private val delayMs: Long
) : PagingSource<Int, TodoEntity>() {

    init {
        delegate.registerInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: PagingSource.LoadParams<Int>): PagingSource.LoadResult<Int, TodoEntity> {
        if (delayMs > 0) delay(delayMs)
        return delegate.load(params)
    }

    override fun getRefreshKey(state: PagingState<Int, TodoEntity>): Int? =
        delegate.getRefreshKey(state)

    override val jumpingSupported: Boolean
        get() = delegate.jumpingSupported

    override val keyReuseSupported: Boolean
        get() = delegate.keyReuseSupported
}
