package com.ghn.composelistkit.wrapper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class CommentThreadEntry<T>(
    val key: Any,
    val item: T,
    val depth: Int,
    val hasChildren: Boolean,
    val expanded: Boolean
)

@Composable
fun <T> CommentThreadWrapper(
    roots: List<T>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    keySelector: ((T) -> Any)? = null,
    childrenSelector: (T) -> List<T>,
    maxDepth: Int = 3,
    initialExpandedDepth: Int = 2,
    indentPerLevel: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    loadMoreFooterState: LoadMoreFooterState = LoadMoreFooterState.Idle,
    onRetryLoadMore: (() -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    loadMoreErrorContent: (@Composable ((() -> Unit)?) -> Unit)? = null,
    noMoreContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T, Int, Boolean, Boolean, () -> Unit) -> Unit
) {
    val resolvedKeySelector: (T) -> Any = keySelector ?: { it.hashCode() }
    val expandedState = remember { mutableStateMapOf<Any, Boolean>() }

    val entries = flattenCommentThread(
        roots = roots,
        keySelector = resolvedKeySelector,
        childrenSelector = childrenSelector,
        maxDepth = maxDepth,
        initialExpandedDepth = initialExpandedDepth,
        expandedState = expandedState
    )

    LazyListContent<T>(
        modifier = modifier,
        listState = listState,
        loadMoreFooterState = loadMoreFooterState,
        onRetryLoadMore = onRetryLoadMore,
        loadingMoreContent = loadingMoreContent,
        loadMoreErrorContent = loadMoreErrorContent,
        noMoreContent = noMoreContent,
        contentBuilder = {
            items(
                items = entries,
                key = { it.key }
            ) { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                        .padding(start = indentPerLevel * entry.depth)
                ) {
                    itemContent(
                        entry.item,
                        entry.depth,
                        entry.hasChildren,
                        entry.expanded
                    ) {
                        if (entry.hasChildren) {
                            expandedState[entry.key] = !entry.expanded
                        }
                    }
                }
            }
        }
    )
}

internal fun <T> flattenCommentThread(
    roots: List<T>,
    keySelector: (T) -> Any,
    childrenSelector: (T) -> List<T>,
    maxDepth: Int,
    initialExpandedDepth: Int,
    expandedState: Map<Any, Boolean>
): List<CommentThreadEntry<T>> {
    if (roots.isEmpty()) return emptyList()
    val resolvedMaxDepth = maxDepth.coerceAtLeast(1)
    val resolvedInitialExpandedDepth = initialExpandedDepth.coerceAtLeast(1)
    val results = mutableListOf<CommentThreadEntry<T>>()
    val path = mutableSetOf<Any>()

    fun visit(node: T, depth: Int) {
        val key = keySelector(node)
        if (!path.add(key)) return

        val shouldLoadChildren = depth < resolvedMaxDepth - 1
        val children = if (shouldLoadChildren) childrenSelector(node) else emptyList()
        val hasChildren = children.isNotEmpty()
        val expanded = if (hasChildren) {
            expandedState[key] ?: (depth + 1 < resolvedInitialExpandedDepth)
        } else {
            false
        }

        results += CommentThreadEntry(
            key = key,
            item = node,
            depth = depth,
            hasChildren = hasChildren,
            expanded = expanded
        )

        if (hasChildren && expanded) {
            children.forEach { child ->
                visit(child, depth + 1)
            }
        }

        path.remove(key)
    }

    roots.forEach { root ->
        visit(root, depth = 0)
    }

    return results
}
