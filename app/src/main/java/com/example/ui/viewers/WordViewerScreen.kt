package com.example.ui.viewers

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.document.word.DocxDocument
import com.example.document.word.DocxElement
import com.example.document.word.TableCell
import com.example.document.word.TableRow
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.ViewerHeaderBar

@Composable
fun WordViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    viewModel: WordViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val docEntity by viewModel.documentEntity.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    LaunchedEffect(uriString) {
        viewModel.loadWordDocument(uriString)
    }

    val resolvedTitle = remember(uriString, docEntity) {
        DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
    }
    val ext = remember(uriString, docEntity) {
        DocumentNameResolver.resolveExtension(uriString, docEntity).ifBlank { "DOCX" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val subtitle = when (val state = uiState) {
                is WordUiState.Success -> "${state.document.paragraphCount} paragraphs • ${state.document.elements.size} elements"
                else -> "Word Processor Document"
            }
            ViewerHeaderBar(
                title = resolvedTitle,
                subtitle = subtitle,
                badgeText = ext.uppercase(),
                badgeColor = Color(0xFF2563EB),
                isDarkTheme = false,
                onBack = onBack,
                backTestTag = "btn_word_back"
            ) {
                // Font Size Controls
                IconButton(onClick = { viewModel.decreaseFont() }, modifier = Modifier.size(36.dp)) {
                    Text("A-", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { viewModel.increaseFont() }, modifier = Modifier.size(36.dp)) {
                    Text("A+", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                // Share
                IconButton(
                    onClick = {
                        val mime = if (ext.lowercase() == "doc") "application/msword" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        DocumentSharing.shareDocument(context, uriString, mime, resolvedTitle)
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .testTag("word_viewer_content")
        ) {
            when (val state = uiState) {
                is WordUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Reading Word Document...", fontSize = 14.sp)
                    }
                }
                is WordUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to open Word document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
                is WordUiState.Success -> {
                    DocxContent(
                        doc = state.document,
                        fontScale = fontScale
                    )
                }
            }
        }
    }
}

@Composable
private fun DocxContent(
    doc: DocxDocument,
    fontScale: Float
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Document Stats Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${doc.wordCount} Words", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("${doc.paragraphCount} Paragraphs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (doc.tableCount > 0) {
                        Text("${doc.tableCount} Tables", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Render document elements
        itemsIndexed(
            items = doc.elements,
            key = { index, _ -> index }
        ) { _, element ->
            when (element) {
                is DocxElement.Paragraph -> {
                    ParagraphView(paragraph = element, fontScale = fontScale)
                }
                is DocxElement.Table -> {
                    TableView(table = element, fontScale = fontScale)
                }
            }
        }
    }
}

@Composable
private fun ParagraphView(paragraph: DocxElement.Paragraph, fontScale: Float) {
    if (paragraph.runs.all { it.text.isBlank() }) {
        Spacer(modifier = Modifier.height(8.dp))
        return
    }

    val defaultTextColor = MaterialTheme.colorScheme.onBackground
    val annotated = remember(paragraph, fontScale, defaultTextColor) {
        buildAnnotatedString {
            if (paragraph.isBullet) {
                append("  •  ")
            }
            for (run in paragraph.runs) {
                val textDeco = when {
                    run.isUnderline && run.isStrike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    run.isUnderline -> TextDecoration.Underline
                    run.isStrike -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }

                var textColor = defaultTextColor
                if (run.colorHex != null) {
                    try {
                        val clean = run.colorHex.removePrefix("#")
                        if (clean.length == 6) {
                            textColor = Color(android.graphics.Color.parseColor("#$clean"))
                        }
                    } catch (e: Exception) {
                        // Ignore parse error
                    }
                }

                withStyle(
                    style = SpanStyle(
                        fontWeight = if (run.isBold || paragraph.isTitle || paragraph.isHeading) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = textDeco,
                        fontSize = (run.sizeSp * fontScale).sp,
                        color = textColor
                    )
                ) {
                    append(run.text)
                }
            }
        }
    }

    val align = when (paragraph.alignment.lowercase()) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        "both", "justify" -> TextAlign.Justify
        else -> TextAlign.Start
    }

    Text(
        text = annotated,
        textAlign = align,
        lineHeight = (22 * fontScale).sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (paragraph.isHeading || paragraph.isTitle) 6.dp else 2.dp)
    )
}

@Composable
private fun TableView(table: DocxElement.Table, fontScale: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            table.rows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIdx == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else if (rowIdx % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.cells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Column {
                                cell.paragraphs.forEach { p ->
                                    ParagraphView(paragraph = p, fontScale = fontScale * 0.9f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
