package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit


/**
 * @author 浩楠
 *
 * @date 2025/5/2-15:22
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DragReorderScreen() {
    val items = remember {
        mutableStateListOf(
            "Kotlin", "Java", "Goland", "C", "C++"
        )
    }
    ComposeListKit {
        data {
            items(items)
        }
        mode {
            dragReorder(
                useLongPress = true,
                dragHandle = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                itemContent = { item, isDragging -> DragItemCard(item, isDragging) }
            )
        }
    }

}

@Composable
private fun DragItemCard(item: String, isDragging: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = if (isDragging) Color(0xFF1F2937) else Color.Black,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Text(text = item, color = Color.White)
    }
}
