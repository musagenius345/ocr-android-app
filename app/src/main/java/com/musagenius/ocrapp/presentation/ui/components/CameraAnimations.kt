package com.musagenius.ocrapp.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * Shutter animation overlay for camera capture
 */
@Composable
fun ShutterAnimation(
    trigger: Boolean,
    onAnimationComplete: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 0.7f else 0f,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "shutter_alpha"
    )

    LaunchedEffect(trigger) {
        if (trigger) {
            isVisible = true
            delay(100)
            isVisible = false
            delay(100)
            onAnimationComplete()
        }
    }

    if (trigger) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(Color.Black)
        )
    }
}

/**
 * Flash animation for success/error feedback
 */
@Composable
fun FlashAnimation(
    show: Boolean,
    color: Color = Color.White,
    durationMillis: Int = 200,
    onComplete: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(
            animationSpec = tween(durationMillis = durationMillis / 2)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = durationMillis / 2)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = 0.3f))
        )
    }

    LaunchedEffect(show) {
        if (show) {
            delay(durationMillis.toLong())
            onComplete()
        }
    }
}

/**
 * Pulsing animation for loading states
 */
@Composable
fun rememberPulseAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    ).value
}
