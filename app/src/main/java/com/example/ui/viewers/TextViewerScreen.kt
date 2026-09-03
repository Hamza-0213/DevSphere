package com.example.ui.viewers

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.document.text.TextDocument
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.ViewerHeaderBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TextViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    viewModel: TextViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val docEntity by viewModel.documentEntity.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val showLineNumbers by viewModel.showLineNumbers.collectAsState()
    val readerTheme by viewModel.readerTheme.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isEditMode by viewModel.isEditMode.collectAsState()
    val editedText by viewModel.editedText.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var isSearching by remember { mutableStateOf(false) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showRevertConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        viewModel.loadTextFile(uriString)
    }

    val (bgColor, textColor, gutterBg, gutterColor) = when (readerTheme) {
        "DARK" -> Quadruple(Color(0xFF0F172A), Color(0xFFF1F5F9), Color(0xFF1E293B), Color(0xFF64748B))
        "SEPIA" -> Quadruple(Color(0xFFFDF6E3), Color(0xFF586E75), Color(0xFFEEE8D5), Color(0xFF93A1A1))
        else -> Quadruple(Color.White, Color(0xFF1E293B), Color(0xFFF8FAFC), Color(0xFF94A3B8))
    }

    val resolvedTitle = remember(uriString, docEntity) {
        DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
    }
    val ext = remember(uriString, docEntity) {
        DocumentNameResolver.resolveExtension(uriString, docEntity).ifBlank { "TXT" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val subtitle = if (isEditMode) {
                    val wordCount = editedText.split("\\s+".toRegex()).count { it.isNotBlank() }
                    "Editing • $wordCount words • ${editedText.length} chars"
                } else {
                    when (val state = uiState) {
                        is TextUiState.Success -> "${state.document.lineCount} lines • ${state.document.characterCount} chars • ${state.document.encoding}"
                        else -> "Plain Text Document"
                    }
                }

                ViewerHeaderBar(
                    title = resolvedTitle,
                    subtitle = subtitle,
                    badgeText = if (isEditMode) "EDIT" else ext.uppercase(),
                    badgeColor = if (isEditMode) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                    isDarkTheme = (readerTheme == "DARK"),
                    onBack = onBack,
                    backTestTag = "btn_text_back"
                ) {
                    // Mode Toggle: View <-> Edit
                    IconButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier.testTag("btn_toggle_text_edit_mode")
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Filled.Visibility else Icons.Filled.Edit,
                            contentDescription = if (isEditMode) "Switch to View Mode" else "Edit Document",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Save Action (visible in Edit mode)
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                viewModel.saveChanges(uriString) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("btn_save_text_edits")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = "Save Changes",
                                    tint = if (hasUnsavedChanges) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Find & Replace button in Edit mode
                        IconButton(onClick = { showFindReplaceDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.FindReplace,
                                contentDescription = "Find and Replace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Search in View mode
                        IconButton(onClick = { isSearching = !isSearching }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search Text",
                                tint = if (isSearching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toggle Line Numbers
                        IconButton(onClick = { viewModel.toggleLineNumbers() }) {
                            Icon(
                                imageVector = Icons.Filled.FormatListNumbered,
                                contentDescription = "Toggle Line Numbers",
                                tint = if (showLineNumbers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Theme switch cycle
                    IconButton(
                        onClick = {
                            val nextTheme = when (readerTheme) {
                                "LIGHT" -> "SEPIA"
                                "SEPIA" -> "DARK"
                                else -> "LIGHT"
                            }
                            viewModel.setReaderTheme(nextTheme)
                        }
                    ) {
                        Icon(
                            imageVector = when (readerTheme) {
                                "DARK" -> Icons.Filled.DarkMode
                                "SEPIA" -> Icons.Filled.LightMode
                                else -> Icons.Filled.LightMode
                            },
                            contentDescription = "Reader Theme"
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            DocumentSharing.shareDocument(context, uriString, "text/plain", resolvedTitle)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }

                    // Print
                    IconButton(
                        onClick = {
                            DocumentPrinter.printDocument(context, Uri.parse(uriString), resolvedTitle)
                        }
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                }

                // Search Bar expansion (View mode)
                AnimatedVisibility(visible = !isEditMode && isSearching) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Find in text...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("text_search_input"),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Close search", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Edit Mode Status Bar (Edit mode)
                if (isEditMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (hasUnsavedChanges) Color(0xFFF59E0B) else Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasUnsavedChanges) "Unsaved changes" else "All changes saved",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hasUnsavedChanges) {
                                    TextButton(
                                        onClick = { showRevertConfirm = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Revert", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveChanges(uriString) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isSaving,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("btn_save_edit_bar")
                                ) {
                                    Text("Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(bgColor)
                .testTag("text_viewer_content")
        ) {
            when (val state = uiState) {
                is TextUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Reading text file...", color = textColor, fontSize = 14.sp)
                    }
                }
                is TextUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to open text document", fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = gutterColor, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
                is TextUiState.Success -> {
                    if (isEditMode) {
                        // EDIT MODE VIEW
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Main Editor TextField
                            OutlinedTextField(
                                value = editedText,
                                onValueChange = { viewModel.onTextChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("text_editor_field"),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSizeSp.sp,
                                    color = textColor,
                                    lineHeight = (fontSizeSp + 6).sp
                                ),
                                placeholder = {
                                    Text("Type document content here...", color = gutterColor)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = bgColor,
                                    unfocusedContainerColor = bgColor,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = gutterBg
                                )
                            )

                            // Quick Formatting & Snippet Toolbar
                            Surface(
                                color = gutterBg,
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Timestamp insertion
                                    OutlinedButton(
                                        onClick = {
                                            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                            viewModel.insertSnippet("[$timeStamp]")
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Time", fontSize = 11.sp)
                                    }

                                    // Bullet point
                                    OutlinedButton(
                                        onClick = { viewModel.insertSnippet("• ") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Bullet", fontSize = 11.sp)
                                    }

                                    // Checklist item
                                    OutlinedButton(
                                        onClick = { viewModel.insertSnippet("[ ] ") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckBox, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Todo", fontSize = 11.sp)
                                    }

                                    // Indent (4 spaces)
                                    OutlinedButton(
                                        onClick = { viewModel.insertSnippet("    ") },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.FormatIndentIncrease, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tab", fontSize = 11.sp)
                                    }

                                    // Find & Replace
                                    OutlinedButton(
                                        onClick = { showFindReplaceDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.FindReplace, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Replace", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // READ / VIEW MODE VIEW
                        val doc = state.document
                        val hScroll = rememberScrollState()

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Metrics bar
                            Surface(
                                color = gutterBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${doc.lineCount} lines", fontSize = 11.sp, color = gutterColor)
                                    Text("${doc.wordCount} words", fontSize = 11.sp, color = gutterColor)
                                    Text("${doc.characterCount} chars", fontSize = 11.sp, color = gutterColor)
                                    Text(doc.encoding, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Text content lines
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(hScroll)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    itemsIndexed(
                                        items = doc.lines,
                                        key = { index, _ -> index }
                                    ) { index, line ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (showLineNumbers) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = (fontSizeSp - 2).sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = gutterColor,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                    modifier = Modifier
                                                        .width(46.dp)
                                                        .background(gutterBg)
                                                        .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            val annotated = buildAnnotatedString {
                                                if (searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)) {
                                                    var startIdx = 0
                                                    val lowerLine = line.lowercase()
                                                    val lowerQuery = searchQuery.lowercase()
                                                    while (startIdx < line.length) {
                                                        val matchIdx = lowerLine.indexOf(lowerQuery, startIdx)
                                                        if (matchIdx == -1) {
                                                            append(line.substring(startIdx))
                                                            break
                                                        }
                                                        append(line.substring(startIdx, matchIdx))
                                                        withStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color.Black, fontWeight = FontWeight.Bold)) {
                                                            append(line.substring(matchIdx, matchIdx + searchQuery.length))
                                                        }
                                                        startIdx = matchIdx + searchQuery.length
                                                    }
                                                } else {
                                                    append(line)
                                                }
                                            }

                                            Text(
                                                text = annotated,
                                                fontSize = fontSizeSp.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = textColor,
                                                lineHeight = (fontSizeSp + 6).sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Find & Replace Dialog
    if (showFindReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showFindReplaceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FindReplace, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Find & Replace", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        label = { Text("Find text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_find_text")
                    )
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        label = { Text("Replace with") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_replace_text")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (findQuery.isNotEmpty()) {
                            viewModel.replaceAll(findQuery, replaceQuery)
                            Toast.makeText(context, "Replaced occurrences of \"$findQuery\"", Toast.LENGTH_SHORT).show()
                            showFindReplaceDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_replace_all")
                ) {
                    Text("Replace All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFindReplaceDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirm Revert Dialog
    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            title = { Text("Revert Changes?", fontWeight = FontWeight.Bold) },
            text = { Text("All unsaved edits in this document will be discarded.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revertChanges()
                        showRevertConfirm = false
                        Toast.makeText(context, "Changes reverted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirm = false }) {
                    Text("Keep Editing")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
