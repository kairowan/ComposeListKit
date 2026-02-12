# ComposeListKit

一个面向 Jetpack Compose 的列表 DSL 组件库。

支持能力：

- 普通列表（含 Header/Footer）
- 首屏状态（Loading/Error/Empty）
- 下拉刷新 + 分页加载（Loading/Error/NoMore）
- 分组列表（可吸顶）
- 侧滑删除
- 拖拽排序（支持状态外提回调）

## 环境要求

- Kotlin: `2.0.21`
- AGP: `8.9.1`
- compileSdk: `35`
- minSdk: `24`
- Java/Kotlin target: `11`

## 接入

```kotlin
dependencies {
    implementation(project(":ComposeListKit"))
}
```

## DSL 结构

统一使用以下分块入口：

- `data { ... }`：列表数据与 item 渲染（也可配置 `key(...)`、`contentType(...)`、`layout(...)`、`divider(...)`）
- `state { ... }`：首屏状态与状态页面 slots
- `refresh { ... }`：下拉刷新（支持 `indicator { ... }` 自定义指示器）
- `paging { ... }`：分页加载（支持 `style(...)` / `slots(...)`）
- `mode { ... }`：列表模式（plain/sectioned/sectionedSwipe/swipeActions/dragReorder/commentThread/multiSelect）
- `listState(state)`：外部传入 `LazyListState`

## 最小示例

```kotlin
ComposeListKit<String> {
    data {
        items(items)
        key { it }
        contentType { "text" }
        item { value ->
            Text(value)
        }
    }
}
```

## 功能示例

### 1) Header / Footer + 首屏状态

```kotlin
ComposeListKit<String> {
    data {
        list(items = items, modifier = Modifier.fillMaxSize())
        item { value -> Text(value) }
    }

    state {
        loading(isLoadingFirstPage)
        error(isError, onRetry = { reload() })
        slots(
            empty = { EmptyView() },
            error = { ErrorView() },
            loading = { LoadingView() }
        )
    }

    mode {
        plain {
            header { HeaderView() }
            footer { FooterView() }
        }
    }
}
```

### 2) 下拉刷新 + 分页

```kotlin
ComposeListKit<String> {
    data {
        items(items)
        item { value -> Text(value) }
    }

    refresh {
        bind(
            isRefreshing = isRefreshing,
            onRefresh = { refresh() }
        )

        // 自定义下拉刷新指示器样式
        indicator { refreshing ->
            Text(
                text = if (refreshing) "正在刷新..." else "下拉刷新",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }

    paging {
        bind(
            isLoadingMore = isLoadingMore,
            hasMore = hasMore,
            isLoadMoreError = isLoadMoreError,
            prefetchDistance = 2,
            onRetryLoadMore = { retryLoadMore() },
            onLoadMore = { loadMore() }
        )

        // 自定义默认加载更多样式（不写 slots 时生效）
        style(
            loadingIndicatorColor = Color(0xFF5B8CFF),
            loadingText = "正在加载更多...",
            loadingTextColor = Color(0xFF5B8CFF),
            errorText = "加载失败",
            errorRetryText = "加载失败，点击重试",
            errorTextColor = Color(0xFFD4380D),
            noMoreText = "已经到底了",
            noMoreTextColor = Color(0xFF8C8C8C)
        )

        slots(
            loading = { PagingLoadingView() },
            error = { onRetry -> PagingErrorView(onRetry) },
            noMore = { PagingNoMoreView() }
        )
    }
}
```

说明：
- `refresh { indicator { ... } }` 用于自定义下拉刷新指示器。
- `paging { style(...) }` 用于自定义默认“加载中/失败/无更多”样式。
- 若你提供了 `paging { slots(...) }`，会优先使用 slots 的自定义 UI。

### 3) 分组列表（可吸顶）

```kotlin
ComposeListKit<Item> {
    mode {
        sectioned(
            groups = groups,
            groupTitleSelector = { it.title },
            groupItemsSelector = { it.items },
            sticky = true,
            groupHeaderContent = { title -> GroupHeader(title) },
            itemContent = { item -> ItemRow(item) }
        )
    }
}
```

### 4) 侧滑动作（多 action）

