package com.example.agri8

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val activity = rememberUpdatedState(context as? Activity)
    var languageSelected by rememberSaveable { mutableStateOf(LanguageManager.isLanguageSelected()) }
    
    if (!languageSelected) {
        LanguageSelectionScreen(
            onLanguageSelected = { languageCode ->
                LanguageManager.setLanguage(languageCode)
                applyLocale(context, languageCode)
                languageSelected = true
                // Recreate so attachBaseContext picks up the new locale
                activity.value?.recreate()
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

private fun applyLocale(context: android.content.Context, languageCode: String) {
    val locale = when (languageCode) {
        "hi" -> Locale("hi", "IN")
        "te" -> Locale("te", "IN")
        "ta" -> Locale("ta", "IN")
        "bn" -> Locale("bn", "IN")
        "gu" -> Locale("gu", "IN")
        "kn" -> Locale("kn", "IN")
        "ml" -> Locale("ml", "IN")
        "mr" -> Locale("mr", "IN")
        "or" -> Locale("or", "IN")
        "pa" -> Locale("pa", "IN")
        "ur" -> Locale("ur", "IN")
        "en" -> Locale("en", "US")
        else -> Locale("en", "US")
    }
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}
