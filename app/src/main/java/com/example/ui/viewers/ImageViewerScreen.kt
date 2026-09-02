package com.example.ui.viewers

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.filemanager.DocumentNameResolver
import com.example.filemanager.DocumentPrinter
import com.example.filemanager.DocumentSharing
import com.example.ui.components.DocumentInfoDialog
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

    LaunchedEffect(uriString) {
        viewModel.loadImage(uriString)
    }

    val resolvedTitle = remember(uriString, docEntity) {
        DocumentNameResolver.resolveDisplayName(uriString, docEntity, context)
    }
    val ext = remember(uriString, docEntity) {
        DocumentNameResolver.resolveExtension(uriString, docEntity).ifBlank { "IMG" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isFullScreen) {
                val subtitle = if (uiState is ImageUiState.Success) {
                    val meta = (uiState as ImageUiState.Success).metadata
                    "${meta.width} × ${meta.height} px • ${meta.mimeType.substringAfter('/')}"
                } else {
                    "Image Preview"
                }

                ViewerHeaderBar(
                    title = resolvedTitle,
                    subtitle = subtitle,
                    badgeText = ext.uppercase(),
                    badgeColor = Color(0xFF8B5CF6),
                    isDarkTheme = true,
                    onBack = onBack,
                    backTestTag = "btn_image_back"
                ) {
                    // Rotate
                    IconButton(onClick = { viewModel.rotate90() }) {
                        Icon(Icons.Filled.RotateRight, contentDescription = "Rotate 90", tint = Color.White)
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
                        showControls = true,
                        isDarkOverlay = true
                    ) { scale, offsetX, offsetY ->
                        AsyncImage(
                            model = Uri.parse(uriString),
                            contentDescription = docEntity?.displayName ?: "Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
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
