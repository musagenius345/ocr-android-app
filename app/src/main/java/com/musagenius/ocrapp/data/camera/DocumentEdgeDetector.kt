package com.musagenius.ocrapp.data.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Document edge detection for camera preview
 * Detects rectangular document edges in camera frames
 */
@Singleton
class DocumentEdgeDetector @Inject constructor() {

    companion object {
        private const val TAG = "DocumentEdgeDetector"

        // Detection parameters
        private const val MIN_CONTOUR_AREA_RATIO = 0.1f // Minimum 10% of image area
        private const val MAX_CONTOUR_AREA_RATIO = 0.9f // Maximum 90% of image area
        private const val EDGE_THRESHOLD = 50 // Canny edge detection threshold
    }

    /**
     * Represents detected document corners
     */
    data class DocumentCorners(
        val topLeft: Point,
        val topRight: Point,
        val bottomRight: Point,
        val bottomLeft: Point,
        val confidence: Float = 0f
    ) {
        /**
         * Check if detection is reliable
         */
        fun isReliable(): Boolean = confidence > 0.5f

        /**
         * Calculate area of detected document
         */
        fun area(): Float {
            val width1 = distance(topLeft, topRight)
            val width2 = distance(bottomLeft, bottomRight)
            val height1 = distance(topLeft, bottomLeft)
            val height2 = distance(topRight, bottomRight)
            return ((width1 + width2) / 2f) * ((height1 + height2) / 2f)
        }

        private fun distance(p1: Point, p2: Point): Float {
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            return sqrt(dx * dx + dy * dy)
        }
    }

    /**
     * Simple 2D point
     */
    data class Point(val x: Float, val y: Float)

    /**
     * Detect document edges in image
     */
    suspend fun detectEdges(imageProxy: ImageProxy): DocumentCorners? = withContext(Dispatchers.Default) {
        try {
            // Convert ImageProxy to bitmap
            val bitmap = imageProxyToBitmap(imageProxy) ?: return@withContext null

            // Downsample for performance
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                bitmap.width / 4,
                bitmap.height / 4,
                true
            )

            // Detect edges
            val corners = detectDocumentCorners(scaledBitmap)

            // Clean up
            if (bitmap != scaledBitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            corners
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting edges", e)
            null
        }
    }

    /**
     * Detect document corners in bitmap
     */
    private fun detectDocumentCorners(bitmap: Bitmap): DocumentCorners? {
        val width = bitmap.width
        val height = bitmap.height

        // Convert to grayscale and detect edges
        val grayPixels = IntArray(width * height)
        bitmap.getPixels(grayPixels, 0, width, 0, 0, width, height)

        // Simple edge detection using contrast
        val edges = detectEdgesSimple(grayPixels, width, height)

        // Find contours (simplified - look for bright regions indicating document)
        val brightRegions = findBrightRegions(edges, width, height)

        if (brightRegions.isEmpty()) {
            return null
        }

        // Find the largest rectangular region
        val documentCorners = findLargestRectangle(brightRegions, width, height)

        return documentCorners?.let {
            // Scale back to original image size
            val scaleX = 4f
            val scaleY = 4f

            DocumentCorners(
                topLeft = Point(it.topLeft.x * scaleX, it.topLeft.y * scaleY),
                topRight = Point(it.topRight.x * scaleX, it.topRight.y * scaleY),
                bottomRight = Point(it.bottomRight.x * scaleX, it.bottomRight.y * scaleY),
                bottomLeft = Point(it.bottomLeft.x * scaleX, it.bottomLeft.y * scaleY),
                confidence = 0.7f // Simplified confidence score
            )
        }
    }

    /**
     * Simple edge detection using brightness gradient
     */
    private fun detectEdgesSimple(pixels: IntArray, width: Int, height: Int): BooleanArray {
        val edges = BooleanArray(pixels.size)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x

                val brightness = getBrightness(pixels[idx])
                val rightBrightness = getBrightness(pixels[idx + 1])
                val bottomBrightness = getBrightness(pixels[idx + width])

                // Detect strong edges
                val gradientX = kotlin.math.abs(brightness - rightBrightness)
                val gradientY = kotlin.math.abs(brightness - bottomBrightness)
                val gradient = max(gradientX, gradientY)

                edges[idx] = gradient > EDGE_THRESHOLD
            }
        }

        return edges
    }

    /**
     * Find bright regions (potential document areas)
     */
    private fun findBrightRegions(edges: BooleanArray, width: Int, height: Int): List<Point> {
        val regions = mutableListOf<Point>()

        // Sample grid for performance
        val step = 10
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val idx = y * width + x
                if (edges.getOrNull(idx) == true) {
                    regions.add(Point(x.toFloat(), y.toFloat()))
                }
            }
        }

        return regions
    }

    /**
     * Find largest rectangle from edge points
     * Simplified: returns approximate document bounds
     */
    private fun findLargestRectangle(points: List<Point>, width: Int, height: Int): DocumentCorners? {
        if (points.isEmpty()) return null

        // Find bounding box of detected edges
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        points.forEach { point ->
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }

        // Calculate area ratio
        val detectedArea = (maxX - minX) * (maxY - minY)
        val imageArea = width * height
        val areaRatio = detectedArea / imageArea

        // Filter by size
        if (areaRatio < MIN_CONTOUR_AREA_RATIO || areaRatio > MAX_CONTOUR_AREA_RATIO) {
            return null
        }

        // Add margin for better UX
        val margin = 20f
        return DocumentCorners(
            topLeft = Point(max(0f, minX - margin), max(0f, minY - margin)),
            topRight = Point(min(width.toFloat(), maxX + margin), max(0f, minY - margin)),
            bottomRight = Point(min(width.toFloat(), maxX + margin), min(height.toFloat(), maxY + margin)),
            bottomLeft = Point(max(0f, minX - margin), min(height.toFloat(), maxY + margin)),
            confidence = 0.6f
        )
    }

    /**
     * Get brightness from color
     * @param color Packed ARGB color value
     * @return Luminance value (0-255)
     */
    private fun getBrightness(@ColorInt color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        // Standard luminance formula
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    /**
     * Convert ImageProxy to Bitmap using stride-aware Y plane extraction
     * Properly handles rowStride and pixelStride for YUV_420_888 format
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val width = imageProxy.width
            val height = imageProxy.height

            // Get Y plane (luminance) from YUV_420_888
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride

            // Reset buffer position to start
            yBuffer.rewind()

            // Validate buffer has sufficient capacity
            // Required capacity is calculated from the last pixel position
            val requiredCapacity = (height - 1) * rowStride + width * pixelStride
            if (yBuffer.capacity() < requiredCapacity) {
                Log.e(TAG, "Buffer too small: ${yBuffer.capacity()} < $requiredCapacity")
                return null
            }

            // Create grayscale bitmap from Y plane
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)

            // Row-by-row copy respecting stride values
            var pixelIndex = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    // Calculate buffer position using rowStride and pixelStride
                    val bufferIndex = y * rowStride + x * pixelStride

                    // Bounds check to prevent crashes on unexpected stride values
                    if (bufferIndex >= yBuffer.capacity()) {
                        Log.e(TAG, "Buffer index out of bounds: $bufferIndex >= ${yBuffer.capacity()}")
                        bitmap.recycle()
                        return null
                    }

                    val yValue = yBuffer[bufferIndex].toInt() and 0xFF

                    // Convert Y (luminance) to grayscale RGB
                    pixels[pixelIndex++] = Color.rgb(yValue, yValue, yValue)
                }
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
}
