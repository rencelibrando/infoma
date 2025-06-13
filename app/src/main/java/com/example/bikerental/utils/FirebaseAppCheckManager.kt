package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import com.example.bikerental.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class FirebaseAppCheckManager(private val context: Context) {
    private val TAG = "AppCheckManager"
    
    fun initializeAppCheck() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()

            if (BuildConfig.DEBUG) {
                // For debug builds, use the debug provider.
                Log.d(TAG, "Initializing App Check with debug provider.")
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                // For release builds, use Play Integrity.
                Log.d(TAG, "Initializing App Check with Play Integrity provider.")
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            }
            Log.d(TAG, "Firebase App Check initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase App Check", e)
        }
    }
} 