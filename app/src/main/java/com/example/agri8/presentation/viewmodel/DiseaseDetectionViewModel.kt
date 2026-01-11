package com.example.agri8.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agri8.data.repository.DiseaseDetectionRepository
import com.example.agri8.data.repository.LanguageRepository
import com.example.agri8.data.repository.TranslationRepository
import com.example.agri8.domain.model.DiseaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Disease Detection Screen.
 * Manages all UI state and coordinates between Repository and View.
 * Follows MVVM architecture pattern.
 */
@HiltViewModel
class DiseaseDetectionViewModel @Inject constructor(
    private val diseaseDetectionRepository: DiseaseDetectionRepository,
    private val languageRepository: LanguageRepository,
    private val translationRepository: TranslationRepository
) : ViewModel() {
    
    // UI State
    private val _selectedImage = MutableStateFlow<Bitmap?>(null)
    val selectedImage: StateFlow<Bitmap?> = _selectedImage.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _isModelLoading = MutableStateFlow(true)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()
    
    private val _diseaseResult = MutableStateFlow<DiseaseResult?>(null)
    val diseaseResult: StateFlow<DiseaseResult?> = _diseaseResult.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentLanguageCode = MutableStateFlow<String>("en")
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()
    
    init {
        initializeModel()
        loadLanguageCode()
    }
    
    /**
     * Initialize the ML model in the repository.
     * This should be called when the ViewModel is created.
     */
    private fun initializeModel() {
        viewModelScope.launch {
            _isModelLoading.value = true
            _error.value = null
            
            try {
                val success = diseaseDetectionRepository.initializeModel()
                if (!success) {
                    _error.value = "Failed to load ML model"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Error initializing model: ${e.message}"
            } finally {
                _isModelLoading.value = false
            }
        }
    }
    
    /**
     * Load the current language code from repository.
     */
    private fun loadLanguageCode() {
        viewModelScope.launch {
            _currentLanguageCode.value = languageRepository.getLanguageCode() ?: "en"
        }
    }
    
    /**
     * Set the selected image for analysis.
     */
    fun setSelectedImage(bitmap: Bitmap?) {
        _selectedImage.value = bitmap
        if (bitmap == null) {
            _diseaseResult.value = null
            _error.value = null
        }
    }
    
    /**
     * Analyze the selected image using the ML model.
     * This method coordinates between repository and translation services.
     */
    fun analyzeImage() {
        val bitmap = _selectedImage.value ?: return
        
        // Check if model is ready
        if (!diseaseDetectionRepository.isModelReady()) {
            _error.value = "Model is not ready. Please wait..."
            return
        }
        
        _isAnalyzing.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Process image through repository (ML model inference)
                val result = diseaseDetectionRepository.processImage(bitmap)
                
                // Get selected language for translation
                val selectedLanguage = languageRepository.getLanguageCode() ?: "en"
                
                // Translate disease name and treatment if needed
                val translatedDiseaseName = if (selectedLanguage.isNotEmpty() && selectedLanguage != "en") {
                    try {
                        translationRepository.translateText(result.diseaseName, selectedLanguage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        result.diseaseName // Fallback to original if translation fails
                    }
                } else {
                    result.diseaseName
                }
                
                val translatedTreatment = if (selectedLanguage.isNotEmpty() && selectedLanguage != "en") {
                    try {
                        translationRepository.translateText(result.treatment, selectedLanguage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        result.treatment // Fallback to original if translation fails
                    }
                } else {
                    result.treatment
                }
                
                // Create translated result
                val translatedResult = DiseaseResult(
                    diseaseName = translatedDiseaseName,
                    confidence = result.confidence,
                    treatment = translatedTreatment
                )
                
                _diseaseResult.value = translatedResult
                
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to analyze image: ${e.message}"
                _diseaseResult.value = DiseaseResult(
                    diseaseName = "Error",
                    confidence = 0f,
                    treatment = "Failed to analyze image. Please try again."
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * Clear the selected image and reset state.
     */
    fun clearImage() {
        _selectedImage.value = null
        _diseaseResult.value = null
        _error.value = null
    }
}
