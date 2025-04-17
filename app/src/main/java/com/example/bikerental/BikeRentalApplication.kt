package com.example.bikerental

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BikeRentalApplication : Application() {
    
    private val TAG = "AppCheck"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Track App Check initialization
    private val _appCheckInitialized = MutableStateFlow(false)
    val appCheckInitialized: StateFlow<Boolean> = _appCheckInitialized
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase first
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check - do this synchronously to avoid race conditions
        initializeAppCheck()
    }
    
    private fun initializeAppCheck() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            
            // Configure caching to reduce token requests
            appCheck.setTokenAutoRefreshEnabled(true)
            
            // Configure limited debug token if needed
            // appCheck.installAppCheckDebugProvider("YOUR-DEBUG-TOKEN")
            
            // Use Play Integrity
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "Play Integrity App Check initialized with token caching")
            
            // Mark as initialized
            _appCheckInitialized.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize App Check", e)
            // Still mark as initialized to prevent blocking app functionality
            _appCheckInitialized.value = true
        }
    }
} 