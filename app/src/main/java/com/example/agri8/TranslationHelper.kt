package com.example.agri8

import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

object TranslationHelper {
    private const val TAG = "TranslationHelper"
    private var translator: Translator? = null
    
    fun getLanguageCodeForTranslation(languageCode: String): String? {
        return when (languageCode) {
            "hi" -> TranslateLanguage.HINDI
            "te" -> TranslateLanguage.TELUGU
            "ta" -> TranslateLanguage.TAMIL
            "bn" -> TranslateLanguage.BENGALI
            "gu" -> TranslateLanguage.GUJARATI
            "kn" -> TranslateLanguage.KANNADA
            "mr" -> TranslateLanguage.MARATHI
            "ur" -> TranslateLanguage.URDU
            "en" -> TranslateLanguage.ENGLISH
            // ML Kit doesn't support Malayalam, Odia, and Punjabi directly
            // Use language codes as fallback or return null to skip translation
            "ml", "or", "pa" -> null // These languages are not supported by ML Kit
            else -> null
        }
    }
    
    suspend fun translateText(text: String, targetLanguageCode: String): String {
        return try {
            val targetLang = getLanguageCodeForTranslation(targetLanguageCode)
            
            // If language is not supported or is English, return original text
            if (targetLang == null || targetLang == TranslateLanguage.ENGLISH) {
                return text
            }
            
            // Create translator options
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLang)
                .build()
            
            val translator = Translation.getClient(options)
            
            // Download model if needed
            try {
                translator.downloadModelIfNeeded().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading translation model", e)
                // If download fails, return original text
                translator.close()
                return text
            }
            
            // Translate text
            val translatedText = translator.translate(text).await()
            translator.close()
            translatedText
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            // Return original text if translation fails
            text
        }
    }
}

