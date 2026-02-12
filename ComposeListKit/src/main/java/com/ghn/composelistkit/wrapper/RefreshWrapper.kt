package com.ghn.composelistkit.wrapper

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * @author 浩楠
 *
 * @date 2025/4/26-15:17
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)? = null,
    indicatorContent: (@Composable (Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onRefresh != null) {
        val pullToRefreshState = rememberPullToRefreshState()
        if (indicatorContent != null) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                indicator = {
                    indicatorContent(isRefreshing)
                }
            ) {
                content()
            }
        } else {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            ) {
                content()
            }
        }
    } else {
        content()
    }
}
