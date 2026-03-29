package com.bnr.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bnr.app.R
import com.bnr.app.presentation.explore.ExploreScreen
import com.bnr.app.presentation.library.LibraryScreen
import com.bnr.app.presentation.noveldetail.NovelDetailScreen
import com.bnr.app.presentation.reader.ReaderScreen
import com.bnr.app.presentation.settings.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Library,  R.string.nav_library,  Icons.Default.Book),
    BottomNavItem(Screen.Explore,  R.string.nav_explore,  Icons.Default.Explore),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Default.Settings)
)

@Composable
fun BNRNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Library.route,
        Screen.Explore.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    onNovelClick = { novel ->
                        navController.navigate(Screen.NovelDetail.createRoute(novel.sourceId, novel.url))
                    }
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    onNovelClick = { novel ->
                        navController.navigate(Screen.NovelDetail.createRoute(novel.sourceId, novel.url))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.NovelDetail.route,
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.StringType },
                    navArgument("encodedUrl") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val sourceId   = backStackEntry.arguments?.getString("sourceId") ?: ""
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                NovelDetailScreen(
                    sourceId   = sourceId,
                    novelUrl   = encodedUrl.decodeUrl(),
                    onBack     = { navController.popBackStack() },
                    onReadChapter = { chapterId ->
                        navController.navigate(Screen.Reader.createRoute(chapterId))
                    }
                )
            }

            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("chapterId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedChapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                ReaderScreen(
                    chapterId = encodedChapterId.decodeUrl(),
                    onBack    = { navController.popBackStack() }
                )
            }
        }
    }
}
