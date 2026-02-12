package com.ghn.composelistkit.ui.nav
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ghn.composelistkit.ComposeListKit
/**
 * @author 浩楠
 *
 * @date 2025/4/30-22:08
 *
 *      _              _           _     _   ____  _             _ _
 *     / \   _ __   __| |_ __ ___ (_) __| | / ___|| |_ _   _  __| (_) ___
 *    / _ \ | '_ \ / _` | '__/ _ \| |/ _` | \___ \| __| | | |/ _` | |/ _ \
 *   / ___ \| | | | (_| | | | (_) | | (_| |  ___) | |_| |_| | (_| | | (_) |
 *  /_/   \_\_| |_|\__,_|_|  \___/|_|\__,_| |____/ \__|\__,_|\__,_|_|\___/
 * @Description: TODO
 */


@Composable
fun AutoNavListScreen(
    navController: NavController,
    titles: List<String>
) {
    ComposeListKit<String> {
        data {
            list(items = titles)
            item { title ->
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(title)
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}
