package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.ghn.composelistkit.ComposeListKit
import com.ghn.composelistkit.config.SwipeAction

/**
 * @author 浩楠
 *
 * @date 2025/5/4-12:54
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun SimpleSwipeToDeleteScreen() {
    val items = remember { mutableStateListOf("A(锁定)", "B", "C", "D") }
    val itemPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ComposeListKit {
        data {
            list(items = items)
            key { it }
            item { item -> SwipeDeleteItem(item) }
        }
        mode {
            swipeActionsBidirectional(
                startActions = listOf(
                    SwipeAction(
                        key = "pin",
                        label = "置顶",
                        containerColor = Color(0xFF0E7490),
                        contentColor = Color.White,
                        content = {
                            SwipeActionContent(
                                title = "置顶",
                                subtitle = "移动到最前"
                            )
                        },
                        onAction = { item ->
                            val index = items.indexOf(item)
                            if (index > 0) {
                                items.removeAt(index)
                                items.add(0, item)
                            }
                        }
                    )
                ),
                endActions = listOf(
                    SwipeAction(
                        key = "archive",
                        label = "归档",
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        content = {
                            SwipeActionContent(
                                title = "归档",
                                subtitle = "稍后处理"
                            )
                        },
                        onAction = { item ->
                            val index = items.indexOf(item)
                            if (index >= 0) {
                                items.removeAt(index)
                                items.add(item)
                            }
                        },
                        onUndo = { item ->
                            val index = items.indexOf(item)
                            if (index >= 0) {
                                items.removeAt(index)
                                items.add(0, item)
                            }
                        },
                        undoMessage = { value -> "$value 已归档" }
                    ),
                    SwipeAction(
                        key = "delete",
                        label = "删除",
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White,
                        content = {
                            SwipeActionContent(
                                title = "删除",
                                subtitle = "全滑可触发"
                            )
                        },
                        enableHapticFeedback = true,
                        onAction = { item ->
                            items.remove(item)
                        },
                        onUndo = { item ->
                            items.add(item)
                        },
                        undoLabel = "撤销删除",
                        undoMessage = { value -> "$value 已删除" }
                    )
                ),
                contentPadding = itemPadding,
                actionSlotWidth = 84.dp,
                openThresholdFraction = 0.18f,
                enableFullSwipeAction = false,
                canSwipe = { value -> !value.startsWith("A(") }
            )
        }
    }
}

@Composable
private fun SwipeActionContent(
    title: String,
    subtitle: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SwipeDeleteItem(item: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Text(
            text = item,
            modifier = Modifier.padding(16.dp)
        )
    }
}