```kotlin
ComposeListKit<String> {
    data {
        items(items)
        item { value -> SwipeItem(value) }
    }

    mode {
        swipeActionsBidirectional(
            startActions = listOf(
                SwipeAction(
                    key = "pin",
                    label = "置顶",
                    onAction = { value -> pinToTop(value) }
                )
            ),
            endActions = listOf(
                SwipeAction(
                    key = "archive",
                    label = "归档",
                    onAction = { value -> archive(value) },
                    onUndo = { value -> restoreArchive(value) },
                    undoMessage = { value -> "$value 已归档" }
                ),
                SwipeAction(
                    key = "delete",
                    label = "删除",
                    content = { DeleteActionContent() },
                    enableHapticFeedback = true,
                    onAction = { value -> items.remove(value) },
                    onUndo = { value -> items.add(value) },
                    undoLabel = "撤销删除"
                )
            ),
            actionSlotWidth = 84.dp,
            openThresholdFraction = 0.28f,
            enableFullSwipeAction = true,
            fullSwipeTriggerFraction = 0.82f,
            canSwipe = { item -> !item.locked },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
```

`swipeActions(...)` 保留为“单侧（end）动作”快捷方式。支持通过 `SwipeAction` 自定义每个按钮的颜色和内容组件（`content`/`confirmContent`），危险动作可开启二次确认（`requiresConfirm`），并可配置确认窗口时长（`confirmTimeoutMillis`）及可选震动反馈（`enableHapticFeedback`）；通过 `enableFullSwipeAction + fullSwipeTriggerFraction` 可开启“全滑直接触发边缘动作”；通过 `onUndo/undoLabel/undoMessage/undoTimeoutMillis` 可使用内置撤销条；也支持 `backgroundContent = { item, actions -> ... }` 完全自定义背景布局。

### 5) 拖拽排序

```kotlin
ComposeListKit<String> {
    data {
        items(items)
    }

    mode {
        dragReorder(
            useLongPress = true,
            enableHighlight = true,
            dragHandle = { DragHandle() },
            itemContent = { value, isDragging -> DragItem(value, isDragging) },
            onMove = { from, to -> println("move: $from -> $to") },
            onReordered = { reordered -> syncToViewModel(reordered) }
        )
    }
}
```

### 6) 分组 + 侧滑（组合）

```kotlin
ComposeListKit<Item> {
    mode {
        sectionedSwipe(
            groups = groups,
            groupTitleSelector = { it.title },
            groupItemsSelector = { it.items },
            sticky = true,
            enableGroupCollapse = true,
            initiallyCollapsedGroupTitles = setOf("归档分组"),
            startActions = listOf(
                SwipeAction(
                    key = "pin",
                    label = "置顶",
                    onAction = { item -> pinToTopInGroup(item) }
                )
            ),
            endActions = listOf(
                SwipeAction(
                    key = "delete",
                    label = "删除",
                    requiresConfirm = true,
                    confirmLabel = "确认删除",
                    onAction = { item -> removeFromGroup(item) }
                )
            ),
            groupHeaderStateContent = { title, isExpanded ->
                Text(if (isExpanded) "v $title" else "> $title")
            },
            itemContent = { item -> ItemRow(item) }
        )
    }
}
```

`sectionedSwipe(...)` 适用于“分组吸顶 + 行级侧滑动作”的组合场景；支持与普通 `swipeActions` 一致的动作参数（包括全滑触发、Undo、确认等能力）。新增 `enableGroupCollapse + initiallyCollapsedGroupTitles` 可开启分组收起/展开，`groupHeaderStateContent` 可拿到 `isExpanded` 自定义头部样式。

### 7) 多选 + 批量操作

```kotlin
ComposeListKit<String> {
    data {
        list(items = items)
        key { it }
    }

    mode {
        multiSelect(
            actions = listOf(
                BatchSelectionAction(
                    key = "delete",
                    label = "批量删除",
                    onAction = { selected -> items.removeAll(selected.toSet()) }
                ),
                BatchSelectionAction(
                    key = "archive",
                    label = "批量归档",
                    onAction = { selected -> archive(selected) },
                    clearSelectionAfterAction = false
                )
            ),
            selectionMode = SelectionMode.Multi, // 或 SelectionMode.Single
            maxSelectionCount = 20,
            showSelectionIndicator = true,
            showSelectionBar = true,
            enableSelectAllAction = true,
            enableInvertSelectionAction = true,
            itemStyle = MultiSelectItemStyle(
                selectedContainerAlpha = 0.36f,
                unselectedContainerAlpha = 1f,
                cornerRadius = 14.dp,
                rowOuterVerticalPadding = 6.dp,
                rowInnerHorizontalPadding = 10.dp,
                rowInnerVerticalPadding = 6.dp
            ),
            itemContent = { value, isSelected ->
                MultiSelectItem(value = value, selected = isSelected)
            },
            onSelectionChanged = { selected ->
                println("当前已选: ${selected.size}")
            }
        )
    }
}
```

