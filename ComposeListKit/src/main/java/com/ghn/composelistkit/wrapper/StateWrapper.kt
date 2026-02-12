package com.ghn.composelistkit.wrapper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * @author 浩楠
 *
 * @date 2025/4/26-15:14
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun StateWrapper(
    isLoadingFirstPage: Boolean,
    isError: Boolean,
    itemsEmpty: Boolean,
    onRetry: (() -> Unit)? = null,
    loadingContent: (@Composable () -> Unit)? = null,
    errorContent: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    when {
        isLoadingFirstPage -> {
            (loadingContent ?: {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            })()
        }

        isError -> {
            (errorContent ?: {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable { onRetry?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载失败，点击重试")
                }
            })()
        }

        itemsEmpty -> {
            (emptyContent ?: {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据")
                }
            })()
        }

        else -> {
            content()
        }
    }
}
