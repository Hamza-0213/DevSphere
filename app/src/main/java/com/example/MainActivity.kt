package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.AppThemeMode
import com.example.ui.MainViewModel
import com.example.ui.navigation.AppNavHost
import com.example.ui.navigation.navigateToViewerByUri
import com.example.ui.theme.DocSphereTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incomingUri: Uri? = intent?.data

        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            val isDarkTheme = when (userSettings.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DocSphereTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(incomingUri) {
                        incomingUri?.let { uri ->
                            viewModel.onDocumentPicked(uri) { doc ->
                                navController.navigateToViewerByUri(doc.uri, doc.displayName)
                            }
                        }
                    }

                    AppNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        initialUri = incomingUri
                    )
                }
            }
        }
    }
}

