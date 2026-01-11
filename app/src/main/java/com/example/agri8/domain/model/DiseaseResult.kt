package com.example.agri8.domain.model

data class DiseaseResult(
    val diseaseName: String,
    val confidence: Float,
    val treatment: String
)
