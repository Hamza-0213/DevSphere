package com.example.ui.viewers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Slide Presentation Transition Host.
 * Supports smooth PowerPoint transition effects: "SLIDE", "FADE", "ZOOM", "NONE"
 */
@Composable
fun SlideTransitionHost(
    slideIndex: Int,
    transitionStyle: String,
    modifier: Modifier = Modifier,
    content: @Composable (targetIndex: Int) -> Unit
) {
    AnimatedContent(
        targetState = slideIndex,
        transitionSpec = {
            when (transitionStyle) {
                "FADE" -> {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(300))
                }
                "ZOOM" -> {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.88f, animationSpec = tween(350))) togetherWith
                            (fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 1.08f, animationSpec = tween(300)))
                }
                "NONE" -> {
                    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                }
                else -> { // "SLIDE"
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(350)) { -it } + fadeOut(animationSpec = tween(250)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(350)) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(350)) { it } + fadeOut(animationSpec = tween(250)))
                    }
                }
            }
        },
        label = "SlideTransition",
        modifier = modifier
    ) { targetIndex ->
        content(targetIndex)
    }
}

/**
 * Transparent annotation & laser pointer canvas overlay on top of slide canvas.
 */
@Composable
fun SlideAnnotationOverlay(
    slideIndex: Int,
    toolMode: PptToolMode,
    penColor: Color,
    existingStrokes: List<DrawnStroke>,
    laserPosition: Pair<Float, Float>?,
    onStrokeFinished: (DrawnStroke) -> Unit,
    onLaserMoved: (Pair<Float, Float>?) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Currently in-progress stroke points
        val currentStrokePoints = remember(slideIndex) { mutableStateListOf<StrokePoint>() }

        val pointerModifier = when (toolMode) {
            PptToolMode.PEN, PptToolMode.HIGHLIGHTER -> {
                Modifier.pointerInput(slideIndex, toolMode, penColor) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStrokePoints.clear()
                            val rx = (offset.x / widthPx).coerceIn(0f, 1f)
                            val ry = (offset.y / heightPx).coerceIn(0f, 1f)
                            currentStrokePoints.add(StrokePoint(rx, ry))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val rx = (change.position.x / widthPx).coerceIn(0f, 1f)
                            val ry = (change.position.y / heightPx).coerceIn(0f, 1f)
                            currentStrokePoints.add(StrokePoint(rx, ry))
                        },
                        onDragEnd = {
                            if (currentStrokePoints.size > 1) {
                                val isHighlighter = toolMode == PptToolMode.HIGHLIGHTER
                                val strokeColor = if (isHighlighter) penColor.copy(alpha = 0.35f) else penColor
                                val strokeWidth = if (isHighlighter) 22f else 6f
                                onStrokeFinished(
                                    DrawnStroke(
                                        points = currentStrokePoints.toList(),
                                        color = strokeColor,
                                        strokeWidth = strokeWidth,
                                        isHighlighter = isHighlighter
                                    )
                                )
                            }
                            currentStrokePoints.clear()
                        },
                        onDragCancel = {
                            currentStrokePoints.clear()
                        }
                    )
                }
            }
            PptToolMode.LASER_POINTER -> {
                Modifier.pointerInput(slideIndex) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val rx = (offset.x / widthPx).coerceIn(0f, 1f)
                            val ry = (offset.y / heightPx).coerceIn(0f, 1f)
                            onLaserMoved(Pair(rx, ry))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val rx = (change.position.x / widthPx).coerceIn(0f, 1f)
                            val ry = (change.position.y / heightPx).coerceIn(0f, 1f)
                            onLaserMoved(Pair(rx, ry))
                        },
                        onDragEnd = {
                            onLaserMoved(null)
                        },
                        onDragCancel = {
                            onLaserMoved(null)
                        }
                    )
                }
            }
            else -> Modifier
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(pointerModifier)
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw existing persisted strokes for this slide
            for (stroke in existingStrokes) {
                if (stroke.points.size < 2) continue
                val path = Path()
                val first = stroke.points[0]
                path.moveTo(first.xRatio * canvasW, first.yRatio * canvasH)
                for (i in 1 until stroke.points.size) {
                    val p = stroke.points[i]
                    path.lineTo(p.xRatio * canvasW, p.yRatio * canvasH)
                }
                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = if (stroke.isHighlighter) BlendMode.SrcOver else BlendMode.SrcOver
                )
            }

            // 2. Draw live active stroke
            if (currentStrokePoints.size > 1) {
                val isHighlighter = toolMode == PptToolMode.HIGHLIGHTER
                val strokeColor = if (isHighlighter) penColor.copy(alpha = 0.35f) else penColor
                val strokeWidth = if (isHighlighter) 22f else 6f
                val path = Path()
                val first = currentStrokePoints[0]
                path.moveTo(first.xRatio * canvasW, first.yRatio * canvasH)
                for (i in 1 until currentStrokePoints.size) {
                    val p = currentStrokePoints[i]
                    path.lineTo(p.xRatio * canvasW, p.yRatio * canvasH)
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Draw Laser Pointer if active
            laserPosition?.let { (lx, ly) ->
                val px = lx * canvasW
                val py = ly * canvasH
                // Glowing outer red ring
                drawCircle(
                    color = Color(0x66FF0000),
                    radius = 24f,
                    center = Offset(px, py)
                )
                // Mid bright halo
                drawCircle(
                    color = Color(0xAAFF3333),
                    radius = 14f,
                    center = Offset(px, py)
                )
                // Intense bright core
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
