package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseAppCheckManager(private val context: Context) {
    private val TAG = "AppCheckManager"
    
    suspend fun initializeAppCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Use Play Integrity
                initializeWithPlayIntegrity()
            } catch (e: Exception) {
                Log.e(TAG, "App Check initialization failed", e)
                false
            }
        }
    }
    
    private suspend fun initializeWithPlayIntegrity(): Boolean {
        return try {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            
            // Force token refresh to verify it's working
            appCheck.getAppCheckToken(true).await()
            Log.d(TAG, "Play Integrity App Check initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Play Integrity initialization failed", e)
            throw e
        }
    }
} 