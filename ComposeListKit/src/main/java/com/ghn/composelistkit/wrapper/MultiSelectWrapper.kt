package com.ghn.composelistkit.wrapper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.config.BatchSelectionAction
import com.ghn.composelistkit.config.MultiSelectBarState
import com.ghn.composelistkit.config.MultiSelectItemStyle
import com.ghn.composelistkit.config.SelectionMode

@Composable
fun <T> MultiSelectWrapper(
    items: List<T>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    keySelector: ((T) -> Any)? = null,
    contentTypeSelector: ((T) -> Any?)? = null,
    actions: List<BatchSelectionAction<T>> = emptyList(),
    maxSelectionCount: Int = Int.MAX_VALUE,
    selectionMode: SelectionMode = SelectionMode.Multi,
    showSelectionIndicator: Boolean = true,
    showSelectionBar: Boolean = true,
    enableSelectAllAction: Boolean = false,
    enableInvertSelectionAction: Boolean = false,
    itemStyle: MultiSelectItemStyle = MultiSelectItemStyle(),
    indicatorContent: (@Composable (Boolean, SelectionMode, () -> Unit) -> Unit)? = null,
    selectionBarContent: (@Composable (MultiSelectBarState<T>) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T, Boolean) -> Unit,
    onSelectionChanged: ((List<T>) -> Unit)? = null
) {
    val resolvedMaxSelectionCount = when (selectionMode) {
        SelectionMode.Single -> 1
        SelectionMode.Multi -> maxSelectionCount.coerceAtLeast(1)
    }
    val selectedKeys = remember { mutableStateListOf<Any>() }

    val keyedItems = remember(items, keySelector) {
        items.mapIndexed { index, item ->
            (keySelector?.invoke(item) ?: index) to item
        }
    }
    val itemByKey = remember(keyedItems) { keyedItems.toMap() }

    LaunchedEffect(keyedItems) {
        val validKeys = keyedItems.map { it.first }.toSet()
        selectedKeys.removeAll { it !in validKeys }
    }

    LaunchedEffect(selectionMode, resolvedMaxSelectionCount, keyedItems) {
        if (selectedKeys.size <= resolvedMaxSelectionCount) return@LaunchedEffect
        when (selectionMode) {
            SelectionMode.Single -> {
                val keep = selectedKeys.lastOrNull()
                selectedKeys.clear()
                if (keep != null) selectedKeys.add(keep)
            }

            SelectionMode.Multi -> {
                val keep = selectedKeys.take(resolvedMaxSelectionCount)
                selectedKeys.clear()
                selectedKeys.addAll(keep)
            }
        }
    }

    val selectedItems = remember(selectedKeys.toList(), itemByKey) {
        selectedKeys.mapNotNull { key -> itemByKey[key] }
    }
    val orderedKeys = remember(keyedItems) { keyedItems.map { it.first } }
    val rowShape = RoundedCornerShape(itemStyle.cornerRadius)
    val selectedAlpha = itemStyle.selectedContainerAlpha.coerceIn(0f, 1f)
    val unselectedAlpha = itemStyle.unselectedContainerAlpha.coerceIn(0f, 1f)

    LaunchedEffect(selectedItems, onSelectionChanged) {
        onSelectionChanged?.invoke(selectedItems)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyListContent<T>(
            modifier = modifier.fillMaxSize(),
            listState = listState,
            loadMoreFooterState = loadMoreFooterState,
            onRetryLoadMore = onRetryLoadMore,
            loadingMoreContent = loadingMoreContent,
            loadMoreErrorContent = loadMoreErrorContent,
            noMoreContent = noMoreContent,
            contentBuilder = {
                itemsIndexed(
                    items = items,
                    key = { index, item -> keySelector?.invoke(item) ?: index },
                    contentType = { _, item -> contentTypeSelector?.invoke(item) }
                ) { index, item ->
                    val rowKey = keySelector?.invoke(item) ?: index
                    val isSelected = selectedKeys.contains(rowKey)
                    val onToggle: () -> Unit = {
                        if (isSelected) {
                            selectedKeys.remove(rowKey)
                        } else {
                            when (selectionMode) {
                                SelectionMode.Single -> {
                                    selectedKeys.clear()
                                    selectedKeys.add(rowKey)
                                }

                                SelectionMode.Multi -> {
                                    if (selectedKeys.size < resolvedMaxSelectionCount) {
                                        selectedKeys.add(rowKey)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = itemStyle.rowOuterVerticalPadding)
                            .clip(rowShape)
                            .background(
                                color = if (isSelected) {
                                    (itemStyle.selectedContainerColor
                                        ?: MaterialTheme.colorScheme.secondaryContainer)
                                        .copy(alpha = selectedAlpha)
                                } else {
                                    (itemStyle.unselectedContainerColor
                                        ?: MaterialTheme.colorScheme.surface)
                                        .copy(alpha = unselectedAlpha)
                                },
                                shape = rowShape
                            )
                            .clickable { onToggle() }
                            .padding(contentPadding)
                            .padding(
                                horizontal = itemStyle.rowInnerHorizontalPadding,
                                vertical = itemStyle.rowInnerVerticalPadding
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSelectionIndicator) {
                            indicatorContent?.invoke(isSelected, selectionMode, onToggle)
                                ?: Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggle() }
                                )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            itemContent(item, isSelected)
                        }
                    }
                }
            }
        )

        val clearSelection: () -> Unit = { selectedKeys.clear() }
        val selectAllAction: (() -> Unit)? =
            if (selectionMode == SelectionMode.Multi && enableSelectAllAction) {
                {
                    selectedKeys.clear()
                    val targetKeys = orderedKeys.take(resolvedMaxSelectionCount)
                    selectedKeys.addAll(targetKeys)
                }
            } else {
                null
            }
        val invertSelectionAction: (() -> Unit)? =
            if (selectionMode == SelectionMode.Multi && enableInvertSelectionAction) {
                {
                    val current = selectedKeys.toSet()
                    val inverted = orderedKeys
                        .filterNot { key -> current.contains(key) }
                        .take(resolvedMaxSelectionCount)
                    selectedKeys.clear()
                    selectedKeys.addAll(inverted)
                }
            } else {
                null
            }
        val executeBatchAction: (BatchSelectionAction<T>) -> Unit = { action ->
            val snapshot = selectedKeys.mapNotNull { key -> itemByKey[key] }
            action.onAction(snapshot)
            if (action.clearSelectionAfterAction) {
                selectedKeys.clear()
            }
        }
        val showBuiltInBatchTools =
            selectionMode == SelectionMode.Multi &&
                    keyedItems.isNotEmpty() &&
                    (enableSelectAllAction || enableInvertSelectionAction)
        if (showSelectionBar) {
            AnimatedVisibility(
                visible = selectedItems.isNotEmpty() || showBuiltInBatchTools,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                if (selectionBarContent != null) {
                    selectionBarContent(
                        MultiSelectBarState(
                            selectedItems = selectedItems,
                            selectedCount = selectedItems.size,
                            selectionMode = selectionMode,
                            clearSelection = clearSelection,
                            selectAll = selectAllAction,
                            invertSelection = invertSelectionAction,
                            executeAction = executeBatchAction
                        )
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "已选择 ${selectedItems.size} 项",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = clearSelection) {
                                Text("清空")
                            }
                            if (selectAllAction != null) {
                                TextButton(onClick = selectAllAction) {
                                    Text("全选")
                                }
                            }
                            if (invertSelectionAction != null) {
                                TextButton(onClick = invertSelectionAction) {
                                    Text("反选")
                                }
                            }
                            actions.forEach { action ->
                                TextButton(
                                    onClick = {
                                        executeBatchAction(action)
                                    }
                                ) {
                                    Text(action.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
