package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.database.DocumentEntity
import com.example.domain.model.DocumentType
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import com.example.ui.navigation.navigateToViewer
import com.example.ui.navigation.navigateToViewerByUri

enum class BottomNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    BROWSE("Browse", Icons.Filled.Folder, Icons.Outlined.Folder, "tab_browse"),
    RECENTS("Recent", Icons.Filled.History, Icons.Outlined.History, "tab_recents"),
    FAVOURITES("Starred", Icons.Filled.Star, Icons.Outlined.StarOutline, "tab_starred"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }

    Scaffold(
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.HorizontalDivider(
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    BottomNavTab.values().forEach { tab ->
                        val isSelected = (currentTab == tab)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    BottomNavTab.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onDocumentClick = { doc ->
                                navController.navigateToViewer(doc)
                            },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route)
                            },
                            onCategoryClick = { type ->
                                viewModel.setSelectedCategory(type)
                            }
                        )
                    }
                    BottomNavTab.BROWSE -> {
                        BrowseScreen(
                            viewModel = viewModel,
                            onOpenFile = { uri, fileName ->
                                navController.navigateToViewerByUri(uri, fileName)
                            }
                        )
                    }
                    BottomNavTab.RECENTS -> {
                        RecentsScreen(
                            viewModel = viewModel,
                            onDocumentClick = { doc ->
                                navController.navigateToViewer(doc)
                            }
                        )
                    }
                    BottomNavTab.FAVOURITES -> {
                        FavouritesScreen(
                            viewModel = viewModel,
                            onDocumentClick = { doc ->
                                navController.navigateToViewer(doc)
                            }
                        )
                    }
                    BottomNavTab.SETTINGS -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
