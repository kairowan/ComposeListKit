package com.ghn.composelistkit.wrapper

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ghn.composelistkit.config.DividerConfig
import com.ghn.composelistkit.config.ListLayoutConfig
import com.ghn.composelistkit.config.ListLayoutStyle

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun <T> HeaderFooterWrapper(
    items: List<T>,
    modifier: Modifier = Modifier,
    keySelector: ((T) -> Any)? = null,
    contentTypeSelector: ((T) -> Any?)? = null,
    listState: LazyListState,
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    layoutConfig: ListLayoutConfig = ListLayoutConfig(),
    divider: DividerConfig = DividerConfig(),
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val indexedKey = keySelector?.let { selector: (T) -> Any ->
        { _: Int, item: T -> selector(item) }
    }
    val indexedContentType: (Int, T) -> Any? = { _, item ->
        contentTypeSelector?.invoke(item)
    }

    when (val style = layoutConfig.style) {
        ListLayoutStyle.Vertical -> {
            LazyColumn(
                modifier = modifier,
                state = listState,
                reverseLayout = layoutConfig.reverseLayout,
                contentPadding = layoutConfig.contentPadding,
                verticalArrangement = Arrangement.spacedBy(layoutConfig.mainAxisSpacing)
            ) {
                headerContent?.let {
                    item("HeaderSlot") { it() }
                }
                itemsIndexed(
                    items = items,
                    key = indexedKey,
                    contentType = indexedContentType
                ) { index, item ->
                    itemContent(item)
                    if (divider.enabled && index < items.lastIndex) {
                        VerticalListDivider(divider)
                    }
                }
                footerContent?.let {
                    item("FooterSlot") { it() }
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

        ListLayoutStyle.Horizontal -> {
            Column(modifier = modifier) {
                headerContent?.invoke()
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    reverseLayout = layoutConfig.reverseLayout,
                    contentPadding = layoutConfig.contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(layoutConfig.mainAxisSpacing)
                ) {
                    itemsIndexed(
                        items = items,
                        key = indexedKey,
                        contentType = indexedContentType
                    ) { index, item ->
                        itemContent(item)
                        if (divider.enabled && index < items.lastIndex) {
                            HorizontalListDivider(divider)
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
                footerContent?.invoke()
            }
        }

        is ListLayoutStyle.Grid -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(style.columns.coerceAtLeast(1)),
                modifier = modifier,
                contentPadding = layoutConfig.contentPadding,
                verticalArrangement = Arrangement.spacedBy(layoutConfig.mainAxisSpacing),
                horizontalArrangement = Arrangement.spacedBy(layoutConfig.crossAxisSpacing)
            ) {
                gridFullSpan(headerContent)
                itemsIndexed(
                    items = items,
                    key = indexedKey,
                    contentType = indexedContentType
                ) { _, item ->
                    itemContent(item)
                }
                gridFullSpan(footerContent)
                gridLoadMoreFooter(
                    state = loadMoreFooterState,
                    onRetryLoadMore = onRetryLoadMore,
                    loadingMoreContent = loadingMoreContent,
                    loadMoreErrorContent = loadMoreErrorContent,
                    noMoreContent = noMoreContent
                )
            }
        }

        is ListLayoutStyle.Staggered -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(style.columns.coerceAtLeast(1)),
                modifier = modifier,
                contentPadding = layoutConfig.contentPadding,
                horizontalArrangement = Arrangement.spacedBy(layoutConfig.crossAxisSpacing),
                verticalItemSpacing = layoutConfig.mainAxisSpacing
            ) {
                headerContent?.let {
                    item(
                        key = "HeaderSlot",
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        it()
                    }
                }
                items(
                    items = items,
                    key = keySelector,
                    contentType = { item -> contentTypeSelector?.invoke(item) }
                ) { item ->
                    itemContent(item)
                }
                footerContent?.let {
                    item(
                        key = "FooterSlot",
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        it()
                    }
                }
                staggeredLoadMoreFooter(
                    state = loadMoreFooterState,
                    onRetryLoadMore = onRetryLoadMore,
                    loadingMoreContent = loadingMoreContent,
                    loadMoreErrorContent = loadMoreErrorContent,
                    noMoreContent = noMoreContent
                )
            }
        }

        is ListLayoutStyle.Flow -> {
            LazyColumn(
                modifier = modifier,
                state = listState,
                reverseLayout = layoutConfig.reverseLayout,
                contentPadding = layoutConfig.contentPadding,
                verticalArrangement = Arrangement.spacedBy(layoutConfig.mainAxisSpacing)
            ) {
                headerContent?.let {
                    item("HeaderSlot") { it() }
                }
                item("FlowContent") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = style.maxItemsInEachRow.coerceAtLeast(1),
                        horizontalArrangement = Arrangement.spacedBy(layoutConfig.crossAxisSpacing),
                        verticalArrangement = Arrangement.spacedBy(layoutConfig.mainAxisSpacing)
                    ) {
                        items.forEach { item ->
                            Box {
                                itemContent(item)
                            }
                        }
                    }
                }
                footerContent?.let {
                    item("FooterSlot") { it() }
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
    }
}

private fun LazyGridScope.gridFullSpan(content: (@Composable () -> Unit)?) {
    content?.let { slot ->
        item(span = { GridItemSpan(maxLineSpan) }) {
            slot()
        }
    }
}

private fun LazyGridScope.gridLoadMoreFooter(
    state: LoadMoreFooterState,
    onRetryLoadMore: (() -> Unit)?,
    loadingMoreContent: (@Composable () -> Unit)?,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)?,
    noMoreContent: (@Composable () -> Unit)?
) {
    when (state) {
        LoadMoreFooterState.Idle -> Unit
        LoadMoreFooterState.Loading -> {
            item(span = { GridItemSpan(maxLineSpan) }, key = "LoadMoreLoading") {
                if (loadingMoreContent != null) loadingMoreContent() else DefaultLoadMoreLoading()
            }
        }

        LoadMoreFooterState.Error -> {
            item(span = { GridItemSpan(maxLineSpan) }, key = "LoadMoreError") {
                if (loadMoreErrorContent != null) {
                    loadMoreErrorContent(onRetryLoadMore)
                } else {
                    DefaultLoadMoreError(onRetryLoadMore)
                }
            }
        }

        LoadMoreFooterState.NoMore -> {
            item(span = { GridItemSpan(maxLineSpan) }, key = "LoadMoreNoMore") {
                if (noMoreContent != null) noMoreContent() else DefaultLoadMoreNoMore()
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.staggeredLoadMoreFooter(
    state: LoadMoreFooterState,
    onRetryLoadMore: (() -> Unit)?,
    loadingMoreContent: (@Composable () -> Unit)?,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)?,
    noMoreContent: (@Composable () -> Unit)?
) {
    when (state) {
        LoadMoreFooterState.Idle -> Unit
        LoadMoreFooterState.Loading -> {
            item(
                key = "LoadMoreLoading",
                span = StaggeredGridItemSpan.FullLine
            ) {
                if (loadingMoreContent != null) loadingMoreContent() else DefaultLoadMoreLoading()
            }
        }

        LoadMoreFooterState.Error -> {
            item(
                key = "LoadMoreError",
                span = StaggeredGridItemSpan.FullLine
            ) {
                if (loadMoreErrorContent != null) {
                    loadMoreErrorContent(onRetryLoadMore)
                } else {
                    DefaultLoadMoreError(onRetryLoadMore)
                }
            }
        }

        LoadMoreFooterState.NoMore -> {
            item(
                key = "LoadMoreNoMore",
                span = StaggeredGridItemSpan.FullLine
            ) {
                if (noMoreContent != null) noMoreContent() else DefaultLoadMoreNoMore()
            }
        }
    }
}

@Composable
private fun VerticalListDivider(divider: DividerConfig) {
    val color = if (divider.color != Color.Unspecified) divider.color else MaterialTheme.colorScheme.outlineVariant
    HorizontalDivider(
        modifier = Modifier.padding(
            start = divider.startIndent,
            end = divider.endIndent
        ),
        thickness = divider.thickness,
        color = color
    )
}

@Composable
private fun HorizontalListDivider(divider: DividerConfig) {
    val color = if (divider.color != Color.Unspecified) divider.color else MaterialTheme.colorScheme.outlineVariant
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                top = divider.startIndent,
                bottom = divider.endIndent
            ),
        thickness = divider.thickness,
        color = color
    )
}
