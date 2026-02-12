package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghn.composelistkit.ComposeListKit
import com.ghn.composelistkit.config.SwipeAction

@Composable
fun SectionedSwipeScreen() {
    val sampleGroups = rememberSampleGroups()
    val groups = remember {
        mutableStateListOf<Category>().apply {
            addAll(
                sampleGroups.map { group ->
                    group.copy(items = group.items.take(2))
                }
            )
        }
    }

    ComposeListKit<Item> {
        data {
            key { it.id }
        }
        mode {
            sectionedSwipe(
                groups = groups,
                groupTitleSelector = { it.groupName },
                groupItemsSelector = { it.items },
                startActions = listOf(
                    SwipeAction(
                        key = "pin",
                        label = "置顶",
                        onAction = { target ->
                            val groupIndex = groups.indexOfFirst { group ->
                                group.items.any { item -> item.id == target.id }
                            }
                            if (groupIndex >= 0) {
                                val group = groups[groupIndex]
                                val currentIndex = group.items.indexOfFirst { it.id == target.id }
                                if (currentIndex > 0) {
                                    val mutableItems = group.items.toMutableList()
                                    mutableItems.removeAt(currentIndex)
                                    mutableItems.add(0, target)
                                    groups[groupIndex] = group.copy(items = mutableItems)
                                }
                            }
                        }
                    )
                ),
                endActions = listOf(
                    SwipeAction(
                        key = "delete",
                        label = "删除",
                        requiresConfirm = true,
                        confirmLabel = "确认删除",
                        onAction = { target ->
                            val groupIndex = groups.indexOfFirst { group ->
                                group.items.any { item -> item.id == target.id }
                            }
                            if (groupIndex >= 0) {
                                val group = groups[groupIndex]
                                groups[groupIndex] = group.copy(
                                    items = group.items.filterNot { it.id == target.id }
                                )
                            }
                        }
                    )
                ),
                actionSlotWidth = 84.dp,
                openThresholdFraction = 0.18f,
                enableFullSwipeAction = false,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                enableGroupCollapse = true,
                initiallyCollapsedGroupTitles = setOf("🎵 音乐类"),
                groupHeaderStateContent = { title, isExpanded ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "${if (isExpanded) "v" else ">"} $title",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF374151)
                        )
                    }
                },
                itemContent = { item -> SectionedSwipeItem(item) }
            )
        }
    }
}

@Composable
private fun SectionedSwipeItem(item: Item) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 2.dp
    ) {
        Text(
            text = item.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 15.sp
        )
    }
}
