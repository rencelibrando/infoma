package com.example.bikerental

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BikeRentalApplication : Application() {
    
    private val TAG = "AppCheck"
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase first
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check
        initializeAppCheck()
    }
    
    private fun initializeAppCheck() {
        // Initialize in a coroutine to not block app startup
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val appCheck = FirebaseAppCheck.getInstance()
                
                // Use Play Integrity
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "Play Integrity App Check initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize App Check", e)
            }
        }
    }
} 