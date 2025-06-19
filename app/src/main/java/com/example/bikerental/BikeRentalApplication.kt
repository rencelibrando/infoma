package com.example.bikerental

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.example.bikerental.utils.FirebaseAppCheckManager
import com.example.bikerental.utils.LogManager
import com.example.bikerental.utils.PlacesApiService
import com.example.bikerental.utils.RoutesApiService
import com.example.bikerental.utils.AppConfigManager
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
import android.content.ComponentCallbacks2

/**
 * Main Application class for the Bike Rental app.
 * Configures logging and other global settings.
 */
@HiltAndroidApp
class BikeRentalApplication : Application() {
    
    private val TAG = "BikeRentalApp"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var appCheckInitialized = false
    
    // App Config Manager instance
    private lateinit var appConfigManager: AppConfigManager
    
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
        
        // Initialize Places API
        PlacesApiService.initialize(this)
        LogManager.d(TAG, "Places API initialization requested")
        
        // Initialize remaining components in background
        initializeInBackground()
        
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
    
    private fun initializePlacesApi() {
        try {
            PlacesApiService.initialize(applicationContext)
            // Also initialize Routes API
            RoutesApiService.initialize(applicationContext)
            LogManager.d(TAG, "Maps APIs initialized in Application class")
        } catch (e: Exception) {
            LogManager.e(TAG, "Failed to initialize Maps APIs", e)
        }
    }
    
    private fun initializeAppConfigManager() {
        try {
            appConfigManager = AppConfigManager.getInstance(applicationContext)
            LogManager.d(TAG, "App Config Manager initialized")
        } catch (e: Exception) {
            LogManager.e(TAG, "Failed to initialize App Config Manager", e)
        }
    }
    
    private fun initializeInBackground() {
        // Move heavy operations to background thread to reduce main thread blocking
        applicationScope.launch {
            // Small delay to let main thread finish critical startup tasks
            delay(100)
            
            LogManager.d(TAG, "Starting background Firebase operations...")
            initializeAppCheck()
            configureFirestore()
            
            // Also initialize Places API in background
            initializePlacesApi()
            
            // Initialize App Config Manager
            initializeAppConfigManager()
            
            LogManager.d(TAG, "Background Firebase operations completed")
        }
    }
    
    /**
     * Called when the OS determines that it's a good time to trim memory
     * This can happen when the app goes to the background
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Shutdown Places API client when app is in background or low on memory
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            LogManager.d(TAG, "Trim memory level $level - shutting down Places API client")
            PlacesApiService.shutdown()
        }
    }
    
    /**
     * Final cleanup when the application is being terminated
     */
    override fun onTerminate() {
        LogManager.d(TAG, "Application is terminating - cleaning up resources")
        
        // Shutdown Places API client
        PlacesApiService.shutdown()
        
        // Clean up App Config Manager
        if (::appConfigManager.isInitialized) {
            appConfigManager.cleanup()
        }
        
        super.onTerminate()
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