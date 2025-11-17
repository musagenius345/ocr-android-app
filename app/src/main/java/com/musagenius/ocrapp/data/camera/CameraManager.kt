package com.musagenius.ocrapp.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.musagenius.ocrapp.presentation.ui.camera.FlashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manages CameraX operations
 */
@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    /**
     * Initialize camera with preview
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        flashMode: FlashMode = FlashMode.OFF
    ) = withContext(Dispatchers.Main) {
        try {
            cameraProvider = getCameraProvider()

            // Unbind all use cases before rebinding
            cameraProvider?.unbindAll()

            // Set up Preview use case
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Set up ImageCapture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(flashMode.toImageCaptureFlashMode())
                .build()

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            Log.d(TAG, "Camera started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            throw e
        }
    }

    /**
     * Capture image and save to file
     */
    suspend fun captureImage(): Uri = suspendCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("ImageCapture not initialized"))
            return@suspendCoroutine
        }

        // Create output file
        val photoFile = File(
            context.getExternalFilesDir(null),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo captured successfully: $savedUri")
                    continuation.resume(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    /**
     * Set flash mode
     */
    fun setFlashMode(flashMode: FlashMode) {
        imageCapture?.flashMode = flashMode.toImageCaptureFlashMode()
    }

    /**
     * Check if camera is available
     */
    suspend fun isCameraAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val provider = getCameraProvider()
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera availability", e)
            false
        }
    }

    /**
     * Release camera resources
     */
    fun release() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        Log.d(TAG, "Camera released")
    }

    /**
     * Get CameraProvider instance
     */
    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                continuation.resume(cameraProviderFuture.get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, executor)
    }
}

/**
 * Convert FlashMode to ImageCapture flash mode
 */
private fun FlashMode.toImageCaptureFlashMode(): Int {
    return when (this) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }
}
