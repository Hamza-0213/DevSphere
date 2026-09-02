package com.example.ui.viewers

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.document.text.TextDocument
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.ViewerHeaderBar

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

    var isSearching by remember { mutableStateOf(false) }

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
                val subtitle = when (val state = uiState) {
                    is TextUiState.Success -> "${state.document.lineCount} lines • ${state.document.characterCount} chars • ${state.document.encoding}"
                    else -> "Plain Text Document"
                }

                ViewerHeaderBar(
                    title = resolvedTitle,
                    subtitle = subtitle,
                    badgeText = ext.uppercase(),
                    badgeColor = Color(0xFF64748B),
                    isDarkTheme = (readerTheme == "DARK"),
                    onBack = onBack,
                    backTestTag = "btn_text_back"
                ) {
                    // Search
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

                // Search Bar expansion
                AnimatedVisibility(visible = isSearching) {
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
