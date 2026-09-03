package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.database.DocumentEntity
import com.example.data.preferences.SortOption
import com.example.domain.model.DocumentType
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryCard
import com.example.ui.components.DocumentCardItem
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.DocumentSearchBar
import com.example.ui.components.DocumentSortChip
import com.example.ui.components.DocumentSortIconButton
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StorageOverviewCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onDocumentClick: (DocumentEntity) -> Unit,
    onSearchClick: () -> Unit,
    onCategoryClick: (DocumentType) -> Unit
) {
    val context = LocalContext.current
    val allDocs by viewModel.allDocuments.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedInfoDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocContent by remember { mutableStateOf("") }

    // SAF Document Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onDocumentPicked(uri) { doc ->
                onDocumentClick(doc)
            }
        }
    }

    val categories = listOf(
        DocumentType.PDF,
        DocumentType.WORD,
        DocumentType.EXCEL,
        DocumentType.POWERPOINT,
        DocumentType.TEXT,
        DocumentType.IMAGE
    )

    val filteredDocs = remember(allDocs, selectedCategory, searchQuery, settings) {
        val categoryFiltered = if (selectedCategory == null) {
            allDocs
        } else {
            allDocs.filter { it.toDocumentType() == selectedCategory }
        }
        val textFiltered = if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { it.displayName.contains(searchQuery.trim(), ignoreCase = true) }
        }
        viewModel.sortDocuments(textFiltered, settings.sortBy, settings.sortAscending)
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Action: Create New Text / Note Document
                FloatingActionButton(
                    onClick = {
                        val num = (allDocs.count { it.toDocumentType() == DocumentType.TEXT } + 1)
                        newDocTitle = "Note $num.txt"
                        newDocContent = ""
                        showNewDocDialog = true
                    },
                    modifier = Modifier.testTag("fab_create_note"),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = "Create New Note",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Primary Action: Open File from Storage
                ExtendedFloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-powerpoint",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "text/*",
                                "image/*"
                            )
                        )
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Open Document") },
                    text = { Text("Open File") },
                    modifier = Modifier.testTag("fab_open_document"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("home_screen_content"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Header & Search Bar
            item {
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
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                shadowElevation = 3.dp
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.devionics_docsphere_logo_1788445831963),
                                    contentDescription = "DocSphere Logo - Devionics Labs",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DocSphere",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Text(
                                    text = "Devionics Labs",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Offline Security Capsule
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(androidx.compose.ui.graphics.Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-time Filename Search Bar
                    DocumentSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Filter documents by filename...",
                        testTag = "home_search_bar"
                    )
                }
            }

            // 2. Storage Overview Card
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    StorageOverviewCard(documents = allDocs)
                }
            }

            // 3. Category Grid / Carousels
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories) { category ->
                            val count = allDocs.count { it.toDocumentType() == category }
                            CategoryCard(
                                type = category,
                                count = count,
                                isSelected = (selectedCategory == category),
                                onClick = {
                                    if (selectedCategory == category) {
                                        viewModel.setSelectedCategory(null)
                                    } else {
                                        viewModel.setSelectedCategory(category)
                                    }
                                },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            // 4. Section Title with Sort & Filter Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = when {
                        searchQuery.isNotBlank() && selectedCategory != null -> 
                            "${selectedCategory?.displayName} matching \"$searchQuery\" (${filteredDocs.size})"
                        searchQuery.isNotBlank() -> 
                            "Results for \"$searchQuery\" (${filteredDocs.size})"
                        selectedCategory != null -> 
                            "${selectedCategory?.displayName} (${filteredDocs.size})"
                        else -> 
                            "All Documents (${filteredDocs.size})"
                    }

                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            TextButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (selectedCategory != null) {
                            TextButton(
                                onClick = { viewModel.setSelectedCategory(null) },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Reset Filter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        DocumentSortChip(
                            currentOption = settings.sortBy,
                            ascending = settings.sortAscending,
                            onSortSelected = { option, asc ->
                                viewModel.setSortOption(option, asc)
                            }
                        )
                    }
                }
            }

            // 5. Document List
            if (filteredDocs.isEmpty()) {
                item {
                    val emptyTitle = if (searchQuery.isNotBlank()) {
                        "No Matches Found"
                    } else if (selectedCategory != null) {
                        "No ${selectedCategory?.displayName} Files"
                    } else {
                        "No Documents Yet"
                    }

                    val emptyDescription = if (searchQuery.isNotBlank()) {
                        "No documents matching \"$searchQuery\" were found in your library."
                    } else if (selectedCategory != null) {
                        "No ${selectedCategory?.displayName} documents currently found in your index."
                    } else {
                        "Your library is empty. Tap 'Open File' to add documents or restore sample files."
                    }

                    val emptyActionText = if (searchQuery.isNotBlank()) {
                        "Clear Search"
                    } else if (selectedCategory != null) {
                        "Show All Documents"
                    } else {
                        "Restore Sample Documents"
                    }

                    EmptyStateView(
                        title = emptyTitle,
                        description = emptyDescription,
                        actionButtonText = emptyActionText,
                        onActionClick = {
                            if (searchQuery.isNotBlank()) {
                                searchQuery = ""
                            } else if (selectedCategory != null) {
                                viewModel.setSelectedCategory(null)
                            } else {
                                viewModel.restoreSampleDocuments()
                            }
                        }
                    )
                }
            } else {
                items(filteredDocs, key = { it.id }) { doc ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
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

    // Sort Dialog
    if (showSortDialog) {
        SortOptionsDialog(
            currentOption = settings.sortBy,
            ascending = settings.sortAscending,
            onDismiss = { showSortDialog = false },
            onApply = { option, asc ->
                viewModel.setSortOption(option, asc)
                showSortDialog = false
            }
        )
    }

    // Info Dialog
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

    // Create New Document Dialog
    if (showNewDocDialog) {
        AlertDialog(
            onDismissRequest = { showNewDocDialog = false },
            modifier = Modifier.testTag("dialog_create_new_doc"),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create New Note", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Create a plain text note or markdown file to edit directly inside the app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("File Name") },
                        placeholder = { Text("e.g., MeetingNotes.txt") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_doc_title")
                    )
                    OutlinedTextField(
                        value = newDocContent,
                        onValueChange = { newDocContent = it },
                        label = { Text("Initial Content (Optional)") },
                        placeholder = { Text("Start typing note...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_doc_content")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedTitle = newDocTitle.trim()
                        if (trimmedTitle.isNotBlank()) {
                            val finalName = if (!trimmedTitle.contains(".")) "$trimmedTitle.txt" else trimmedTitle
                            viewModel.createNewTextDocument(finalName, newDocContent) { newDoc ->
                                showNewDocDialog = false
                                onDocumentClick(newDoc)
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_create_doc")
                ) {
                    Text("Create & Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDocDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}

@Composable
fun SortOptionsDialog(
    currentOption: SortOption,
    ascending: Boolean,
    onDismiss: () -> Unit,
    onApply: (SortOption, Boolean) -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentOption) }
    var isAscending by remember { mutableStateOf(ascending) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sort Documents", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Sort By", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                SortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedOption = option }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == option),
                            onClick = { selectedOption = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                SortOption.NAME -> "File Name (Alphabetical)"
                                SortOption.DATE -> "Date (Recently Modified)"
                                SortOption.SIZE -> "File Size"
                                SortOption.TYPE -> "Document Type"
                            },
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Order", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = isAscending,
                        onClick = { isAscending = true },
                        label = { Text("Ascending") },
                        leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = !isAscending,
                        onClick = { isAscending = false },
                        label = { Text("Descending") },
                        leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(selectedOption, isAscending) },
                modifier = Modifier.testTag("btn_apply_sort")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
