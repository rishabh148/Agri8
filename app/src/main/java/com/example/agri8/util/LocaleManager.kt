package com.example.agri8.util

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * CompositionLocal for current locale state.
 * This allows us to update the locale without Activity recreation.
 */
val LocalLocale = compositionLocalOf<Locale> { Locale.getDefault() }

/**
 * Helper function to get a localized string resource.
 * This uses the updated configuration from LocalConfiguration to get the correct locale.
 */
@Composable
fun localizedStringResource(@androidx.annotation.StringRes id: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    
    // Create a context with the updated configuration to get the right resources
    val resourcesContext = remember(context, configuration) {
        context.createConfigurationContext(configuration)
    }
    
    return if (formatArgs.isEmpty()) {
        resourcesContext.resources.getString(id)
    } else {
        resourcesContext.resources.getString(id, *formatArgs)
    }
}

/**
 * Manager to handle locale changes in Compose without Activity recreation.
 */
object LocaleManager {
    /**
     * Get Locale from language code.
     */
    fun getLocale(languageCode: String): Locale {
        return LocaleHelper.getLocale(languageCode)
    }
    
    /**
     * Update the context configuration with new locale.
     * Returns a new context with updated configuration.
     */
    fun updateContextLocale(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

/**
 * Composable that provides locale to the composition tree.
 * This allows dynamic locale changes without Activity recreation.
 * 
 * IMPORTANT: We preserve the Activity context (needed by Hilt) and only update
 * LocalConfiguration. The Activity context is kept intact so Hilt can create ViewModels.
 */
@Composable
fun LocaleProvider(
    currentLanguageCode: String,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = androidx.compose.ui.platform.LocalConfiguration.current
    
    // Get the locale for the current language
    val locale = remember(currentLanguageCode) {
        LocaleManager.getLocale(currentLanguageCode)
    }
    
    // Set the default locale (important for some system operations)
    Locale.setDefault(locale)
    
    // Create a new configuration with the updated locale
    val updatedConfiguration = remember(baseConfiguration, locale) {
        Configuration(baseConfiguration).apply {
            setLocale(locale)
        }
    }
    
    // Only update LocalConfiguration, NOT LocalContext
    // This preserves the Activity context for Hilt while allowing the UI to use the new locale
    // Note: stringResource() may not automatically pick up the new locale, but we can work around this
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalConfiguration provides updatedConfiguration,
        LocalLocale provides locale,
        content = content
    )
}
