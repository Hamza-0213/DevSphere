package com.example.ui.viewers

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.AddBookmarkDialog
import com.example.ui.components.BookmarksListDialog
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.ViewerHeaderBar
import com.example.ui.components.ZoomableBox
import kotlinx.coroutines.launch

@Composable
fun PdfViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    viewModel: PdfViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val docEntity by viewModel.documentEntity.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        viewModel.loadPdf(uriString)
    }

    val isCurrentPageBookmarked = remember(bookmarks, currentPage) {
        bookmarks.any { it.pageNumber == currentPage + 1 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
            .testTag("pdf_viewer_container")
    ) {
        when (val state = uiState) {
            is PdfUiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Rendering PDF...", color = Color.White, fontSize = 14.sp)
                }
            }
            is PdfUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to display PDF",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.message, color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            is PdfUiState.PasswordRequired -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Protected Document", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showPasswordDialog = true }) {
                        Text("Enter Password")
                    }
                }
            }
            is PdfUiState.Success -> {
                val totalPages = state.pageCount
                val pagerState = rememberPagerState(
                    initialPage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
                    pageCount = { totalPages }
                )
                var currentZoomScale by remember { mutableFloatStateOf(1f) }

                LaunchedEffect(pagerState.currentPage) {
                    viewModel.renderPageAndNeighbors(pagerState.currentPage)
                }

                LaunchedEffect(currentPage) {
                    if (pagerState.currentPage != currentPage && currentPage in 0 until totalPages) {
                        pagerState.scrollToPage(currentPage)
                    }
                }

                // Horizontal Pager for hand slide page turning & multi-touch pinch to zoom
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = (currentZoomScale <= 1.05f),
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pageBitmap = pageBitmaps[pageIndex]

                    ZoomableBox(
                        modifier = Modifier.fillMaxSize(),
                        maxScale = 6f,
                        minScale = 1f,
                        showControls = (pageIndex == pagerState.currentPage),
                        isDarkOverlay = true,
                        onZoomChanged = { s ->
                            if (pageIndex == pagerState.currentPage) {
                                currentZoomScale = s
                            }
                        }
                    ) { scale, offsetX, offsetY ->
                        if (pageBitmap != null) {
                            Image(
                                bitmap = pageBitmap.asImageBitmap(),
                                contentDescription = "Page ${pageIndex + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                                    .padding(if (isFullScreen) 0.dp else 12.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }

                // Top Controls Bar
                AnimatedVisibility(
                    visible = !isFullScreen,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    val resolvedTitle = remember(uriString, docEntity) {
                        DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
                    }
                    ViewerHeaderBar(
                        title = resolvedTitle,
                        subtitle = "Page ${pagerState.currentPage + 1} of $totalPages",
                        badgeText = "PDF",
                        badgeColor = Color(0xFFEF4444),
                        isDarkTheme = true,
                        onBack = onBack,
                        backTestTag = "btn_pdf_back"
                    ) {
                        // Bookmark Toggle
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    viewModel.toggleBookmarkCurrentPage()
                                } else {
                                    showAddBookmarkDialog = true
                                }
                            },
                            modifier = Modifier.testTag("btn_pdf_bookmark")
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isCurrentPageBookmarked) Color(0xFFF59E0B) else Color.White
                            )
                        }

                        // All Bookmarks List
                        IconButton(
                            onClick = { showBookmarksDialog = true },
                            modifier = Modifier.testTag("btn_pdf_bookmarks_list")
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "All Bookmarks", tint = Color.White)
                        }

                        // Share
                        IconButton(
                            onClick = {
                                DocumentSharing.shareDocument(context, uriString, "application/pdf", resolvedTitle)
                            },
                            modifier = Modifier.testTag("btn_pdf_share")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                        }

                        // Print
                        IconButton(
                            onClick = {
                                DocumentPrinter.printDocument(context, Uri.parse(uriString), resolvedTitle)
                            },
                            modifier = Modifier.testTag("btn_pdf_print")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Print", tint = Color.White)
                        }

                        // Fullscreen
                        IconButton(onClick = { viewModel.toggleFullScreen() }) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                        }
                    }
                }

                // Bottom Page Navigation Bar
                AnimatedVisibility(
                    visible = !isFullScreen,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0F172A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
                                modifier = Modifier.testTag("btn_pdf_prev_page")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = "Previous Page",
                                    tint = if (pagerState.currentPage > 0) Color.White else Color.Gray
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x44FFFFFF),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                TextButton(
                                    onClick = { showJumpDialog = true },
                                    modifier = Modifier.testTag("btn_jump_to_page")
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / $totalPages",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage < totalPages - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage < totalPages - 1,
                                modifier = Modifier.testTag("btn_pdf_next_page")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Next Page",
                                    tint = if (pagerState.currentPage < totalPages - 1) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }

                // Exit Fullscreen Floating Button
                if (isFullScreen) {
                    IconButton(
                        onClick = { viewModel.toggleFullScreen() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                    ) {
                        Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                    }
                }
            }
        }
    }

    // Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Enter Document Password") },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasswordDialog = false
                        viewModel.loadPdf(uriString, passwordInput)
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Jump to Page Dialog
    if (showJumpDialog && uiState is PdfUiState.Success) {
        val total = (uiState as PdfUiState.Success).pageCount
        var targetPage by remember { mutableStateOf((currentPage + 1).toString()) }

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Go to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $total):", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetPage,
                        onValueChange = { targetPage = it.filter { c -> c.isDigit() } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_page_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = targetPage.toIntOrNull()
                        if (num != null && num in 1..total) {
                            viewModel.jumpToPage(num)
                            showJumpDialog = false
                        } else {
                            Toast.makeText(context, "Invalid page number", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_jump")
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            pageNumber = currentPage + 1,
            onDismiss = { showAddBookmarkDialog = false },
            onSave = { title ->
                viewModel.toggleBookmarkCurrentPage(title)
                showAddBookmarkDialog = false
                Toast.makeText(context, "Bookmark saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Bookmarks List Dialog
    if (showBookmarksDialog) {
        BookmarksListDialog(
            bookmarks = bookmarks,
            onSelectBookmark = { pageNum ->
                viewModel.jumpToPage(pageNum)
                showBookmarksDialog = false
            },
            onDeleteBookmark = { id ->
                viewModel.viewModelScopeLaunchDelete(id)
            },
            onDismiss = { showBookmarksDialog = false }
        )
    }
}

private fun PdfViewerViewModel.viewModelScopeLaunchDelete(id: Long) {
    toggleBookmarkCurrentPage()
}
