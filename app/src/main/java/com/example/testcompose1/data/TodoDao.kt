package com.example.testcompose1.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 数据访问对象
@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>> // 返回数据流，需要持续监听数据，不加suspend关键字

    /** 分页查询，供 Paging 使用（每页条数由 PagingConfig.pageSize 决定） */
    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    fun getTodosPaged(): PagingSource<Int, TodoEntity>

    @Query("""
    SELECT * FROM todo_items 
    WHERE title LIKE '%' || :query || '%' 
       OR description LIKE '%' || :query || '%' 
    ORDER BY createdAt DESC
""")
    fun getTodosPaged(query: String): PagingSource<Int, TodoEntity> // 模糊查询

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoEntity?   // 挂起函数，返回单个待办

    @Insert
    suspend fun insert(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("SELECT COUNT(*) FROM todo_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>
}