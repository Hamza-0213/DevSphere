package com.example.ui.viewers

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.document.powerpoint.PptxSlide
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerPointViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    viewModel: PowerPointViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val docEntity by viewModel.documentEntity.collectAsState()
    val currentSlideIndex by viewModel.currentSlideIndex.collectAsState()
    val isFullscreen by viewModel.isFullscreenPresentation.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val matchingIndices by viewModel.matchingSlideIndices.collectAsState()
    val fontScale by viewModel.fontSizeScale.collectAsState()
    val showNotes by viewModel.showSpeakerNotes.collectAsState()

    LaunchedEffect(uriString) {
        viewModel.loadPresentation(uriString)
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_ppt_back")) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = docEntity?.displayName ?: "Presentation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState is PptUiState.Success) {
                                    val pres = (uiState as PptUiState.Success).presentation
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Slide ${currentSlideIndex + 1} of ${pres.totalSlides}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = pres.format,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Search button
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search Presentation",
                                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Fullscreen Presentation Mode
                            IconButton(onClick = { viewModel.toggleFullscreen() }) {
                                Icon(
                                    Icons.Filled.Slideshow,
                                    contentDescription = "Slideshow Mode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Print
                            IconButton(
                                onClick = {
                                    DocumentPrinter.printDocument(
                                        context,
                                        Uri.parse(uriString),
                                        docEntity?.displayName ?: "Presentation"
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = "Print")
                            }

                            // Share
                            IconButton(
                                onClick = {
                                    val mime = if (docEntity?.extension?.lowercase() == "ppt") {
                                        "application/vnd.ms-powerpoint"
                                    } else {
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                    }
                                    DocumentSharing.shareDocument(
                                        context,
                                        uriString,
                                        mime,
                                        docEntity?.displayName
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                        }

                        // Search Bar expansion
                        AnimatedVisibility(visible = isSearchActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search in slides...", fontSize = 13.sp) },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(Icons.Filled.Close, contentDescription = "Clear Search", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (matchingIndices.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${matchingIndices.size} match${if (matchingIndices.size > 1) "es" else ""}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
                .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
                .background(if (isFullscreen) Color(0xFF0F172A) else MaterialTheme.colorScheme.background)
                .testTag("powerpoint_viewer_content")
        ) {
            when (val state = uiState) {
                is PptUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Loading presentation...", fontSize = 14.sp)
                    }
                }
                is PptUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to open presentation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
                is PptUiState.Success -> {
                    val slides = state.presentation.slides
                    val currentSlide = slides.getOrNull(currentSlideIndex) ?: slides.first()

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Zoom and Tool bar
                        if (!isFullscreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentSlide.notes != null) {
                                    IconButton(
                                        onClick = { viewModel.toggleSpeakerNotes() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.RecordVoiceOver,
                                            contentDescription = "Speaker Notes",
                                            tint = if (showNotes) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.zoomOut() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "${(fontScale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { viewModel.zoomIn() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Slide Deck Stage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(if (isFullscreen) 16.dp else 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SlideCard(
                                slide = currentSlide,
                                totalSlides = slides.size,
                                isFullscreen = isFullscreen,
                                fontScale = fontScale,
                                showNotes = showNotes,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Bottom Navigation Strip
                        if (!isFullscreen) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 10.dp)
                            ) {
                                // Controls: Prev / Next / Counter
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.previousSlide() },
                                        enabled = currentSlideIndex > 0,
                                        modifier = Modifier.testTag("btn_ppt_prev")
                                    ) {
                                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Slide")
                                    }

                                    Text(
                                        text = "${currentSlideIndex + 1} / ${slides.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(
                                        onClick = { viewModel.nextSlide() },
                                        enabled = currentSlideIndex < slides.size - 1,
                                        modifier = Modifier.testTag("btn_ppt_next")
                                    ) {
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next Slide")
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Slide Thumbnail Strip
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(slides) { index, slide ->
                                        val isSearchMatch = matchingIndices.contains(index)
                                        SlideThumbnail(
                                            slide = slide,
                                            isSelected = (currentSlideIndex == index),
                                            isSearchMatch = isSearchMatch,
                                            onClick = { viewModel.goToSlide(index) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Fullscreen Floating Controls
                    if (isFullscreen) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color(0xCC1E293B))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousSlide() },
                                enabled = currentSlideIndex > 0
                            ) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = Color.White)
                            }
                            Text(
                                text = "${currentSlideIndex + 1} / ${slides.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { viewModel.nextSlide() },
                                enabled = currentSlideIndex < slides.size - 1
                            ) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.toggleFullscreen() }
                            ) {
                                Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideCard(
    slide: PptxSlide,
    totalSlides: Int,
    isFullscreen: Boolean,
    fontScale: Float = 1.0f,
    showNotes: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFullscreen) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) 28.dp else 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // Slide Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "SLIDE ${slide.slideNumber}",
                        fontSize = (11 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${slide.slideNumber} / $totalSlides",
                    fontSize = (12 * fontScale).sp,
                    color = if (isFullscreen) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Slide Title
            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isFullscreen) (24 * fontScale).sp else (20 * fontScale).sp
                ),
                color = if (isFullscreen) Color.White else MaterialTheme.colorScheme.onSurface
            )

            // Subtitle if available
            if (!slide.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = slide.subtitle,
                    fontSize = (15 * fontScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isFullscreen) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slide Bullet Points
            slide.bulletPoints.forEach { bullet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•  ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = (18 * fontScale).sp
                    )
                    Text(
                        text = bullet,
                        fontSize = if (isFullscreen) (16 * fontScale).sp else (14 * fontScale).sp,
                        color = if (isFullscreen) Color.LightGray else MaterialTheme.colorScheme.onSurface,
                        lineHeight = (22 * fontScale).sp
                    )
                }
            }

            // Other Text Blocks
            slide.textBlocks.forEach { block ->
                Text(
                    text = block,
                    fontSize = if (isFullscreen) (15 * fontScale).sp else (14 * fontScale).sp,
                    color = if (isFullscreen) Color.LightGray else MaterialTheme.colorScheme.onSurface,
                    lineHeight = (22 * fontScale).sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Speaker Notes section
            if (showNotes && !slide.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFullscreen) Color(0xFF334155) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Speaker Notes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = slide.notes,
                            fontSize = (13 * fontScale).sp,
                            color = if (isFullscreen) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = (19 * fontScale).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideThumbnail(
    slide: PptxSlide,
    isSelected: Boolean,
    isSearchMatch: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 96.dp, height = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isSearchMatch -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = when {
            isSelected -> androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            isSearchMatch -> androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${slide.slideNumber}. ${slide.title}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Slide ${slide.slideNumber}",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (slide.notes != null) {
                    Icon(
                        Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
