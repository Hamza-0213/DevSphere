package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.SortOption

data class SortMenuItem(
    val option: SortOption,
    val ascending: Boolean,
    val title: String,
    val icon: ImageVector
)

val SORT_MENU_ITEMS = listOf(
    SortMenuItem(
        option = SortOption.DATE,
        ascending = false,
        title = "Date Modified (Newest first)",
        icon = Icons.Filled.DateRange
    ),
    SortMenuItem(
        option = SortOption.DATE,
        ascending = true,
        title = "Date Modified (Oldest first)",
        icon = Icons.Filled.DateRange
    ),
    SortMenuItem(
        option = SortOption.NAME,
        ascending = true,
        title = "File Name (A to Z)",
        icon = Icons.Filled.SortByAlpha
    ),
    SortMenuItem(
        option = SortOption.NAME,
        ascending = false,
        title = "File Name (Z to A)",
        icon = Icons.Filled.SortByAlpha
    ),
    SortMenuItem(
        option = SortOption.SIZE,
        ascending = false,
        title = "File Size (Largest first)",
        icon = Icons.Filled.Storage
    ),
    SortMenuItem(
        option = SortOption.SIZE,
        ascending = true,
        title = "File Size (Smallest first)",
        icon = Icons.Filled.Storage
    )
)

/**
 * Dropdown Menu to select document sort ordering
 */
@Composable
fun DocumentSortMenu(
    expanded: Boolean,
    currentOption: SortOption,
    ascending: Boolean,
    onDismissRequest: () -> Unit,
    onSortSelected: (SortOption, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("sort_dropdown_menu")
    ) {
        Text(
            text = "Sort Documents By",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SORT_MENU_ITEMS.forEach { item ->
            val isSelected = (currentOption == item.option && ascending == item.ascending)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                onClick = {
                    onSortSelected(item.option, item.ascending)
                    onDismissRequest()
                },
                colors = MenuDefaults.itemColors(
                    textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("sort_item_${item.option.name}_${if (item.ascending) "asc" else "desc"}")
            )
        }
    }
}

/**
 * An interactive Sort Button that triggers the Sort Dropdown Menu
 */
@Composable
fun DocumentSortIconButton(
    currentOption: SortOption,
    ascending: Boolean,
    onSortSelected: (SortOption, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("btn_sort_menu")
        ) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = "Sort Documents",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DocumentSortMenu(
            expanded = expanded,
            currentOption = currentOption,
            ascending = ascending,
            onDismissRequest = { expanded = false },
            onSortSelected = onSortSelected
        )
    }
}

/**
 * An interactive Sort Chip showing current sort state with instant dropdown menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSortChip(
    currentOption: SortOption,
    ascending: Boolean,
    onSortSelected: (SortOption, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val labelText = when (currentOption) {
        SortOption.DATE -> if (ascending) "Date (Oldest)" else "Date (Newest)"
        SortOption.NAME -> if (ascending) "Name (A-Z)" else "Name (Z-A)"
        SortOption.SIZE -> if (ascending) "Size (Smallest)" else "Size (Largest)"
        SortOption.TYPE -> "Format"
    }

    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = {
                Text(
                    text = labelText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("chip_sort_menu")
        )

        DocumentSortMenu(
            expanded = expanded,
            currentOption = currentOption,
            ascending = ascending,
            onDismissRequest = { expanded = false },
            onSortSelected = onSortSelected
        )
    }
}
