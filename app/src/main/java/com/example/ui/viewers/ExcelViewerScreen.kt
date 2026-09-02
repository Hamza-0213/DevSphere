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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.document.excel.XlsxCell
import com.example.document.excel.XlsxSheet
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.ViewerHeaderBar

@Composable
fun ExcelViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    viewModel: ExcelViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val docEntity by viewModel.documentEntity.collectAsState()
    val activeSheetIndex by viewModel.activeSheetIndex.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearching by remember { mutableStateOf(false) }
    var selectedCell by remember { mutableStateOf<XlsxCell?>(null) }

    LaunchedEffect(uriString) {
        viewModel.loadSpreadsheet(uriString)
    }

    val resolvedTitle = remember(uriString, docEntity) {
        DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
    }
    val ext = remember(uriString, docEntity) {
        DocumentNameResolver.resolveExtension(uriString, docEntity).ifBlank { "XLSX" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val subtitle = when (val state = uiState) {
                    is ExcelUiState.Success -> {
                        val activeSheet = state.workbook.sheets.getOrNull(activeSheetIndex)
                        if (activeSheet != null) {
                            "${activeSheet.name} • ${activeSheet.maxRows} rows × ${activeSheet.maxColumns} cols"
                        } else {
                            "${state.workbook.sheets.size} sheet(s)"
                        }
                    }
                    else -> "Spreadsheet Workbook"
                }

                ViewerHeaderBar(
                    title = resolvedTitle,
                    subtitle = subtitle,
                    badgeText = ext.uppercase(),
                    badgeColor = Color(0xFF10B981),
                    isDarkTheme = false,
                    onBack = onBack,
                    backTestTag = "btn_excel_back"
                ) {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search Sheet",
                            tint = if (isSearching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            val mime = if (ext.lowercase() == "xls") "application/vnd.ms-excel" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            DocumentSharing.shareDocument(context, uriString, mime, resolvedTitle)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }

                    IconButton(
                        onClick = {
                            DocumentPrinter.printDocument(context, Uri.parse(uriString), resolvedTitle)
                        }
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                }

                // Search field bar when searching
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
                                placeholder = { Text("Find in spreadsheet...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("excel_search_input"),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Sheets Tabs
                if (uiState is ExcelUiState.Success) {
                    val sheets = (uiState as ExcelUiState.Success).workbook.sheets
                    if (sheets.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = activeSheetIndex.coerceIn(0, sheets.size - 1),
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            sheets.forEachIndexed { index, sheet ->
                                Tab(
                                    selected = (activeSheetIndex == index),
                                    onClick = { viewModel.selectSheet(index) },
                                    text = { Text(sheet.name, fontWeight = if (activeSheetIndex == index) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Cell Inspection Bar
            selectedCell?.let { cell ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = cell.columnRef.ifBlank { "Cell" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cell.value.ifBlank { "(Empty)" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (cell.formula != null) {
                                Text(
                                    text = "=${cell.formula}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = { selectedCell = null }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Dismiss")
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
                .background(MaterialTheme.colorScheme.background)
                .testTag("excel_viewer_content")
        ) {
            when (val state = uiState) {
                is ExcelUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Reading spreadsheet...", fontSize = 14.sp)
                    }
                }
                is ExcelUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to open spreadsheet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
                is ExcelUiState.Success -> {
                    val currentSheet = state.workbook.sheets.getOrNull(activeSheetIndex)
                    if (currentSheet != null) {
                        SpreadsheetMatrix(
                            sheet = currentSheet,
                            searchQuery = searchQuery,
                            selectedCell = selectedCell,
                            onCellClick = { selectedCell = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpreadsheetMatrix(
    sheet: XlsxSheet,
    searchQuery: String,
    selectedCell: XlsxCell?,
    onCellClick: (XlsxCell) -> Unit
) {
    val hScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(hScrollState)
    ) {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // Header Row (A, B, C, D...)
            item {
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Top-Left origin corner
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 32.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    for (col in 0 until sheet.maxColumns) {
                        val colLetter = if (col < 26) "${('A' + col)}" else "Col ${col + 1}"
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 32.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = colLetter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Data Rows
            items(
                items = sheet.rows,
                key = { it.rowIndex }
            ) { row ->
                val cellMap = remember(row) {
                    val map = arrayOfNulls<XlsxCell>(sheet.maxColumns)
                    row.cells.forEach { c ->
                        if (c.columnIndex in 0 until sheet.maxColumns) {
                            map[c.columnIndex] = c
                        }
                    }
                    map
                }

                Row(
                    modifier = Modifier.background(
                        if (row.rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    // Row Number Gutter
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${row.rowIndex}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Fast O(1) indexed cells in row
                    for (colIdx in 0 until sheet.maxColumns) {
                        val cell = cellMap[colIdx]
                        val cellValue = cell?.value ?: ""
                        val isMatched = searchQuery.isNotBlank() && cellValue.contains(searchQuery, ignoreCase = true)
                        val isSelected = selectedCell != null && selectedCell.rowIndex == row.rowIndex - 1 && selectedCell.columnIndex == colIdx

                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 36.dp)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isMatched -> Color(0xFFFEF08A)
                                        cell?.isHeader == true -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                                .clickable {
                                    if (cell != null) onCellClick(cell)
                                    else onCellClick(XlsxCell("${('A' + colIdx)}${row.rowIndex}", colIdx, row.rowIndex - 1, ""))
                                }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = cellValue,
                                fontSize = 12.sp,
                                fontWeight = if (cell?.isHeader == true) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMatched) Color.Black else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
