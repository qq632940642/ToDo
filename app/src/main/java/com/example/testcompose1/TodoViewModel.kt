package com.example.testcompose1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.testcompose1.data.TodoEntity
import com.example.testcompose1.data.TodoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    // 搜索关键词（默认为空）
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 根据搜索关键词动态生成分页数据流
    val todoPagingFlow: Flow<PagingData<TodoEntity>> = _searchQuery
        .flatMapLatest { query -> // flatMapLatest 是专门用于处理“搜索场景”的操作符。
            repository.getTodosPaged(query)
                .cachedIn(viewModelScope)// cachedIn 是 Paging 库提供的扩展函数，作用是将 PagingData 的加载状态缓存到指定的协程作用域中。如果不加 cachedIn，每次流被重新订阅时，都会从头开始加载数据，导致重复请求和滚动位置丢失
        }
        .stateIn( // stateIn 是 Kotlin Flow 的“热流转换”操作符。它把一个冷流（Cold Flow）变成热流（Hot Flow），并提供一个共享的 State。
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 在最后一个订阅者取消后，等待 5 秒再停止上游流。这可以避免频繁开启/关闭流（比如屏幕旋转时）。
            initialValue = PagingData.empty()
        )

    // 更新搜索关键词
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** 分页待办列表（懒加载，每页 [TodoRepository.PAGE_SIZE] 条） */
//    val todoPagingFlow: Flow<PagingData<TodoEntity>> =
//        repository.getTodosPaged().cachedIn(viewModelScope)

    // 统计信息
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getTotalCount().collect { count ->
                _totalCount.value = count
            }
        }

        viewModelScope.launch {
            repository.getCompletedCount().collect { count ->
                _completedCount.value = count
            }
        }
    }

    fun addTodo(title: String) {
        if (title.isNotBlank()) {
            viewModelScope.launch {
                repository.addTodo(title)
            }
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun toggleComplete(todo: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
        }
    }

    // 单个待办
    private val _currentTodo = MutableStateFlow<TodoEntity?>(null)
    val currentTodo: StateFlow<TodoEntity?> = _currentTodo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadTodoById(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val todo = repository.getTodoById(id)
            _currentTodo.value = todo
            _isLoading.value = false
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateTodo(todo)
            _isLoading.value = false
        }
    }
}
