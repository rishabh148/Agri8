package com.example.agri8.data.repository

import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Google ML Kit Translation API.
 * Handles translation of disease names and treatments to various languages.
 * Uses ML Kit's on-device translation for fast, offline-capable translations.
 */
@Singleton
class TranslationRepository @Inject constructor() {
    companion object {
        private const val TAG = "TranslationRepository"
    }
    
    // Cache translators to avoid recreating them for each translation
    private val translatorCache = mutableMapOf<String, Translator>()
    
    /**
     * Maps app language codes to ML Kit TranslateLanguage codes.
     * Returns null for languages not supported by ML Kit.
     */
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
            "ml", "or", "pa" -> null // Not supported by ML Kit - will return original text
            else -> null
        }
    }
    
    /**
     * Get or create a translator for the given target language.
     * Translators are cached for better performance.
     */
    private suspend fun getTranslator(targetLang: String): Translator? {
        val cacheKey = "${TranslateLanguage.ENGLISH}_$targetLang"
        
        // Return cached translator if available
        translatorCache[cacheKey]?.let { return it }
        
        // Create new translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLang)
            .build()
        
        val translator = Translation.getClient(options)
        
        try {
            // Download model if needed (first time only)
            translator.downloadModelIfNeeded().await()
            Log.d(TAG, "Translation model ready for: $targetLang")
            
            // Cache the translator for future use
            translatorCache[cacheKey] = translator
            return translator
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading translation model for $targetLang", e)
            translator.close()
            return null
        }
    }
    
    /**
     * Translate text from English to the target language using Google ML Kit.
     * 
     * @param text The text to translate (should be in English)
     * @param targetLanguageCode The target language code (e.g., "hi", "te", "ta")
     * @return Translated text, or original text if translation fails or language not supported
     */
    suspend fun translateText(text: String, targetLanguageCode: String): String {
        return try {
            // If text is empty, return as-is
            if (text.isBlank()) {
                return text
            }
            
            val targetLang = getLanguageCodeForTranslation(targetLanguageCode)
            
            // If target language is English or not supported, return original text
            if (targetLang == null || targetLang == TranslateLanguage.ENGLISH) {
                Log.d(TAG, "No translation needed for language: $targetLanguageCode")
                return text
            }
            
            // Get translator (cached or new)
            val translator = getTranslator(targetLang) ?: run {
                Log.w(TAG, "Failed to get translator for $targetLang, returning original text")
                return text
            }
            
            // Perform translation
            val translatedText = translator.translate(text).await()
            Log.d(TAG, "Translated: '$text' -> '$translatedText' (lang: $targetLang)")
            
            translatedText
        } catch (e: Exception) {
            Log.e(TAG, "Translation error for text: '$text', language: $targetLanguageCode", e)
            // Return original text on error
            text
        }
    }
    
    /**
     * Translate multiple texts in batch.
     * More efficient than calling translateText multiple times.
     * 
     * @param texts List of texts to translate
     * @param targetLanguageCode The target language code
     * @return List of translated texts in the same order
     */
    suspend fun translateTexts(texts: List<String>, targetLanguageCode: String): List<String> {
        val targetLang = getLanguageCodeForTranslation(targetLanguageCode)
        
        if (targetLang == null || targetLang == TranslateLanguage.ENGLISH) {
            return texts
        }
        
        val translator = getTranslator(targetLang) ?: return texts
        
        return try {
            texts.map { text ->
                if (text.isBlank()) {
                    text
                } else {
                    try {
                        translator.translate(text).await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error translating: '$text'", e)
                        text
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch translation error", e)
            texts
        }
    }
    
    /**
     * Clean up cached translators.
     * Call this when translations are no longer needed to free resources.
     */
    fun cleanup() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        Log.d(TAG, "Translation cache cleared")
    }
}
