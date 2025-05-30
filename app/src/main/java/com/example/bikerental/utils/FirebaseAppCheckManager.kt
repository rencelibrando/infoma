package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseAppCheckManager(private val context: Context) {
    private val TAG = "AppCheckManager"
    
    suspend fun initializeAppCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try Play Integrity first (preferred method)
                val playIntegritySuccess = withTimeoutOrNull(5000) {
                    initializeWithPlayIntegrity()
                } ?: false
                
                if (playIntegritySuccess) {
                    Log.d(TAG, "Successfully initialized App Check with Play Integrity")
                    return@withContext true
                }
                
                // If Play Integrity fails, try SafetyNet as fallback
                val safetyNetSuccess = withTimeoutOrNull(5000) {
                    initializeWithSafetyNet()
                } ?: false
                
                if (safetyNetSuccess) {
                    Log.d(TAG, "Successfully initialized App Check with SafetyNet")
                    return@withContext true
                }
                
                // If in debug mode and all else fails, use debug provider
                if (com.example.bikerental.BuildConfig.DEBUG) {
                    Log.w(TAG, "Using debug provider for App Check in DEBUG mode")
                    val debugSuccess = initializeWithDebugProvider()
                    return@withContext debugSuccess
                }
                
                // If we get here, all methods failed
                Log.e(TAG, "All App Check initialization methods failed")
                return@withContext false
            } catch (e: Exception) {
                Log.e(TAG, "App Check initialization failed", e)
                return@withContext false
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
            Log.w(TAG, "Play Integrity initialization failed", e)
            false
        }
    }
    
    private suspend fun initializeWithSafetyNet(): Boolean {
        return try {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance()
            )
            
            // Force token refresh to verify it's working
            appCheck.getAppCheckToken(true).await()
            Log.d(TAG, "SafetyNet App Check initialized successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "SafetyNet initialization failed", e)
            false
        }
    }
    
    private fun initializeWithDebugProvider(): Boolean {
        return try {
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Debug App Check provider initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Debug provider initialization failed", e)
            false
        }
    }
} 