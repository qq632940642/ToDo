package com.example.testcompose1

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.testcompose1.data.TodoEntity
import com.example.testcompose1.data.TodoRepository
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToDetail: (Int) -> Unit = {}
) {
    var showInfiniteList by remember { mutableStateOf(false) }
    if (showInfiniteList) { // 测试无限滚动列表
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("无限滚动列表") },
                    navigationIcon = {
                        IconButton(onClick = { showInfiniteList = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                InfiniteListPage()
            }
        }
    } else {
        // 主界面开始
        var showAddDialog by remember { mutableStateOf(false) } // 显示添加弹框
        var inputText by remember { mutableStateOf("") }

        val lazyPagingItems = viewModel.todoPagingFlow.collectAsLazyPagingItems()

        // 已有数据时的下拉刷新才显示指示器，避免首屏空白时一直转圈
        val pullRefreshing =
            lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount > 0
        val pullRefreshState = rememberPullRefreshState(
            refreshing = pullRefreshing,
            onRefresh = { lazyPagingItems.refresh() }
        )

        val totalCount by viewModel.totalCount.collectAsState()
        val completedCount by viewModel.completedCount.collectAsState()

        Scaffold(floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加待办")
            }
        }) {paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                ThemeSwitch(settingsViewModel)
                Spacer(modifier = Modifier.height(8.dp))

                // --- 新增搜索框 ---
//                var searchText by remember { mutableStateOf("") }
//                LaunchedEffect(Unit) {// 当从待办事项详情页面返回时，又调用了，而且searchText打印为空字符串
//                    println("remember searchText initialized, value = $searchText")
//                }
                // 直接从 ViewModel 读取搜索词
                val searchText by viewModel.searchQuery.collectAsState()
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { newText ->
//                        searchText = newText
                        viewModel.updateSearchQuery(newText)   // 实时更新搜索关键词
                    },
                    label = { Text("搜索待办") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
//                                searchText = ""
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("待办列表（每页${TodoRepository.PAGE_SIZE}条，已完成:${completedCount},总数:${totalCount}）", style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                        ,contentPadding = PaddingValues(bottom = 80.dp) // 底部内边距，防止最后一条数据的删除按钮被悬浮的添加按钮遮挡。
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.id }
                        ) { index ->
                            val todo = lazyPagingItems[index]
                            if (todo != null) {
                                // 每个项独立管理对话框显示状态
                                var showDeleteDialog by remember { mutableStateOf(false) }

                                TodoItemRow(
                                    todo = todo,
                                    onDelete = {
//                                    viewModel.deleteTodo(todo)
                                        // 点击删除按钮时，显示对话框，而不是直接删除
                                        showDeleteDialog = true
                                    },
                                    onToggle = { viewModel.toggleComplete(todo) },
                                    onClick = { onNavigateToDetail(todo.id) }
                                )

                                // 确认删除对话框
                                if (showDeleteDialog) {
                                    DeleteConfirmDialog(title = "确定删除", msg = "确定要删除“${todo.title}”吗？"
                                        , positiveBtnText = "删除", negativeBtnText = "取消"
                                        , onConfirm = {showDeleteDialog = false
                                            viewModel.deleteTodo(todo)
                                        }
                                        , onDismiss = {showDeleteDialog = false})
                                }
                            } else {
                                Spacer(Modifier.height(72.dp))
                            }
                        }

                        item {
                            when (val append = lazyPagingItems.loadState.append) {
                                is LoadState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                is LoadState.Error -> {
                                    Text(
                                        text = "加载更多失败：${append.error.localizedMessage}",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }

                                else -> {}
                            }
                        }
                    }

                    // 搜索数据为空的提示：
                    if (lazyPagingItems.itemCount == 0 && !pullRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "没有找到匹配的待办项",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PullRefreshIndicator( // 呈现刷新动画
                        refreshing = pullRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    if (lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    val refreshError = lazyPagingItems.loadState.refresh as? LoadState.Error
                    if (refreshError != null && lazyPagingItems.itemCount == 0) {
                        Text(
                            text = "加载失败：${refreshError.error.localizedMessage}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTodoDialog(
                onAdd = { title ->
                    viewModel.addTodo(title)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun AddTodoDialog(
    onAdd: (String) -> Unit,   // 添加回调，传入标题
    onDismiss: () -> Unit      // 关闭回调
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增待办") },
        text = {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("待办标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onAdd(inputText)
                        // 由父组件负责关闭对话框
                    }
                }
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                // 取消时清空输入并关闭
                inputText = ""
                onDismiss()
            }) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TodoItemRow(
    todo: TodoEntity,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ThemeSwitch(settingsViewModel: SettingsViewModel) {
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "深色模式",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = isDarkTheme,
            onCheckedChange = { settingsViewModel.toggleDarkMode() }
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    msg: String,
    positiveBtnText: String,
    negativeBtnText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(msg) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(positiveBtnText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(negativeBtnText)
            }
        }
    )
}
