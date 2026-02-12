package com.ghn.composelistkit.wrapper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.config.GroupedItems
import com.ghn.composelistkit.config.SwipeAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class SectionedSwipeRowKey(
    val groupIndex: Int,
    val itemKey: Any
)

private fun buildSectionedSwipeLazyKey(groupIndex: Int, itemKey: Any): String {
    return "sectioned_swipe_item_${groupIndex}_$itemKey"
}

private fun <T> resolveSectionedSwipeItemIdentity(
    itemKeySelector: ((T) -> Any)?,
    item: T,
    index: Int
): Any {
    itemKeySelector?.invoke(item)?.let { return it }
    val fallbackIdentity = item as Any?
    return fallbackIdentity ?: index
}

private fun buildSectionedSwipeHeaderTag(groupIndex: Int): String {
    return "sectioned_swipe_header_$groupIndex"
}

private fun buildSectionedSwipeGroupStateKey(groupIndex: Int, title: String): String {
    return "sectioned_swipe_group_${groupIndex}_$title"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SectionedSwipeWrapper(
    groups: List<GroupedItems<T>>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    isSticky: Boolean = true,
    itemKey: ((T) -> Any)? = null,
    itemContentType: ((T) -> Any?)? = null,
    startActions: List<SwipeAction<T>> = emptyList(),
    endActions: List<SwipeAction<T>> = emptyList(),
    actionSlotWidth: Dp = 92.dp,
    openThresholdFraction: Float = 0.35f,
    enableFullSwipeAction: Boolean = false,
    fullSwipeTriggerFraction: Float = 0.86f,
    canSwipe: ((T) -> Boolean)? = null,
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null,
    enableGroupCollapse: Boolean = false,
    initiallyCollapsedGroupTitles: Set<String> = emptySet(),
    groupHeaderContent: @Composable (String, Boolean) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    val resolvedActionSlotWidth = actionSlotWidth.coerceAtLeast(56.dp)
    val resolvedOpenThreshold = openThresholdFraction.coerceIn(0.1f, 0.9f)
    val resolvedFullSwipeTriggerFraction = fullSwipeTriggerFraction.coerceIn(0.55f, 1f)
    val startSwipeWidth = with(LocalDensity.current) {
        (resolvedActionSlotWidth * startActions.size).toPx()
    }
    val endSwipeWidth = with(LocalDensity.current) {
        (resolvedActionSlotWidth * endActions.size).toPx()
    }
    val hasSwipeActions = startActions.isNotEmpty() || endActions.isNotEmpty()
    val groupStateKeys = groups.mapIndexed { groupIndex, group ->
        buildSectionedSwipeGroupStateKey(groupIndex = groupIndex, title = group.title)
    }
    val collapsedStateByGroup = remember(
        enableGroupCollapse,
        initiallyCollapsedGroupTitles,
        groupStateKeys
    ) {
        mutableStateMapOf<String, Boolean>().apply {
            groups.forEachIndexed { groupIndex, group ->
                val groupStateKey = buildSectionedSwipeGroupStateKey(
                    groupIndex = groupIndex,
                    title = group.title
                )
                this[groupStateKey] =
                    enableGroupCollapse && initiallyCollapsedGroupTitles.contains(group.title)
            }
        }
    }
    var openedItemKey by remember { mutableStateOf<Any?>(null) }
    var pendingUndo by remember { mutableStateOf<PendingSwipeUndo<T>?>(null) }
    var undoToken by remember { mutableStateOf(0L) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(pendingUndo?.id) {
        val currentUndo = pendingUndo ?: return@LaunchedEffect
        delay(currentUndo.action.undoTimeoutMillis.coerceIn(1_200L, 10_000L))
        if (pendingUndo?.id == currentUndo.id) {
            pendingUndo = null
        }
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
                groups.forEachIndexed { groupIndex, group ->
                    val headerKey = "sectioned_swipe_header_${groupIndex}_${group.title}"
                    val groupStateKey = buildSectionedSwipeGroupStateKey(
                        groupIndex = groupIndex,
                        title = group.title
                    )
                    val isGroupCollapsed = collapsedStateByGroup[groupStateKey] == true
                    val isGroupExpanded = !isGroupCollapsed
                    val headerModifier = Modifier
                        .fillMaxWidth()
                        .testTag(buildSectionedSwipeHeaderTag(groupIndex))
                        .then(
                            if (enableGroupCollapse) {
                                Modifier.clickable {
                                    val nextCollapsed = !isGroupCollapsed
                                    collapsedStateByGroup[groupStateKey] = nextCollapsed
                                    if (nextCollapsed) {
                                        val openedRow = openedItemKey as? SectionedSwipeRowKey
                                        if (openedRow?.groupIndex == groupIndex) {
                                            openedItemKey = null
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                    if (isSticky) {
                        stickyHeader(key = headerKey) {
                            Box(modifier = headerModifier) {
                                groupHeaderContent(group.title, isGroupExpanded)
                            }
                        }
                    } else {
                        item(key = headerKey) {
                            Box(modifier = headerModifier) {
                                groupHeaderContent(group.title, isGroupExpanded)
                            }
                        }
                    }

                    if (isGroupCollapsed) return@forEachIndexed

                    itemsIndexed(
                        items = group.items,
                        key = { index, item ->
                            val rowIdentity = resolveSectionedSwipeItemIdentity(
                                itemKeySelector = itemKey,
                                item = item,
                                index = index
                            )
                            buildSectionedSwipeLazyKey(
                                groupIndex = groupIndex,
                                itemKey = rowIdentity
                            )
                        },
                        contentType = { _, item ->
                            itemContentType?.invoke(item)
                        }
                    ) { index, item ->
                        val rowIdentity = resolveSectionedSwipeItemIdentity(
                            itemKeySelector = itemKey,
                            item = item,
                            index = index
                        )
                        val rowKey = SectionedSwipeRowKey(
                            groupIndex = groupIndex,
                            itemKey = rowIdentity
                        )
                        val swipeEnabled = hasSwipeActions && (canSwipe?.invoke(item) ?: true)
                        val offsetX = remember(rowKey) { Animatable(0f, Float.VectorConverter) }
                        val scope = rememberCoroutineScope()

                        val minOffset = if (swipeEnabled) -endSwipeWidth else 0f
                        val maxOffset = if (swipeEnabled) startSwipeWidth else 0f
                        val startRevealProgress =
                            if (startSwipeWidth > 0f) (offsetX.value / startSwipeWidth).coerceIn(0f, 1f) else 0f
                        val endRevealProgress =
                            if (endSwipeWidth > 0f) ((-offsetX.value) / endSwipeWidth).coerceIn(0f, 1f) else 0f

                        LaunchedEffect(openedItemKey, rowKey, swipeEnabled) {
                            if ((openedItemKey != rowKey || !swipeEnabled) && offsetX.value != 0f) {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                            if (!swipeEnabled && openedItemKey == rowKey) {
                                openedItemKey = null
                            }
                        }

                        val onActionInvoked: (SwipeAction<T>, T) -> Unit = { action, targetItem ->
                            val onUndo = action.onUndo
                            if (onUndo != null) {
                                undoToken += 1
                                pendingUndo = PendingSwipeUndo(
                                    id = undoToken,
                                    action = action,
                                    item = targetItem
                                )
                            } else {
                                pendingUndo = null
                            }
                            action.onAction(targetItem)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(contentPadding)
                            ) {
                                if (backgroundContent != null) {
                                    val activeActions = resolveActiveActions(
                                        offset = offsetX.value,
                                        startActions = startActions,
                                        endActions = endActions
                                    )
                                    backgroundContent(item, activeActions)
                                } else {
                                    DefaultSwipeActionsBackground(
                                        rowIdentity = rowKey,
                                        item = item,
                                        startActions = startActions,
                                        endActions = endActions,
                                        actionSlotWidth = resolvedActionSlotWidth,
                                        startRevealProgress = startRevealProgress,
                                        endRevealProgress = endRevealProgress,
                                        onActionExecuted = {
                                            if (openedItemKey == rowKey) {
                                                openedItemKey = null
                                            }
                                            scope.launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                            }
                                        },
                                        onActionInvoked = onActionInvoked
                                    )
                                }
                            }

                            val gestureModifier = if (swipeEnabled) {
                                Modifier.pointerInput(
                                    rowKey,
                                    startSwipeWidth,
                                    endSwipeWidth,
                                    resolvedOpenThreshold,
                                    enableFullSwipeAction,
                                    resolvedFullSwipeTriggerFraction,
                                    startActions,
                                    endActions
                                ) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            openedItemKey = rowKey
                                        },
                                        onHorizontalDrag = { change, delta ->
                                            change.consume()
                                            scope.launch {
                                                val newOffset = calculateResistantOffset(
                                                    current = offsetX.value,
                                                    delta = delta,
                                                    min = minOffset,
                                                    max = maxOffset
                                                )
                                                offsetX.snapTo(newOffset)
                                            }
                                        },
                                        onDragEnd = {
                                            scope.launch {
                                                val fullSwipeAction = resolveFullSwipeAction(
                                                    current = offsetX.value,
                                                    startWidth = startSwipeWidth,
                                                    endWidth = endSwipeWidth,
                                                    startActions = startActions,
                                                    endActions = endActions,
                                                    enableFullSwipeAction = enableFullSwipeAction,
                                                    triggerFraction = resolvedFullSwipeTriggerFraction
                                                )
                                                if (fullSwipeAction != null) {
                                                    offsetX.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                    )
                                                    openedItemKey = null
                                                    if (fullSwipeAction.enableHapticFeedback) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    onActionInvoked(fullSwipeAction, item)
                                                } else {
                                                    val target = resolveSwipeTargetOffset(
                                                        current = offsetX.value,
                                                        startWidth = startSwipeWidth,
                                                        endWidth = endSwipeWidth,
                                                        openThresholdFraction = resolvedOpenThreshold
                                                    )
                                                    offsetX.animateTo(
                                                        targetValue = target,
                                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                    )
                                                    openedItemKey = if (target != 0f) rowKey else null
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            scope.launch {
                                                val target = resolveSwipeTargetOffset(
                                                    current = offsetX.value,
                                                    startWidth = startSwipeWidth,
                                                    endWidth = endSwipeWidth,
                                                    openThresholdFraction = resolvedOpenThreshold
                                                )
                                                offsetX.animateTo(
                                                    targetValue = target,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                                openedItemKey = if (target != 0f) rowKey else null
                                            }
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .fillMaxWidth()
                                    .padding(contentPadding)
                                    .then(gestureModifier)
                            ) {
                                itemContent(item)
                            }
                        }
                    }
                }
            }
        )

        val currentUndo = pendingUndo
        AnimatedVisibility(
            visible = currentUndo != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            if (currentUndo != null) {
                SwipeUndoBar(
                    action = currentUndo.action,
                    item = currentUndo.item,
                    onUndoClick = {
                        val undo = pendingUndo ?: return@SwipeUndoBar
                        pendingUndo = null
                        undo.action.onUndo?.invoke(undo.item)
                    },
                    onDismiss = {
                        pendingUndo = null
                    }
                )
            }
        }
    }
}
