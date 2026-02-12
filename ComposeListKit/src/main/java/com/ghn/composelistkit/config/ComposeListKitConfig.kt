package com.ghn.composelistkit.config

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ComposeListKitConfig<T>(
    val data: ListDataConfig<T> = ListDataConfig(),
    val pageState: PageStateConfig = PageStateConfig(),
    val refresh: RefreshConfig = RefreshConfig(),
    val loadMore: LoadMoreConfig = LoadMoreConfig(),
    val mode: ComposeListKitMode<T> = ComposeListKitMode.Plain(),
    val listState: LazyListState? = null
)

data class ListDataConfig<T>(
    val items: List<T> = emptyList(),
    val keySelector: ((T) -> Any)? = null,
    val contentTypeSelector: ((T) -> Any?)? = null,
    val modifier: Modifier = Modifier,
    val layout: ListLayoutConfig = ListLayoutConfig(),
    val divider: DividerConfig = DividerConfig(),
    val itemContent: @Composable (T) -> Unit = { _ -> }
)

data class ListLayoutConfig(
    val style: ListLayoutStyle = ListLayoutStyle.Vertical,
    val contentPadding: PaddingValues = PaddingValues(0.dp),
    val mainAxisSpacing: Dp = 0.dp,
    val crossAxisSpacing: Dp = 0.dp,
    val reverseLayout: Boolean = false
)

sealed interface ListLayoutStyle {
    data object Vertical : ListLayoutStyle
    data object Horizontal : ListLayoutStyle
    data class Grid(val columns: Int = 2) : ListLayoutStyle
    data class Staggered(val columns: Int = 2) : ListLayoutStyle
    data class Flow(val maxItemsInEachRow: Int = 3) : ListLayoutStyle
}

data class DividerConfig(
    val enabled: Boolean = false,
    val thickness: Dp = 0.5.dp,
    val color: Color = Color.Unspecified,
    val startIndent: Dp = 0.dp,
    val endIndent: Dp = 0.dp
)

data class PageStateConfig(
    val isLoadingFirstPage: Boolean = false,
    val isError: Boolean = false,
    val onRetry: (() -> Unit)? = null,
    val loadingContent: (@Composable () -> Unit)? = null,
    val errorContent: (@Composable () -> Unit)? = null,
    val emptyContent: (@Composable () -> Unit)? = null
)

data class RefreshConfig(
    val isRefreshing: Boolean = false,
    val onRefresh: (() -> Unit)? = null,
    val indicatorContent: (@Composable (Boolean) -> Unit)? = null
)

data class LoadMoreConfig(
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val isLoadMoreError: Boolean = false,
    val prefetchDistance: Int = 0,
    val onLoadMore: (() -> Unit)? = null,
    val onRetryLoadMore: (() -> Unit)? = null,
    val loadingMoreContent: (@Composable () -> Unit)? = null,
    val loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    val noMoreContent: (@Composable () -> Unit)? = null,
    val style: LoadMoreStyle = LoadMoreStyle()
)

data class LoadMoreStyle(
    val loadingIndicatorColor: Color? = null,
    val loadingText: String? = null,
    val loadingTextColor: Color? = null,
    val errorText: String = "加载失败",
    val errorRetryText: String = "加载失败，点击重试",
    val errorTextColor: Color? = null,
    val noMoreText: String = "没有更多了",
    val noMoreTextColor: Color? = null
)

data class SwipeAction<T>(
    val key: String,
    val label: String,
    val onAction: (T) -> Unit,
    val onUndo: ((T) -> Unit)? = null,
    val undoLabel: String = "撤销",
    val undoMessage: ((T) -> String)? = null,
    val undoTimeoutMillis: Long = 3200L,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
    val content: (@Composable () -> Unit)? = null,
    val requiresConfirm: Boolean = false,
    val confirmLabel: String = "确认",
    val confirmContent: (@Composable () -> Unit)? = null,
    val confirmTimeoutMillis: Long = 1800L,
    val enableHapticFeedback: Boolean = false
)

data class BatchSelectionAction<T>(
    val key: String,
    val label: String,
    val onAction: (List<T>) -> Unit,
    val clearSelectionAfterAction: Boolean = true
)

data class MultiSelectItemStyle(
    val selectedContainerColor: Color? = null,
    val unselectedContainerColor: Color? = null,
    val selectedContainerAlpha: Float = 0.30f,
    val unselectedContainerAlpha: Float = 1f,
    val cornerRadius: Dp = 12.dp,
    val rowOuterVerticalPadding: Dp = 4.dp,
    val rowInnerHorizontalPadding: Dp = 8.dp,
    val rowInnerVerticalPadding: Dp = 4.dp
)

