package com.nabdaldhikr.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nabdaldhikr.app.ui.screens.AzkarDetailScreen
import com.nabdaldhikr.app.ui.screens.AzkarListScreen
import com.nabdaldhikr.app.ui.screens.FavoritesScreen
import com.nabdaldhikr.app.ui.screens.HomeScreen
import com.nabdaldhikr.app.ui.screens.QiblaCompassScreen
import com.nabdaldhikr.app.ui.screens.SearchScreen
import com.nabdaldhikr.app.ui.screens.SettingsScreen
import com.nabdaldhikr.app.ui.screens.SplashScreen
import com.nabdaldhikr.app.ui.screens.TasbihScreen
import com.nabdaldhikr.app.viewmodel.AzkarViewModel
import com.nabdaldhikr.app.viewmodel.SettingsViewModel
import com.nabdaldhikr.app.viewmodel.TasbihViewModel

@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val azkarViewModel: AzkarViewModel = viewModel()
    val tasbihViewModel: TasbihViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeOut(tween(300))
        }
    ) {
        composable(
            "splash",
            exitTransition = { fadeOut(tween(500)) }
        ) {
            SplashScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(
                navController = navController,
                azkarViewModel = azkarViewModel
            )
        }
        composable("azkar/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            AzkarListScreen(
                categoryId = categoryId,
                navController = navController,
                azkarViewModel = azkarViewModel
            )
        }
        composable("azkar_detail/{categoryId}/{azkarIndex}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val azkarIndex = backStackEntry.arguments?.getString("azkarIndex")?.toIntOrNull() ?: 0
            AzkarDetailScreen(
                categoryId = categoryId,
                initialIndex = azkarIndex,
                navController = navController,
                azkarViewModel = azkarViewModel,
                settingsViewModel = settingsViewModel
            )
        }
        composable("tasbih") {
            TasbihScreen(
                navController = navController,
                viewModel = tasbihViewModel,
                settingsViewModel = settingsViewModel
            )
        }
        composable("qibla") {
            QiblaCompassScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                navController = navController,
                viewModel = settingsViewModel
            )
        }
        composable("favorites") {
            FavoritesScreen(
                navController = navController,
                azkarViewModel = azkarViewModel
            )
        }
        composable("search") {
            SearchScreen(
                navController = navController,
                azkarViewModel = azkarViewModel
            )
        }
    }
}
