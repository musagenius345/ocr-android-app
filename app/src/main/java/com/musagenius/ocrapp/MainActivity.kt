package com.musagenius.ocrapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musagenius.ocrapp.presentation.navigation.Screen
import com.musagenius.ocrapp.presentation.ui.camera.CameraScreen
import com.musagenius.ocrapp.presentation.ui.ocr.OCRResultScreen
import com.musagenius.ocrapp.presentation.ui.theme.OCRAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the OCR application
 * Uses Jetpack Compose for UI and Hilt for dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OCRAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OCRAppNavigation()
                }
            }
        }
    }
}

@Composable
fun OCRAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route
    ) {
        // Camera screen
        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { imageUri ->
                    // Navigate to results screen with the captured image
                    val encodedUri = Uri.encode(imageUri.toString())
                    navController.navigate(Screen.Results.createRoute(encodedUri))
                },
                onNavigateBack = {
                    // Exit app or navigate to home (if we add one later)
                    // For now, just finish the activity
                }
            )
        }

        // OCR Results screen
        composable(
            route = Screen.Results.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val imageUri = Uri.parse(imageUriString)

            OCRResultScreen(
                imageUri = imageUri,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // History screen (placeholder for Phase 4)
        composable(Screen.History.route) {
            PlaceholderScreen(
                title = "History Screen",
                subtitle = "Coming in Phase 4",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screen (placeholder for Phase 5)
        composable(Screen.Settings.route) {
            PlaceholderScreen(
                title = "Settings Screen",
                subtitle = "Coming in Phase 5",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text(title) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .androidx.compose.foundation.layout.padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium
                )
                androidx.compose.material3.Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.androidx.compose.foundation.layout.padding(top = 8.dp)
                )
            }
        }
    }
}
