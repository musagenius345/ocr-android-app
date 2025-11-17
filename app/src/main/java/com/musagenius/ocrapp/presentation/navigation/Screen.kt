package com.musagenius.ocrapp.presentation.navigation

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object Results : Screen("results/{imageUri}") {
        fun createRoute(imageUri: String) = "results/$imageUri"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
