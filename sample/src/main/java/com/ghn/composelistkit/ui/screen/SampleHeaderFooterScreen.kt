package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit

/**
 * @author 浩楠
 *
 * @date 2025/4/27-18:10
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun SampleHeaderFooterScreen() {
    val items = remember { mutableStateListOf<String>().apply { addAll((1..5).map { "Item $it" }) } }
    var index by remember { mutableStateOf(items.size) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部按钮区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                index += 1
                items.add(0, "Item $index (头部添加)")
            }) {
                Text(text = "添加到头部")
            }

            Button(onClick = {
                index += 1
                items.add("Item $index (尾部添加)")
            }) {
                Text(text = "添加到尾部")
            }
        }
        ComposeListKit {
            data {
                list(items = items, modifier = Modifier.weight(1f))
                item { item -> HeaderFooterItem(item) }
            }
            mode {
                plain {
                    header { HeaderSlot() }
                    footer { FooterSlot() }
                }
            }
        }

    }
}

@Composable
private fun HeaderSlot() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "我是 Header 区域", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FooterSlot() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "我是 Footer 区域", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HeaderFooterItem(item: String) {
    Text(
        text = item,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
