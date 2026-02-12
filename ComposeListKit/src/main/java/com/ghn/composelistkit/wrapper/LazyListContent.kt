package com.ghn.composelistkit.wrapper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.config.LoadMoreStyle

enum class LoadMoreFooterState {
    Idle,
    Loading,
    Error,
    NoMore
}

internal val LocalLoadMoreStyle = compositionLocalOf { LoadMoreStyle() }

/**
 * @author 浩楠
 *
 * @date 2025/4/26-15:18
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO 最底层的容器
 */
@Composable
fun <T> LazyListContent(
    items: List<T> = emptyList(),
    modifier: Modifier = Modifier,
    keySelector: ((T) -> Any)? = null,
    contentTypeSelector: ((T) -> Any?)? = null,
    listState: LazyListState,
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit = {},
    contentBuilder: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        if (contentBuilder != null) {
            contentBuilder()
        } else {
            items(
                items = items,
                key = keySelector,
                contentType = { item -> contentTypeSelector?.invoke(item) }
            ) { item ->
                itemContent(item)
            }
        }

        addLoadMoreFooter(
            state = loadMoreFooterState,
            onRetryLoadMore = onRetryLoadMore,
            loadingMoreContent = loadingMoreContent,
            loadMoreErrorContent = loadMoreErrorContent,
            noMoreContent = noMoreContent
        )
    }
}

internal fun LazyListScope.addLoadMoreFooter(
    state: LoadMoreFooterState,
    onRetryLoadMore: (() -> Unit)?,
    loadingMoreContent: (@Composable () -> Unit)?,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)?,
    noMoreContent: (@Composable () -> Unit)?
) {
    when (state) {
        LoadMoreFooterState.Idle -> Unit
        LoadMoreFooterState.Loading -> {
            item(key = "LoadMoreLoading") {
                if (loadingMoreContent != null) {
                    loadingMoreContent()
                } else {
                    DefaultLoadMoreLoading()
                }
            }
        }

        LoadMoreFooterState.Error -> {
            item(key = "LoadMoreError") {
                if (loadMoreErrorContent != null) {
                    loadMoreErrorContent(onRetryLoadMore)
                } else {
                    DefaultLoadMoreError(onRetryLoadMore)
                }
            }
        }

        LoadMoreFooterState.NoMore -> {
            item(key = "LoadMoreNoMore") {
                if (noMoreContent != null) {
                    noMoreContent()
                } else {
                    DefaultLoadMoreNoMore()
                }
            }
        }
    }
}

@Composable
internal fun DefaultLoadMoreLoading() {
    val style = LocalLoadMoreStyle.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = style.loadingIndicatorColor ?: MaterialTheme.colorScheme.primary
            )
            style.loadingText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = style.loadingTextColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun DefaultLoadMoreError(onRetryLoadMore: (() -> Unit)?) {
    val style = LocalLoadMoreStyle.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(
                if (onRetryLoadMore != null) {
                    Modifier.clickable { onRetryLoadMore() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (onRetryLoadMore != null) style.errorRetryText else style.errorText,
            style = MaterialTheme.typography.bodyMedium,
            color = style.errorTextColor ?: MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun DefaultLoadMoreNoMore() {
    val style = LocalLoadMoreStyle.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = style.noMoreText,
            style = MaterialTheme.typography.bodyMedium,
            color = style.noMoreTextColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
