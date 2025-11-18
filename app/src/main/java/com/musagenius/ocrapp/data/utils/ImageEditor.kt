package com.musagenius.ocrapp.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for editing images (rotate, crop)
 */
@Singleton
class ImageEditor @Inject constructor(
    private val context: Context,
    private val imageCompressor: ImageCompressor
) {

    /**
     * Rotate bitmap by specified degrees
     * @param bitmap Source bitmap
     * @param degrees Rotation angle in degrees (90, 180, 270)
     * @return Rotated bitmap
     */
    suspend fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap = withContext(Dispatchers.Default) {
        if (degrees == 0f) return@withContext bitmap

        val matrix = Matrix().apply {
            postRotate(degrees)
        }

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        // Recycle original if a new bitmap was created
        if (rotated != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }

        rotated
    }

    /**
     * Crop bitmap to specified rectangle
     * @param bitmap Source bitmap
     * @param x Starting x coordinate
     * @param y Starting y coordinate
     * @param width Crop width
     * @param height Crop height
     * @return Cropped bitmap
     */
    suspend fun cropBitmap(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Bitmap = withContext(Dispatchers.Default) {
        // Ensure crop bounds are within bitmap bounds
        val cropX = x.coerceIn(0, bitmap.width)
        val cropY = y.coerceIn(0, bitmap.height)
        val cropWidth = width.coerceIn(1, bitmap.width - cropX)
        val cropHeight = height.coerceIn(1, bitmap.height - cropY)

        val cropped = Bitmap.createBitmap(
            bitmap,
            cropX,
            cropY,
            cropWidth,
            cropHeight
        )

        // Recycle original if a new bitmap was created
        if (cropped != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }

        cropped
    }

    /**
     * Save bitmap to temporary file and return URI
     * @param bitmap Bitmap to save
     * @param quality JPEG quality (0-100)
     * @return URI of saved file
     */
    suspend fun saveBitmapToUri(
        bitmap: Bitmap,
        quality: Int = 90
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val outputFile = createTempImageFile()

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            Result.success(Uri.fromFile(outputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Apply rotation and crop to image from URI
     * @param sourceUri Source image URI
     * @param rotationDegrees Rotation angle (0, 90, 180, 270)
     * @param cropRect Crop rectangle (x, y, width, height), null for no crop
     * @return Result containing URI of edited image
     */
    suspend fun editImage(
        sourceUri: Uri,
        rotationDegrees: Float = 0f,
        cropRect: CropRect? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Load bitmap from URI
            var bitmap = imageCompressor.loadBitmapFromUri(sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to load image"))

            // Apply rotation if specified
            if (rotationDegrees != 0f) {
                bitmap = rotateBitmap(bitmap, rotationDegrees)
            }

            // Apply crop if specified
            if (cropRect != null) {
                bitmap = cropBitmap(
                    bitmap,
                    cropRect.x,
                    cropRect.y,
                    cropRect.width,
                    cropRect.height
                )
            }

            // Save edited bitmap to file
            val result = saveBitmapToUri(bitmap, quality = 90)

            // Clean up bitmap
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create temporary image file
     */
    private fun createTempImageFile(): File {
        val storageDir = context.getExternalFilesDir(null)
        return File.createTempFile(
            "edited_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
    }

    /**
     * Crop rectangle definition
     */
    data class CropRect(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
}
