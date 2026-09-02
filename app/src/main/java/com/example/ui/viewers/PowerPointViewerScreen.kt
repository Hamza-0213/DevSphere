package com.example.ui.viewers

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.document.powerpoint.PptElementType
import com.example.document.powerpoint.PptSlide
import com.example.document.powerpoint.PptSlideElement
import com.example.document.powerpoint.PptTextRun
import com.example.document.powerpoint.PptxPresentation
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.ZoomableBox
import com.example.ui.components.DocumentInfoDialog
import com.example.ui.components.ViewerHeaderBar

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
    val viewMode by viewModel.viewMode.collectAsState()
    val isFullscreen by viewModel.isFullscreenPresentation.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val matchingIndices by viewModel.matchingSlideIndices.collectAsState()
    val fontScale by viewModel.fontSizeScale.collectAsState()
    val showNotes by viewModel.showSpeakerNotes.collectAsState()

    var showControlsInFullscreen by remember { mutableStateOf(true) }

    LaunchedEffect(uriString) {
        viewModel.loadPresentation(uriString)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isFullscreen) {
                val resolvedTitle = remember(uriString, docEntity) {
                    DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
                }
                val ext = remember(uriString, docEntity) {
                    DocumentNameResolver.resolveExtension(uriString, docEntity).ifBlank { "PPTX" }
                }
                val subtitleText = if (uiState is PptUiState.Success) {
                    val pres = (uiState as PptUiState.Success).presentation
                    "Slide ${currentSlideIndex + 1} of ${pres.totalSlides}"
                } else {
                    "Presentation Slides"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    ViewerHeaderBar(
                        title = resolvedTitle,
                        subtitle = subtitleText,
                        badgeText = ext.uppercase(),
                        badgeColor = Color(0xFFF97316),
                        isDarkTheme = false,
                        onBack = onBack,
                        backTestTag = "btn_ppt_back"
                    ) {
                        // Search button
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search Presentation",
                                tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fullscreen Slideshow Mode
                        IconButton(onClick = { viewModel.toggleFullscreen() }) {
                            Icon(
                                Icons.Filled.Slideshow,
                                contentDescription = "Slideshow Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val mime = if (ext.lowercase() == "ppt") {
                                    "application/vnd.ms-powerpoint"
                                } else {
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                }
                                DocumentSharing.shareDocument(
                                    context,
                                    uriString,
                                    mime,
                                    resolvedTitle
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }

                        // Print
                        IconButton(
                            onClick = {
                                DocumentPrinter.printDocument(
                                    context,
                                    Uri.parse(uriString),
                                    resolvedTitle
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Print")
                        }
                    }

                    // Search Bar expansion
                    AnimatedVisibility(visible = isSearchActive) {
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
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search in presentation slides...", fontSize = 13.sp) },
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
                .background(if (isFullscreen) Color(0xFF090D16) else MaterialTheme.colorScheme.background)
                .testTag("powerpoint_viewer_content")
        ) {
            when (val state = uiState) {
                is PptUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Rendering slide deck & media assets...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    val pres = state.presentation
                    val slides = pres.slides
                    val currentSlide = slides.getOrNull(currentSlideIndex) ?: slides.first()

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Toolbar: View Mode Switcher, Image Count & Zoom
                        if (!isFullscreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // View Mode Toggle (Slide vs Notes)
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (viewMode == PptViewMode.SLIDE_CANVAS) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier.clickable { viewModel.setViewMode(PptViewMode.SLIDE_CANVAS) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.ViewCarousel,
                                                        contentDescription = "Slide View",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = if (viewMode == PptViewMode.SLIDE_CANVAS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        "Slide",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (viewMode == PptViewMode.SLIDE_CANVAS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (viewMode == PptViewMode.OUTLINE) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier.clickable { viewModel.setViewMode(PptViewMode.OUTLINE) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.FormatListBulleted,
                                                        contentDescription = "Outline View",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = if (viewMode == PptViewMode.OUTLINE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        "Notes",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (viewMode == PptViewMode.OUTLINE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (currentSlide.images.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    "${currentSlide.images.size} ${if (currentSlide.images.size > 1) "images" else "image"}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }

                                    if (currentSlide.notes != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.toggleSpeakerNotes() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.RecordVoiceOver,
                                                contentDescription = "Speaker Notes",
                                                tint = if (showNotes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.zoomOut() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${(fontScale * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { viewModel.zoomIn() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Slide Presentation Stage
                        var pptZoomScale by remember { mutableFloatStateOf(1f) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(if (isFullscreen) 8.dp else 12.dp)
                                .pointerInput(pptZoomScale) {
                                    if (pptZoomScale <= 1.05f) {
                                        var totalDragX = 0f
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, dragAmount ->
                                                change.consume()
                                                totalDragX += dragAmount
                                            },
                                            onDragEnd = {
                                                if (totalDragX < -50f) {
                                                    viewModel.nextSlide()
                                                } else if (totalDragX > 50f) {
                                                    viewModel.previousSlide()
                                                }
                                                totalDragX = 0f
                                            },
                                            onDragCancel = {
                                                totalDragX = 0f
                                            }
                                        )
                                    }
                                }
                                .clickable {
                                    if (isFullscreen) {
                                        showControlsInFullscreen = !showControlsInFullscreen
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewMode == PptViewMode.SLIDE_CANVAS) {
                                ZoomableBox(
                                    modifier = Modifier.fillMaxSize(),
                                    maxScale = 5f,
                                    minScale = 1f,
                                    showControls = true,
                                    isDarkOverlay = isFullscreen,
                                    onZoomChanged = { pptZoomScale = it }
                                ) { scale, offsetX, offsetY ->
                                    DesignedSlideCanvas(
                                        slide = currentSlide,
                                        presentation = pres,
                                        totalSlides = slides.size,
                                        isFullscreen = isFullscreen,
                                        fontScale = fontScale,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.Center)
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                            )
                                    )
                                }
                            } else {
                                OutlineNotesView(
                                    slide = currentSlide,
                                    totalSlides = slides.size,
                                    fontScale = fontScale,
                                    showNotes = showNotes,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Bottom Navigation Strip & Thumbnails
                        if (!isFullscreen) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(top = 4.dp, bottom = 8.dp)
                            ) {
                                // Controls: Prev / Next / Counter
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
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

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Slide ${currentSlideIndex + 1} of ${slides.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.nextSlide() },
                                        enabled = currentSlideIndex < slides.size - 1,
                                        modifier = Modifier.testTag("btn_ppt_next")
                                    ) {
                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next Slide")
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Slide Thumbnail Strip
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(
                                        items = slides,
                                        key = { index, slide -> "${slide.slideNumber}_$index" }
                                    ) { index, slide ->
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

                    // Fullscreen Floating Presentation Controls
                    if (isFullscreen && showControlsInFullscreen) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xE61E293B))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousSlide() },
                                enabled = currentSlideIndex > 0
                            ) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = if (currentSlideIndex > 0) Color.White else Color.Gray)
                            }
                            Text(
                                text = "${currentSlideIndex + 1} / ${slides.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            IconButton(
                                onClick = { viewModel.nextSlide() },
                                enabled = currentSlideIndex < slides.size - 1
                            ) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = if (currentSlideIndex < slides.size - 1) Color.White else Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
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

/**
 * High-Fidelity Presentation Slide Canvas with responsive 16:9 layout,
 * image embedding, shape backgrounds, custom typography, and theme styling.
 */
@Composable
private fun DesignedSlideCanvas(
    slide: PptSlide,
    presentation: PptxPresentation,
    totalSlides: Int,
    isFullscreen: Boolean,
    fontScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val theme = getSlideTheme(slide.themeVariant, slide.backgroundColorHex)
    val aspectRatio = 16f / 9f

    Card(
        modifier = modifier
            .widthIn(max = 850.dp)
            .aspectRatio(aspectRatio),
        shape = RoundedCornerShape(if (isFullscreen) 8.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.backgroundSurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFullscreen) 0.dp else 6.dp),
        border = BorderStroke(1.dp, theme.canvasBorderColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (theme.backgroundGradient != null) {
                        Brush.linearGradient(theme.backgroundGradient)
                    } else {
                        Brush.verticalGradient(listOf(theme.backgroundSurfaceColor, theme.backgroundSurfaceColor))
                    }
                )
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            // Check if slide contains rich absolute positioned elements from OpenXML parser
            val hasPositionedElements = slide.elements.size > 1 || (slide.elements.isNotEmpty() && slide.elements.any { it.type == PptElementType.IMAGE || it.type == PptElementType.TABLE })

            if (hasPositionedElements) {
                // Render absolute positioned slide elements exactly as defined in OpenXML
                AbsoluteElementsCanvas(
                    slide = slide,
                    theme = theme,
                    totalSlides = totalSlides,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    fontScale = fontScale
                )
            } else {
                // Synthesize a structured, magazine-grade designed slide layout
                StructuredDesignLayout(
                    slide = slide,
                    theme = theme,
                    totalSlides = totalSlides,
                    fontScale = fontScale,
                    isFullscreen = isFullscreen
                )
            }

            // Slide Index Pill in bottom right
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.badgeBgColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${slide.slideNumber} / $totalSlides",
                    fontSize = (9 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.badgeTextColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Renders positioned OpenXML shapes, images, text frames and tables.
 */
@Composable
private fun AbsoluteElementsCanvas(
    slide: PptSlide,
    theme: SlideDesignTheme,
    totalSlides: Int,
    canvasWidth: androidx.compose.ui.unit.Dp,
    canvasHeight: androidx.compose.ui.unit.Dp,
    fontScale: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        slide.elements.forEach { element ->
            val elementX = canvasWidth * element.xRatio
            val elementY = canvasHeight * element.yRatio
            val elementW = canvasWidth * element.widthRatio
            val elementH = canvasHeight * element.heightRatio

            Box(
                modifier = Modifier
                    .offset(x = elementX, y = elementY)
                    .width(elementW)
                    .height(elementH)
                    .clip(RoundedCornerShape(element.cornerRadiusDp.dp))
                    .then(
                        if (element.backgroundColorHex != null) {
                            Modifier.background(Color(element.backgroundColorHex))
                        } else Modifier
                    )
                    .then(
                        if (element.borderColorHex != null && element.borderWidthDp > 0) {
                            Modifier.border(element.borderWidthDp.dp, Color(element.borderColorHex), RoundedCornerShape(element.cornerRadiusDp.dp))
                        } else Modifier
                    )
                    .padding(4.dp)
            ) {
                when (element.type) {
                    PptElementType.IMAGE -> {
                        if (element.imageBytes != null) {
                            val bitmap = remember(element.imageBytes) {
                                try {
                                    BitmapFactory.decodeByteArray(element.imageBytes, 0, element.imageBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Slide Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    PptElementType.TABLE -> {
                        RenderSlideTable(element.tableRows, theme, fontScale)
                    }
                    PptElementType.TEXT_BOX, PptElementType.SHAPE -> {
                        RenderTextBox(element, theme, fontScale)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTextBox(
    element: PptSlideElement,
    theme: SlideDesignTheme,
    fontScale: Float
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = when (element.alignment) {
            "center" -> Alignment.CenterHorizontally
            "right" -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        if (element.textRuns.isNotEmpty()) {
            element.textRuns.forEach { run ->
                val runColor = if (run.fontColorHex != null) Color(run.fontColorHex) else theme.primaryTextColor
                Text(
                    text = if (run.isBullet) "${run.bulletChar ?: "•"} ${run.text}" else run.text,
                    fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                    fontSize = (run.fontSizeSp * 0.85f * fontScale).sp,
                    color = runColor,
                    textAlign = when (element.alignment) {
                        "center" -> TextAlign.Center
                        "right" -> TextAlign.End
                        else -> TextAlign.Start
                    }
                )
            }
        } else if (element.plainText.isNotBlank()) {
            Text(
                text = element.plainText,
                fontWeight = if (element.title != null) FontWeight.Bold else FontWeight.Normal,
                fontSize = (if (element.title != null) 18f else 13f * fontScale).sp,
                color = theme.primaryTextColor,
                textAlign = when (element.alignment) {
                    "center" -> TextAlign.Center
                    "right" -> TextAlign.End
                    else -> TextAlign.Start
                }
            )
        }
    }
}

@Composable
private fun RenderSlideTable(
    rows: List<List<String>>,
    theme: SlideDesignTheme,
    fontScale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, theme.canvasBorderColor, RoundedCornerShape(6.dp))
    ) {
        rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (isHeader) theme.badgeBgColor else Color.Transparent)
                    .border(0.5.dp, theme.canvasBorderColor.copy(alpha = 0.5f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cell,
                            fontSize = (11 * fontScale).sp,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHeader) theme.accentColor else theme.primaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Balanced, magazine-grade designed layout for slides with text, bullets, and extracted images.
 */
@Composable
private fun StructuredDesignLayout(
    slide: PptSlide,
    theme: SlideDesignTheme,
    totalSlides: Int,
    fontScale: Float,
    isFullscreen: Boolean
) {
    val hasImages = slide.images.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isFullscreen) 20.dp else 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header Bar with category/slide pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.badgeBgColor
                ) {
                    Text(
                        text = "SLIDE ${slide.slideNumber}",
                        fontSize = (10 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slide Title
            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (20 * fontScale).sp,
                    lineHeight = (26 * fontScale).sp
                ),
                color = theme.primaryTextColor
            )

            // Subtitle if available
            if (!slide.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slide.subtitle,
                    fontSize = (13 * fontScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.secondaryTextColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content: If images exist, render side-by-side or stacked
            if (hasImages) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left column: Bullet points & text blocks
                    Column(modifier = Modifier.weight(0.55f)) {
                        renderBulletsAndBlocks(slide, theme, fontScale)
                    }

                    // Right column: Images
                    Column(
                        modifier = Modifier.weight(0.45f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slide.images.take(2).forEach { imageBytes ->
                            val bitmap = remember(imageBytes) {
                                try {
                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, theme.canvasBorderColor)
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Slide Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Full width bullet points and cards layout
                renderBulletsAndBlocks(slide, theme, fontScale)
            }
        }
    }
}

@Composable
private fun renderBulletsAndBlocks(
    slide: PptSlide,
    theme: SlideDesignTheme,
    fontScale: Float
) {
    if (slide.bulletPoints.isNotEmpty()) {
        slide.bulletPoints.forEach { bullet ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = theme.accentColor.copy(alpha = 0.18f),
                    modifier = Modifier
                        .size((18 * fontScale).dp)
                        .padding(top = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor,
                            fontSize = (12 * fontScale).sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bullet,
                    fontSize = (13 * fontScale).sp,
                    color = theme.primaryTextColor,
                    lineHeight = (18 * fontScale).sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }

    slide.textBlocks.forEach { block ->
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.cardBgColor,
            border = BorderStroke(1.dp, theme.canvasBorderColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = block,
                fontSize = (12.5f * fontScale).sp,
                color = theme.primaryTextColor,
                lineHeight = (18 * fontScale).sp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

/**
 * Outline and notes view for document reading.
 */
@Composable
private fun OutlineNotesView(
    slide: PptSlide,
    totalSlides: Int,
    fontScale: Float,
    showNotes: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "SLIDE ${slide.slideNumber} OF $totalSlides",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = slide.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                fontSize = (22 * fontScale).sp
            )

            if (!slide.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slide.subtitle,
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extracted images preview
            if (slide.images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    itemsIndexed(slide.images) { _, imgBytes ->
                        val bitmap = remember(imgBytes) {
                            try {
                                BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Slide Image",
                                    modifier = Modifier
                                        .height(120.dp)
                                        .widthIn(max = 200.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            // Bullet points
            slide.bulletPoints.forEach { bullet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    Text(text = bullet, fontSize = (14 * fontScale).sp, lineHeight = (20 * fontScale).sp)
                }
            }

            // Text Blocks
            slide.textBlocks.forEach { block ->
                Text(
                    text = block,
                    fontSize = (14 * fontScale).sp,
                    lineHeight = (20 * fontScale).sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Speaker notes section
            if (showNotes && !slide.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Speaker Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = slide.notes,
                            fontSize = (13 * fontScale).sp,
                            lineHeight = (19 * fontScale).sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide Thumbnail preview.
 */
@Composable
private fun SlideThumbnail(
    slide: PptSlide,
    isSelected: Boolean,
    isSearchMatch: Boolean = false,
    onClick: () -> Unit
) {
    val theme = getSlideTheme(slide.themeVariant, slide.backgroundColorHex)

    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 66.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else theme.backgroundSurfaceColor
        ),
        border = when {
            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            isSearchMatch -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else theme.primaryTextColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slide ${slide.slideNumber}",
                    fontSize = 8.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else theme.secondaryTextColor
                )
                if (slide.images.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Has image",
                        modifier = Modifier.size(10.dp),
                        tint = theme.accentColor
                    )
                }
            }
        }
    }
}

/**
 * Theme definition for slide canvas styling.
 */
data class SlideDesignTheme(
    val backgroundSurfaceColor: Color,
    val backgroundGradient: List<Color>? = null,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val cardBgColor: Color,
    val canvasBorderColor: Color
)

private fun getSlideTheme(variant: Int, customBgHex: Long?): SlideDesignTheme {
    if (customBgHex != null) {
        val bgColor = Color(customBgHex)
        val isDark = isDarkColor(bgColor)
        return SlideDesignTheme(
            backgroundSurfaceColor = bgColor,
            primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
            badgeBgColor = if (isDark) Color(0x3360A5FA) else Color(0x222563EB),
            badgeTextColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF),
            cardBgColor = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000),
            canvasBorderColor = if (isDark) Color(0x26FFFFFF) else Color(0x1A000000)
        )
    }

    return when (variant % 6) {
        0 -> SlideDesignTheme(
            // Modern Studio White
            backgroundSurfaceColor = Color(0xFFFFFFFF),
            primaryTextColor = Color(0xFF0F172A),
            secondaryTextColor = Color(0xFF475569),
            accentColor = Color(0xFF2563EB),
            badgeBgColor = Color(0xFFEFF6FF),
            badgeTextColor = Color(0xFF1D4ED8),
            cardBgColor = Color(0xFFF8FAFC),
            canvasBorderColor = Color(0xFFE2E8F0)
        )
        1 -> SlideDesignTheme(
            // Executive Slate / Dark
            backgroundSurfaceColor = Color(0xFF0F172A),
            backgroundGradient = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
            primaryTextColor = Color(0xFFF8FAFC),
            secondaryTextColor = Color(0xFF94A3B8),
            accentColor = Color(0xFF38BDF8),
            badgeBgColor = Color(0x2638BDF8),
            badgeTextColor = Color(0xFF7DD3FC),
            cardBgColor = Color(0x1AFFFFFF),
            canvasBorderColor = Color(0x26FFFFFF)
        )
        2 -> SlideDesignTheme(
            // Emerald Tech
            backgroundSurfaceColor = Color(0xFFF0FDF4),
            primaryTextColor = Color(0xFF064E3B),
            secondaryTextColor = Color(0xFF065F46),
            accentColor = Color(0xFF059669),
            badgeBgColor = Color(0xFFDCFCE7),
            badgeTextColor = Color(0xFF047857),
            cardBgColor = Color(0xFFFFFFFF),
            canvasBorderColor = Color(0xFFBBF7D0)
        )
        3 -> SlideDesignTheme(
            // Warm Ochre / Cream
            backgroundSurfaceColor = Color(0xFFFFFBEB),
            primaryTextColor = Color(0xFF451A03),
            secondaryTextColor = Color(0xFF78350F),
            accentColor = Color(0xFFD97706),
            badgeBgColor = Color(0xFFFEF3C7),
            badgeTextColor = Color(0xFFB45309),
            cardBgColor = Color(0xFFFFFFFF),
            canvasBorderColor = Color(0xFFFDE68A)
        )
        4 -> SlideDesignTheme(
            // Royal Indigo
            backgroundSurfaceColor = Color(0xFF1E1B4B),
            backgroundGradient = listOf(Color(0xFF1E1B4B), Color(0xFF312E81)),
            primaryTextColor = Color(0xFFEEF2FF),
            secondaryTextColor = Color(0xFFC7D2FE),
            accentColor = Color(0xFF818CF8),
            badgeBgColor = Color(0x33818CF8),
            badgeTextColor = Color(0xFFA5B4FC),
            cardBgColor = Color(0x1AFFFFFF),
            canvasBorderColor = Color(0x26818CF8)
        )
        else -> SlideDesignTheme(
            // Violet Clean
            backgroundSurfaceColor = Color(0xFFFAF5FF),
            primaryTextColor = Color(0xFF3B0764),
            secondaryTextColor = Color(0xFF581C87),
            accentColor = Color(0xFF7C3AED),
            badgeBgColor = Color(0xFFF3E8FF),
            badgeTextColor = Color(0xFF6D28D9),
            cardBgColor = Color(0xFFFFFFFF),
            canvasBorderColor = Color(0xFFE9D5FF)
        )
    }
}

private fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}
