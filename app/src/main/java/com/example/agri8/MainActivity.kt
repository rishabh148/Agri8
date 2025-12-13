package com.example.agri8

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.agri8.ui.theme.Agri8Theme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize LanguageManager
        LanguageManager.init(this)
        
        // Set app locale based on saved language
        setAppLocale()
        
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
    
    override fun attachBaseContext(newBase: android.content.Context) {
        LanguageManager.init(newBase)
        val languageCode = LanguageManager.getSelectedLanguage()
        val context = if (languageCode.isNotEmpty()) {
            LocaleHelper.setLocale(newBase, languageCode)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }
    
    private fun setAppLocale() {
        val languageCode = LanguageManager.getSelectedLanguage()
        if (languageCode.isNotEmpty()) {
            val locale = LanguageManager.getLocale()
            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    var languageSelected by remember { mutableStateOf(false) } // Always start with false to show language selection
    
    // Update locale when language is selected (without recreating activity)
    LaunchedEffect(languageSelected) {
        if (languageSelected) {
            val locale = LanguageManager.getLocale()
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
    
    if (!languageSelected) {
        LanguageSelectionScreen(
            onLanguageSelected = { languageCode ->
                LanguageManager.setLanguage(languageCode)
                languageSelected = true
            }
        )
    } else {
        DiseaseDetectionScreen(
            onBackToLanguageSelection = {
                languageSelected = false
            }
        )
    }
}
