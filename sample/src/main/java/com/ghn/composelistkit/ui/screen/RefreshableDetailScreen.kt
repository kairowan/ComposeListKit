package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author 浩楠
 *
 * @date 2025/4/25-22:40
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun RefreshableDetailScreen() {
    val items = remember { mutableStateListOf<String>() }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isLoadMoreError by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var failedOnce by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(1) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        items.addAll((1..20).map { "Item $it" })
    }

    fun requestLoadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        isLoadMoreError = false
        coroutineScope.launch {
            delay(1000)
            if (!failedOnce) {
                failedOnce = true
                isLoadingMore = false
                isLoadMoreError = true
                return@launch
            }
            val start = page * 20 + 1
            val end = start + 19
            items.addAll((start..end).map { "Loaded $it" })
            page += 1
            hasMore = page < 4
            isLoadingMore = false
        }
    }

    ComposeListKit {
        data {
            list(items = items)
            item { item -> RefreshableItem(item) }
        }
        refresh {
            bind(isRefreshing = isRefreshing) {
                isRefreshing = true
                coroutineScope.launch {
                    delay(1000)
                    items.clear()
                    items.addAll((101..120).map { "Refreshed $it" })
                    page = 1
                    hasMore = true
                    isLoadMoreError = false
                    failedOnce = false
                    isRefreshing = false
                }
            }
        }
        paging {
            bind(
                isLoadingMore = isLoadingMore,
                hasMore = hasMore,
                isLoadMoreError = isLoadMoreError,
                prefetchDistance = 2,
                onRetryLoadMore = { requestLoadMore() }
            ) {
                requestLoadMore()
            }
            slots(
                error = { onRetry ->
                    val modifier = if (onRetry != null) {
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { onRetry() }
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    }
                    Text(
                        text = "加载失败，点击重试",
                        modifier = modifier
                    )
                },
                noMore = {
                    Text(
                        text = "没有更多数据了",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun RefreshableItem(item: String) {
    Text(
        text = item,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
