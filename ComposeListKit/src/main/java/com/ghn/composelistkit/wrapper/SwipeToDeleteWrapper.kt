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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.config.SwipeAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class SwipeRevealSide {
    Start,
    End
}

internal data class PendingSwipeUndo<T>(
    val id: Long,
    val action: SwipeAction<T>,
    val item: T
)

private fun <T> resolveSwipeRowIdentity(
    keySelector: ((T) -> Any)?,
    item: T,
    index: Int
): Any {
    keySelector?.invoke(item)?.let { return it }
    val fallbackIdentity = item as Any?
    return fallbackIdentity ?: index
}

@Composable
fun <T> SimpleSwipeToDeleteWrapper(
    items: List<T>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    keySelector: ((T) -> Any)? = null,
    contentTypeSelector: ((T) -> Any?)? = null,
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

    val indexedKey = keySelector?.let { selector: (T) -> Any ->
        { _: Int, item: T -> selector(item) }
    }
    val indexedContentType: (Int, T) -> Any? = { _, item ->
        contentTypeSelector?.invoke(item)
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
                itemsIndexed(
                    items = items,
                    key = indexedKey,
                    contentType = indexedContentType
                ) { index, item ->
                    val rowKey = resolveSwipeRowIdentity(
                        keySelector = keySelector,
                        item = item,
                        index = index
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

@Composable
internal fun <T> DefaultSwipeActionsBackground(
    rowIdentity: Any,
    item: T,
    startActions: List<SwipeAction<T>>,
    endActions: List<SwipeAction<T>>,
    actionSlotWidth: Dp,
    startRevealProgress: Float,
    endRevealProgress: Float,
    onActionExecuted: () -> Unit,
    onActionInvoked: (SwipeAction<T>, T) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var pendingConfirmIdentity by remember(rowIdentity) { mutableStateOf<String?>(null) }
    val pendingAction = remember(startActions, endActions, pendingConfirmIdentity) {
        findPendingAction(
            pendingIdentity = pendingConfirmIdentity,
            startActions = startActions,
            endActions = endActions
        )
    }

    LaunchedEffect(rowIdentity, pendingConfirmIdentity, pendingAction?.confirmTimeoutMillis) {
        if (pendingConfirmIdentity != null && pendingAction != null) {
            delay(pendingAction.confirmTimeoutMillis.coerceIn(400L, 10_000L))
            pendingConfirmIdentity = null
        }
    }

    val revealProgress = max(startRevealProgress, endRevealProgress).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = colors.surfaceVariant.copy(alpha = 0.18f * revealProgress),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        SwipeActionStrip(
            side = SwipeRevealSide.Start,
            item = item,
            actions = startActions,
            actionSlotWidth = actionSlotWidth,
            revealProgress = startRevealProgress,
            pendingConfirmIdentity = pendingConfirmIdentity,
            onPendingConfirmIdentityChange = { pendingConfirmIdentity = it },
            haptic = haptic,
            onActionExecuted = onActionExecuted,
            onActionInvoked = onActionInvoked
        )
        SwipeActionStrip(
            side = SwipeRevealSide.End,
            item = item,
            actions = endActions,
            actionSlotWidth = actionSlotWidth,
            revealProgress = endRevealProgress,
            pendingConfirmIdentity = pendingConfirmIdentity,
            onPendingConfirmIdentityChange = { pendingConfirmIdentity = it },
            haptic = haptic,
            onActionExecuted = onActionExecuted,
            onActionInvoked = onActionInvoked
        )
    }
}

@Composable
internal fun <T> SwipeActionStrip(
    side: SwipeRevealSide,
    item: T,
    actions: List<SwipeAction<T>>,
    actionSlotWidth: Dp,
    revealProgress: Float,
    pendingConfirmIdentity: String?,
    onPendingConfirmIdentityChange: (String?) -> Unit,
    haptic: HapticFeedback,
    onActionExecuted: () -> Unit,
    onActionInvoked: (SwipeAction<T>, T) -> Unit
) {
    if (actions.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = if (side == SwipeRevealSide.Start) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            val staggerIndex = if (side == SwipeRevealSide.Start) {
                index
            } else {
                (actions.lastIndex - index).coerceAtLeast(0)
            }
            val revealStart = staggerIndex * 0.12f
            val localProgress =
                ((revealProgress - revealStart) / (1f - revealStart).coerceAtLeast(0.2f)).coerceIn(0f, 1f)

            val actionIdentity = "${side.name}:${action.key}"
            val isConfirming = pendingConfirmIdentity == actionIdentity
            val isDanger = when (side) {
                SwipeRevealSide.Start -> index == 0
                SwipeRevealSide.End -> index == actions.lastIndex
            }
            val defaultContainer = if (isDanger) colors.error else colors.secondaryContainer
            val defaultContent = if (isDanger) colors.onError else colors.onSecondaryContainer
            val container = if (isConfirming) colors.error else action.containerColor ?: defaultContainer
            val content = if (isConfirming) colors.onError else action.contentColor ?: defaultContent
            val actionShape = when (side) {
                SwipeRevealSide.Start -> RoundedCornerShape(
                    topStart = if (index == 0) 12.dp else 0.dp,
                    bottomStart = if (index == 0) 12.dp else 0.dp,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp
                )

                SwipeRevealSide.End -> RoundedCornerShape(
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                    topEnd = if (index == actions.lastIndex) 12.dp else 0.dp,
                    bottomEnd = if (index == actions.lastIndex) 12.dp else 0.dp
                )
            }

            Surface(
                color = container,
                contentColor = content,
                shape = actionShape,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionSlotWidth)
                    .graphicsLayer {
                        alpha = localProgress
                        scaleX = 0.92f + (0.08f * localProgress)
                        scaleY = 0.92f + (0.08f * localProgress)
                        translationX = when (side) {
                            SwipeRevealSide.Start -> -(1f - localProgress) * 24f
                            SwipeRevealSide.End -> (1f - localProgress) * 24f
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(enabled = revealProgress > 0.35f) {
                            if (pendingConfirmIdentity != null && pendingConfirmIdentity != actionIdentity) {
                                onPendingConfirmIdentityChange(null)
                            }
                            if (action.requiresConfirm && !isConfirming) {
                                onPendingConfirmIdentityChange(actionIdentity)
                                if (action.enableHapticFeedback) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else {
                                onPendingConfirmIdentityChange(null)
                                if (action.enableHapticFeedback) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onActionExecuted()
                                onActionInvoked(action, item)
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val customContent = if (isConfirming) action.confirmContent else action.content
                    if (customContent != null) {
                        customContent()
                    } else {
                        Text(
                            text = if (isConfirming) action.confirmLabel else action.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun <T> SwipeUndoBar(
    action: SwipeAction<T>,
    item: T,
    onUndoClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = action.undoMessage?.invoke(item) ?: "${action.label} 已执行",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onUndoClick) {
                Text(action.undoLabel)
            }
        }
    }
}

internal fun <T> resolveFullSwipeAction(
    current: Float,
    startWidth: Float,
    endWidth: Float,
    startActions: List<SwipeAction<T>>,
    endActions: List<SwipeAction<T>>,
    enableFullSwipeAction: Boolean,
    triggerFraction: Float
): SwipeAction<T>? {
    if (!enableFullSwipeAction) return null
    return when {
        current > 0f && startWidth > 0f && current >= startWidth * triggerFraction -> {
            startActions.firstOrNull { !it.requiresConfirm }
        }

        current < 0f && endWidth > 0f && abs(current) >= endWidth * triggerFraction -> {
            endActions.lastOrNull { !it.requiresConfirm }
        }

        else -> null
    }
}

internal fun <T> resolveActiveActions(
    offset: Float,
    startActions: List<SwipeAction<T>>,
    endActions: List<SwipeAction<T>>
): List<SwipeAction<T>> {
    return when {
        offset > 0f && startActions.isNotEmpty() -> startActions
        offset < 0f && endActions.isNotEmpty() -> endActions
        endActions.isNotEmpty() -> endActions
        else -> startActions
    }
}

internal fun <T> findPendingAction(
    pendingIdentity: String?,
    startActions: List<SwipeAction<T>>,
    endActions: List<SwipeAction<T>>
): SwipeAction<T>? {
    if (pendingIdentity == null) return null
    return startActions.firstOrNull { "Start:${it.key}" == pendingIdentity }
        ?: endActions.firstOrNull { "End:${it.key}" == pendingIdentity }
}

internal fun resolveSwipeTargetOffset(
    current: Float,
    startWidth: Float,
    endWidth: Float,
    openThresholdFraction: Float
): Float {
    return when {
        current > 0f && startWidth > 0f -> {
            if (current > startWidth * openThresholdFraction) startWidth else 0f
        }

        current < 0f && endWidth > 0f -> {
            if (abs(current) > endWidth * openThresholdFraction) -endWidth else 0f
        }

        else -> 0f
    }
}

internal fun calculateResistantOffset(
    current: Float,
    delta: Float,
    min: Float,
    max: Float,
    resistanceFactor: Float = 0.36f
): Float {
    val next = current + delta
    return when {
        next < min -> min + (next - min) * resistanceFactor
        next > max -> max + (next - max) * resistanceFactor
        else -> next
    }
}
