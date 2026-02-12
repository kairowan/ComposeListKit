package com.ghn.composelistkit.wrapper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable


/**
 * @author 浩楠
 *
 * @date 2025/5/2-14:26
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
fun <T> DragReorderWrapper(
    items: SnapshotStateList<T>,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    enableHighlight: Boolean = true,
    useLongPress: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
    itemKey: ((T) -> Any)? = null,
    itemContentType: ((T) -> Any?)? = null,
    onMove: ((Int, Int) -> Unit)? = null,
    onItemsReordered: ((List<T>) -> Unit)? = null,
    itemContent: @Composable (item: T, isDragging: Boolean) -> Unit
) {
    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to ->
            applyMoveAndNotify(
                items = items,
                fromIndex = from.index,
                toIndex = to.index,
                onMove = onMove,
                onItemsReordered = onItemsReordered
            )
        }
    )

    val draggedIndex = reorderState.draggingItemIndex

    LazyListContent<T>(
        listState = reorderState.listState,
        loadMoreFooterState = loadMoreFooterState,
        onRetryLoadMore = onRetryLoadMore,
        loadingMoreContent = loadingMoreContent,
        loadMoreErrorContent = loadMoreErrorContent,
        noMoreContent = noMoreContent,
        modifier = modifier
            .reorderable(reorderState)
            .then(
                if (useLongPress)
                    Modifier.detectReorderAfterLongPress(reorderState)
                else
                    Modifier.detectReorder(reorderState)
            ),
        contentBuilder = {
            itemsIndexed(
                items = items,
                key = { _, item -> itemKey?.invoke(item) ?: item.hashCode() },
                contentType = { _, item -> itemContentType?.invoke(item) }
            ) { index, item ->
                val isDragging = index == draggedIndex
                val scale by animateFloatAsState(
                    targetValue = if (isDragging && enableHighlight) 1.03f else 1f,
                    label = "drag_scale_anim"
                )

                ReorderableItem(reorderState, index) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (enableHighlight) {
                                    this.scaleX = scale
                                    this.scaleY = scale
                                }
                            }
                            .animateItem()
                            .padding(8.dp)
                    ) {
                        dragHandle?.let {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                it()
                            }
                        }
                        itemContent(item, isDragging)
                    }
                }
            }
        }
    )
}

internal fun <T> applyMoveAndNotify(
    items: MutableList<T>,
    fromIndex: Int,
    toIndex: Int,
    onMove: ((Int, Int) -> Unit)? = null,
    onItemsReordered: ((List<T>) -> Unit)? = null
) {
    if (fromIndex == toIndex) return
    val moved = items.move(fromIndex, toIndex)
    if (!moved) return
    onMove?.invoke(fromIndex, toIndex)
    onItemsReordered?.invoke(items.toList())
}

fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int): Boolean {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in 0..size) return false
    val item = removeAt(fromIndex)
    val targetIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
    add(targetIndex.coerceIn(0, size), item)
    return true
}
