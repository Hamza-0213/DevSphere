package com.example.ui.viewers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

enum class PptToolMode {
    NONE,
    LASER_POINTER,
    PEN,
    HIGHLIGHTER
}

data class DrawnStroke(
    val points: List<StrokePoint>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

data class StrokePoint(
    val xRatio: Float,
    val yRatio: Float
)
