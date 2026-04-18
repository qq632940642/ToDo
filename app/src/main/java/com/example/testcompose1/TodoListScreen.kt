package com.example.testcompose1

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToDetail: (Int) -> Unit = {}
) {
    var showInfiniteList by remember { mutableStateOf(false) }
    if (showInfiniteList) {
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
        val lazyPagingItems = viewModel.todoPagingFlow.collectAsLazyPagingItems()

        // 已有数据时的下拉刷新才显示指示器，避免首屏空白时一直转圈
        val pullRefreshing =
            lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount > 0
        val pullRefreshState = rememberPullRefreshState(
            refreshing = pullRefreshing,
            onRefresh = { lazyPagingItems.refresh() }
        )

        var text by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ThemeSwitch(settingsViewModel)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("输入待办事项") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.addTodo(text)
                    text = ""
                },
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("添加")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalCount by viewModel.totalCount.collectAsState()
            val completedCount by viewModel.completedCount.collectAsState()

            Text("待办列表（每页 ${TodoRepository.PAGE_SIZE} 条，已完成:${completedCount},总数:${totalCount}）", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
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

                PullRefreshIndicator(
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
