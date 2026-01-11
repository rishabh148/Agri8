package com.example.agri8.di

import com.example.agri8.data.repository.DiseaseDetectionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiseaseDetectionRepositoryEntryPoint {
    fun diseaseDetectionRepository(): DiseaseDetectionRepository
}
