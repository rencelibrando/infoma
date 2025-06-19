package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Utility class to manage app-wide configuration settings fetched from Firebase
 */
class AppConfigManager private constructor(context: Context) {
    
    private val TAG = "AppConfigManager"
    
    // Firestore reference for configuration
    private val firestore = FirebaseFirestore.getInstance()
    
    // Realtime Database reference for faster updates
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val configRef: DatabaseReference = realtimeDb.getReference("app_config")
    
    // Config listeners
    private var firestoreListener: ListenerRegistration? = null
    private var realtimeDbListener: ValueEventListener? = null
    
    // Location restriction configuration
    private val _isLocationRestrictionEnabled = MutableStateFlow(true) // Default to true/enabled
    val isLocationRestrictionEnabled: StateFlow<Boolean> = _isLocationRestrictionEnabled.asStateFlow()
    
    // Initialize and start listening for config changes
    init {
        startConfigListeners()
        Log.d(TAG, "AppConfigManager initialized")
    }
    
    /**
     * Start listening for configuration changes from both Firestore and Realtime Database
     */
    private fun startConfigListeners() {
        // Primary: Listen to Realtime Database for immediate updates
        realtimeDbListener = configRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val locationRestrictionEnabled = snapshot.child("locationRestrictionEnabled").getValue(Boolean::class.java)
                    
                    if (locationRestrictionEnabled != null) {
                        _isLocationRestrictionEnabled.value = locationRestrictionEnabled
                        Log.d(TAG, "Location restriction setting updated from Realtime DB: $locationRestrictionEnabled")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing config from Realtime Database", e)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Realtime Database config listener cancelled: ${error.message}")
            }
        })
        
        // Secondary: Listen to Firestore for backup/consistency
        firestoreListener = firestore.collection("app_config").document("settings")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen to Firestore config failed", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val locationRestrictionEnabled = snapshot.getBoolean("locationRestrictionEnabled")
                        
                        if (locationRestrictionEnabled != null) {
                            _isLocationRestrictionEnabled.value = locationRestrictionEnabled
                            Log.d(TAG, "Location restriction setting updated from Firestore: $locationRestrictionEnabled")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing config from Firestore", e)
                    }
                }
            }
    }
    
    /**
     * Stop configuration listeners to prevent memory leaks
     */
    fun cleanup() {
        firestoreListener?.remove()
        realtimeDbListener?.let { configRef.removeEventListener(it) }
        Log.d(TAG, "AppConfigManager cleaned up")
    }
    
    companion object {
        @Volatile private var instance: AppConfigManager? = null
        
        fun getInstance(context: Context): AppConfigManager {
            return instance ?: synchronized(this) {
                instance ?: AppConfigManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
} 