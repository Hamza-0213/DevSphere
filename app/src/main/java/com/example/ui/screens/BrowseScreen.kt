package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DocumentType
import com.example.filemanager.FileItem
import com.example.ui.MainViewModel
import com.example.ui.components.DocumentSearchBar
import com.example.ui.components.DocumentSortIconButton
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatFileSize
import java.io.File

@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    onOpenFile: (uri: String, fileName: String) -> Unit
) {
    val currentPath by viewModel.currentBrowserPath.collectAsState()
    val browserItems by viewModel.browserItems.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val isLoading by viewModel.isBrowserLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val sortedItems = remember(browserItems, settings, currentPath, searchQuery) {
        val base = if (currentPath != null) {
            viewModel.sortFileItems(browserItems, settings.sortBy, settings.sortAscending)
        } else {
            browserItems
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    // Handle Android system Back button to navigate parent directory
    BackHandler(enabled = currentPath != null) {
        viewModel.navigateUp()
    }

    // SAF folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            // Folder picked
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentPath != null) {
                            IconButton(
                                onClick = { viewModel.navigateUp() },
                                modifier = Modifier.testTag("btn_browse_back")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Up",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentPath == null) "Storage Browser" else currentPath!!.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentPath?.absolutePath ?: "Device & Application Storage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        DocumentSortIconButton(
                            currentOption = settings.sortBy,
                            ascending = settings.sortAscending,
                            onSortSelected = { option, asc ->
                                viewModel.setSortOption(option, asc)
                            }
                        )
                    }

                    if (browserItems.isNotEmpty() && currentPath != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DocumentSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Filter folder items by name...",
                            testTag = "browse_search_bar"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("browse_screen_content")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (browserItems.isEmpty()) {
                EmptyStateView(
                    title = "Empty Directory",
                    description = "No accessible files or subfolders found in this directory.",
                    actionButtonText = "Go to Storage Roots",
                    onActionClick = { viewModel.loadRootBrowserLocations() }
                )
            } else if (sortedItems.isEmpty()) {
                EmptyStateView(
                    title = "No files match \"$searchQuery\"",
                    description = "No files or folders in this directory matched your search filter.",
                    actionButtonText = "Clear Search",
                    onActionClick = { searchQuery = "" }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = sortedItems,
                        key = { it.path }
                    ) { item ->
                        FileItemRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory && item.file != null) {
                                    viewModel.navigateToDirectory(item.file)
                                } else {
                                    val uriStr = item.uri?.toString() ?: Uri.fromFile(item.file).toString()
                                    onOpenFile(uriStr, item.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemRow(
    item: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("browser_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (item.isDirectory) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else item.documentType.primaryColor.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Filled.Folder else item.documentType.icon,
                    contentDescription = null,
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else item.documentType.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (item.isDirectory) "${item.childCount} items" else "${formatFileSize(item.sizeBytes)} • ${item.documentType.displayName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
