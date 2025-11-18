package com.musagenius.ocrapp.presentation.navigation

import android.net.Uri

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object ImageEditor : Screen("image_editor/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = Uri.encode(imageUri)
            return "image_editor/$encodedUri"
        }
    }
    data object Results : Screen("results/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = Uri.encode(imageUri)
            return "results/$encodedUri"
        }
    }
    data object ScanDetail : Screen("scan_detail/{scanId}") {
        fun createRoute(scanId: Long): String {
            return "scan_detail/$scanId"
        }
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
