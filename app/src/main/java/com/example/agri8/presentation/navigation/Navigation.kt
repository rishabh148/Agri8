package com.example.agri8.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.agri8.presentation.ui.DiseaseDetectionScreen
import com.example.agri8.presentation.ui.LanguageSelectionScreen
import com.example.agri8.presentation.viewmodel.LanguageSelectionViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.LanguageSelection.route,
    modifier: Modifier = Modifier,
    languageViewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                onNavigateToDiseaseDetection = {
                    navController.navigate(Screen.DiseaseDetection.route) {
                        popUpTo(Screen.LanguageSelection.route) {
                            inclusive = true
                        }
                    }
                },
                viewModel = languageViewModel
            )
        }
        
        composable(route = Screen.DiseaseDetection.route) {
            DiseaseDetectionScreen(
                onNavigateToLanguageSelection = {
                    navController.navigate(Screen.LanguageSelection.route) {
                        popUpTo(Screen.DiseaseDetection.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
