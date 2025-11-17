package com.musagenius.ocrapp.presentation.navigation

import android.net.Uri

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object Results : Screen("results/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = Uri.encode(imageUri)
            return "results/$encodedUri"
        }
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
