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
import android.util.Log

/**
 * Main Application class for the Bike Rental app.
 * Configures logging and other global settings.
 */
@HiltAndroidApp
class BikeRentalApplication : Application() {
    
    private val TAG = "BikeRentalApp"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var appCheckInitialized = false
    
    companion object {
        @Volatile
        private var INSTANCE: BikeRentalApplication? = null
        
        val instance: BikeRentalApplication
            get() = INSTANCE ?: throw IllegalStateException("Application not initialized")
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Set the static instance
        INSTANCE = this
        
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
        try {
            // Reduce excessive logging from various Google services
            // This helps with the "Too many Flogger logs" issue
            
            // Set system properties to reduce log verbosity
            System.setProperty("java.util.logging.manager", "com.google.common.flogger.backend.android.AndroidLoggingBackend")
            System.setProperty("flogger.backend_factory", "com.google.common.flogger.backend.android.AndroidLoggerBackendFactory#getInstance")
            
            // Configure log levels for specific packages
            java.util.logging.Logger.getLogger("com.google.firebase").level = java.util.logging.Level.WARNING
            java.util.logging.Logger.getLogger("com.google.android.gms").level = java.util.logging.Level.WARNING
            java.util.logging.Logger.getLogger("ProxyAndroidLoggerBackend").level = java.util.logging.Level.SEVERE
            
            Log.d("BikeRentalApp", "Logging configuration completed")
        } catch (e: Exception) {
            Log.w("BikeRentalApp", "Failed to configure logging: ${e.message}")
        }
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
    
    private fun initializeAppCheck() {
        try {
            val appCheckManager = FirebaseAppCheckManager(applicationContext)
            appCheckManager.initializeAppCheck()
        } catch (e: Exception) {
            LogManager.e(TAG, "Failed to initialize App Check", e)
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