package com.musagenius.ocrapp.presentation.ui.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable wrapper for CameraX PreviewView
 * Simplified version that works with LifecycleCameraController
 * Controller is bound in CameraManager, not here
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
): PreviewView {
    val context = LocalContext.current

    // Create stable PreviewView instance
    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // Use COMPATIBLE (TextureView) mode for better Compose integration
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // Set content description for accessibility
            contentDescription = "Camera preview for document scanning"

            Log.d("CameraPreview", "PreviewView created")
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    return previewView
}
