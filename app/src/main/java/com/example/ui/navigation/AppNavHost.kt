package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewers.ExcelViewerScreen
import com.example.ui.viewers.ImageViewerScreen
import com.example.ui.viewers.PdfViewerScreen
import com.example.ui.viewers.PowerPointViewerScreen
import com.example.ui.viewers.TextViewerScreen
import com.example.ui.viewers.WordViewerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    initialUri: Uri? = null
) {
    val startDestination = if (initialUri != null) Screen.Main.route else Screen.Splash.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDocumentClick = { doc ->
                    navController.navigateToViewer(doc)
                }
            )
        }

        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.decode(it) } ?: ""
            PdfViewerScreen(
                uriString = uri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WordViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.decode(it) } ?: ""
            WordViewerScreen(
                uriString = uri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExcelViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.decode(it) } ?: ""
            ExcelViewerScreen(
                uriString = uri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PowerPointViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.decode(it) } ?: ""
            PowerPointViewerScreen(
                uriString = uri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TextViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.decode(it) } ?: ""
            TextViewerScreen(
                uriString = uri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.decode(it) } ?: ""
            ImageViewerScreen(
                uriString = uri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
