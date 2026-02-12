package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit

private data class DemoItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val height: Int
)

private enum class DemoLayout(val label: String) {
    Vertical("纵向"),
    Horizontal("横向"),
    Grid("网格"),
    Staggered("瀑布"),
    Flow("流式")
}

@Composable
fun ListStyleShowcaseScreen() {
    var currentLayout by remember { mutableStateOf(DemoLayout.Vertical) }
    var showDivider by remember { mutableStateOf(true) }

    val items = remember {
        (1..30).map { index ->
            DemoItem(
                id = index,
                title = "Item $index",
                subtitle = "样式演示",
                height = 72 + (index % 5) * 22
            )
        }
    }

    ComposeListKit<DemoItem> {
        data {
            list(items = items)
            key { it.id }

            when (currentLayout) {
                DemoLayout.Vertical -> vertical(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    itemSpacing = 8.dp
                )

                DemoLayout.Horizontal -> horizontal(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    itemSpacing = 10.dp
                )

                DemoLayout.Grid -> grid(
                    columns = 2,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalSpacing = 10.dp,
                    horizontalSpacing = 10.dp
                )

                DemoLayout.Staggered -> staggered(
                    columns = 2,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalSpacing = 10.dp,
                    horizontalSpacing = 10.dp
                )

                DemoLayout.Flow -> flow(
                    maxItemsInEachRow = 3,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalSpacing = 10.dp,
                    horizontalSpacing = 10.dp
                )
            }

            divider(
                enabled = showDivider && (currentLayout == DemoLayout.Vertical || currentLayout == DemoLayout.Horizontal),
                startIndent = 8.dp,
                endIndent = 8.dp
            )

            item { item ->
                DemoItemCard(
                    item = item,
                    layout = currentLayout
                )
            }
        }

        mode {
            plain {
                header {
                    HeaderBar(
                        currentLayout = currentLayout,
                        showDivider = showDivider,
                        onLayoutChange = { currentLayout = it },
                        onToggleDivider = { showDivider = !showDivider }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    currentLayout: DemoLayout,
    showDivider: Boolean,
    onLayoutChange: (DemoLayout) -> Unit,
    onToggleDivider: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoLayout.entries.forEach { layout ->
                FilterChip(
                    selected = currentLayout == layout,
                    onClick = { onLayoutChange(layout) },
                    label = { Text(layout.label) }
                )
            }
            FilterChip(
                selected = showDivider,
                onClick = onToggleDivider,
                label = { Text("分割线") }
            )
        }
    }
}

@Composable
private fun DemoItemCard(
    item: DemoItem,
    layout: DemoLayout
) {
    val widthModifier = if (layout == DemoLayout.Horizontal) Modifier.width(180.dp) else Modifier
    val heightModifier = if (layout == DemoLayout.Staggered) Modifier.height(item.height.dp) else Modifier

    Surface(
        modifier = Modifier
            .then(widthModifier)
            .then(heightModifier)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
