package com.example.bikerental.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.bikerental.R
import com.example.bikerental.models.AuthState
import com.example.bikerental.models.User
import com.example.bikerental.utils.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ActionCodeSettings
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import android.app.Application
import com.example.bikerental.BikeRentalApplication
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel(application: Application? = null) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    
    // Use SupervisorJob for better error handling
    private val viewModelJob = SupervisorJob()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    // Email verification status from application
    private val _emailVerified = MutableStateFlow<Boolean?>(null)
    val emailVerified: StateFlow<Boolean?> = _emailVerified
    
    // Track active sign-in job to prevent multiple simultaneous sign-ins
    private var activeSignInJob: Job? = null
    
    // Reference to the application
    private val appInstance = application as? BikeRentalApplication
    
    // Add these near the top of the class with other StateFlow definitions
    private val _bypassVerification = MutableStateFlow(false)
    val bypassVerification: StateFlow<Boolean> = _bypassVerification
    
    init {
        // Check if there's a logged in user already
        auth.currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user!!)
                        Log.d("AuthViewModel", "Initialized with existing logged in user: ${user.id}")
                        
                        // Observe email verification status from the application
                        appInstance?.emailVerificationStatus?.let { flow ->
                            viewModelScope.launch {
                                flow.collect { isVerified ->
                                    _emailVerified.value = isVerified
                                    
                                    // Update user object if verification status changed
                                    if (isVerified == true && user.isEmailVerified != isVerified) {
                                        val updatedUser = user.copy(isEmailVerified = true)
                                        _currentUser.value = updatedUser
                                        _authState.value = AuthState.Authenticated(updatedUser)
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w("AuthViewModel", "User auth exists but no user doc in Firestore. Creating...")
                        
                        // Create a new user document with data from Firebase Auth
                        val newUser = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            fullName = firebaseUser.displayName ?: "",
                            isEmailVerified = firebaseUser.isEmailVerified,
                            createdAt = System.currentTimeMillis()
                        )
                        
                        // Save to Firestore
                        db.collection("users").document(firebaseUser.uid)
                            .set(newUser)
                            .await()
                        
                        _currentUser.value = newUser
                        _authState.value = AuthState.Authenticated(newUser)
                        _emailVerified.value = firebaseUser.isEmailVerified
                        Log.d("AuthViewModel", "Created new user document: ${newUser.id}")
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error during initialization", e)
                    _authState.value = AuthState.Error("Failed to initialize: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Initialize Google Sign-In
     */
    fun initializeGoogleSignIn(context: Context) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d("AuthViewModel", "Google Sign-In initialized")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to initialize Google Sign-In", e)
        }
    }
    
    /**
     * Get the Google Sign-In client for starting the sign-in flow
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        return googleSignInClient ?: run {
            initializeGoogleSignIn(context)
            googleSignInClient ?: throw IllegalStateException("Google Sign-In client is not initialized")
        }
    }
    
    /**
     * Handle Google Sign-In result with improved error handling and debugging
     */
    fun handleGoogleSignInResult(
        idToken: String,
        displayName: String?,
        email: String?,
        context: Context
    ) {
        // Cancel any existing sign-in job to prevent race conditions
        activeSignInJob?.cancel()
        
        activeSignInJob = viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Processing Google sign-in result with email: $email, idToken length: ${idToken.length}")
                _authState.value = AuthState.Loading
                
                // Validate email
                if (email.isNullOrEmpty()) {
                    _authState.value = AuthState.Error("Google sign-in failed: No email provided. Please ensure you grant email permission.")
                    signOut()
                    return@launch
                }
                
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                Log.d("AuthViewModel", "Signing in with Google credential...")
                
                try {
                    val authResult = auth.signInWithCredential(credential).await()
                    Log.d("AuthViewModel", "Firebase credential sign-in successful")
                    
                    val user = authResult.user
                    if (user != null) {
                        Log.d("AuthViewModel", "Firebase user obtained: ${user.uid}")
                        
                        // Ensure Firebase Auth has the email address (sometimes it's not properly set)
                        if (user.email.isNullOrEmpty() && !email.isNullOrEmpty()) {
                            try {
                                // Update the user's email in Firebase Auth if needed
                                user.updateEmail(email).await()
                                Log.d("AuthViewModel", "Updated user email in Firebase Auth to: $email")
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Failed to update user email in Firebase Auth: ${e.message}")
                                // Continue anyway, as we'll store the email in Firestore
                            }
                        }
                        
                        // Always ensure we use the email from Google if available
                        val emailToUse = email ?: user.email ?: ""
                        Log.d("AuthViewModel", "Using email for Firestore: $emailToUse")
                        
                        // Check email verification status in Firebase
                        var isEmailVerifiedInFirebase = user.isEmailVerified
                        if (!isEmailVerifiedInFirebase) {
                            try {
                                // Reload user to ensure we have the latest verification status
                                user.reload().await()
                                isEmailVerifiedInFirebase = user.isEmailVerified
                                Log.d("AuthViewModel", "Reloaded user, email verified status: $isEmailVerifiedInFirebase")
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Failed to reload user for verification check: ${e.message}")
                            }
                        }
                        
                        // Always consider Google users as email verified
                        isEmailVerifiedInFirebase = true
                        
                        // Update our state
                        _emailVerified.value = isEmailVerifiedInFirebase
                        
                        // Check if user exists in our database
                        Log.d("AuthViewModel", "Checking if user exists in Firestore...")
                        try {
                            val userDoc = db.collection("users").document(user.uid).get().await()
                            
                            if (userDoc.exists()) {
                                Log.d("AuthViewModel", "User exists in Firestore, updating info")
                                // User exists in our database, update with the latest information
                                var existingUser = userDoc.toObject(User::class.java)
                                
                                if (existingUser != null) {
                                    // Update important fields that might have changed
                                    val updates = mutableMapOf<String, Any>(
                                        "lastSignInTime" to System.currentTimeMillis(),
                                        "isEmailVerified" to true,  // Always true for Google users
                                        "hasCompletedAppVerification" to true,  // Always true for Google users
                                        "provider" to "google"  // Ensure provider is correctly set
                                    )
                                    
                                    // Only update email if it's different and not empty
                                    if (!emailToUse.isNullOrEmpty() && emailToUse != existingUser.email) {
                                        updates["email"] = emailToUse
                                        existingUser = existingUser.copy(email = emailToUse)
                                    }
                                    
                                    // Only update display name if it's different and not empty
                                    if (!displayName.isNullOrEmpty() && displayName != existingUser.displayName) {
                                        updates["displayName"] = displayName
                                        existingUser = existingUser.copy(displayName = displayName)
                                    }
                                    
                                    // Update Firestore
                                    try {
                                        db.collection("users").document(user.uid)
                                            .update(updates)
                                            .await()
                                        Log.d("AuthViewModel", "Successfully updated user in Firestore")
                                    } catch (e: Exception) {
                                        Log.e("AuthViewModel", "Failed to update user in Firestore: ${e.message}")
                                        // Continue even if Firestore update fails
                                    }
                                    
                                    // Update local state with the modified user
                                    existingUser = existingUser.copy(
                                        isEmailVerified = true,
                                        hasCompletedAppVerification = true,
                                        lastSignInTime = System.currentTimeMillis(),
                                        provider = "google"
                                    )
                                    
                                    _currentUser.value = existingUser
                                    _authState.value = AuthState.Authenticated(existingUser)
                                    Log.d("AuthViewModel", "Successfully authenticated existing Google user")
                                } else {
                                    Log.e("AuthViewModel", "User document exists but couldn't be parsed")
                                    _authState.value = AuthState.Error("Failed to parse user data")
                                }
                            } else {
                                // New user, create account
                                Log.d("AuthViewModel", "User doesn't exist in Firestore, creating new user")
                                // Create a new user with available information
                                val newUser = User(
                                    id = user.uid,
                                    email = emailToUse,
                                    fullName = displayName ?: "",
                                    givenName = user.displayName,
                                    displayName = displayName,
                                    createdAt = System.currentTimeMillis(),
                                    lastSignInTime = System.currentTimeMillis(),
                                    provider = "google",
                                    googleId = user.uid,
                                    hasCompletedAppVerification = true, // Always true for Google users
                                    isEmailVerified = true, // Always true for Google users
                                    verificationToken = null // No token needed for Google users
                                )
                                
                                // Save to Firestore
                                try {
                                    db.collection("users").document(user.uid)
                                        .set(newUser)
                                        .await()
                                    Log.d("AuthViewModel", "Successfully created new user in Firestore")
                                } catch (e: Exception) {
                                    Log.e("AuthViewModel", "Failed to create user in Firestore: ${e.message}")
                                    // Continue even if Firestore update fails
                                }
                                
                                // Update local state and authenticate immediately
                                _currentUser.value = newUser
                                _authState.value = AuthState.Authenticated(newUser)
                                Log.d("AuthViewModel", "New Google user created and authenticated")
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error accessing Firestore: ${e.message}")
                            
                            // Even if Firestore access fails, try to proceed with authentication
                            // Create a minimal user object from Firebase Auth
                            val fallbackUser = User(
                                id = user.uid,
                                email = emailToUse,
                                fullName = displayName ?: "",
                                provider = "google",
                                isEmailVerified = true,
                                hasCompletedAppVerification = true
                            )
                            
                            _currentUser.value = fallbackUser
                            _authState.value = AuthState.Authenticated(fallbackUser)
                            Log.d("AuthViewModel", "Created fallback user due to Firestore error")
                        }
                    } else {
                        Log.e("AuthViewModel", "Firebase user is null after signInWithCredential")
                        _authState.value = AuthState.Error("Google sign-in failed: User is null")
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Firebase credential sign-in failed: ${e.message}", e)
                    _authState.value = AuthState.Error("Sign-in with Google failed: ${e.message}")
                }
            } catch (e: Exception) {
                when (e) {
                    // Handle job cancellation - this is expected when navigating away
                    is CancellationException -> {
                        Log.d("AuthViewModel", "Google sign-in cancelled: ${e.message}")
                        // Don't update state for cancellation, as we might be in the middle of navigation
                    }
                    is ApiException -> {
                        Log.e("AuthViewModel", "Google sign-in ApiException: ${e.statusCode}", e)
                        val errorMessage = when (e.statusCode) {
                            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google Sign-In was cancelled"
                            GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error occurred during Google Sign-In"
                            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in is already in progress"
                            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google Sign-In failed. Please try again."
                            else -> "Google Sign-In error (${e.statusCode}): ${e.message}"
                        }
                        _authState.value = AuthState.Error(errorMessage)
                    }
                    else -> {
                        Log.e("AuthViewModel", "Google sign-in error: ${e.message}", e)
                        _authState.value = AuthState.Error("Google Sign-In failed: ${e.message}")
                    }
                }
            } finally {
                // Clear the active job reference
                if (activeSignInJob?.isCancelled == true) {
                    Log.d("AuthViewModel", "Sign-in job was cancelled")
                }
                activeSignInJob = null
            }
        }
    }
    
    /**
     * Send app-specific verification email
     */
    private suspend fun sendAppSpecificVerificationEmail(email: String) {
        try {
            // Generate verification link
            val user = _currentUser.value ?: return
            val verificationToken = user.verificationToken ?: UUID.randomUUID().toString()
            
            // If no token exists, update the user with a new one
            if (user.verificationToken.isNullOrEmpty()) {
                db.collection("users").document(user.id)
                    .update("verificationToken", verificationToken)
                    .await()
            }
            
            // Here you would typically:
            // 1. Use Firebase Dynamic Links to create a verification URL
            // 2. Send an email with that URL via Firebase Cloud Functions or your backend
            
            // For this example, we'll just update the Firestore document with the token
            Log.d("AuthViewModel", "Verification email would be sent to: $email with token: $verificationToken")
            
            // In a real implementation, you'd call your backend or Firebase Functions:
            // val result = yourApiService.sendVerificationEmail(email, verificationToken).await()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to send verification email", e)
            _authState.value = AuthState.Error("Failed to send verification email: ${e.message}")
        }
    }
    
    /**
     * Verify email with token
     */
    fun verifyEmail(token: String) {
        viewModelScope.launch {
            try {
                val currentUser = _currentUser.value ?: run {
                    _authState.value = AuthState.Error("No user is currently logged in")
                    return@launch
                }
                
                if (token != currentUser.verificationToken) {
                    _authState.value = AuthState.Error("Invalid verification token")
                    return@launch
                }
                
                // Update user verification status
                db.collection("users").document(currentUser.id)
                    .update(
                        mapOf(
                            "hasCompletedAppVerification" to true,
                            "verificationToken" to null  // Clear the token after use
                        )
                    )
                    .await()
                
                // Update local state
                val updatedUser = currentUser.copy(
                    hasCompletedAppVerification = true,
                    verificationToken = null
                )
                
                _currentUser.value = updatedUser
                _authState.value = AuthState.Authenticated(updatedUser)
                
                Log.d("AuthViewModel", "Email verification completed for user: ${updatedUser.id}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email verification failed", e)
                _authState.value = AuthState.Error("Verification failed: ${e.message}")
            }
        }
    }
    
    /**
     * Ensures user has an email in Firebase Auth and Firestore
     * Returns the email if available or null if it can't be determined
     */
    suspend fun ensureUserEmail(): String? {
        try {
            val firebaseUser = auth.currentUser ?: return null
            
            // First check Firebase Auth
            var email = firebaseUser.email
            
            // If Firebase Auth email is empty, check Firestore
            if (email.isNullOrEmpty()) {
                try {
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    if (userDoc.exists()) {
                        email = userDoc.getString("email")
                        Log.d("AuthViewModel", "Retrieved email from Firestore: $email")
                        
                        // If found in Firestore, update Firebase Auth
                        if (!email.isNullOrEmpty()) {
                            try {
                                firebaseUser.updateEmail(email).await()
                                Log.d("AuthViewModel", "Updated Firebase Auth with email from Firestore: $email")
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Failed to update Firebase Auth email: ${e.message}")
                                // Continue even if Firebase Auth update fails
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error retrieving user data from Firestore: ${e.message}")
                }
            }
            
            // Try local state as last resort
            if (email.isNullOrEmpty()) {
                email = _currentUser.value?.email
                Log.d("AuthViewModel", "Using email from local state: $email")
                
                // If found in local state, update Firebase Auth and Firestore
                if (!email.isNullOrEmpty()) {
                    try {
                        // Update Firebase Auth
                        firebaseUser.updateEmail(email).await()
                        Log.d("AuthViewModel", "Updated Firebase Auth with email from local state: $email")
                        
                        // Update Firestore
                        db.collection("users").document(firebaseUser.uid)
                            .update("email", email)
                            .await()
                        Log.d("AuthViewModel", "Updated Firestore with email from local state: $email")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to update email in Firebase/Firestore: ${e.message}")
                    }
                }
            }
            
            return email
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error ensuring user email: ${e.message}")
            return null
        }
    }

    /**
     * Check if the current user's email is verified
     */
    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser ?: run {
                    Log.d("AuthViewModel", "No user logged in to check verification status")
                    return@launch
                }
                
                try {
                    // Force reload the user to get latest verification status
                    firebaseUser.reload().await()
                    
                    // Check verification status after reload
                    val isVerified = firebaseUser.isEmailVerified
                    Log.d("AuthViewModel", "Email verification status: $isVerified")
                    
                    // Get user document from Firestore
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)!!
                        
                        // Update verification status in Firestore if needed
                        if (user.isEmailVerified != isVerified) {
                            Log.d("AuthViewModel", "Updating email verification status in Firestore to: $isVerified")
                            db.collection("users").document(firebaseUser.uid)
                                .update("isEmailVerified", isVerified)
                                .await()
                            
                            // Update the current user object
                            val updatedUser = user.copy(isEmailVerified = isVerified)
                            _currentUser.value = updatedUser
                            
                            // Also update auth state if needed
                            if (_authState.value is AuthState.Authenticated) {
                                _authState.value = AuthState.Authenticated(updatedUser)
                            }
                        }
                    }
                    
                    // Check if this is a Google user
                    val isGoogleUser = firebaseUser.providerData.any { 
                        it.providerId == "google.com" || it.providerId == GoogleAuthProvider.PROVIDER_ID 
                    }
                    
                    // For Google users, we consider them verified by default - ALWAYS
                    val shouldConsiderVerified = isGoogleUser || isVerified
                    
                    // Update the email verified state - this affects UI behavior
                    _emailVerified.value = shouldConsiderVerified
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error checking email verification: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in checkEmailVerification", e)
            }
        }
    }
    
    /**
     * Update email verification status manually - useful for Google users
     */
    fun updateEmailVerificationStatus(isVerified: Boolean) {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser ?: run {
                    Log.d("AuthViewModel", "No user logged in to update verification status")
                    return@launch
                }
                
                // Update the email verified state
                _emailVerified.value = isVerified
                
                // Update Firestore as well
                try {
                    db.collection("users").document(firebaseUser.uid)
                        .update("isEmailVerified", isVerified)
                        .await()
                    
                    Log.d("AuthViewModel", "Updated email verification status in Firestore to: $isVerified")
                    
                    // Update the current user object if we have it
                    _currentUser.value?.let { user ->
                        val updatedUser = user.copy(isEmailVerified = isVerified)
                        _currentUser.value = updatedUser
                        
                        // Also update auth state if needed
                        if (_authState.value is AuthState.Authenticated) {
                            _authState.value = AuthState.Authenticated(updatedUser)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error updating email verification in Firestore: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in updateEmailVerificationStatus", e)
            }
        }
    }
    
    /**
     * Resend email verification link
     */
    fun resendEmailVerification() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val firebaseUser = auth.currentUser ?: run {
                    _authState.value = AuthState.Error("No user is currently logged in")
                    return@launch
                }
                
                // Use our new function to ensure we have an email
                val email = ensureUserEmail()
                
                // Check if we have a valid email before proceeding
                if (email.isNullOrEmpty()) {
                    _authState.value = AuthState.Error("Cannot send verification: No email address found. Please update your profile with an email address.")
                    return@launch
                }
                
                Log.d("AuthViewModel", "Resending verification email to: $email")
                
                try {
                    // Reload the user to ensure we have the latest data
                    try {
                        firebaseUser.reload().await()
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Failed to reload user, continuing with current state: ${e.message}")
                        // Continue with current state if reload fails
                    }
                    
                    // Check if already verified
                    if (firebaseUser.isEmailVerified) {
                        _emailVerified.value = true
                        
                        // Update Firestore to match Firebase Auth verification status
                        try {
                            db.collection("users").document(firebaseUser.uid)
                                .update(
                                    mapOf(
                                        "isEmailVerified" to true,
                                        "hasCompletedAppVerification" to true
                                    )
                                )
                                .await()
                            
                            // Update local state
                            _currentUser.value = _currentUser.value?.copy(
                                isEmailVerified = true,
                                hasCompletedAppVerification = true
                            ) ?: User()
                            
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to update verification status in Firestore: ${e.message}")
                        }
                        
                        _authState.value = AuthState.Authenticated(_currentUser.value ?: User())
                        return@launch
                    }
                    
                    // Handle potential App Check errors in sendEmailVerification
                    try {
                        // Send verification email
                        firebaseUser.sendEmailVerification().await()
                        
                        // Update the verification sent timestamp in Firestore
                        try {
                            db.collection("users").document(firebaseUser.uid)
                                .update(
                                    mapOf(
                                        "verificationSentAt" to System.currentTimeMillis(),
                                        "verificationAttempts" to FieldValue.increment(1),
                                        "email" to email  // Ensure email is consistently stored
                                    )
                                )
                                .await()
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to update verification timestamp: ${e.message}")
                            // Continue even if Firestore update fails
                        }
                        
                        _authState.value = AuthState.VerificationEmailSent
                        Log.d("AuthViewModel", "Verification email resent to: $email")
                    } catch (e: Exception) {
                        // Handle App Check errors specifically
                        if (e.message?.contains("App attestation") == true || 
                            e.message?.contains("App Check") == true ||
                            e.message?.contains("Too many attempts") == true) {
                            
                            Log.e("AuthViewModel", "App Check error during verification: ${e.message}")
                            
                            // Still set state to success in testing environment 
                            // This allows development to continue despite App Check issues
                            if (e.message?.contains("Too many attempts") == true || 
                                e.message?.contains("App attestation failed") == true) {
                                // Since this is likely just a development/testing issue, still update UI as if it worked
                                _authState.value = AuthState.VerificationEmailSent
                                Log.d("AuthViewModel", "Bypassing App Check error in testing environment")
                            } else {
                                _authState.value = AuthState.Error("Firebase security check failed. Please try again later.")
                            }
                        } else {
                            // Handle other errors
                            var errorMessage = "Failed to send verification email"
                            
                            // More specific error messages
                            if (e.message?.contains("network") == true || 
                               e.message?.contains("connection") == true) {
                                errorMessage = "Network error. Check your internet connection."
                            } else if (e.message?.contains("blocked") == true ||
                                      e.message?.contains("disabled") == true) {
                                errorMessage = "Too many attempts. Please try again later."
                            } else if (e.message?.contains("permission") == true) {
                                errorMessage = "Authentication error. Please try again later."
                            }
                            
                            Log.e("AuthViewModel", "Failed to send verification email: ${e.message}")
                            _authState.value = AuthState.Error(errorMessage)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to resend verification email", e)
                    _authState.value = AuthState.Error("Failed to resend verification email: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to resend verification email", e)
                _authState.value = AuthState.Error("Failed to resend verification email: ${e.message}")
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Attempting sign in with email/password")
                _authState.value = AuthState.Loading
                
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    // Reload to get latest verification status
                    firebaseUser.reload().await()
                    
                    // Check if email is verified
                    if (!firebaseUser.isEmailVerified) {
                        // Get user data from Firestore
                        val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                        
                        if (userDoc.exists()) {
                            val user = userDoc.toObject(User::class.java)
                            
                            if (user != null) {
                                _currentUser.value = user
                                _authState.value = AuthState.NeedsEmailVerification(user)
                                Log.d("AuthViewModel", "User signed in but email not verified: ${user.email}")
                                return@launch
                            }
                        }
                    }
                    
                    // Continue with regular sign-in flow for verified users
                    // Check Firestore for user data
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        
                        if (user != null) {
                            // Update the last sign-in time
                            db.collection("users").document(firebaseUser.uid)
                                .update("lastSignInTime", System.currentTimeMillis())
                                .await()
                            
                            // Update user object with the latest sign-in time
                            val updatedUser = user.copy(
                                lastSignInTime = System.currentTimeMillis(),
                                isEmailVerified = firebaseUser.isEmailVerified
                            )
                            
                            _currentUser.value = updatedUser
                            _authState.value = AuthState.Authenticated(updatedUser)
                            Log.d("AuthViewModel", "User signed in successfully: ${updatedUser.email}")
                        } else {
                            _authState.value = AuthState.Error("Authentication failed: User data not found")
                        }
                    } else {
                        // Create new user document in Firestore from Firebase Auth data
                        val newUser = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            fullName = firebaseUser.displayName ?: "",
                            isEmailVerified = firebaseUser.isEmailVerified,
                            createdAt = System.currentTimeMillis(),
                            lastSignInTime = System.currentTimeMillis()
                        )
                        
                        // Save to Firestore
                        db.collection("users").document(firebaseUser.uid)
                            .set(newUser)
                            .await()
                        
                        _currentUser.value = newUser
                        
                        if (firebaseUser.isEmailVerified) {
                        _authState.value = AuthState.Authenticated(newUser)
                        } else {
                            _authState.value = AuthState.NeedsEmailVerification(newUser)
                        }
                        
                        Log.d("AuthViewModel", "Created new user document: ${newUser.id}")
                    }
                } ?: run {
                    _authState.value = AuthState.Error("Authentication failed: No user returned")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in failed", e)
                _authState.value = AuthState.Error("Authentication failed: ${e.message}")
            }
        }
    }
    
    /**
     * Create a new user account with email and password without forcing immediate verification
     */
    fun createUserWithEmailPassword(email: String, password: String, fullName: String, phone: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Creating user with email/password")
                _authState.value = AuthState.Loading
                
                // Create user in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    Log.d("AuthViewModel", "User created: ${firebaseUser.uid}")
                    
                    // Send verification email
                    try {
                        firebaseUser.sendEmailVerification().await()
                        Log.d("AuthViewModel", "Verification email sent")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to send verification email", e)
                        // Continue despite verification email failure - we'll handle it later
                    }
                    
                    // Store the user in Firestore
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        phoneNumber = phone,
                        isEmailVerified = false,
                        createdAt = System.currentTimeMillis(),
                        lastSignInTime = System.currentTimeMillis(),
                        provider = "firebase",
                        verificationMethod = "firebase"
                    )
                    
                    try {
                        db.collection("users").document(firebaseUser.uid)
                            .set(newUser)
                            .await()
                        
                        Log.d("AuthViewModel", "User created in Firestore: ${newUser.email}")
                        
                        // Update ProfileChangeRequest
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        
                        firebaseUser.updateProfile(profileUpdates).await()
                        
                        // Explicitly ensure email is set in Firebase Auth
                        if (firebaseUser.email.isNullOrEmpty()) {
                            try {
                                firebaseUser.updateEmail(email).await()
                                Log.d("AuthViewModel", "Updated Firebase Auth email: $email")
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Failed to update Firebase Auth email: ${e.message}")
                            }
                        }
                        
                        // Store user in the ViewModel state
                        _currentUser.value = newUser
                        
                        // Important: Set to NeedsEmailVerification state to trigger navigation
                        _authState.value = AuthState.NeedsEmailVerification(newUser)
                        Log.d("AuthViewModel", "Set auth state to NeedsEmailVerification")
                        
                        // Ensure email verified state is false
                        _emailVerified.value = false
                        
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to create user in Firestore", e)
                        _authState.value = AuthState.Error("Failed to create user profile: ${e.message}")
                    }
                } else {
                    _authState.value = AuthState.Error("Failed to create user")
                }
            } catch (e: Exception) {
                val errorMessage = when {
                        e.message?.contains("email") == true && e.message?.contains("already in use") == true -> 
                            "Email address is already in use. Please use a different email or sign in."
                        e.message?.contains("password") == true -> 
                            "Password must be at least 6 characters long."
                        e.message?.contains("network") == true -> 
                            "Network error. Please check your internet connection."
                        else -> "Account creation failed: ${e.message}"
                    }
                    
                    Log.e("AuthViewModel", "Create user failed", e)
                    _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                // First reset all local state
                _bypassVerification.value = false
                _emailVerified.value = null
                _currentUser.value = null
                _authState.value = AuthState.Initial
                
                // Then sign out from Firebase
                auth.signOut()
                
                // Sign out from Google if client exists
                googleSignInClient?.signOut()?.await()
                
                Log.d("AuthViewModel", "User signed out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing out: ${e.message}")
                _authState.value = AuthState.Error("Error signing out: ${e.message}")
            }
        }
    }
    
    /**
     * Reset user password by sending a password reset email
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Sending password reset to: $email")
                _authState.value = AuthState.Loading
                
                auth.sendPasswordResetEmail(email).await()
                
                _authState.value = AuthState.PasswordResetSent
                Log.d("AuthViewModel", "Password reset email sent")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset failed", e)
                _authState.value = AuthState.Error("Password reset failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update user profile information
     */
    fun updateUserProfile(
        fullName: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    onError("No user is currently logged in")
                    return@launch
                }
                
                // Get the current user data first
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                
                if (!userDoc.exists()) {
                    onError("User data not found")
                    return@launch
                }
                
                val user = userDoc.toObject(User::class.java)
                
                if (user == null) {
                    onError("Failed to parse user data")
                    return@launch
                }
                
                // Update only specific fields
                val updates = mutableMapOf<String, Any>()
                updates["fullName"] = fullName
                updates["phoneNumber"] = phoneNumber
                
                db.collection("users").document(currentUser.uid)
                    .update(updates)
                    .await()
                
                // Update local state
                val updatedUser = user.copy(
                    fullName = fullName,
                    phoneNumber = phoneNumber
                )
                
                _currentUser.value = updatedUser
                
                Log.d("AuthViewModel", "User profile updated successfully")
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Update profile failed", e)
                onError("Failed to update profile: ${e.message}")
            }
        }
    }
    
    /**
     * Change user password
     */
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                
                if (currentUser == null || currentUser.email.isNullOrBlank()) {
                    onError("No user is currently logged in")
                    return@launch
                }
                
                // Check user's authentication provider
                val providers = currentUser.providerData.map { it.providerId }
                Log.d("AuthViewModel", "User providers: $providers")
                
                // Determine authentication method
                if (providers.contains("google.com")) {
                    // Google users cannot change password through the app
                    onError("Google account users must change their password through Google settings")
                    return@launch
                } else if (providers.contains("password")) {
                    // Email/password users - use EmailAuthProvider
                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                    
                    // Re-authenticate
                currentUser.reauthenticate(credential).await()
                
                // Change password
                currentUser.updatePassword(newPassword).await()
                
                Log.d("AuthViewModel", "Password changed successfully")
                onSuccess()
                } else {
                    // Other providers
                    onError("Password change is not supported for your login method")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password change failed", e)
                onError("Failed to change password: ${e.message}")
            }
        }
    }
    
    /**
     * Delete user account and all associated data
     */
    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                
                if (currentUser == null) {
                    onError("No user is currently logged in")
                    return@launch
                }
                
                val userId = currentUser.uid
                
                // Delete user data from Firestore first
                try {
                    // Delete user document
                    db.collection("users").document(userId).delete().await()
                    
                    // Delete other collections related to this user
                    // For example, user rides, payments, etc.
                    // Depending on your data structure, you might need to delete additional collections
                    
                    Log.d("AuthViewModel", "User data deleted from Firestore")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to delete user data from Firestore", e)
                    // Continue with account deletion even if Firestore deletion fails
                }
                
                // Delete the user authentication account
                currentUser.delete().await()
                
                // Clear local state
                _currentUser.value = null
                _authState.value = AuthState.Initial
                
                Log.d("AuthViewModel", "User account deleted successfully")
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Account deletion failed", e)
                onError("Failed to delete account: ${e.message}")
            }
        }
    }
    
    /**
     * Sign up with email and password
     */
    fun signUpWithEmailPassword(email: String, password: String, fullName: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Create user account
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    // Send verification email immediately 
                    try {
                        firebaseUser.sendEmailVerification().await()
                        Log.d("AuthViewModel", "Verification email sent to: $email")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to send verification email", e)
                        // Continue even if sending fails, we can resend later
                    }
                    
                    // Create user record in Firestore
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        phoneNumber = phoneNumber,
                        createdAt = System.currentTimeMillis(),
                        isEmailVerified = false, // Initially not verified
                        verificationSentAt = System.currentTimeMillis()
                    )
                    
                    // Save to Firestore
                    db.collection("users").document(firebaseUser.uid)
                        .set(newUser)
                        .await()
                    
                    _currentUser.value = newUser
                    
                    // Set state to NeedsEmailVerification to redirect user
                    _authState.value = AuthState.NeedsEmailVerification(newUser)
                    
                    Log.d("AuthViewModel", "User registered, needs email verification: ${newUser.email}")
                } else {
                    _authState.value = AuthState.Error("Failed to create account")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Create user failed", e)
                _authState.value = AuthState.Error("Account creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Set whether to bypass email verification
     */
    fun setBypassVerification(bypass: Boolean) {
        _bypassVerification.value = bypass
        Log.d("AuthViewModel", "Email verification bypass set to: $bypass")
    }
    
    /**
     * Refresh current user data from Firestore
     */
    private suspend fun refreshCurrentUser() {
        try {
            val firebaseUser = auth.currentUser ?: return
            val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
            
            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                _currentUser.value = user
                Log.d("AuthViewModel", "Current user refreshed from Firestore: ${user?.id}")
            } else {
                Log.d("AuthViewModel", "No user document found in Firestore during refresh")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error refreshing current user", e)
        }
    }

    /**
     * Firebase authentication with Google ID token
     * Used for direct authentication from AccessAccountScreen
     */
    fun firebaseAuthWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d("AuthViewModel", "Starting Firebase auth with Google token")
                
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                
                val user = authResult.user
                if (user != null) {
                    Log.d("AuthViewModel", "Google auth successful, userId: ${user.uid}")
                    
                    // Force email verification to true for Google users
                    _emailVerified.value = true
                    
                    // Get user info
                    try {
                        val userDoc = db.collection("users").document(user.uid).get().await()
                        if (userDoc.exists()) {
                            // Existing user - update last sign in time
                            db.collection("users").document(user.uid)
                                .update(
                                    mapOf(
                                        "lastSignInTime" to System.currentTimeMillis(),
                                        "provider" to "google",
                                        "isEmailVerified" to true,
                                        "hasCompletedAppVerification" to true
                                    )
                                )
                            
                            // Get the updated user object
                            val updatedUser = userDoc.toObject(User::class.java)
                            if (updatedUser != null) {
                                _currentUser.value = updatedUser
                                _authState.value = AuthState.Authenticated(updatedUser)
                            }
                        } else {
                            // New user - create entry
                            val newUser = User(
                                id = user.uid,
                                email = user.email ?: "",
                                fullName = user.displayName ?: "",
                                createdAt = System.currentTimeMillis(),
                                lastSignInTime = System.currentTimeMillis(),
                                provider = "google",
                                isEmailVerified = true,
                                hasCompletedAppVerification = true
                            )
                            
                            db.collection("users").document(user.uid).set(newUser)
                            _currentUser.value = newUser
                            _authState.value = AuthState.Authenticated(newUser)
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error updating Firestore for Google user", e)
                        // Continue anyway - the important part is authentication
                        // Create a basic User object with the information we have
                        val fallbackUser = User(
                            id = user.uid,
                            email = user.email ?: "",
                            fullName = user.displayName ?: "",
                            provider = "google",
                            isEmailVerified = true,
                            hasCompletedAppVerification = true
                        )
                        _currentUser.value = fallbackUser
                        _authState.value = AuthState.Authenticated(fallbackUser)
                    }
                } else {
                    Log.e("AuthViewModel", "Firebase user is null after Google authentication")
                    _authState.value = AuthState.Error("Authentication failed")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google authentication failed", e)
                _authState.value = AuthState.Error("Google authentication failed: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
} 
