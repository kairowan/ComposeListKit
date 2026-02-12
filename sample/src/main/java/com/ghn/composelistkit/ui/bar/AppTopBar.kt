package com.ghn.composelistkit.ui.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ghn.composelistkit.ui.nav.RouteRegistry

/**
 * @author 浩楠
 *
 * @date 2025/4/30-22:17
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */
@Composable
fun AppTopBar(
    navController: NavHostController,
    startRoute: String = "ui/list",
    homeTitle: String = "ComposeListKit"
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isHome = currentRoute == null || currentRoute == startRoute
    val title = if (isHome) homeTitle else (RouteRegistry.routeTitleMap[currentRoute] ?: homeTitle)

    TopBar(
        title = title,
        onBackClick = if (isHome) null else ({ navController.popBackStack() })
    )
}
