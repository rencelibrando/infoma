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
    
    // Email verification status flow that components can observe
    private val _emailVerificationStatus = MutableStateFlow<Boolean?>(null)
    val emailVerificationStatus: StateFlow<Boolean?> = _emailVerificationStatus
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase first
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check
        initializeAppCheck()
        
        // Start monitoring email verification status
        monitorEmailVerificationStatus()
    }
    
    private fun initializeAppCheck() {
        // Initialize in a coroutine to not block app startup
        applicationScope.launch {
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
    
    /**
     * Updates the email verification status in Firestore if the user has verified their email in Firebase Auth
     * Returns the current verification status
     */
    suspend fun checkAndUpdateEmailVerification(): Boolean {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
            
            try {
                // Reload user to get latest verification status
                currentUser.reload().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reload user: ${e.message}")
            }
            
            val isVerified = currentUser.isEmailVerified
            
            // Update the shared flow value
            _emailVerificationStatus.value = isVerified
            
            if (isVerified) {
                try {
                    // Check if Firestore needs updating
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                        
                    if (doc.exists() && !(doc.getBoolean("isEmailVerified") ?: false)) {
                        // Update Firestore since it's out of sync
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.uid)
                            .update("isEmailVerified", true)
                            .await()
                        Log.d(TAG, "Updated Firestore with verified email status")
                    }
                } catch (e: Exception) {
                    // Log but don't fail the verification check
                    Log.e(TAG, "Failed to update Firestore: ${e.message}")
                }
            }
            
            return isVerified
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email verification: ${e.message}")
            return false
        }
    }
    
    /**
     * Sets up a listener to monitor email verification status changes
     */
    private fun monitorEmailVerificationStatus() {
        applicationScope.launch {
            // Initial check
            try {
                checkAndUpdateEmailVerification()
            } catch (e: Exception) {
                Log.e(TAG, "Initial verification check failed: ${e.message}")
            }
            
            // Set up auth state listener to react to changes
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                applicationScope.launch {
                    if (auth.currentUser != null) {
                        try {
                            checkAndUpdateEmailVerification()
                        } catch (e: Exception) {
                            Log.e(TAG, "Verification check on auth change failed: ${e.message}")
                        }
                    } else {
                        _emailVerificationStatus.value = null
                    }
                }
            }
        }
    }
} 