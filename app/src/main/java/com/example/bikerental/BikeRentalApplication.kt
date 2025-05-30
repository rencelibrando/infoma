package com.example.bikerental

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.example.bikerental.utils.FirebaseAppCheckManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BikeRentalApplication : Application() {
    
    private val TAG = "BikeRentalApp"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var appCheckInitialized = false
    
    override fun onCreate() {
        super.onCreate()
        
        LogManager.d(TAG, "Application onCreate - starting initialization")
        
        // Initialize logging immediately (lightweight)
        initializeLogging()
        
        // Initialize Firebase app immediately (required for other operations)
        FirebaseApp.initializeApp(this)
        LogManager.d(TAG, "Firebase app initialized")
        
        // Move heavy operations to background thread to reduce main thread blocking
        // Use a slight delay to prevent ANR during startup
        applicationScope.launch {
            // Small delay to let main thread finish critical startup tasks
            kotlinx.coroutines.delay(100)
            
            LogManager.d(TAG, "Starting background Firebase operations...")
            initializeAppCheck()
            configureFirestore()
            LogManager.d(TAG, "Background Firebase operations completed")
        }
        
        // StrictMode setup only in debug (lightweight)
        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }
        
        LogManager.d(TAG, "Application initialization completed")
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
    
    private suspend fun configureFirestore() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            
            FirebaseFirestore.getInstance().firestoreSettings = settings
            LogManager.d(TAG, "Firestore settings configured")
        } catch (e: Exception) {
            LogManager.e(TAG, "Failed to configure Firestore settings", e)
        }
    }
    
    private suspend fun initializeAppCheck() {
        try {
            val appCheckManager = FirebaseAppCheckManager(applicationContext)
            appCheckInitialized = appCheckManager.initializeAppCheck()
            
            if (!appCheckInitialized) {
                // If app check initialization failed completely, we'll still let the app continue
                LogManager.w(TAG, "App Check initialization failed, proceeding without App Check")
                appCheckInitialized = true
            }
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