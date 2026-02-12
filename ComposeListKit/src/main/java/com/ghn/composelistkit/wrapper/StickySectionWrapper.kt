package com.ghn.composelistkit.wrapper

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ghn.composelistkit.config.GroupedItems

/**
 * @author 浩楠
 *
 * @date 2025/4/30-21:34
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> StickySectionWrapper(
    groups: List<GroupedItems<T>>,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    isSticky: Boolean = true,
    itemKey: ((T) -> Any)? = null,
    itemContentType: ((T) -> Any?)? = null,
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    groupHeaderContent: @Composable (String) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    LazyListContent<T>(
        listState = listState,
        modifier = modifier,
        keySelector = itemKey,
        contentTypeSelector = itemContentType,
        loadMoreFooterState = loadMoreFooterState,
        onRetryLoadMore = onRetryLoadMore,
        loadingMoreContent = loadingMoreContent,
        loadMoreErrorContent = loadMoreErrorContent,
        noMoreContent = noMoreContent,
        contentBuilder = {
            groups.forEach { group ->
                if (isSticky) {
                    stickyHeader(key = "sticky_${group.title}") {
                        groupHeaderContent(group.title)
                    }
                } else {
                    item(key = "title_${group.title}") {
                        groupHeaderContent(group.title)
                    }
                }

                this.items(
                    items = group.items,
                    key = itemKey,
                    contentType = { item -> itemContentType?.invoke(item) }
                ) { item ->
                    itemContent(item)
                }
            }
        }
    )
}
