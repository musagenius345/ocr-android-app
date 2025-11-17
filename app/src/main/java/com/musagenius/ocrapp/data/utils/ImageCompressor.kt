package com.musagenius.ocrapp.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for compressing and optimizing images
 */
@Singleton
class ImageCompressor @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val DEFAULT_QUALITY = 85 // JPEG quality (0-100)
        private const val MAX_IMAGE_SIZE = 2048 // Max dimension in pixels
    }

    /**
     * Creates a compressed JPEG copy of the image at the given URI.
     *
     * @param sourceUri URI of the source image to compress.
     * @param quality JPEG quality between 0 and 100; higher means better quality and larger file size.
     * @param maxSize Maximum width or height in pixels for the sampled bitmap; larger images are downscaled to fit.
     * @return A Result containing the URI of the compressed JPEG file on success, or a failure with the encountered exception.
     */
    suspend fun compressImage(
        sourceUri: Uri,
        quality: Int = DEFAULT_QUALITY,
        maxSize: Int = MAX_IMAGE_SIZE
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Load bitmap with sampling if needed
            val bitmap = loadSampledBitmap(sourceUri, maxSize) ?: return@withContext Result.failure(
                IOException("Failed to load image")
            )

            // Fix orientation based on EXIF data
            val orientedBitmap = fixOrientation(sourceUri, bitmap)

            // Create output file
            val outputFile = createTempImageFile()

            // Compress and save
            FileOutputStream(outputFile).use { out ->
                orientedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            // Clean up bitmaps
            if (bitmap != orientedBitmap) {
                bitmap.recycle()
            }
            orientedBitmap.recycle()

            Result.success(Uri.fromFile(outputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load a bitmap from a URI scaled to fit within the given maximum dimension and corrected for EXIF orientation.
     *
     * @param uri The source image Uri to load from.
     * @param maxSize Maximum width or height in pixels; the decoded bitmap's largest dimension will not exceed this value.
     * @return The decoded Bitmap corrected for EXIF orientation, or `null` if the image could not be loaded.
     */
    suspend fun loadBitmapFromUri(
        uri: Uri,
        maxSize: Int = MAX_IMAGE_SIZE
    ): Bitmap? = withContext(Dispatchers.IO) {
        loadSampledBitmap(uri, maxSize)?.let { bitmap ->
            // Fix orientation based on EXIF data
            fixOrientation(uri, bitmap)
        }
    }

    /**
     * Decodes a bitmap from the provided Uri while downsampling to keep its largest dimension at or below [maxSize].
     *
     * @param uri The content Uri of the image to decode.
     * @param maxSize Maximum allowed width or height in pixels for the decoded bitmap.
     * @return A downsampled Bitmap if decoding succeeds, or `null` on failure.
     */
    private fun loadSampledBitmap(uri: Uri, maxSize: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // First decode bounds only
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(input, null, options)

                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
                options.inJustDecodeBounds = false

                // Decode with sample size
                context.contentResolver.openInputStream(uri)?.use { secondInput ->
                    BitmapFactory.decodeStream(secondInput, null, options)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate appropriate sample size for downsampling
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return bitmap
                }

                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Create temporary image file
     */
    private fun createTempImageFile(): File {
        val storageDir = context.getExternalFilesDir(null)
        return File.createTempFile(
            "compressed_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
    }

    /**
     * Get compressed file size in KB
     */
    fun getFileSizeKB(uri: Uri): Long {
        return try {
            val file = File(uri.path ?: return 0L)
            file.length() / 1024
        } catch (e: Exception) {
            0L
        }
    }
}