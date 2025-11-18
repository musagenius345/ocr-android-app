package com.musagenius.ocrapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.musagenius.ocrapp.domain.model.AppTheme
import com.musagenius.ocrapp.presentation.navigation.Screen
import com.musagenius.ocrapp.presentation.ui.camera.CameraScreen
import com.musagenius.ocrapp.presentation.ui.detail.ScanDetailScreen
import com.musagenius.ocrapp.presentation.ui.editor.ImageEditorScreen
import com.musagenius.ocrapp.presentation.ui.history.HistoryScreen
import com.musagenius.ocrapp.presentation.ui.language.LanguageManagementScreen
import com.musagenius.ocrapp.presentation.ui.ocr.OCRResultScreen
import com.musagenius.ocrapp.presentation.ui.settings.SettingsScreen
import com.musagenius.ocrapp.presentation.ui.theme.OCRAppTheme
import com.musagenius.ocrapp.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the OCR application
 * Uses Jetpack Compose for UI and Hilt for dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by settingsViewModel.preferences.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (preferences.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> systemInDarkTheme
            }

            OCRAppTheme(
                darkTheme = darkTheme,
                dynamicColor = preferences.useDynamicColor
            ) {
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
                    navController.navigate(Screen.Results.createRoute(imageUri.toString()))
                },
                onGalleryImageSelected = { imageUri ->
                    // Navigate to image editor for gallery images
                    navController.navigate(Screen.ImageEditor.createRoute(imageUri.toString()))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Image Editor screen
        composable(
            route = Screen.ImageEditor.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUriString = backStackEntry.arguments?.getString("imageUri")
            if (encodedUriString == null) {
                navController.popBackStack()
                return@composable
            }

            val decodedUriString = Uri.decode(encodedUriString)
            val imageUri = Uri.parse(decodedUriString)

            ImageEditorScreen(
                sourceImageUri = imageUri,
                onImageSaved = { editedUri ->
                    // Navigate to results with edited image
                    navController.navigate(Screen.Results.createRoute(editedUri.toString())) {
                        // Remove image editor from back stack
                        popUpTo(Screen.Camera.route)
                    }
                },
                onCancel = {
                    // Return to camera screen
                    navController.popBackStack()
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
            val encodedUriString = backStackEntry.arguments?.getString("imageUri")
            if (encodedUriString == null) {
                // Missing imageUri argument - fail fast and navigate back
                navController.popBackStack()
                return@composable
            }

            val decodedUriString = Uri.decode(encodedUriString)
            val imageUri = Uri.parse(decodedUriString)

            OCRResultScreen(
                imageUri = imageUri,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // History screen
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanClick = { scanId ->
                    navController.navigate(Screen.ScanDetail.createRoute(scanId))
                }
            )
        }

        // Scan Detail screen
        composable(
            route = Screen.ScanDetail.route,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType }
            )
        ) {
            ScanDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLanguageManagement = {
                    navController.navigate(Screen.LanguageManagement.route)
                }
            )
        }

        // Language Management screen
        composable(Screen.LanguageManagement.route) {
            LanguageManagementScreen(
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
                .padding(padding),
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
