package com.example.agri8

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "Agri8Prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getSelectedLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "") ?: ""
    }
    
    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    fun isLanguageSelected(): Boolean {
        return getSelectedLanguage().isNotEmpty()
    }
    
    fun getLocale(): Locale {
        val langCode = getSelectedLanguage()
        return when (langCode) {
            "hi" -> Locale("hi", "IN") // Hindi
            "te" -> Locale("te", "IN") // Telugu
            "ta" -> Locale("ta", "IN") // Tamil
            "bn" -> Locale("bn", "IN") // Bengali
            "gu" -> Locale("gu", "IN") // Gujarati
            "kn" -> Locale("kn", "IN") // Kannada
            "ml" -> Locale("ml", "IN") // Malayalam
            "mr" -> Locale("mr", "IN") // Marathi
            "or" -> Locale("or", "IN") // Odia
            "pa" -> Locale("pa", "IN") // Punjabi
            "ur" -> Locale("ur", "IN") // Urdu
            "en" -> Locale("en", "US") // English
            else -> Locale("en", "US")
        }
    }
}

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)

object SupportedLanguages {
    val languages = listOf(
        Language("hi", "Hindi", "हिंदी"),
        Language("te", "Telugu", "తెలుగు"),
        Language("ta", "Tamil", "தமிழ்"),
        Language("bn", "Bengali", "বাংলা"),
        Language("gu", "Gujarati", "ગુજરાતી"),
        Language("kn", "Kannada", "ಕನ್ನಡ"),
        Language("ml", "Malayalam", "മലയാളം"),
        Language("mr", "Marathi", "मराठी"),
        Language("or", "Odia", "ଓଡ଼ିଆ"),
        Language("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        Language("ur", "Urdu", "اردو"),
        Language("en", "English", "English")
    )
}

