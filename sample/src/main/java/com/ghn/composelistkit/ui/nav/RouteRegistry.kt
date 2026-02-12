package com.ghn.composelistkit.ui.nav

import androidx.compose.runtime.Composable
import com.ghn.composelistkit.ui.screen.DragReorderScreen
import com.ghn.composelistkit.ui.screen.StateScreen
import com.ghn.composelistkit.ui.screen.CommentThreadScreen
import com.ghn.composelistkit.ui.screen.RefreshableDetailScreen
import com.ghn.composelistkit.ui.screen.SampleHeaderFooterScreen
import com.ghn.composelistkit.ui.screen.SectionedSwipeScreen
import com.ghn.composelistkit.ui.screen.SimpleDetailScreen
import com.ghn.composelistkit.ui.screen.SimpleSwipeToDeleteScreen
import com.ghn.composelistkit.ui.screen.ListStyleShowcaseScreen
import com.ghn.composelistkit.ui.screen.SelectionModeScreen
import com.ghn.composelistkit.ui.screen.StickySectionScreen

/**
 * @author 浩楠
 *
 * @date 2025/4/30-22:09
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
object RouteRegistry {
    val entries: List<Pair<String, @Composable () -> Unit>> = listOf(
        "普通列表" to { SimpleDetailScreen() },
        "刷新加载更多" to { RefreshableDetailScreen() },
        "状态页面" to { StateScreen() },
        "添加头和尾" to { SampleHeaderFooterScreen() },
        "吸顶分组" to { StickySectionScreen() },
        "分组侧滑(组合)" to { SectionedSwipeScreen() },
        "拖拽排序" to { DragReorderScreen() },
        "侧滑删除" to { SimpleSwipeToDeleteScreen() },
        "选择模式(单/多)" to { SelectionModeScreen() },
        "评论线程(1-3级)" to { CommentThreadScreen() },
        "多布局样式" to { ListStyleShowcaseScreen() }
    )

    val routeMap: Map<String, @Composable () -> Unit> =
        entries.associate { (it.first to it.second) }
    val routeTitleMap: Map<String, String> =
        entries.associate { (it.first to it.first) }
}
