package com.ghn.composelistkit.listkit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.config.BatchSelectionAction
import com.ghn.composelistkit.config.ComposeListKitConfig
import com.ghn.composelistkit.config.ComposeListKitMode
import com.ghn.composelistkit.config.DividerConfig
import com.ghn.composelistkit.config.GroupedItems
import com.ghn.composelistkit.config.ListDataConfig
import com.ghn.composelistkit.config.ListLayoutConfig
import com.ghn.composelistkit.config.ListLayoutStyle
import com.ghn.composelistkit.config.LoadMoreConfig
import com.ghn.composelistkit.config.LoadMoreStyle
import com.ghn.composelistkit.config.MultiSelectBarState
import com.ghn.composelistkit.config.MultiSelectItemStyle
import com.ghn.composelistkit.config.PageStateConfig
import com.ghn.composelistkit.config.RefreshConfig
import com.ghn.composelistkit.config.SelectionMode
import com.ghn.composelistkit.config.SwipeAction

@DslMarker
annotation class ComposeListKitDsl

@ComposeListKitDsl
class ComposeListKitBuilder<T> {
    private enum class ModeType { PLAIN, SECTIONED, SECTIONED_SWIPE, SWIPE, DRAG, COMMENT_THREAD, MULTI_SELECT }

    private var dataConfig = ListDataConfig<T>()
    private var pageStateConfig = PageStateConfig()
    private var refreshConfig = RefreshConfig()
    private var loadMoreConfig = LoadMoreConfig()

    private var plainHeader: (@Composable () -> Unit)? = null
    private var plainFooter: (@Composable () -> Unit)? = null

    private var sectionGroups: List<GroupedItems<T>> = emptyList()
    private var sectionSticky = true
    private var sectionHeader: (@Composable (String) -> Unit)? = null
    private var sectionedSwipeEnableGroupCollapse = false
    private var sectionedSwipeInitiallyCollapsedGroupTitles: Set<String> = emptySet()
    private var sectionedSwipeHeaderState: (@Composable (String, Boolean) -> Unit)? = null

    private var swipeStartActions: List<SwipeAction<T>> = emptyList()
    private var swipeEndActions: List<SwipeAction<T>> = emptyList()
    private var swipeActionSlotWidth: Dp = 92.dp
    private var swipeOpenThresholdFraction: Float = 0.35f
    private var swipeEnableFullSwipeAction = false
    private var swipeFullSwipeTriggerFraction: Float = 0.86f
    private var swipeCanSwipe: ((T) -> Boolean)? = null
    private var swipePadding: PaddingValues = PaddingValues(0.dp)
    private var swipeBackground: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null

    private var dragUseLongPress = false
    private var dragEnableHighlight = true
    private var dragHandleContent: (@Composable () -> Unit)? = null
    private var dragItemRenderer: (@Composable (T, Boolean) -> Unit)? = null
    private var dragOnMove: ((Int, Int) -> Unit)? = null
    private var dragOnReordered: ((List<T>) -> Unit)? = null

    private var commentChildrenSelector: ((T) -> List<T>)? = null
    private var commentMaxDepth: Int = 3
    private var commentInitialExpandedDepth: Int = 2
    private var commentIndentPerLevel: Dp = 16.dp
    private var commentContentPadding: PaddingValues = PaddingValues(0.dp)
    private var commentItemRenderer: (@Composable (T, Int, Boolean, Boolean, () -> Unit) -> Unit)? = null

    private var multiSelectActions: List<BatchSelectionAction<T>> = emptyList()
    private var multiSelectMaxSelectionCount: Int = Int.MAX_VALUE
    private var multiSelectSelectionMode: SelectionMode = SelectionMode.Multi
    private var multiSelectShowSelectionIndicator = true
    private var multiSelectShowSelectionBar = true
    private var multiSelectEnableSelectAllAction = false
    private var multiSelectEnableInvertSelectionAction = false
    private var multiSelectItemStyle: MultiSelectItemStyle = MultiSelectItemStyle()
    private var multiSelectIndicatorContent: (@Composable (Boolean, SelectionMode, () -> Unit) -> Unit)? = null
    private var multiSelectBarContent: (@Composable (MultiSelectBarState<T>) -> Unit)? = null
    private var multiSelectContentPadding: PaddingValues = PaddingValues(0.dp)
    private var multiSelectItemRenderer: (@Composable (T, Boolean) -> Unit)? = null
    private var multiSelectOnSelectionChanged: ((List<T>) -> Unit)? = null

