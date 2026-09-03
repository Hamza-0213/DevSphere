package com.example.ui.viewers

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.ViewerHeaderBar
import com.example.ui.components.ZoomableBox

@Composable
fun ImageViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    viewModel: ImageViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val docEntity by viewModel.documentEntity.collectAsState()
    val rotationDegrees by viewModel.rotationDegrees.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()

    val isEditMode by viewModel.isEditMode.collectAsState()
    val flipH by viewModel.flipH.collectAsState()
    val flipV by viewModel.flipV.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(uriString) {
        viewModel.loadImage(uriString)
    }

    val resolvedTitle = remember(uriString, docEntity) {
        DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
    }
    val ext = remember(uriString, docEntity) {
        DocumentNameResolver.resolveExtension(uriString, docEntity).ifBlank { "IMG" }
    }

    // Color matrix filter calculation for preview
    val colorFilter = remember(activeFilter) {
        when (activeFilter) {
            "GRAYSCALE" -> {
                val cm = ColorMatrix()
                cm.setToSaturation(0f)
                ColorFilter.colorMatrix(cm)
            }
            "SEPIA" -> {
                val cm = ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                ColorFilter.colorMatrix(cm)
            }
            "INVERT" -> {
                val cm = ColorMatrix(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
                ColorFilter.colorMatrix(cm)
            }
            "WARM" -> {
                val cm = ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.85f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                ColorFilter.colorMatrix(cm)
            }
            else -> null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isFullScreen) {
                val subtitle = if (isEditMode) {
                    "Image Studio • ${activeFilter.lowercase().replaceFirstChar { it.uppercase() }} filter"
                } else if (uiState is ImageUiState.Success) {
                    val meta = (uiState as ImageUiState.Success).metadata
                    "${meta.width} × ${meta.height} px • ${meta.mimeType.substringAfter('/')}"
                } else {
                    "Image Preview"
                }

                ViewerHeaderBar(
                    title = resolvedTitle,
                    subtitle = subtitle,
                    badgeText = if (isEditMode) "EDIT" else ext.uppercase(),
                    badgeColor = if (isEditMode) MaterialTheme.colorScheme.primary else Color(0xFF8B5CF6),
                    isDarkTheme = true,
                    onBack = onBack,
                    backTestTag = "btn_image_back"
                ) {
                    // Edit Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier.testTag("btn_toggle_image_edit")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Edit Image",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    // Rotate
                    IconButton(onClick = { viewModel.rotate90() }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90", tint = Color.White)
                    }

                    // Share
                    IconButton(
                        onClick = {
                            DocumentSharing.shareDocument(context, uriString, "image/*", resolvedTitle)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                    }

                    // Print
                    IconButton(
                        onClick = {
                            DocumentPrinter.printDocument(context, Uri.parse(uriString), resolvedTitle)
                        }
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print", tint = Color.White)
                    }

                    // Fullscreen
                    IconButton(onClick = { viewModel.toggleFullScreen() }) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                    }
                }
            }
        },
        bottomBar = {
            // Image Editing Studio Bottom Bar
            AnimatedVisibility(
                visible = isEditMode && !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Title row with Reset and Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit & Filters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { viewModel.resetEdits() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp),
                                    border = BorderStroke(1.dp, Color(0xFF64748B))
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = Color.LightGray, fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.saveEditedImage(
                                            onSuccess = { newUri ->
                                                Toast.makeText(context, "Edited image saved to Library!", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    enabled = !isSaving,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("btn_save_edited_image"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save As Copy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Transform tools: Rotate, Flip H, Flip V
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate 90
                            OutlinedButton(
                                onClick = { viewModel.rotate90() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rotate 90°", color = Color.White, fontSize = 11.sp)
                            }

                            // Flip H
                            FilterChip(
                                selected = flipH,
                                onClick = { viewModel.toggleFlipH() },
                                label = { Text("Flip H", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color.LightGray,
                                    iconColor = Color.LightGray
                                )
                            )

                            // Flip V
                            FilterChip(
                                selected = flipV,
                                onClick = { viewModel.toggleFlipV() },
                                label = { Text("Flip V", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color.LightGray,
                                    iconColor = Color.LightGray
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset Color Filters
                        Text("Filters", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                "NORMAL" to "Original",
                                "GRAYSCALE" to "B&W Mono",
                                "SEPIA" to "Sepia Vintage",
                                "WARM" to "Warm Glow",
                                "INVERT" to "Inverted"
                            )

                            filters.forEach { (filterKey, filterLabel) ->
                                FilterChip(
                                    selected = (activeFilter == filterKey),
                                    onClick = { viewModel.setFilter(filterKey) },
                                    label = { Text(filterLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color.LightGray
                                    )
                                )
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
                .padding(if (isFullScreen) PaddingValues(0.dp) else paddingValues)
                .background(Color(0xFF0F172A))
                .testTag("image_viewer_content"),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ImageUiState.Loading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                is ImageUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to display image", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message, color = Color.LightGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
                is ImageUiState.Success -> {
                    ZoomableBox(
                        modifier = Modifier.fillMaxSize(),
                        maxScale = 6f,
                        minScale = 1f,
                        showControls = !isEditMode,
                        isDarkOverlay = true
                    ) { scale, offsetX, offsetY ->
                        AsyncImage(
                            model = Uri.parse(uriString),
                            contentDescription = docEntity?.displayName ?: "Image",
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = if (flipH) -scale else scale,
                                    scaleY = if (flipV) -scale else scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                    rotationZ = rotationDegrees
                                )
                        )
                    }

                    // Exit Fullscreen Button
                    if (isFullScreen) {
                        IconButton(
                            onClick = { viewModel.toggleFullScreen() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                        ) {
                            Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
