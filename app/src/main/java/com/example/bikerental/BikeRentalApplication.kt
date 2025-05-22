package com.example.bikerental

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.example.bikerental.utils.LogManager
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BikeRentalApplication : Application() {
    
    private val TAG = "AppCheck"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var appCheckInitialized = false
    
    override fun onCreate() {
        super.onCreate()
        
        initializeLogging()
        
        FirebaseApp.initializeApp(this)
        
        initializeAppCheck()
        
        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }
        
        val settings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
    
    private fun initializeLogging() {
        val logLevel = if (BuildConfig.DEBUG) {
            LogManager.LogLevel.DEBUG
        } else {
            LogManager.LogLevel.INFO
        }
        
        LogManager.configure(logLevel)
        
        LogManager.i(TAG, "Application starting, logging system initialized")
    }
    
    private fun initializeAppCheck() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            
            appCheck.setTokenAutoRefreshEnabled(true)
            
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            LogManager.d(TAG, "Play Integrity App Check initialized with token caching")
            
            appCheckInitialized = true
        } catch (e: Exception) {
            LogManager.e(TAG, "Failed to initialize App Check", e)
            appCheckInitialized = true
        }
    }
    
    private fun setupStrictMode() {
        LogManager.d(TAG, "Setting up StrictMode to detect main thread violations")
        
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog() 
            .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectCleartextNetwork()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        detectNonSdkApiUsage()
                    }
                }
                .penaltyLog()
                .build()
        )
    }
} 