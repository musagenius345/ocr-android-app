package com.musagenius.ocrapp.presentation.ui.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.musagenius.ocrapp.data.camera.DocumentEdgeDetector

/**
 * Overlay that draws detected document edges on camera preview
 */
@Composable
fun DocumentOverlay(
    corners: DocumentEdgeDetector.DocumentCorners?,
    modifier: Modifier = Modifier,
    viewWidth: Float,
    viewHeight: Float
) {
    // Animated pulse effect for detected document
    val infiniteTransition = rememberInfiniteTransition(label = "document_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlay_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (corners != null && viewWidth > 0 && viewHeight > 0) {
            // Calculate scale factors from camera coordinates to view coordinates
            val scaleX = size.width / viewWidth
            val scaleY = size.height / viewHeight

            // Draw document outline
            drawDocumentOutline(
                corners = corners,
                scaleX = scaleX,
                scaleY = scaleY,
                alpha = if (corners.isReliable()) alpha else 0.3f,
                isReliable = corners.isReliable()
            )

            // Draw corner markers
            drawCornerMarkers(
                corners = corners,
                scaleX = scaleX,
                scaleY = scaleY,
                alpha = if (corners.isReliable()) alpha else 0.3f
            )
        }
    }
}

/**
 * Draw document outline path
 */
private fun DrawScope.drawDocumentOutline(
    corners: DocumentEdgeDetector.DocumentCorners,
    scaleX: Float,
    scaleY: Float,
    alpha: Float,
    isReliable: Boolean
) {
    val path = Path().apply {
        // Scale corner points to view coordinates
        val p1 = Offset(corners.topLeft.x * scaleX, corners.topLeft.y * scaleY)
        val p2 = Offset(corners.topRight.x * scaleX, corners.topRight.y * scaleY)
        val p3 = Offset(corners.bottomRight.x * scaleX, corners.bottomRight.y * scaleY)
        val p4 = Offset(corners.bottomLeft.x * scaleX, corners.bottomLeft.y * scaleY)

        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }

    // Choose color based on reliability
    val color = if (isReliable) {
        Color(0xFF4CAF50) // Green for good detection
    } else {
        Color(0xFFFFC107) // Amber for uncertain detection
    }

    // Draw filled overlay
    drawPath(
        path = path,
        color = color.copy(alpha = alpha * 0.2f)
    )

    // Draw outline with dashed effect
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = 4f,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(20f, 10f),
                phase = 0f
            )
        )
    )
}

/**
 * Draw corner markers
 */
private fun DrawScope.drawCornerMarkers(
    corners: DocumentEdgeDetector.DocumentCorners,
    scaleX: Float,
    scaleY: Float,
    alpha: Float
) {
    val cornerSize = 40f
    val strokeWidth = 6f
    val color = Color.White.copy(alpha = alpha)

    listOf(
        corners.topLeft,
        corners.topRight,
        corners.bottomRight,
        corners.bottomLeft
    ).forEach { corner ->
        val x = corner.x * scaleX
        val y = corner.y * scaleY

        // Draw L-shaped corner marker
        drawRoundRect(
            color = color,
            topLeft = Offset(x - strokeWidth / 2, y - strokeWidth / 2),
            size = Size(cornerSize, strokeWidth),
            cornerRadius = CornerRadius(strokeWidth / 2, strokeWidth / 2),
            style = Stroke(width = strokeWidth)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(x - strokeWidth / 2, y - strokeWidth / 2),
            size = Size(strokeWidth, cornerSize),
            cornerRadius = CornerRadius(strokeWidth / 2, strokeWidth / 2),
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * No document detected overlay
 */
@Composable
fun NoDocumentDetectedOverlay(
    modifier: Modifier = Modifier
) {
    // Optional: Show hint when no document is detected
    // Could add text or guide rectangle here
}
