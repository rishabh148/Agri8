package com.example.agri8

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.agri8.presentation.navigation.AppNavigation
import com.example.agri8.presentation.navigation.Screen
import com.example.agri8.presentation.viewmodel.LanguageSelectionViewModel
import com.example.agri8.ui.theme.Agri8Theme
import com.example.agri8.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Agri8Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    AppContent()
                }
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Get the saved language code from SharedPreferences (quick access)
        val prefs = newBase.getSharedPreferences("Agri8Prefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("selected_language", null)
        val context = if (languageCode != null && languageCode.isNotEmpty()) {
            LocaleHelper.setLocale(newBase, languageCode)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }
}

@Composable
fun AppContent(
    languageViewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLanguageSelected by languageViewModel.isLanguageSelected.collectAsState()
    
    // Determine start destination based on language selection state
    val startDestination = remember(isLanguageSelected) {
        when (isLanguageSelected) {
            true -> Screen.DiseaseDetection.route
            false -> Screen.LanguageSelection.route
            null -> Screen.LanguageSelection.route
        }
    }
    
    AppNavigation(
        navController = navController,
        startDestination = startDestination,
        languageViewModel = languageViewModel
    )
}