    private var modeType = ModeType.PLAIN
    private var lazyListState: LazyListState? = null

    private fun activateMode(target: ModeType) {
        if (target == ModeType.PLAIN) {
            check(modeType == ModeType.PLAIN) {
                "ComposeListKit mode conflict: mode=$modeType, target=$target. " +
                        "Only one of plain/sectioned/sectionedSwipe/swipeActions/dragReorder/commentThread/multiSelect can be active."
            }
            return
        }
        check(modeType == ModeType.PLAIN || modeType == target) {
            "ComposeListKit mode conflict: mode=$modeType, target=$target. " +
                    "Only one of plain/sectioned/sectionedSwipe/swipeActions/dragReorder/commentThread/multiSelect can be active."
        }
        modeType = target
    }

    @ComposeListKitDsl
    inner class DataScope {
        fun list(
            items: List<T>,
            keySelector: ((T) -> Any)? = null,
            contentTypeSelector: ((T) -> Any?)? = null,
            modifier: Modifier? = null,
            itemContent: (@Composable (T) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                items = items,
                keySelector = keySelector ?: this@ComposeListKitBuilder.dataConfig.keySelector,
                contentTypeSelector = contentTypeSelector ?: this@ComposeListKitBuilder.dataConfig.contentTypeSelector,
                modifier = modifier ?: this@ComposeListKitBuilder.dataConfig.modifier,
                itemContent = itemContent ?: this@ComposeListKitBuilder.dataConfig.itemContent
            )
        }

        fun items(items: List<T>) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                items = items
            )
        }

        fun key(selector: (T) -> Any) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                keySelector = selector
            )
        }

        fun contentType(selector: (T) -> Any?) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                contentTypeSelector = selector
            )
        }

        fun modifier(modifier: Modifier) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                modifier = modifier
            )
        }

        fun item(content: @Composable (T) -> Unit) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                itemContent = content
            )
        }

        fun layout(
            style: ListLayoutStyle,
            contentPadding: PaddingValues = this@ComposeListKitBuilder.dataConfig.layout.contentPadding,
            mainAxisSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.mainAxisSpacing,
            crossAxisSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.crossAxisSpacing,
            reverseLayout: Boolean = this@ComposeListKitBuilder.dataConfig.layout.reverseLayout
        ) {
            val resolvedStyle = when (style) {
                is ListLayoutStyle.Grid -> style.copy(columns = style.columns.coerceAtLeast(1))
                is ListLayoutStyle.Staggered -> style.copy(columns = style.columns.coerceAtLeast(1))
                is ListLayoutStyle.Flow -> style.copy(maxItemsInEachRow = style.maxItemsInEachRow.coerceAtLeast(1))
                ListLayoutStyle.Horizontal -> style
                ListLayoutStyle.Vertical -> style
            }
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                layout = ListLayoutConfig(
                    style = resolvedStyle,
                    contentPadding = contentPadding,
                    mainAxisSpacing = mainAxisSpacing,
                    crossAxisSpacing = crossAxisSpacing,
                    reverseLayout = reverseLayout
                )
            )
        }

        fun vertical(
            contentPadding: PaddingValues = this@ComposeListKitBuilder.dataConfig.layout.contentPadding,
            itemSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.mainAxisSpacing,
            reverseLayout: Boolean = this@ComposeListKitBuilder.dataConfig.layout.reverseLayout
        ) = layout(
            style = ListLayoutStyle.Vertical,
            contentPadding = contentPadding,
            mainAxisSpacing = itemSpacing,
            crossAxisSpacing = this@ComposeListKitBuilder.dataConfig.layout.crossAxisSpacing,
            reverseLayout = reverseLayout
        )

        fun horizontal(
            contentPadding: PaddingValues = this@ComposeListKitBuilder.dataConfig.layout.contentPadding,
            itemSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.mainAxisSpacing,
            reverseLayout: Boolean = this@ComposeListKitBuilder.dataConfig.layout.reverseLayout
        ) = layout(
            style = ListLayoutStyle.Horizontal,
            contentPadding = contentPadding,
            mainAxisSpacing = itemSpacing,
            crossAxisSpacing = this@ComposeListKitBuilder.dataConfig.layout.crossAxisSpacing,
            reverseLayout = reverseLayout
        )

        fun grid(
            columns: Int,
            contentPadding: PaddingValues = this@ComposeListKitBuilder.dataConfig.layout.contentPadding,
            verticalSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.mainAxisSpacing,
            horizontalSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.crossAxisSpacing
        ) = layout(
            style = ListLayoutStyle.Grid(columns = columns),
            contentPadding = contentPadding,
            mainAxisSpacing = verticalSpacing,
            crossAxisSpacing = horizontalSpacing
        )

        fun staggered(
            columns: Int,
            contentPadding: PaddingValues = this@ComposeListKitBuilder.dataConfig.layout.contentPadding,
            verticalSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.mainAxisSpacing,
            horizontalSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.crossAxisSpacing
        ) = layout(
            style = ListLayoutStyle.Staggered(columns = columns),
            contentPadding = contentPadding,
            mainAxisSpacing = verticalSpacing,
            crossAxisSpacing = horizontalSpacing
        )

        fun flow(
            maxItemsInEachRow: Int = 3,
            contentPadding: PaddingValues = this@ComposeListKitBuilder.dataConfig.layout.contentPadding,
            verticalSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.mainAxisSpacing,
            horizontalSpacing: Dp = this@ComposeListKitBuilder.dataConfig.layout.crossAxisSpacing
        ) = layout(
            style = ListLayoutStyle.Flow(maxItemsInEachRow = maxItemsInEachRow),
            contentPadding = contentPadding,
            mainAxisSpacing = verticalSpacing,
            crossAxisSpacing = horizontalSpacing
        )

        fun divider(
            enabled: Boolean = true,
            thickness: Dp = 0.5.dp,
            color: Color = this@ComposeListKitBuilder.dataConfig.divider.color,
            startIndent: Dp = 0.dp,
            endIndent: Dp = 0.dp
        ) {
            this@ComposeListKitBuilder.dataConfig = this@ComposeListKitBuilder.dataConfig.copy(
                divider = DividerConfig(
                    enabled = enabled,
                    thickness = thickness,
                    color = color,
                    startIndent = startIndent,
                    endIndent = endIndent
                )
            )
        }
    }

    @ComposeListKitDsl
    inner class StateScope {
        fun loading(isLoading: Boolean) {
            this@ComposeListKitBuilder.pageStateConfig =
                this@ComposeListKitBuilder.pageStateConfig.copy(isLoadingFirstPage = isLoading)
        }

        fun error(
            isError: Boolean,
            onRetry: (() -> Unit)? = this@ComposeListKitBuilder.pageStateConfig.onRetry
        ) {
            this@ComposeListKitBuilder.pageStateConfig =
                this@ComposeListKitBuilder.pageStateConfig.copy(
                isError = isError,
                onRetry = onRetry
            )
        }

        fun onRetry(action: () -> Unit) {
            this@ComposeListKitBuilder.pageStateConfig =
                this@ComposeListKitBuilder.pageStateConfig.copy(onRetry = action)
        }

        fun slots(
            empty: (@Composable () -> Unit)? = null,
            error: (@Composable () -> Unit)? = null,
            loading: (@Composable () -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.pageStateConfig =
                this@ComposeListKitBuilder.pageStateConfig.copy(
                emptyContent = empty ?: this@ComposeListKitBuilder.pageStateConfig.emptyContent,
                errorContent = error ?: this@ComposeListKitBuilder.pageStateConfig.errorContent,
                loadingContent = loading ?: this@ComposeListKitBuilder.pageStateConfig.loadingContent
            )
        }
    }

    @ComposeListKitDsl
    inner class RefreshScope {
        fun refreshing(value: Boolean) {
            this@ComposeListKitBuilder.refreshConfig =
                this@ComposeListKitBuilder.refreshConfig.copy(isRefreshing = value)
        }

        fun onRefresh(action: () -> Unit) {
            this@ComposeListKitBuilder.refreshConfig =
                this@ComposeListKitBuilder.refreshConfig.copy(onRefresh = action)
        }

        fun bind(
            isRefreshing: Boolean,
            onRefresh: () -> Unit
        ) {
            this@ComposeListKitBuilder.refreshConfig =
                this@ComposeListKitBuilder.refreshConfig.copy(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }

        fun indicator(content: @Composable (Boolean) -> Unit) {
            this@ComposeListKitBuilder.refreshConfig =
                this@ComposeListKitBuilder.refreshConfig.copy(
                    indicatorContent = content
                )
        }
    }

    @ComposeListKitDsl
    inner class PagingScope {
        fun loading(value: Boolean) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(isLoadingMore = value)
        }

        fun error(value: Boolean) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(isLoadMoreError = value)
        }

        fun hasMore(value: Boolean) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(hasMore = value)
        }

        fun prefetchDistance(value: Int) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(prefetchDistance = value.coerceAtLeast(0))
        }

        fun onLoadMore(action: () -> Unit) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(onLoadMore = action)
        }

        fun onRetry(action: () -> Unit) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(onRetryLoadMore = action)
        }

        fun slots(
            loading: (@Composable () -> Unit)? = null,
            error: (@Composable ((() -> Unit)?) -> Unit)? = null,
            noMore: (@Composable () -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(
                    loadingMoreContent = loading ?: this@ComposeListKitBuilder.loadMoreConfig.loadingMoreContent,
                    loadMoreErrorContent = error ?: this@ComposeListKitBuilder.loadMoreConfig.loadMoreErrorContent,
                    noMoreContent = noMore ?: this@ComposeListKitBuilder.loadMoreConfig.noMoreContent
                )
        }

        fun style(
            loadingIndicatorColor: Color? = this@ComposeListKitBuilder.loadMoreConfig.style.loadingIndicatorColor,
            loadingText: String? = this@ComposeListKitBuilder.loadMoreConfig.style.loadingText,
            loadingTextColor: Color? = this@ComposeListKitBuilder.loadMoreConfig.style.loadingTextColor,
            errorText: String = this@ComposeListKitBuilder.loadMoreConfig.style.errorText,
            errorRetryText: String = this@ComposeListKitBuilder.loadMoreConfig.style.errorRetryText,
            errorTextColor: Color? = this@ComposeListKitBuilder.loadMoreConfig.style.errorTextColor,
            noMoreText: String = this@ComposeListKitBuilder.loadMoreConfig.style.noMoreText,
            noMoreTextColor: Color? = this@ComposeListKitBuilder.loadMoreConfig.style.noMoreTextColor
        ) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(
                    style = LoadMoreStyle(
                        loadingIndicatorColor = loadingIndicatorColor,
                        loadingText = loadingText,
                        loadingTextColor = loadingTextColor,
                        errorText = errorText,
                        errorRetryText = errorRetryText,
                        errorTextColor = errorTextColor,
                        noMoreText = noMoreText,
                        noMoreTextColor = noMoreTextColor
                    )
                )
        }

        fun style(style: LoadMoreStyle) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(style = style)
        }

        fun bind(
            isLoadingMore: Boolean,
            hasMore: Boolean = this@ComposeListKitBuilder.loadMoreConfig.hasMore,
            isLoadMoreError: Boolean = this@ComposeListKitBuilder.loadMoreConfig.isLoadMoreError,
            prefetchDistance: Int = this@ComposeListKitBuilder.loadMoreConfig.prefetchDistance,
            onRetryLoadMore: (() -> Unit)? = this@ComposeListKitBuilder.loadMoreConfig.onRetryLoadMore,
            onLoadMore: () -> Unit
        ) {
            this@ComposeListKitBuilder.loadMoreConfig =
                this@ComposeListKitBuilder.loadMoreConfig.copy(
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    isLoadMoreError = isLoadMoreError,
                    prefetchDistance = prefetchDistance.coerceAtLeast(0),
                    onLoadMore = onLoadMore,
                    onRetryLoadMore = onRetryLoadMore
                )
        }
    }

    @ComposeListKitDsl
    inner class ModeScope {
        fun plain(block: PlainScope.() -> Unit) {
            this@ComposeListKitBuilder.activateMode(ModeType.PLAIN)
            PlainScope().apply(block)
        }

        @ComposeListKitDsl
        inner class PlainScope {
            fun header(content: @Composable () -> Unit) {
                this@ComposeListKitBuilder.plainHeader = content
            }

            fun footer(content: @Composable () -> Unit) {
                this@ComposeListKitBuilder.plainFooter = content
            }
        }

        fun <G> sectioned(
            groups: List<G>,
            groupTitleSelector: (G) -> String,
            groupItemsSelector: (G) -> List<T>,
            sticky: Boolean = true,
            groupHeaderContent: (@Composable (String) -> Unit)? = null,
            itemContent: (@Composable (T) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.SECTIONED)
            this@ComposeListKitBuilder.sectionGroups = groups.map { group ->
                GroupedItems(
                    title = groupTitleSelector(group),
                    items = groupItemsSelector(group)
                )
            }
            this@ComposeListKitBuilder.sectionSticky = sticky
            groupHeaderContent?.let { this@ComposeListKitBuilder.sectionHeader = it }
            itemContent?.let {
                this@ComposeListKitBuilder.dataConfig =
                    this@ComposeListKitBuilder.dataConfig.copy(itemContent = it)
            }
        }

        fun <G> sectionedSwipe(
            groups: List<G>,
            groupTitleSelector: (G) -> String,
            groupItemsSelector: (G) -> List<T>,
            startActions: List<SwipeAction<T>> = emptyList(),
            endActions: List<SwipeAction<T>> = emptyList(),
            sticky: Boolean = true,
            actionSlotWidth: Dp = 92.dp,
            openThresholdFraction: Float = 0.35f,
            enableFullSwipeAction: Boolean = false,
            fullSwipeTriggerFraction: Float = 0.86f,
            canSwipe: ((T) -> Boolean)? = null,
            contentPadding: PaddingValues = PaddingValues(0.dp),
            backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null,
            groupHeaderContent: (@Composable (String) -> Unit)? = null,
            itemContent: (@Composable (T) -> Unit)? = null,
            enableGroupCollapse: Boolean = false,
            initiallyCollapsedGroupTitles: Set<String> = emptySet(),
            groupHeaderStateContent: (@Composable (String, Boolean) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.SECTIONED_SWIPE)
            this@ComposeListKitBuilder.sectionGroups = groups.map { group ->
                GroupedItems(
                    title = groupTitleSelector(group),
                    items = groupItemsSelector(group)
                )
            }
            this@ComposeListKitBuilder.sectionSticky = sticky
            groupHeaderContent?.let { this@ComposeListKitBuilder.sectionHeader = it }
            this@ComposeListKitBuilder.swipeStartActions = startActions
            this@ComposeListKitBuilder.swipeEndActions = endActions
            this@ComposeListKitBuilder.swipeActionSlotWidth = actionSlotWidth.coerceAtLeast(56.dp)
            this@ComposeListKitBuilder.swipeOpenThresholdFraction = openThresholdFraction.coerceIn(0.1f, 0.9f)
            this@ComposeListKitBuilder.swipeEnableFullSwipeAction = enableFullSwipeAction
            this@ComposeListKitBuilder.swipeFullSwipeTriggerFraction =
                fullSwipeTriggerFraction.coerceIn(0.55f, 1f)
            this@ComposeListKitBuilder.swipeCanSwipe = canSwipe
            this@ComposeListKitBuilder.swipePadding = contentPadding
            backgroundContent?.let { this@ComposeListKitBuilder.swipeBackground = it }
            this@ComposeListKitBuilder.sectionedSwipeEnableGroupCollapse = enableGroupCollapse
            this@ComposeListKitBuilder.sectionedSwipeInitiallyCollapsedGroupTitles =
                initiallyCollapsedGroupTitles
            this@ComposeListKitBuilder.sectionedSwipeHeaderState = groupHeaderStateContent
            itemContent?.let {
                this@ComposeListKitBuilder.dataConfig =
                    this@ComposeListKitBuilder.dataConfig.copy(itemContent = it)
            }
        }

        fun swipeActions(
            actions: List<SwipeAction<T>>,
            contentPadding: PaddingValues = PaddingValues(0.dp),
            actionSlotWidth: Dp = 92.dp,
            openThresholdFraction: Float = 0.35f,
            enableFullSwipeAction: Boolean = false,
            fullSwipeTriggerFraction: Float = 0.86f,
            canSwipe: ((T) -> Boolean)? = null,
            backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.SWIPE)
            this@ComposeListKitBuilder.swipeStartActions = emptyList()
            this@ComposeListKitBuilder.swipeEndActions = actions
            this@ComposeListKitBuilder.swipeActionSlotWidth = actionSlotWidth.coerceAtLeast(56.dp)
            this@ComposeListKitBuilder.swipeOpenThresholdFraction = openThresholdFraction.coerceIn(0.1f, 0.9f)
            this@ComposeListKitBuilder.swipeEnableFullSwipeAction = enableFullSwipeAction
            this@ComposeListKitBuilder.swipeFullSwipeTriggerFraction =
                fullSwipeTriggerFraction.coerceIn(0.55f, 1f)
            this@ComposeListKitBuilder.swipeCanSwipe = canSwipe
            this@ComposeListKitBuilder.swipePadding = contentPadding
            backgroundContent?.let { this@ComposeListKitBuilder.swipeBackground = it }
        }

        fun swipeActions(
            vararg actions: SwipeAction<T>,
            contentPadding: PaddingValues = PaddingValues(0.dp),
            actionSlotWidth: Dp = 92.dp,
            openThresholdFraction: Float = 0.35f,
            enableFullSwipeAction: Boolean = false,
            fullSwipeTriggerFraction: Float = 0.86f,
            canSwipe: ((T) -> Boolean)? = null,
            backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null
        ) = swipeActions(
            actions = actions.toList(),
            contentPadding = contentPadding,
            actionSlotWidth = actionSlotWidth,
            openThresholdFraction = openThresholdFraction,
            enableFullSwipeAction = enableFullSwipeAction,
            fullSwipeTriggerFraction = fullSwipeTriggerFraction,
            canSwipe = canSwipe,
            backgroundContent = backgroundContent
        )

        fun swipeActionsBidirectional(
            startActions: List<SwipeAction<T>> = emptyList(),
            endActions: List<SwipeAction<T>> = emptyList(),
            contentPadding: PaddingValues = PaddingValues(0.dp),
            actionSlotWidth: Dp = 92.dp,
            openThresholdFraction: Float = 0.35f,
            enableFullSwipeAction: Boolean = false,
            fullSwipeTriggerFraction: Float = 0.86f,
            canSwipe: ((T) -> Boolean)? = null,
            backgroundContent: (@Composable (T, List<SwipeAction<T>>) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.SWIPE)
            this@ComposeListKitBuilder.swipeStartActions = startActions
            this@ComposeListKitBuilder.swipeEndActions = endActions
            this@ComposeListKitBuilder.swipeActionSlotWidth = actionSlotWidth.coerceAtLeast(56.dp)
            this@ComposeListKitBuilder.swipeOpenThresholdFraction = openThresholdFraction.coerceIn(0.1f, 0.9f)
            this@ComposeListKitBuilder.swipeEnableFullSwipeAction = enableFullSwipeAction
            this@ComposeListKitBuilder.swipeFullSwipeTriggerFraction =
                fullSwipeTriggerFraction.coerceIn(0.55f, 1f)
            this@ComposeListKitBuilder.swipeCanSwipe = canSwipe
            this@ComposeListKitBuilder.swipePadding = contentPadding
            backgroundContent?.let { this@ComposeListKitBuilder.swipeBackground = it }
        }

        fun dragReorder(
            useLongPress: Boolean = false,
            enableHighlight: Boolean = true,
            dragHandle: (@Composable () -> Unit)? = null,
            itemContent: (@Composable (T, Boolean) -> Unit)? = null,
            onMove: ((Int, Int) -> Unit)? = null,
            onReordered: ((List<T>) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.DRAG)
            this@ComposeListKitBuilder.dragUseLongPress = useLongPress
            this@ComposeListKitBuilder.dragEnableHighlight = enableHighlight
            this@ComposeListKitBuilder.dragHandleContent = dragHandle
            itemContent?.let { this@ComposeListKitBuilder.dragItemRenderer = it }
            this@ComposeListKitBuilder.dragOnMove = onMove
            this@ComposeListKitBuilder.dragOnReordered = onReordered
        }

        fun commentThread(
            childrenSelector: (T) -> List<T>,
            maxDepth: Int = 3,
            initialExpandedDepth: Int = 2,
            indentPerLevel: Dp = 16.dp,
            contentPadding: PaddingValues = PaddingValues(0.dp),
            itemContent: (@Composable (T, Int, Boolean, Boolean, () -> Unit) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.COMMENT_THREAD)
            this@ComposeListKitBuilder.commentChildrenSelector = childrenSelector
            this@ComposeListKitBuilder.commentMaxDepth = maxDepth.coerceAtLeast(1)
            this@ComposeListKitBuilder.commentInitialExpandedDepth = initialExpandedDepth.coerceAtLeast(1)
            this@ComposeListKitBuilder.commentIndentPerLevel = indentPerLevel
            this@ComposeListKitBuilder.commentContentPadding = contentPadding
            this@ComposeListKitBuilder.commentItemRenderer = itemContent
        }

        fun multiSelect(
            actions: List<BatchSelectionAction<T>>,
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
            itemContent: (@Composable (T, Boolean) -> Unit)? = null,
            onSelectionChanged: ((List<T>) -> Unit)? = null
        ) {
            this@ComposeListKitBuilder.activateMode(ModeType.MULTI_SELECT)
            this@ComposeListKitBuilder.multiSelectActions = actions
            this@ComposeListKitBuilder.multiSelectSelectionMode = selectionMode
            this@ComposeListKitBuilder.multiSelectMaxSelectionCount = when (selectionMode) {
                SelectionMode.Single -> 1
                SelectionMode.Multi -> maxSelectionCount.coerceAtLeast(1)
            }
            this@ComposeListKitBuilder.multiSelectShowSelectionIndicator = showSelectionIndicator
            this@ComposeListKitBuilder.multiSelectShowSelectionBar = showSelectionBar
            this@ComposeListKitBuilder.multiSelectEnableSelectAllAction = enableSelectAllAction
            this@ComposeListKitBuilder.multiSelectEnableInvertSelectionAction = enableInvertSelectionAction
            this@ComposeListKitBuilder.multiSelectItemStyle = itemStyle
            this@ComposeListKitBuilder.multiSelectIndicatorContent = indicatorContent
            this@ComposeListKitBuilder.multiSelectBarContent = selectionBarContent
            this@ComposeListKitBuilder.multiSelectContentPadding = contentPadding
            this@ComposeListKitBuilder.multiSelectItemRenderer = itemContent
            this@ComposeListKitBuilder.multiSelectOnSelectionChanged = onSelectionChanged
        }

        fun multiSelect(
            vararg actions: BatchSelectionAction<T>,
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
            itemContent: (@Composable (T, Boolean) -> Unit)? = null,
            onSelectionChanged: ((List<T>) -> Unit)? = null
        ) = multiSelect(
            actions = actions.toList(),
            maxSelectionCount = maxSelectionCount,
            selectionMode = selectionMode,
            showSelectionIndicator = showSelectionIndicator,
            showSelectionBar = showSelectionBar,
            enableSelectAllAction = enableSelectAllAction,
            enableInvertSelectionAction = enableInvertSelectionAction,
            itemStyle = itemStyle,
            indicatorContent = indicatorContent,
            selectionBarContent = selectionBarContent,
            contentPadding = contentPadding,
            itemContent = itemContent,
            onSelectionChanged = onSelectionChanged
        )
    }

    fun data(block: DataScope.() -> Unit) = apply {
        DataScope().apply(block)
    }

    fun state(block: StateScope.() -> Unit) = apply {
        StateScope().apply(block)
    }

    fun refresh(block: RefreshScope.() -> Unit) = apply {
        RefreshScope().apply(block)
    }

    fun paging(block: PagingScope.() -> Unit) = apply {
        PagingScope().apply(block)
    }

    fun mode(block: ModeScope.() -> Unit) = apply {
        ModeScope().apply(block)
    }

    fun listState(state: LazyListState) = apply {
        lazyListState = state
    }

    fun build(): ComposeListKitConfig<T> {
        val mode = when (modeType) {
            ModeType.PLAIN -> ComposeListKitMode.Plain(
                header = plainHeader,
                footer = plainFooter
            )

            ModeType.SECTIONED -> ComposeListKitMode.Sectioned(
                groups = sectionGroups,
                isSticky = sectionSticky,
                headerContent = sectionHeader
            )

            ModeType.SECTIONED_SWIPE -> {
                check(swipeStartActions.isNotEmpty() || swipeEndActions.isNotEmpty()) {
                    "sectionedSwipe mode requires at least one action."
                }
                ComposeListKitMode.SectionedSwipe(
                    groups = sectionGroups,
                    isSticky = sectionSticky,
                    headerContent = sectionHeader,
                    startActions = swipeStartActions,
                    endActions = swipeEndActions,
                    actionSlotWidth = swipeActionSlotWidth,
                    openThresholdFraction = swipeOpenThresholdFraction,
                    enableFullSwipeAction = swipeEnableFullSwipeAction,
                    fullSwipeTriggerFraction = swipeFullSwipeTriggerFraction,
                    canSwipe = swipeCanSwipe,
                    contentPadding = swipePadding,
                    backgroundContent = swipeBackground,
                    enableGroupCollapse = sectionedSwipeEnableGroupCollapse,
                    initiallyCollapsedGroupTitles = sectionedSwipeInitiallyCollapsedGroupTitles,
                    headerStateContent = sectionedSwipeHeaderState
                )
            }

            ModeType.SWIPE -> {
                check(swipeStartActions.isNotEmpty() || swipeEndActions.isNotEmpty()) {
                    "swipe mode requires at least one action. Use swipeActions(...) or swipeActionsBidirectional(...)."
                }
                ComposeListKitMode.Swipe(
                    startActions = swipeStartActions,
                    endActions = swipeEndActions,
                    actionSlotWidth = swipeActionSlotWidth,
                    openThresholdFraction = swipeOpenThresholdFraction,
                    enableFullSwipeAction = swipeEnableFullSwipeAction,
                    fullSwipeTriggerFraction = swipeFullSwipeTriggerFraction,
                    canSwipe = swipeCanSwipe,
                    contentPadding = swipePadding,
                    backgroundContent = swipeBackground
                )
            }

            ModeType.DRAG -> ComposeListKitMode.Drag(
                useLongPress = dragUseLongPress,
                enableHighlight = dragEnableHighlight,
                dragHandle = dragHandleContent,
                itemContent = dragItemRenderer ?: { item, _ ->
                    dataConfig.itemContent(item)
                },
                onMove = dragOnMove,
                onReordered = dragOnReordered
            )

            ModeType.COMMENT_THREAD -> {
                val childrenSelector = checkNotNull(commentChildrenSelector) {
                    "commentThread mode requires childrenSelector."
                }
                ComposeListKitMode.CommentThread(
                    childrenSelector = childrenSelector,
                    maxDepth = commentMaxDepth,
                    initialExpandedDepth = commentInitialExpandedDepth,
                    indentPerLevel = commentIndentPerLevel,
                    contentPadding = commentContentPadding,
                    itemContent = commentItemRenderer
                )
            }

            ModeType.MULTI_SELECT -> ComposeListKitMode.MultiSelect(
                actions = multiSelectActions,
                maxSelectionCount = multiSelectMaxSelectionCount,
                selectionMode = multiSelectSelectionMode,
                showSelectionIndicator = multiSelectShowSelectionIndicator,
                showSelectionBar = multiSelectShowSelectionBar,
                enableSelectAllAction = multiSelectEnableSelectAllAction,
                enableInvertSelectionAction = multiSelectEnableInvertSelectionAction,
                itemStyle = multiSelectItemStyle,
                indicatorContent = multiSelectIndicatorContent,
                selectionBarContent = multiSelectBarContent,
                contentPadding = multiSelectContentPadding,
                itemContent = multiSelectItemRenderer ?: { item, _ ->
                    dataConfig.itemContent(item)
                },
                onSelectionChanged = multiSelectOnSelectionChanged
            )
        }
        return ComposeListKitConfig(
            data = dataConfig,
            pageState = pageStateConfig,
            refresh = refreshConfig,
            loadMore = loadMoreConfig,
            mode = mode,
            listState = lazyListState
        )
    }
}
