package com.musagenius.ocrapp.presentation.ui.camera

import android.view.ViewGroup
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable wrapper for CameraX PreviewView
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // Use COMPATIBLE (TextureView) mode - more reliable for surface timing
                // SurfaceView (PERFORMANCE) can cause timeout issues with Compose
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                // Set content description for accessibility
                contentDescription = "Camera preview for document scanning"

                // Ensure view is visible - required for surface readiness
                visibility = android.view.View.VISIBLE

                // Wait for view to be laid out AND visible before notifying
                // This ensures the Surface is ready when camera binds
                viewTreeObserver.addOnGlobalLayoutListener(
                    object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (width > 0 && height > 0) {
                                viewTreeObserver.removeOnGlobalLayoutListener(this)
                                post {
                                    onPreviewViewCreated(this@apply)
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}
