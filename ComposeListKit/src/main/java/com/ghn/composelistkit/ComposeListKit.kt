package com.ghn.composelistkit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.config.ComposeListKitConfig
import com.ghn.composelistkit.config.ComposeListKitMode
import com.ghn.composelistkit.config.ListLayoutStyle
import com.ghn.composelistkit.listkit.ComposeListKitBuilder
import com.ghn.composelistkit.wrapper.DragReorderWrapper
import com.ghn.composelistkit.wrapper.HeaderFooterWrapper
import com.ghn.composelistkit.wrapper.CommentThreadWrapper
import com.ghn.composelistkit.wrapper.LoadMoreFooterState
import com.ghn.composelistkit.wrapper.MultiSelectWrapper
import com.ghn.composelistkit.wrapper.RefreshWrapper
import com.ghn.composelistkit.wrapper.SectionedSwipeWrapper
import com.ghn.composelistkit.wrapper.SimpleSwipeToDeleteWrapper
import com.ghn.composelistkit.wrapper.StateWrapper
import com.ghn.composelistkit.wrapper.StickySectionWrapper
import com.ghn.composelistkit.wrapper.LocalLoadMoreStyle
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * @author 浩楠
 *
 * @date 2025/4/25-15:23
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun <T> ComposeListKit(build: ComposeListKitBuilder<T>.() -> Unit) {
    val builder = ComposeListKitBuilder<T>().apply(build)
    InternalComposeListKit(builder.build())
}