`multiSelect(...)` 支持 `selectionMode = Single | Multi`。  
多选模式下可通过 `enableSelectAllAction` / `enableInvertSelectionAction` 打开内置“全选/反选”能力；同时保留批量动作栏（已选数量 + 清空 + 自定义批量动作）。  
默认选中指示器使用 `Checkbox`；若你希望完全自定义勾选控件，可使用 `indicatorContent` 覆盖默认实现：

```kotlin
multiSelect(
    actions = actions,
    indicatorContent = { isSelected, mode, onToggle ->
        when (mode) {
            SelectionMode.Single -> RadioButton(
                selected = isSelected,
                onClick = onToggle
            )
            SelectionMode.Multi -> Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
)
```

若你希望隐藏底栏，可直接：

```kotlin
multiSelect(
    actions = actions,
    showSelectionBar = false
)
```

若你希望完全自定义底栏，可使用 `selectionBarContent`：

```kotlin
multiSelect(
    actions = actions,
    selectionBarContent = { bar ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("已选 ${bar.selectedCount} 项", modifier = Modifier.weight(1f))
            TextButton(onClick = { bar.clearSelection() }) { Text("清空") }
            bar.selectAll?.let { TextButton(onClick = it) { Text("全选") } }
            bar.invertSelection?.let { TextButton(onClick = it) { Text("反选") } }
        }
    }
)
```

### 8) 评论线程（一级/二级/三级）

```kotlin
data class Comment(
    val id: String,
    val text: String,
    val replies: List<Comment> = emptyList()
)

ComposeListKit<Comment> {
    data {
        list(items = comments)
        key { it.id }
    }

    mode {
        commentThread(
            childrenSelector = { it.replies },
            maxDepth = 3,
            initialExpandedDepth = 2,
            itemContent = { comment, depth, hasChildren, expanded, onToggle ->
                CommentRow(
                    comment = comment,
                    depth = depth,
                    hasChildren = hasChildren,
                    expanded = expanded,
                    onToggle = onToggle
                )
            }
        )
    }
}
```

示例 `CommentThreadScreen` 包含完整的“发布评论 + 对任意评论回复（最多到三级）+ 展开/收起回复”交互。

### 9) 列表布局与分割线

```kotlin
ComposeListKit<Item> {
    data {
        list(items = items)
        key { it.id }
        contentType { it.type }

        // 纵向 / 横向 / 网格 / 瀑布 / 流式
        vertical(itemSpacing = 8.dp)
        // horizontal(itemSpacing = 8.dp)
        // grid(columns = 2, verticalSpacing = 8.dp, horizontalSpacing = 8.dp)
        // staggered(columns = 2, verticalSpacing = 8.dp, horizontalSpacing = 8.dp)
        // flow(maxItemsInEachRow = 3, verticalSpacing = 8.dp, horizontalSpacing = 8.dp)

        divider(
            enabled = true,
            thickness = 0.5.dp,
            startIndent = 12.dp,
            endIndent = 12.dp
        )

        item { item -> ItemCard(item) }
    }
}
```

`horizontal/grid/staggered/flow` 目前仅在 `plain` 模式下可用（用于避免与分组吸顶、拖拽、侧滑等模式的手势/布局冲突）。

## 大数据量滚动优化建议

- 给每个 item 提供稳定 `key { ... }`，避免快速滑动时状态错位和额外重组。
- 给不同样式 item 提供 `contentType { ... }`，提升 `Lazy*` 复用效率（尤其混合布局/多类型卡片）。
- 分页建议始终绑定 `paging { bind(isLoadingMore = ..., hasMore = ..., onLoadMore = ...) }`，避免重复触发加载。

## 模式约束

`mode { ... }` 中以下模式互斥：

- `plain`
- `sectioned`
- `sectionedSwipe`
- `swipeActions`
- `dragReorder`
- `commentThread`
- `multiSelect`

同一配置中同时设置多个模式会抛出异常，避免 silent override。

## Demo 入口

- `sample/src/main/java/com/ghn/composelistkit/ui/screen`
- `sample/src/main/java/com/ghn/composelistkit/ui/nav`

## License

请按你的项目实际 License 补充。
