package com.ghn.composelistkit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghn.composelistkit.ComposeListKit
import com.ghn.composelistkit.config.BatchSelectionAction
import com.ghn.composelistkit.config.MultiSelectItemStyle
import com.ghn.composelistkit.config.SelectionMode

@Composable
fun SelectionModeScreen() {
    val items = remember { mutableStateListOf("A", "B", "C", "D", "E", "F") }
    var selectionMode by remember { mutableStateOf(SelectionMode.Multi) }
    val isMultiMode = selectionMode == SelectionMode.Multi
    val selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
    val unselectedContainerColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionModeChip(
                label = "单选",
                selected = selectionMode == SelectionMode.Single,
                onClick = { selectionMode = SelectionMode.Single }
            )
            SelectionModeChip(
                label = "多选",
                selected = selectionMode == SelectionMode.Multi,
                onClick = { selectionMode = SelectionMode.Multi }
            )
        }

        Text(
            text = if (isMultiMode) "多选模式：支持全选/反选/批量删除" else "单选模式：每次仅保留 1 项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.fillMaxSize()) {
            ComposeListKit<String> {
                data {
                    list(items = items, modifier = Modifier.fillMaxSize())
                    key { it }
                }
                mode {
                    multiSelect(
                        actions = listOf(
                            BatchSelectionAction(
                                key = "delete",
                                label = "批量删除",
                                onAction = { selected ->
                                    items.removeAll(selected.toSet())
                                }
                            )
                        ),
                        selectionMode = selectionMode,
                        enableSelectAllAction = isMultiMode,
                        enableInvertSelectionAction = isMultiMode,
                        itemStyle = MultiSelectItemStyle(
                            selectedContainerColor = selectedContainerColor,
                            unselectedContainerColor = unselectedContainerColor,
                            selectedContainerAlpha = 0.70f,
                            unselectedContainerAlpha = 1f,
                            cornerRadius = 14.dp,
                            rowOuterVerticalPadding = 5.dp,
                            rowInnerHorizontalPadding = 10.dp,
                            rowInnerVerticalPadding = 6.dp
                        ),
                        itemContent = { value, isSelected ->
                            SelectionModeItem(value = value, selected = isSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SelectionModeItem(
    value: String,
    selected: Boolean
) {
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
}
