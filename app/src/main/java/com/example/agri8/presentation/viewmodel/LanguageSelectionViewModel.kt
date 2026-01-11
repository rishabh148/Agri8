package com.example.agri8.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agri8.data.repository.LanguageRepository
import com.example.agri8.domain.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val languageRepository: LanguageRepository
) : ViewModel() {
    
    private val _isLanguageSelected = MutableStateFlow<Boolean?>(null)
    val isLanguageSelected: StateFlow<Boolean?> = _isLanguageSelected.asStateFlow()
    
    private val _selectedLanguageCode = MutableStateFlow<String?>(null)
    val selectedLanguageCode: StateFlow<String?> = _selectedLanguageCode.asStateFlow()
    
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()
    
    init {
        checkLanguageSelection()
    }
    
    private fun checkLanguageSelection() {
        viewModelScope.launch {
            val isSelected = languageRepository.isLanguageSelected()
            _isLanguageSelected.value = isSelected
            if (isSelected) {
                _selectedLanguageCode.value = languageRepository.getLanguageCode()
            }
        }
    }
    
    fun selectLanguage(languageCode: String) {
        viewModelScope.launch {
            _isNavigating.value = true
            languageRepository.setLanguage(languageCode)
            _isLanguageSelected.value = true
            _selectedLanguageCode.value = languageCode
        }
    }
    
    /**
     * Select language synchronously (for immediate locale change).
     * This saves the language immediately before calling AppCompatDelegate.setApplicationLocales().
     */
    fun selectLanguageSync(languageCode: String) {
        languageRepository.setLanguageSync(languageCode)
        _isLanguageSelected.value = true
        _selectedLanguageCode.value = languageCode
    }
    
    fun clearNavigatingState() {
        _isNavigating.value = false
    }
    
    suspend fun getCurrentLanguageCode(): String? {
        return languageRepository.getLanguageCode()
    }
    
    /**
     * Check if a language has been selected (synchronous).
     * Used for determining initial navigation destination.
     */
    fun isLanguageSelectedSync(): Boolean {
        return languageRepository.isLanguageSelected()
    }
    
    companion object {
        val supportedLanguages = listOf(
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
}
