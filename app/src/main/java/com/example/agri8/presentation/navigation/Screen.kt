package com.example.agri8.presentation.navigation

sealed class Screen(val route: String) {
    object LanguageSelection : Screen("language_selection")
    object DiseaseDetection : Screen("disease_detection")
}
