package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.database.DocumentEntity
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.MainViewModel
import com.example.ui.components.DocumentCardItem
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.DocumentSearchBar
import com.example.ui.components.DocumentSortIconButton
import com.example.ui.components.EmptyStateView

@Composable
fun RecentsScreen(
    viewModel: MainViewModel,
    onDocumentClick: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val recents by viewModel.recentDocuments.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedInfoDoc by remember { mutableStateOf<DocumentEntity?>(null) }

    val filteredRecents = remember(recents, searchQuery) {
        if (searchQuery.isBlank()) {
            recents
        } else {
            recents.filter { it.displayName.contains(searchQuery.trim(), ignoreCase = true) }
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
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                            Column {
                                Text(
                                    text = "Recent Documents",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (recents.size == 1) "1 recent document" else "${recents.size} recent documents",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (recents.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.testTag("btn_clear_recents")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (recents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DocumentSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Filter recents by filename...",
                            testTag = "recents_search_bar"
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
                .testTag("recents_screen_content")
        ) {
            if (recents.isEmpty()) {
                EmptyStateView(
                    title = "No Recent Activity",
                    description = "Documents you open and read will appear here with your saved reading progress.",
                    icon = Icons.Outlined.History
                )
            } else if (filteredRecents.isEmpty()) {
                EmptyStateView(
                    title = "No Matches Found",
                    description = "No recent documents matched \"$searchQuery\".",
                    actionButtonText = "Clear Search",
                    onActionClick = { searchQuery = "" }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecents, key = { it.id }) { doc ->
                        DocumentCardItem(
                            document = doc,
                            onClick = { onDocumentClick(doc) },
                            onToggleFavourite = { viewModel.toggleFavourite(doc) },
                            onShare = {
                                DocumentSharing.shareDocument(context, doc.uri, doc.toDocumentType().mimeType, doc.displayName)
                            },
                            onPrint = {
                                DocumentPrinter.printDocument(context, Uri.parse(doc.uri), doc.displayName)
                            },
                            onInfo = { selectedInfoDoc = doc },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Recent History") },
            text = { Text("Are you sure you want to clear your recently opened document history? Your documents will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearRecents()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedInfoDoc?.let { doc ->
        DocumentInfoDialog(
            document = doc,
            onDismiss = { selectedInfoDoc = null },
            onRename = { newName ->
                viewModel.renameDocument(doc.uri, newName)
                selectedInfoDoc = null
            },
            onDelete = {
                viewModel.deleteDocument(doc)
                selectedInfoDoc = null
            }
        )
    }
}
