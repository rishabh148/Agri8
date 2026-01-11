package com.example.agri8.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String): Context {
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
        return context.createConfigurationContext(config)
    }
    
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
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
    }
}
