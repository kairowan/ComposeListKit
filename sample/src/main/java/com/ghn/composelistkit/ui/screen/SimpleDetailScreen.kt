package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit

/**
 * @author 浩楠
 *
 * @date 2025/4/25-22:39
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun SimpleDetailScreen() {
    val items = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) {
        items.addAll(1..20)
    }
    ComposeListKit {
        data {
            list(items = items)
            item { item -> SimpleItem(item) }
        }
    }
}

@Composable
private fun SimpleItem(item: Int) {
    Text(
        text = "Item $item",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