data class MultiSelectBarState<T>(
    val selectedItems: List<T>,
    val selectedCount: Int,
    val selectionMode: SelectionMode,
    val clearSelection: () -> Unit,
    val selectAll: (() -> Unit)?,
    val invertSelection: (() -> Unit)?,
    val executeAction: (BatchSelectionAction<T>) -> Unit
)

enum class SelectionMode {
    Single,
    Multi
}

sealed interface ComposeListKitMode<T> {
    data class Plain<T>(
        val header: (@Composable () -> Unit)? = null,
        val footer: (@Composable () -> Unit)? = null
    ) : ComposeListKitMode<T>

    data class Sectioned<T>(
        val groups: List<GroupedItems<T>> = emptyList(),
        val isSticky: Boolean = true,
        val headerContent: (@Composable (String) -> Unit)? = null
    ) : ComposeListKitMode<T>

    data class SectionedSwipe<T>(
        val groups: List<GroupedItems<T>> = emptyList(),
        val isSticky: Boolean = true,
        val headerContent: (@Composable (String) -> Unit)? = null,
        val startActions: List<SwipeAction<T>> = emptyList(),
        val endActions: List<SwipeAction<T>> = emptyList(),
        val actionSlotWidth: Dp = 92.dp,
        val openThresholdFraction: Float = 0.35f,
        val enableFullSwipeAction: Boolean = false,
        val fullSwipeTriggerFraction: Float = 0.86f,
        val canSwipe: ((T) -> Boolean)? = null,
        val contentPadding: PaddingValues = PaddingValues(0.dp),
        val backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null,
        val enableGroupCollapse: Boolean = false,
        val initiallyCollapsedGroupTitles: Set<String> = emptySet(),
        val headerStateContent: (@Composable (String, Boolean) -> Unit)? = null
    ) : ComposeListKitMode<T>

    data class Swipe<T>(
        val startActions: List<SwipeAction<T>> = emptyList(),
        val endActions: List<SwipeAction<T>> = emptyList(),
        val actionSlotWidth: Dp = 92.dp,
        val openThresholdFraction: Float = 0.35f,
        val enableFullSwipeAction: Boolean = false,
        val fullSwipeTriggerFraction: Float = 0.86f,
        val canSwipe: ((T) -> Boolean)? = null,
        val contentPadding: PaddingValues = PaddingValues(0.dp),
        val backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null
    ) : ComposeListKitMode<T>

    data class Drag<T>(
        val useLongPress: Boolean = false,
        val enableHighlight: Boolean = true,
        val dragHandle: (@Composable () -> Unit)? = null,
        val itemContent: (@Composable (T, Boolean) -> Unit)? = null,
        val onMove: ((Int, Int) -> Unit)? = null,
        val onReordered: ((List<T>) -> Unit)? = null
    ) : ComposeListKitMode<T>

    data class CommentThread<T>(
        val childrenSelector: (T) -> List<T>,
        val maxDepth: Int = 3,
        val initialExpandedDepth: Int = 2,
        val indentPerLevel: Dp = 16.dp,
        val contentPadding: PaddingValues = PaddingValues(0.dp),
        val itemContent: (@Composable (T, Int, Boolean, Boolean, () -> Unit) -> Unit)? = null
    ) : ComposeListKitMode<T>

    data class MultiSelect<T>(
        val actions: List<BatchSelectionAction<T>> = emptyList(),
        val maxSelectionCount: Int = Int.MAX_VALUE,
        val selectionMode: SelectionMode = SelectionMode.Multi,
        val showSelectionIndicator: Boolean = true,
        val showSelectionBar: Boolean = true,
        val enableSelectAllAction: Boolean = false,
        val enableInvertSelectionAction: Boolean = false,
        val itemStyle: MultiSelectItemStyle = MultiSelectItemStyle(),
        val indicatorContent: (@Composable (Boolean, SelectionMode, () -> Unit) -> Unit)? = null,
        val selectionBarContent: (@Composable (MultiSelectBarState<T>) -> Unit)? = null,
        val contentPadding: PaddingValues = PaddingValues(0.dp),
        val itemContent: (@Composable (T, Boolean) -> Unit)? = null,
        val onSelectionChanged: ((List<T>) -> Unit)? = null
    ) : ComposeListKitMode<T>
}

data class GroupedItems<T>(
    val title: String,
    val items: List<T>
)
