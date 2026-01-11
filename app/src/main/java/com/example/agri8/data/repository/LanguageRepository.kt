package com.example.agri8.data.repository

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageRepository @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("Agri8Prefs", Context.MODE_PRIVATE)
    private val KEY_LANGUAGE = "selected_language"
    
    fun getLanguageCode(): String? {
        return prefs.getString(KEY_LANGUAGE, null)
    }
    
    suspend fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    /**
     * Set language synchronously (blocks until saved).
     * Use this when you need the language to be saved immediately before locale change.
     */
    fun setLanguageSync(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).commit()
    }
    
    fun isLanguageSelected(): Boolean {
        return getLanguageCode()?.isNotEmpty() == true
    }
}
