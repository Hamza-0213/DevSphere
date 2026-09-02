package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High performance Zoomable Container providing:
 * - Multi-touch pinch-to-zoom with fingers (1x up to 6x)
 * - Freeform 2D panning when zoomed in
 * - Double-tap to zoom in / reset to 1x
 * - Optional floating zoom overlay pills (+, -, reset)
 * - Dynamic scroll locking callback for parent Pagers / Swipers
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 6f,
    minScale: Float = 1f,
    showControls: Boolean = true,
    isDarkOverlay: Boolean = true,
    onZoomChanged: ((scale: Float) -> Unit)? = null,
    content: @Composable BoxScope.(scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun updateScale(newScale: Float, focusX: Float = 0f, focusY: Float = 0f) {
        val clamped = newScale.coerceIn(minScale, maxScale)
        scale = clamped
        if (clamped <= minScale + 0.02f) {
            offsetX = 0f
            offsetY = 0f
            scale = 1f
        }
        onZoomChanged?.invoke(scale)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                            // Center on tap point
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val maxOffsetX = (size.width * (2.5f - 1f)) / 2f
                            val maxOffsetY = (size.height * (2.5f - 1f)) / 2f
                            offsetX = ((cx - tapOffset.x) * 1.5f).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = ((cy - tapOffset.y) * 1.5f).coerceIn(-maxOffsetY, maxOffsetY)
                        }
                        onZoomChanged?.invoke(scale)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    scale = newScale

                    if (newScale > 1.02f) {
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    onZoomChanged?.invoke(scale)
                }
            }
    ) {
        // Content rendering
        content(scale, offsetX, offsetY)

        // Floating Zoom Controls Pill
        if (showControls) {
            AnimatedVisibility(
                visible = scale > 1.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDarkOverlay) Color(0xDD0F172A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                updateScale(scale - 0.5f)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_zoom_out")
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Zoom Out",
                                tint = if (isDarkOverlay) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${(scale * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkOverlay) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                updateScale(scale + 0.5f)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_zoom_in")
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Zoom In",
                                tint = if (isDarkOverlay) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                onZoomChanged?.invoke(1f)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_zoom_reset")
                        ) {
                            Icon(
                                Icons.Filled.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = if (isDarkOverlay) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
