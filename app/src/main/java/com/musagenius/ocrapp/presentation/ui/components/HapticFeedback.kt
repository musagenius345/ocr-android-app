package com.musagenius.ocrapp.presentation.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Manager for haptic feedback
 */
class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }?.takeIf { it.hasVibrator() }

    /**
     * Light tap feedback (for buttons)
     */
    fun performLightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10)
        }
    }

    /**
     * Medium tap feedback (for important actions)
     */
    fun performMediumTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    }

    /**
     * Medium tap feedback (for capture)
     */
    fun performCaptureFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    /**
     * Success feedback (for successful operations)
     */
    fun performSuccessFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    }

    /**
     * Error feedback (for errors)
     */
    fun performErrorFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }
}

/**
 * Remember haptic feedback manager
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return remember(context) { HapticFeedbackManager(context) }
}
