package com.musagenius.ocrapp.presentation.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musagenius.ocrapp.presentation.ui.components.rememberHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Expandable bottom sheet with zoom controls
 * Features:
 * - Horizontal row of zoom preset buttons (0.6x, 1x, 2x, 5x)
 * - Vertical slider for fine-grained zoom control
 * - Auto-dismiss after 4 seconds of inactivity
 * - Haptic feedback on preset selection and slider snap points
 * - Material 3 styling with semi-transparent background
 */
@Composable
fun ZoomControlSheet(
    visible: Boolean,
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    zoomPresets: List<Float>,
    selectedPresetIndex: Int,
    onZoomChange: (Float) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    // Auto-dismiss timer
    LaunchedEffect(visible) {
        if (visible) {
            delay(4000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(tween(300)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .pointerInput(Unit) {
                    // Reset auto-dismiss timer on interaction
                    detectTapGestures {
                        scope.launch {
                            delay(4000)
                            onDismiss()
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Zoom slider (vertical)
            ZoomSlider(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom,
                zoomPresets = zoomPresets,
                onZoomChange = onZoomChange,
                haptic = haptic
            )

            // Zoom preset buttons (horizontal row)
            ZoomPresetButtons(
                zoomPresets = zoomPresets,
                currentZoom = currentZoom,
                selectedPresetIndex = selectedPresetIndex,
                onPresetSelected = { index ->
                    haptic.performLightTap()
                    onPresetSelected(index)
                    onZoomChange(zoomPresets[index])
                }
            )
        }
    }
}

/**
 * Vertical zoom slider for fine-grained control
 */
@Composable
private fun ZoomSlider(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    zoomPresets: List<Float>,
    onZoomChange: (Float) -> Unit,
    haptic: com.musagenius.ocrapp.presentation.ui.components.HapticFeedbackManager,
    modifier: Modifier = Modifier
) {
    var lastSnapValue by remember { mutableFloatStateOf(currentZoom) }

    Surface(
        modifier = modifier
            .width(64.dp)
            .height(200.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Zoom value display
            Text(
                text = String.format("%.1fx", currentZoom),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical slider (rotated)
            Slider(
                value = currentZoom,
                onValueChange = { newZoom ->
                    val clampedZoom = newZoom.coerceIn(minZoom, maxZoom)

                    // Snap to preset if close enough (within 0.1)
                    val snappedZoom = zoomPresets.firstOrNull { preset ->
                        abs(clampedZoom - preset) < 0.15f
                    } ?: clampedZoom

                    // Haptic feedback when snapping to preset
                    if (snappedZoom != lastSnapValue && zoomPresets.contains(snappedZoom)) {
                        haptic.performLightTap()
                        lastSnapValue = snappedZoom
                    }

                    onZoomChange(snappedZoom)
                },
                valueRange = minZoom..maxZoom,
                steps = 50,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .height(140.dp)
                    .graphicsLayer {
                        rotationZ = 270f
                    }
            )
        }
    }
}

/**
 * Horizontal row of zoom preset buttons
 */
@Composable
private fun ZoomPresetButtons(
    zoomPresets: List<Float>,
    currentZoom: Float,
    selectedPresetIndex: Int,
    onPresetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomPresets.forEachIndexed { index, preset ->
                ZoomPresetButton(
                    zoom = preset,
                    isActive = abs(currentZoom - preset) < 0.15f,
                    isSelected = index == selectedPresetIndex,
                    onClick = { onPresetSelected(index) }
                )
            }
        }
    }
}

/**
 * Individual zoom preset button
 */
@Composable
private fun ZoomPresetButton(
    zoom: Float,
    isActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .widthIn(min = 56.dp, max = 72.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = formatZoomLabel(zoom),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        )
    }
}

/**
 * Format zoom value for display
 * 0.6 -> "0.6x"
 * 1.0 -> "1x"
 * 2.0 -> "2x"
 */
private fun formatZoomLabel(zoom: Float): String {
    return when {
        zoom < 1.0f -> String.format("%.1fx", zoom)
        zoom % 1.0f == 0.0f -> String.format("%.0fx", zoom)
        else -> String.format("%.1fx", zoom)
    }
}
