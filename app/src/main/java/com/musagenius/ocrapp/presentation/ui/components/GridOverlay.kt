package com.musagenius.ocrapp.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Grid overlay for camera composition
 * Implements rule of thirds grid for better photo composition
 */
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    strokeWidth: Float = with(LocalDensity.current) { 1.dp.toPx() }
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw vertical lines (rule of thirds)
        for (i in 1..2) {
            val x = width * i / 3f
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Draw horizontal lines (rule of thirds)
        for (i in 1..2) {
            val y = height * i / 3f
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
