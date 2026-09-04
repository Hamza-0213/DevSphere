package com.example.ui.viewers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.document.powerpoint.PptSlide
import com.example.document.powerpoint.PptxPresentation

/**
 * Slide Sorter / Grid View Modal Dialog:
 * Shows all slides in a clean multi-column card grid, allowing instant jumping,
 * visual scanning, and overview of the deck.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideSorterBottomSheet(
    presentation: PptxPresentation,
    currentSlideIndex: Int,
    onSlideSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Slide Sorter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${presentation.slides.size} slides in presentation",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Slide Sorter")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(450.dp)
            ) {
                itemsIndexed(presentation.slides) { index, slide ->
                    val isCurrent = index == currentSlideIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSlideSelected(index)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isCurrent) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${slide.slideNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        "Current",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = slide.title.ifBlank { "Slide ${slide.slideNumber}" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (slide.bulletPoints.isNotEmpty()) {
                                    "${slide.bulletPoints.size} bullets"
                                } else if (slide.textBlocks.isNotEmpty()) {
                                    "${slide.textBlocks.size} paragraphs"
                                } else if (slide.images.isNotEmpty()) {
                                    "${slide.images.size} images"
                                } else {
                                    "Slide item"
                                },
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide Presentation Options & Transitions Sheet:
 * Allows configuring slide transition animations, auto-advance timing,
 * and presentation timing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationSettingsBottomSheet(
    currentTransition: String,
    onTransitionChange: (String) -> Unit,
    isAutoPlayRunning: Boolean,
    onToggleAutoPlay: () -> Unit,
    autoPlayInterval: Int,
    onIntervalChange: (Int) -> Unit,
    elapsedSeconds: Int,
    isTimerRunning: Boolean,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slide Show Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Transitions Section
            Text(
                text = "Slide Transition Animation",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val transitions = listOf("SLIDE" to "Push", "FADE" to "Fade", "ZOOM" to "Zoom", "NONE" to "Cut")
                transitions.forEach { (key, label) ->
                    FilterChip(
                        selected = currentTransition == key,
                        onClick = { onTransitionChange(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Presenter Timer Section
            Text(
                text = "Presenter Timer",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    val formattedTime = String.format("%02d:%02d", minutes, seconds)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = null,
                            tint = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleTimer) {
                            Icon(
                                if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause Timer" else "Start Timer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onResetTimer) {
                            Icon(
                                Icons.Filled.Restore,
                                contentDescription = "Reset Timer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Auto-Advance Slideshow
            Text(
                text = "Auto-Play Slideshow",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 8, 10).forEach { sec ->
                        FilterChip(
                            selected = autoPlayInterval == sec,
                            onClick = { onIntervalChange(sec) },
                            label = { Text("${sec}s", fontSize = 12.sp) }
                        )
                    }
                }
                TextButton(
                    onClick = onToggleAutoPlay
                ) {
                    Icon(
                        if (isAutoPlayRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAutoPlayRunning) "Stop Loop" else "Start Loop", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Presenter Tools Bar:
 * Pen, Highlighter, Laser Pointer, Color selector, Undo, and Clear
 */
@Composable
fun PresenterToolsBar(
    activeTool: PptToolMode,
    onToolSelected: (PptToolMode) -> Unit,
    penColor: Color,
    onColorSelected: (Color) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF01E293B),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Pointer
            IconButton(
                onClick = { onToolSelected(if (activeTool == PptToolMode.LASER_POINTER) PptToolMode.NONE else PptToolMode.LASER_POINTER) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.FlashlightOn,
                    contentDescription = "Laser Pointer",
                    tint = if (activeTool == PptToolMode.LASER_POINTER) Color(0xFFFF4444) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Pen
            IconButton(
                onClick = { onToolSelected(if (activeTool == PptToolMode.PEN) PptToolMode.NONE else PptToolMode.PEN) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Create,
                    contentDescription = "Pen Tool",
                    tint = if (activeTool == PptToolMode.PEN) penColor else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Highlighter
            IconButton(
                onClick = { onToolSelected(if (activeTool == PptToolMode.HIGHLIGHTER) PptToolMode.NONE else PptToolMode.HIGHLIGHTER) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Highlight,
                    contentDescription = "Highlighter",
                    tint = if (activeTool == PptToolMode.HIGHLIGHTER) Color(0xFFFACC15) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Palette colors if Pen or Highlighter active
            if (activeTool == PptToolMode.PEN || activeTool == PptToolMode.HIGHLIGHTER) {
                val colors = listOf(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color.White)
                colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (penColor == c) 2.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(c) }
                    )
                }

                // Undo
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Undo, contentDescription = "Undo stroke", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Clear
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear ink", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