@Composable
internal fun <T> InternalComposeListKit(config: ComposeListKitConfig<T>) {
    check(
        config.mode is ComposeListKitMode.Plain ||
                config.data.layout.style == ListLayoutStyle.Vertical
    ) {
        "Custom list layout styles(horizontal/grid/staggered/flow) are only supported in plain mode."
    }

    val listState = config.listState ?: rememberLazyListState()
    val latestIsLoadingMore by rememberUpdatedState(config.loadMore.isLoadingMore)
    val latestHasMore by rememberUpdatedState(config.loadMore.hasMore)
    val latestIsLoadMoreError by rememberUpdatedState(config.loadMore.isLoadMoreError)
    val latestPrefetchDistance by rememberUpdatedState(config.loadMore.prefetchDistance)
    val latestOnLoadMore by rememberUpdatedState(config.loadMore.onLoadMore)
    val modeItemsSize = when (val mode = config.mode) {
        is ComposeListKitMode.Sectioned -> mode.groups.sumOf { it.items.size }
        is ComposeListKitMode.SectionedSwipe -> mode.groups.sumOf { it.items.size }
        else -> config.data.items.size
    }
    var loadMoreArmed by remember { mutableStateOf(true) }

    LaunchedEffect(
        config.loadMore.hasMore,
        config.loadMore.isLoadingMore,
        config.loadMore.isLoadMoreError,
        modeItemsSize
    ) {
        when {
            !config.loadMore.hasMore -> loadMoreArmed = false
            config.loadMore.isLoadMoreError -> loadMoreArmed = false
            !config.loadMore.isLoadingMore -> loadMoreArmed = true
        }
    }

    if (
        config.loadMore.onLoadMore != null &&
        config.loadMore.hasMore &&
        !config.loadMore.isLoadMoreError
    ) {
        LaunchedEffect(listState) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                val totalItems = layoutInfo.totalItemsCount
                lastVisible to totalItems
            }
                .distinctUntilChanged()
                .collect { (lastVisible, totalItems) ->
                    if (
                        loadMoreArmed &&
                        shouldTriggerLoadMore(
                            lastVisibleIndex = lastVisible,
                            totalItems = totalItems,
                            hasMore = latestHasMore,
                            isLoadingMore = latestIsLoadingMore,
                            isLoadMoreError = latestIsLoadMoreError,
                            prefetchDistance = latestPrefetchDistance
                        )
                    ) {
                        loadMoreArmed = false
                        latestOnLoadMore?.invoke()
                    }
                }
        }
    }

    val groupedItemCount = when (val mode = config.mode) {
        is ComposeListKitMode.Sectioned -> mode.groups.sumOf { it.items.size }
        is ComposeListKitMode.SectionedSwipe -> mode.groups.sumOf { it.items.size }
        else -> 0
    }
    val loadMoreFooterState = resolveLoadMoreFooterState(
        isLoadingMore = config.loadMore.isLoadingMore,
        hasMore = config.loadMore.hasMore,
        isLoadMoreError = config.loadMore.isLoadMoreError
    )
    val onRetryLoadMore = config.loadMore.onRetryLoadMore ?: config.loadMore.onLoadMore

    CompositionLocalProvider(LocalLoadMoreStyle provides config.loadMore.style) {
        StateWrapper(
            isLoadingFirstPage = config.pageState.isLoadingFirstPage,
            isError = config.pageState.isError,
            itemsEmpty = when (config.mode) {
                is ComposeListKitMode.Sectioned -> groupedItemCount == 0
                is ComposeListKitMode.SectionedSwipe -> groupedItemCount == 0
                else -> config.data.items.isEmpty()
            },
            onRetry = config.pageState.onRetry,
            loadingContent = config.pageState.loadingContent,
            errorContent = config.pageState.errorContent,
            emptyContent = config.pageState.emptyContent
        ) {
            val listContent = @Composable {
                when (val mode = config.mode) {
                is ComposeListKitMode.Sectioned -> {
                    StickySectionWrapper(
                        groups = mode.groups,
                        listState = listState,
                        modifier = config.data.modifier,
                        isSticky = mode.isSticky,
                        itemKey = config.data.keySelector,
                        itemContentType = config.data.contentTypeSelector,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        groupHeaderContent = mode.headerContent ?: { title ->
                            Text(
                                text = title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        },
                        itemContent = config.data.itemContent
                    )
                }

                is ComposeListKitMode.SectionedSwipe -> {
                    val sectionedSwipeHeaderContent: @Composable (String, Boolean) -> Unit =
                        mode.headerStateContent
                            ?: mode.headerContent?.let { legacyHeader ->
                                { title, _ -> legacyHeader(title) }
                            }
                            ?: { title, isExpanded ->
                                val displayTitle = if (mode.enableGroupCollapse) {
                                    if (isExpanded) "v $title" else "> $title"
                                } else {
                                    title
                                }
                                Text(
                                    text = displayTitle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                    SectionedSwipeWrapper(
                        groups = mode.groups,
                        listState = listState,
                        modifier = config.data.modifier,
                        isSticky = mode.isSticky,
                        itemKey = config.data.keySelector,
                        itemContentType = config.data.contentTypeSelector,
                        startActions = mode.startActions,
                        endActions = mode.endActions,
                        actionSlotWidth = mode.actionSlotWidth,
                        openThresholdFraction = mode.openThresholdFraction,
                        enableFullSwipeAction = mode.enableFullSwipeAction,
                        fullSwipeTriggerFraction = mode.fullSwipeTriggerFraction,
                        canSwipe = mode.canSwipe,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        contentPadding = mode.contentPadding,
                        backgroundContent = mode.backgroundContent,
                        enableGroupCollapse = mode.enableGroupCollapse,
                        initiallyCollapsedGroupTitles = mode.initiallyCollapsedGroupTitles,
                        groupHeaderContent = sectionedSwipeHeaderContent,
                        itemContent = config.data.itemContent
                    )
                }

                is ComposeListKitMode.Swipe -> {
                    SimpleSwipeToDeleteWrapper(
                        items = config.data.items,
                        listState = listState,
                        modifier = config.data.modifier,
                        keySelector = config.data.keySelector,
                        contentTypeSelector = config.data.contentTypeSelector,
                        startActions = mode.startActions,
                        endActions = mode.endActions,
                        actionSlotWidth = mode.actionSlotWidth,
                        openThresholdFraction = mode.openThresholdFraction,
                        enableFullSwipeAction = mode.enableFullSwipeAction,
                        fullSwipeTriggerFraction = mode.fullSwipeTriggerFraction,
                        canSwipe = mode.canSwipe,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        contentPadding = mode.contentPadding,
                        backgroundContent = mode.backgroundContent,
                        itemContent = config.data.itemContent
                    )
                }

                is ComposeListKitMode.Drag -> {
                    val dragItems = rememberMutableItems(config.data.items)
                    DragReorderWrapper(
                        items = dragItems,
                        listState = listState,
                        modifier = config.data.modifier,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        enableHighlight = mode.enableHighlight,
                        useLongPress = mode.useLongPress,
                        dragHandle = mode.dragHandle,
                        itemKey = config.data.keySelector,
                        itemContentType = config.data.contentTypeSelector,
                        itemContent = mode.itemContent ?: { item, _ ->
                            config.data.itemContent(item)
                        },
                        onMove = mode.onMove,
                        onItemsReordered = mode.onReordered
                    )
                }

                is ComposeListKitMode.CommentThread -> {
                    CommentThreadWrapper(
                        roots = config.data.items,
                        listState = listState,
                        modifier = config.data.modifier,
                        keySelector = config.data.keySelector,
                        childrenSelector = mode.childrenSelector,
                        maxDepth = mode.maxDepth,
                        initialExpandedDepth = mode.initialExpandedDepth,
                        indentPerLevel = mode.indentPerLevel,
                        contentPadding = mode.contentPadding,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        itemContent = mode.itemContent ?: { item, _, _, _, _ ->
                            config.data.itemContent(item)
                        }
                    )
                }

                is ComposeListKitMode.MultiSelect -> {
                    MultiSelectWrapper(
                        items = config.data.items,
                        listState = listState,
                        modifier = config.data.modifier,
                        keySelector = config.data.keySelector,
                        contentTypeSelector = config.data.contentTypeSelector,
                        actions = mode.actions,
                        maxSelectionCount = mode.maxSelectionCount,
                        selectionMode = mode.selectionMode,
                        showSelectionIndicator = mode.showSelectionIndicator,
                        showSelectionBar = mode.showSelectionBar,
                        enableSelectAllAction = mode.enableSelectAllAction,
                        enableInvertSelectionAction = mode.enableInvertSelectionAction,
                        itemStyle = mode.itemStyle,
                        indicatorContent = mode.indicatorContent,
                        selectionBarContent = mode.selectionBarContent,
                        contentPadding = mode.contentPadding,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        itemContent = mode.itemContent ?: { item, _ ->
                            config.data.itemContent(item)
                        },
                        onSelectionChanged = mode.onSelectionChanged
                    )
                }

                is ComposeListKitMode.Plain -> {
                    HeaderFooterWrapper(
                        items = config.data.items,
                        modifier = config.data.modifier,
                        keySelector = config.data.keySelector,
                        listState = listState,
                        loadMoreFooterState = loadMoreFooterState,
                        onRetryLoadMore = onRetryLoadMore,
                        loadingMoreContent = config.loadMore.loadingMoreContent,
                        loadMoreErrorContent = config.loadMore.loadMoreErrorContent,
                        noMoreContent = config.loadMore.noMoreContent,
                        layoutConfig = config.data.layout,
                        divider = config.data.divider,
                        contentTypeSelector = config.data.contentTypeSelector,
                        headerContent = mode.header,
                        footerContent = mode.footer,
                        itemContent = config.data.itemContent
                    )
                }
            }
        }
            if (config.refresh.onRefresh != null) {
                RefreshWrapper(
                    isRefreshing = config.refresh.isRefreshing,
                    onRefresh = config.refresh.onRefresh,
                    indicatorContent = config.refresh.indicatorContent
                ) {
                    listContent()
                }
            } else {
                listContent()
            }
        }
    }
}

internal fun shouldTriggerLoadMore(
    lastVisibleIndex: Int,
    totalItems: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    isLoadMoreError: Boolean,
    prefetchDistance: Int
): Boolean {
    if (totalItems <= 0) return false
    val thresholdIndex = (totalItems - 1 - prefetchDistance.coerceAtLeast(0)).coerceAtLeast(0)
    return hasMore && !isLoadingMore && !isLoadMoreError && lastVisibleIndex >= thresholdIndex
}

internal fun resolveLoadMoreFooterState(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isLoadMoreError: Boolean
): LoadMoreFooterState {
    return when {
        isLoadingMore -> LoadMoreFooterState.Loading
        isLoadMoreError -> LoadMoreFooterState.Error
        !hasMore -> LoadMoreFooterState.NoMore
        else -> LoadMoreFooterState.Idle
    }
}

@Composable
private fun <T> rememberMutableItems(items: List<T>): SnapshotStateList<T> {
    @Suppress("UNCHECKED_CAST")
    val snapshotItems = items as? SnapshotStateList<T>
    return snapshotItems ?: remember(items) { items.toMutableStateList() }
}
