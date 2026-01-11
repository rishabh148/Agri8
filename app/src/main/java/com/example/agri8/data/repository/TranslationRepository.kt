package com.example.agri8.data.repository

import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor() {
    companion object {
        private const val TAG = "TranslationRepository"
    }
    
    private fun getLanguageCodeForTranslation(languageCode: String): String? {
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
            "ml", "or", "pa" -> null // Not supported by ML Kit
            else -> null
        }
    }
    
    suspend fun translateText(text: String, targetLanguageCode: String): String {
        return try {
            val targetLang = getLanguageCodeForTranslation(targetLanguageCode)
            
            if (targetLang == null || targetLang == TranslateLanguage.ENGLISH) {
                return text
            }
            
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLang)
                .build()
            
            val translator = Translation.getClient(options)
            
            try {
                translator.downloadModelIfNeeded().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading translation model", e)
                translator.close()
                return text
            }
            
            val translatedText = translator.translate(text).await()
            translator.close()
            translatedText
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            text
        }
    }
}
