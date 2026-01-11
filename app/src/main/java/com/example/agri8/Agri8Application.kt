package com.example.agri8

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Agri8Application : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
