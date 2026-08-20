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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
fun FavouritesScreen(
    viewModel: MainViewModel,
    onDocumentClick: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val favourites by viewModel.favouriteDocuments.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedInfoDoc by remember { mutableStateOf<DocumentEntity?>(null) }

    val filteredFavourites = remember(favourites, searchQuery, settings) {
        val filtered = if (searchQuery.isBlank()) {
            favourites
        } else {
            favourites.filter { it.displayName.contains(searchQuery.trim(), ignoreCase = true) }
        }
        viewModel.sortDocuments(filtered, settings.sortBy, settings.sortAscending)
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = "Starred & Favourites",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
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

                    if (favourites.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DocumentSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Filter favourites by filename...",
                            testTag = "favourites_search_bar"
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
                .testTag("favourites_screen_content")
        ) {
            if (favourites.isEmpty()) {
                EmptyStateView(
                    title = "No Favourites Yet",
                    description = "Star important documents in your library or while reading to quickly access them anytime.",
                    icon = Icons.Outlined.StarOutline
                )
            } else if (filteredFavourites.isEmpty()) {
                EmptyStateView(
                    title = "No files match \"$searchQuery\"",
                    description = "No starred documents matched your search filter.",
                    actionButtonText = "Clear Search",
                    onActionClick = { searchQuery = "" }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFavourites, key = { it.id }) { doc ->
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

    selectedInfoDoc?.let { doc ->
        DocumentInfoDialog(document = doc, onDismiss = { selectedInfoDoc = null })
    }
}
