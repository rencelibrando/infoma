package com.example.bikerental.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.BikeRentalApplication
import com.example.bikerental.R
import com.example.bikerental.models.AuthState
import com.example.bikerental.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthViewModel(application: Application? = null) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null

    // Use SupervisorJob for better error handling in the main scope
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

    private val _bypassVerification = MutableStateFlow(false)
    val bypassVerification: StateFlow<Boolean> = _bypassVerification

    init {
        // Check if there's a logged in user already
        auth.currentUser?.let { firebaseUser ->
            viewModelScope.launch(viewModelJob) { // Use viewModelJob here
                try {
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()

                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        if (user != null) { // Check parsing success
                            _currentUser.value = user
                            _authState.value = AuthState.Authenticated(user)
                            Log.d("AuthViewModel", "Initialized with existing logged in user: ${user.id}")

                            // Perform initial verification check
                            checkEmailVerification()

                        } else {
                            Log.e("AuthViewModel", "Failed to parse existing user document: ${firebaseUser.uid}")
                            // Handle error, maybe sign out or set error state
                            _authState.value = AuthState.Error("Failed to load user profile.")
                        }
                    } else {
                        Log.w("AuthViewModel", "User auth exists but no user doc in Firestore. Creating...")
                        // Create a new user document with data from Firebase Auth
                        val newUser = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            fullName = firebaseUser.displayName ?: "",
                            isEmailVerified = firebaseUser.isEmailVerified, // Use Firebase Auth status initially
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
        context: Context // Context might not be needed here anymore if GSO init is separate
    ) {
        activeSignInJob?.cancel()
        activeSignInJob = viewModelScope.launch(viewModelJob) { // Use viewModelJob
            try { // Outer try (L158)
                Log.d("AuthViewModel", "Processing Google sign-in result...")
                _authState.value = AuthState.Loading

                if (email.isNullOrEmpty()) {
                    _authState.value = AuthState.Error("Google sign-in failed: No email provided.")
                    // Consider calling signOut() here or let the UI handle it
                    // signOut() // Call to class member function
                    Log.w("AuthViewModel", "Google sign-in failed: Email is null or empty.")
                    return@launch
                }

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                Log.d("AuthViewModel", "Signing in with Google credential...")

                try { // Inner try for Firebase sign-in (L171)
                    val authResult = auth.signInWithCredential(credential).await()
                    Log.d("AuthViewModel", "Firebase credential sign-in successful")
                    val user = authResult.user

                    if (user != null) { // Start L176 if
                        Log.d("AuthViewModel", "Firebase user obtained: ${user.uid}")

                        // Use email from Google result primarily
                        val emailToUse = email ?: user.email ?: ""

                        // Ensure Firebase Auth has the email address
                        if (user.email.isNullOrEmpty() && emailToUse.isNotEmpty()) {
                            try {
                                user.updateEmail(emailToUse).await()
                                Log.d("AuthViewModel", "Updated user email in Firebase Auth to: $emailToUse")
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Failed to update user email in Firebase Auth: ${e.message}")
                            }
                        }
                         // Always consider Google users as email verified
                        _emailVerified.value = true

                        // Check/Update Firestore document
                        try { // Innermost try for Firestore access (L217)
                            val userDocRef = db.collection("users").document(user.uid)
                            val userDoc = userDocRef.get().await()

                            if (userDoc.exists()) { // Start L219 if
                                Log.d("AuthViewModel", "User exists in Firestore, updating info")
                                var existingUser = userDoc.toObject(User::class.java)

                                if (existingUser != null) {
                                    // Create map for updates
                                    val updates = mutableMapOf<String, Any>(
                                        "lastSignInTime" to System.currentTimeMillis(),
                                        "isEmailVerified" to true,
                                        "hasCompletedAppVerification" to true,
                                        "provider" to "google"
                                    )
                                    // Update email if different
                                    if (emailToUse.isNotEmpty() && emailToUse != existingUser.email) {
                                        updates["email"] = emailToUse
                                    }
                                    // Update displayName if different
                                    if (!displayName.isNullOrEmpty() && displayName != existingUser.displayName) {
                                        updates["displayName"] = displayName
                                        updates["fullName"] = displayName // Assuming fullName should match
                                    }

                                    // Apply Firestore updates
                                    userDocRef.update(updates).await()

                                    // Create updated user object for local state
                                    val updatedUser = existingUser.copy(
                                        email = updates["email"] as? String ?: existingUser.email,
                                        displayName = updates["displayName"] as? String ?: existingUser.displayName,
                                        fullName = updates["fullName"] as? String ?: existingUser.fullName,
                                        lastSignInTime = updates["lastSignInTime"] as Long,
                                        isEmailVerified = true,
                                        hasCompletedAppVerification = true,
                                        provider = "google"
                                    )

                                    _currentUser.value = updatedUser
                                    _authState.value = AuthState.Authenticated(updatedUser)
                                    Log.d("AuthViewModel", "Updated existing user in Firestore: ${updatedUser.id}")

                                } else { // Else for if (existingUser != null)
                                    Log.e("AuthViewModel", "User document ${user.uid} exists but couldn't be parsed")
                                    _authState.value = AuthState.Error("Failed to parse user data")
                                }
                            } else { // Else for if (userDoc.exists()) L219
                                Log.d("AuthViewModel", "User doesn't exist in Firestore, creating new user")
                                val newUser = User(
                                    id = user.uid,
                                    email = emailToUse,
                                    fullName = displayName ?: "",
                                    displayName = displayName,
                                    createdAt = System.currentTimeMillis(),
                                    lastSignInTime = System.currentTimeMillis(),
                                    provider = "google",
                                    hasCompletedAppVerification = true,
                                    isEmailVerified = true,
                                    verificationToken = null
                                )
                                userDocRef.set(newUser).await()
                                _currentUser.value = newUser
                                _authState.value = AuthState.Authenticated(newUser)
                                Log.d("AuthViewModel", "New Google user created and authenticated: ${newUser.id}")
                            } // End else L219
                        } catch (e: Exception) { // Catch for L217 try (Firestore access)
                            Log.e("AuthViewModel", "Error accessing/updating Firestore for user ${user.uid}: ${e.message}", e)
                            // Even if Firestore fails, user is authenticated in Firebase Auth. Proceed with a fallback state.
                             val fallbackUser = User(
                                id = user.uid,
                                email = emailToUse,
                                fullName = displayName ?: user.displayName ?: "",
                                provider = "google",
                                isEmailVerified = true,
                                hasCompletedAppVerification = true
                            )
                            _currentUser.value = fallbackUser
                            _authState.value = AuthState.Authenticated(fallbackUser)
                            Log.w("AuthViewModel", "Authenticated user with fallback data due to Firestore error.")
                        } // End catch L217
                    } else { // Else for L176 if (user != null)
                        Log.e("AuthViewModel", "Firebase user is null after signInWithCredential")
                        _authState.value = AuthState.Error("Google sign-in failed: User data unavailable")
                    } // End else L176
                } catch (e: Exception) { // Catch for L171 try (Firebase sign-in)
                    Log.e("AuthViewModel", "Firebase credential sign-in failed: ${e.message}", e)
                    _authState.value = AuthState.Error("Sign-in with Google failed: ${e.message}")
                } // End catch L171

            } catch (e: Exception) { // Catch for L158 Outer try
                 when (e) {
                    is CancellationException -> Log.d("AuthViewModel", "Google sign-in cancelled") // Expected on scope cancellation
                    is ApiException -> {
                        Log.e("AuthViewModel", "Google sign-in ApiException: ${e.statusCode}", e)
                        val errorMessage = when (e.statusCode) {
                            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google Sign-In was cancelled"
                            GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error during Google Sign-In"
                            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Sign-in already in progress"
                            // Add other specific codes if needed
                            else -> "Google Sign-In failed (${e.statusCode})"
                        }
                        _authState.value = AuthState.Error(errorMessage)
                    }
                    else -> {
                        Log.e("AuthViewModel", "Unexpected Google sign-in error: ${e.message}", e)
                        _authState.value = AuthState.Error("Google Sign-In failed: ${e.message}")
                    }
                }
            } finally { // Finally for L158 Outer try
                if (activeSignInJob?.isCancelled == true) {
                    Log.d("AuthViewModel", "Sign-in job was cancelled")
                }
                activeSignInJob = null // Ensure job reference is cleared
                Log.d("AuthViewModel", "Google sign-in process finished.")
            }
        } // End viewModelScope.launch
    } // End handleGoogleSignInResult function

    // --- All subsequent functions are now Class Members ---

    /**
     * Send app-specific verification email (Placeholder - requires backend/functions)
     */
    private suspend fun sendAppSpecificVerificationEmail(email: String) {
        Log.w("AuthViewModel", "sendAppSpecificVerificationEmail called but not implemented.")
        // Implementation would involve Dynamic Links and a backend/cloud function
        // For now, maybe just log or set a specific state if needed for testing flow
        try {
            val user = _currentUser.value ?: run {
                 Log.e("AuthViewModel", "Cannot send verification email, no current user.")
                 return
            }
            val verificationToken = user.verificationToken ?: UUID.randomUUID().toString()
            if (user.verificationToken.isNullOrEmpty()) {
                db.collection("users").document(user.id).update("verificationToken", verificationToken).await()
                 Log.d("AuthViewModel", "Generated verification token for user ${user.id}")
            }
             Log.d("AuthViewModel", "[Placeholder] Verification email would be sent to: $email with token: $verificationToken")
        } catch (e: Exception) {
             Log.e("AuthViewModel", "Error during placeholder verification email setup", e)
             _authState.value = AuthState.Error("Setup for verification email failed: ${e.message}")
        }
    }

    /**
     * Verify email with token (Placeholder - relies on token mechanism)
     */
    fun verifyEmail(token: String) {
         Log.w("AuthViewModel", "verifyEmail called but relies on unimplemented token sending.")
        viewModelScope.launch(viewModelJob) {
            try {
                val currentUser = _currentUser.value ?: run {
                    _authState.value = AuthState.Error("Verification failed: Not logged in")
                    return@launch
                }
                if (token.isBlank() || token != currentUser.verificationToken) {
                    _authState.value = AuthState.Error("Invalid or missing verification token")
                    return@launch
                }
                // Update Firestore
                db.collection("users").document(currentUser.id).update(mapOf(
                    "hasCompletedAppVerification" to true,
                    "isEmailVerified" to true, // Assume token verification implies email verification
                    "verificationToken" to null
                )).await()
                // Update local state
                val updatedUser = currentUser.copy(
                    hasCompletedAppVerification = true,
                    isEmailVerified = true,
                    verificationToken = null
                )
                _currentUser.value = updatedUser
                _authState.value = AuthState.Authenticated(updatedUser)
                _emailVerified.value = true
                 Log.d("AuthViewModel", "Email verification via token successful for user: ${updatedUser.id}")
            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Token verification failed", e)
                _authState.value = AuthState.Error("Verification failed: ${e.message}")
            }
        }
    }

    /**
     * Ensures user has an email in Firebase Auth and potentially Firestore.
     * Prioritizes Firebase Auth email. Updates Firebase Auth if only found locally.
     */
    suspend fun ensureUserEmail(): String? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            // 1. Check Firebase Auth directly
            var email = firebaseUser.email
            if (!email.isNullOrEmpty()) {
                 Log.d("AuthViewModel", "Email found in Firebase Auth: $email")
                return email
            }

            // 2. Check local ViewModel state (_currentUser)
            email = _currentUser.value?.email
            if (!email.isNullOrEmpty()) {
                 Log.d("AuthViewModel", "Email found in local state: $email. Updating Firebase Auth.")
                try {
                    firebaseUser.updateEmail(email).await() // Update Firebase Auth
                     Log.d("AuthViewModel", "Updated Firebase Auth email from local state.")
                    return email
                } catch (e: Exception) {
                     Log.e("AuthViewModel", "Failed to update Firebase Auth email from local state: ${e.message}")
                     // Proceed to check Firestore, maybe local state was stale but Firestore is correct
                }
            }

            // 3. Check Firestore as a last resort
             Log.d("AuthViewModel", "Email not in Auth or local state, checking Firestore.")
            try {
                val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    email = userDoc.getString("email")
                    if (!email.isNullOrEmpty()) {
                         Log.d("AuthViewModel", "Email found in Firestore: $email. Updating Firebase Auth.")
                        try {
                            firebaseUser.updateEmail(email).await() // Update Firebase Auth
                             Log.d("AuthViewModel", "Updated Firebase Auth email from Firestore.")
                            return email
                        } catch (e: Exception) {
                             Log.e("AuthViewModel", "Failed to update Firebase Auth email from Firestore: ${e.message}")
                            // If update fails, return the email found in Firestore anyway
                            return email
                        }
                    } else {
                         Log.w("AuthViewModel", "Email field empty or missing in Firestore for user ${firebaseUser.uid}")
                    }
                } else {
                     Log.w("AuthViewModel", "User document not found in Firestore for user ${firebaseUser.uid}")
                }
            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Error reading Firestore during email check: ${e.message}")
            }

             Log.w("AuthViewModel", "Could not determine user email for ${firebaseUser.uid}")
            return null // Email could not be found/retrieved

        } catch (e: Exception) {
             Log.e("AuthViewModel", "Unexpected error in ensureUserEmail: ${e.message}", e)
            return null
        }
    }


    /**
     * Check if the current user's email is verified in Firebase Auth.
     * Updates local state and Firestore if necessary.
     */
    fun checkEmailVerification() {
        viewModelScope.launch(viewModelJob) {
            val firebaseUser = auth.currentUser ?: run {
                 Log.d("AuthViewModel", "checkEmailVerification: No user logged in.")
                return@launch
            }
            try {
                 Log.d("AuthViewModel", "Checking email verification for ${firebaseUser.uid}")
                firebaseUser.reload().await() // Get latest status from Firebase
                val isVerified = firebaseUser.isEmailVerified
                 Log.d("AuthViewModel", "Firebase Auth email verified status: $isVerified")

                 // Check if this is a Google user (should always be treated as verified)
                val isGoogleUser = firebaseUser.providerData.any {
                    it.providerId == GoogleAuthProvider.PROVIDER_ID
                }
                val effectiveVerificationStatus = isGoogleUser || isVerified

                 Log.d("AuthViewModel", "Effective verification status (isGoogle: $isGoogleUser): $effectiveVerificationStatus")
                _emailVerified.value = effectiveVerificationStatus

                // Update Firestore if its state doesn't match the effective status
                try {
                    val userDocRef = db.collection("users").document(firebaseUser.uid)
                    val userDoc = userDocRef.get().await()
                    if (userDoc.exists()) {
                        val firestoreVerified = userDoc.getBoolean("isEmailVerified")
                        if (firestoreVerified != effectiveVerificationStatus) {
                             Log.d("AuthViewModel", "Updating Firestore verification status to $effectiveVerificationStatus")
                            userDocRef.update(mapOf(
                                "isEmailVerified" to effectiveVerificationStatus,
                                // Also mark app verification complete if email is effectively verified
                                "hasCompletedAppVerification" to effectiveVerificationStatus
                            )).await()

                            // Update local user state if it exists
                            _currentUser.value?.let {
                                val updatedUser = it.copy(
                                    isEmailVerified = effectiveVerificationStatus,
                                    hasCompletedAppVerification = effectiveVerificationStatus
                                )
                                _currentUser.value = updatedUser
                                // Update AuthState if user is currently authenticated
                                if (_authState.value is AuthState.Authenticated) {
                                     _authState.value = AuthState.Authenticated(updatedUser)
                                } else if (_authState.value is AuthState.NeedsEmailVerification && effectiveVerificationStatus) {
                                     _authState.value = AuthState.Authenticated(updatedUser) // Move to authenticated
                                }
                            }
                        }
                    } else {
                         Log.w("AuthViewModel", "User document not found in Firestore during verification check for ${firebaseUser.uid}")
                    }
                } catch (e: Exception) {
                     Log.e("AuthViewModel", "Error updating Firestore during verification check: ${e.message}", e)
                }

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Error during email verification check: ${e.message}", e)
                 // Optionally set an error state or keep the previous state
                 // _emailVerified.value = null // Indicate uncertainty?
            }
        }
    }

    /**
     * Manually update email verification status (e.g., for Google sign-in).
     */
    fun updateEmailVerificationStatus(isVerified: Boolean) {
         Log.d("AuthViewModel", "Manually updating email verification status to: $isVerified")
        _emailVerified.value = isVerified
        // Optionally update Firestore as well, though checkEmailVerification should handle sync
        viewModelScope.launch(viewModelJob) {
            auth.currentUser?.uid?.let { userId ->
                try {
                    db.collection("users").document(userId).update(mapOf(
                         "isEmailVerified" to isVerified,
                         "hasCompletedAppVerification" to isVerified // Assuming manual update implies app verification too
                     )).await()
                    _currentUser.value?.let { user ->
                         _currentUser.value = user.copy(isEmailVerified = isVerified, hasCompletedAppVerification = isVerified)
                     }
                } catch (e: Exception) {
                     Log.e("AuthViewModel", "Error updating Firestore from manual verification update: ${e.message}")
                }
            }
        }
    }

    /**
     * Resend Firebase Auth email verification link.
     */
    fun resendEmailVerification() {
        viewModelScope.launch(viewModelJob) {
            _authState.value = AuthState.Loading
            val firebaseUser = auth.currentUser ?: run {
                _authState.value = AuthState.Error("Cannot resend verification: Not logged in")
                return@launch
            }

            try {
                // Ensure we have an email address
                 val email = ensureUserEmail()
                 if (email.isNullOrEmpty()) {
                     _authState.value = AuthState.Error("Cannot send verification: Email address missing.")
                     return@launch
                 }

                 Log.d("AuthViewModel", "Attempting to resend verification email to: $email")
                 // Reload user first to check current status
                firebaseUser.reload().await()
                if (firebaseUser.isEmailVerified) {
                     Log.d("AuthViewModel", "User email is already verified.")
                     _emailVerified.value = true
                     // Ensure Firestore and local state reflect this
                     checkEmailVerification() // Run full check/update
                     // Might already be Authenticated, but ensure it
                     _currentUser.value?.let {
                         _authState.value = AuthState.Authenticated(it.copy(isEmailVerified = true, hasCompletedAppVerification = true))
                     } ?: run {
                          _authState.value = AuthState.Error("User verified but profile data missing.")
                     }
                    return@launch
                }

                 // Send the verification email
                firebaseUser.sendEmailVerification().await()
                 Log.d("AuthViewModel", "Verification email resent successfully to $email.")

                 // Update Firestore timestamp (optional but good practice)
                try {
                    db.collection("users").document(firebaseUser.uid).update(mapOf(
                        "verificationSentAt" to System.currentTimeMillis(),
                        "verificationAttempts" to FieldValue.increment(1)
                    )).await()
                } catch (e: Exception) {
                     Log.w("AuthViewModel", "Failed to update verification timestamp in Firestore: ${e.message}")
                }

                 _authState.value = AuthState.VerificationEmailSent

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Failed to resend verification email", e)
                 // Provide more specific error messages
                 val errorMessage = when {
                     e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check connection."
                     e.message?.contains("too many requests", ignoreCase = true) == true -> "Too many attempts. Please try again later."
                     // Add more specific Firebase Auth error checks if needed
                     else -> "Failed to resend email: ${e.message}"
                 }
                 _authState.value = AuthState.Error(errorMessage)
            }
        }
    }


    /**
     * Sign in with email and password.
     */
    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch(viewModelJob) {
            _authState.value = AuthState.Loading
            try {
                 Log.d("AuthViewModel", "Attempting sign in with email: $email")
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) { // Start L741 if
                     Log.d("AuthViewModel", "Firebase Auth successful for ${firebaseUser.uid}. Checking verification.")
                    firebaseUser.reload().await() // Get latest state

                    if (!firebaseUser.isEmailVerified) {
                         Log.d("AuthViewModel", "Email not verified for ${firebaseUser.uid}.")
                        // Fetch user data to pass to NeedsEmailVerification state
                        try {
                            val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                            val user = if (userDoc.exists()) userDoc.toObject(User::class.java) else null
                            val userForState = user ?: User(id = firebaseUser.uid, email = email) // Fallback

                            _currentUser.value = userForState // Update local user even if not verified
                            _authState.value = AuthState.NeedsEmailVerification(userForState)
                             Log.d("AuthViewModel", "Set state to NeedsEmailVerification.")
                            return@launch
                        } catch (e: Exception) {
                             Log.e("AuthViewModel", "Error fetching user data for NeedsEmailVerification state: ${e.message}")
                            _authState.value = AuthState.Error("Login error: Could not retrieve profile.")
                            return@launch
                        }
                    }

                     // Email is verified, proceed with full login
                     Log.d("AuthViewModel", "Email verified for ${firebaseUser.uid}. Fetching/updating profile.")
                    try {
                        val userDocRef = db.collection("users").document(firebaseUser.uid)
                        val userDoc = userDocRef.get().await()
                        var user: User?

                        if (userDoc.exists()) {
                            user = userDoc.toObject(User::class.java)
                            if (user != null) {
                                 Log.d("AuthViewModel", "User profile found in Firestore. Updating last sign-in.")
                                userDocRef.update("lastSignInTime", System.currentTimeMillis()).await()
                                user = user.copy(lastSignInTime = System.currentTimeMillis(), isEmailVerified = true)
                            } else {
                                 Log.e("AuthViewModel", "Failed to parse user document for ${firebaseUser.uid}")
                                _authState.value = AuthState.Error("Login error: Could not load profile.")
                                return@launch
                            }
                        } else {
                             Log.w("AuthViewModel", "User profile not found for verified user ${firebaseUser.uid}. Creating.")
                            user = User( // Create user object from Auth data
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: email, // Use provided email as fallback
                                fullName = firebaseUser.displayName ?: "",
                                isEmailVerified = true,
                                createdAt = System.currentTimeMillis(), // Approximated
                                lastSignInTime = System.currentTimeMillis()
                            )
                            userDocRef.set(user).await() // Create document in Firestore
                        }

                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                         Log.d("AuthViewModel", "User signed in successfully: ${user.email}")

                    } catch (e: Exception) {
                         Log.e("AuthViewModel", "Error accessing/updating Firestore during sign-in: ${e.message}", e)
                         _authState.value = AuthState.Error("Login error: Could not update profile.")
                    }

                } else { // Else for L741 if (firebaseUser != null)
                    Log.e("AuthViewModel", "Authentication failed: Firebase user is null after successful call.")
                    _authState.value = AuthState.Error("Authentication failed: Unknown error.")
                } // End else L741

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Sign in with email/password failed", e)
                 // Map common errors
                val errorMessage = when {
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true -> "Invalid email or password."
                    e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found with this email."
                    e.message?.contains("wrong-password", ignoreCase = true) == true -> "Incorrect password." // Might be covered by INVALID_LOGIN_CREDENTIALS
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check connection."
                    else -> "Authentication failed: ${e.message}"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }


    /**
     * Create a new user account with email/password. Sends verification email.
     * Sets state to NeedsEmailVerification.
     */
    fun createUserWithEmailPassword(email: String, password: String, fullName: String, phone: String) {
        viewModelScope.launch(viewModelJob) {
            _authState.value = AuthState.Loading
            try {
                 Log.d("AuthViewModel", "Attempting to create user with email: $email")
                 // 1. Create Firebase Auth user
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                     Log.d("AuthViewModel", "Firebase Auth user created: ${firebaseUser.uid}")

                     // 2. Send verification email (best effort)
                    try {
                        firebaseUser.sendEmailVerification().await()
                         Log.d("AuthViewModel", "Verification email sent to $email.")
                    } catch (e: Exception) {
                         Log.e("AuthViewModel", "Failed to send verification email during signup", e)
                         // Continue anyway, user can resend later
                    }

                     // 3. Update Firebase Auth Profile (Display Name)
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                         Log.d("AuthViewModel", "Updated Firebase Auth profile display name.")
                    } catch (e: Exception) {
                         Log.e("AuthViewModel", "Failed to update Firebase Auth profile name", e)
                    }
                    // Ensure email is set in Auth profile if somehow missing
                     if (firebaseUser.email.isNullOrEmpty()) {
                         try { firebaseUser.updateEmail(email).await() }
                         catch (e: Exception) { Log.e("AuthViewModel", "Failed to update Firebase Auth email post-creation", e) }
                     }


                     // 4. Create Firestore user document
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        phoneNumber = phone, // Store phone number
                        isEmailVerified = false, // Starts as not verified
                        createdAt = System.currentTimeMillis(),
                        lastSignInTime = System.currentTimeMillis(), // Set initial sign-in time
                        provider = "firebase", // Indicate email/password provider
                        verificationMethod = "firebase", // Indicate standard email verification
                        verificationSentAt = System.currentTimeMillis() // Record when email was sent
                    )
                    try {
                        db.collection("users").document(firebaseUser.uid).set(newUser).await()
                         Log.d("AuthViewModel", "User document created in Firestore for ${newUser.email}")

                         // 5. Update ViewModel state
                        _currentUser.value = newUser
                        _authState.value = AuthState.NeedsEmailVerification(newUser)
                        _emailVerified.value = false // Ensure verification state is false
                         Log.d("AuthViewModel", "Set state to NeedsEmailVerification for new user.")

                    } catch (e: Exception) {
                         Log.e("AuthViewModel", "Failed to create user document in Firestore", e)
                         // Critical failure - Auth user exists but profile doesn't.
                         // Maybe try deleting the Auth user or set a specific error state.
                        _authState.value = AuthState.Error("Failed to save user profile: ${e.message}")
                         // Consider deleting the auth user if Firestore fails critically:
                         // try { firebaseUser.delete().await() } catch (delErr: Exception) { Log.e("AuthViewModel", "Failed to clean up auth user after Firestore failure", delErr) }
                    }
                } else {
                    // This case should ideally not happen if createUserWithEmailAndPassword succeeds without exception
                     Log.e("AuthViewModel", "Firebase Auth user creation succeeded but user object is null.")
                    _authState.value = AuthState.Error("Account creation failed: Unknown error.")
                }
            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Create user with email/password failed", e)
                val errorMessage = when {
                    e.message?.contains("EMAIL_EXISTS", ignoreCase = true) == true || // Check for specific Firebase error codes
                    e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                        "Email address is already in use. Please sign in or use a different email."
                    e.message?.contains("WEAK_PASSWORD", ignoreCase = true) == true ->
                        "Password is too weak. Please use at least 6 characters."
                    e.message?.contains("invalid-email", ignoreCase = true) == true ->
                         "Invalid email address format."
                    e.message?.contains("network", ignoreCase = true) == true ->
                         "Network error. Please check connection."
                    else -> "Account creation failed: ${e.message}"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }


    /**
     * Sign out the current user from Firebase and Google.
     */
    fun signOut() {
        viewModelScope.launch(viewModelJob) {
             Log.d("AuthViewModel", "Signing out user...")
            try {
                // Sign out from Firebase Auth
                auth.signOut()

                // Sign out from Google Sign-In if initialized
                googleSignInClient?.signOut()?.await()
                 Log.d("AuthViewModel", "Google Sign-In client signed out.")

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Error during sign out: ${e.message}", e)
                 // Even if Google sign out fails, Firebase sign out likely succeeded.
                 // Proceed with resetting local state.
            } finally {
                 // Reset local state regardless of errors during sign out calls
                _bypassVerification.value = false
                _emailVerified.value = null
                _currentUser.value = null
                _authState.value = AuthState.Initial // Reset to initial state
                activeSignInJob?.cancel() // Cancel any ongoing auth job
                activeSignInJob = null
                 Log.d("AuthViewModel", "Local state cleared after sign out.")
            }
        }
    }

    /**
     * Send a password reset email to the user.
     */
    fun resetPassword(email: String) {
        viewModelScope.launch(viewModelJob) {
            if (email.isBlank()) {
                 _authState.value = AuthState.Error("Please enter your email address.")
                 return@launch
            }
            _authState.value = AuthState.Loading
            try {
                 Log.d("AuthViewModel", "Sending password reset email to: $email")
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetSent
                 Log.d("AuthViewModel", "Password reset email sent successfully.")
            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Password reset failed", e)
                 val errorMessage = when {
                     e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found with this email address."
                     e.message?.contains("invalid-email", ignoreCase = true) == true -> "Invalid email address format."
                     e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check connection."
                     else -> "Password reset failed: ${e.message}"
                 }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    /**
     * Update user profile information in Firestore.
     */
    fun updateUserProfile(
        fullName: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(viewModelJob) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                onError("Update failed: Not logged in.")
                return@launch
            }

            try {
                 Log.d("AuthViewModel", "Updating profile for user: $userId")
                val userDocRef = db.collection("users").document(userId)
                val updates = mutableMapOf<String, Any>()
                // Basic validation
                 if (fullName.isNotBlank()) {
                     updates["fullName"] = fullName
                     // Also update displayName if storing separately
                     updates["displayName"] = fullName
                 } else {
                     // Optional: Handle blank name error or ignore
                 }
                 // Basic phone validation (example: check if not blank)
                 if (phoneNumber.isNotBlank()) {
                     updates["phoneNumber"] = phoneNumber
                 } else {
                      // Optional: Handle blank phone error or ignore
                 }

                 if (updates.isEmpty()) {
                     Log.w("AuthViewModel", "Update profile called with no changes.")
                     onSuccess() // Or onError("No changes submitted")
                     return@launch
                 }

                userDocRef.update(updates).await()

                // Update Firebase Auth display name if changed
                 if (updates.containsKey("fullName")) {
                     try {
                         val profileUpdates = UserProfileChangeRequest.Builder()
                             .setDisplayName(updates["fullName"] as String)
                             .build()
                         auth.currentUser?.updateProfile(profileUpdates)?.await()
                     } catch (e: Exception) {
                          Log.e("AuthViewModel", "Failed to update Firebase Auth display name", e)
                          // Non-critical, proceed
                     }
                 }

                // Update local state
                _currentUser.value?.let { currentUser ->
                    _currentUser.value = currentUser.copy(
                        fullName = updates["fullName"] as? String ?: currentUser.fullName,
                        displayName = updates["displayName"] as? String ?: currentUser.displayName,
                        phoneNumber = updates["phoneNumber"] as? String ?: currentUser.phoneNumber
                    )
                }
                 Log.d("AuthViewModel", "User profile updated successfully in Firestore.")
                onSuccess()

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Update profile failed", e)
                onError("Failed to update profile: ${e.message}")
            }
        }
    }

    /**
     * Change the user's password (for email/password auth only).
     */
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(viewModelJob) {
            val currentUser = auth.currentUser
            val userEmail = currentUser?.email

            if (currentUser == null || userEmail.isNullOrBlank()) {
                onError("Cannot change password: User not logged in or email missing.")
                return@launch
            }

            // Check if user signed in with email/password
            val isEmailPasswordUser = currentUser.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }

            if (!isEmailPasswordUser) {
                onError("Password change is only available for accounts created with email/password.")
                return@launch
            }

             if (currentPassword.isBlank() || newPassword.isBlank()) {
                 onError("Passwords cannot be empty.")
                 return@launch
             }
             if (newPassword.length < 6) {
                 onError("New password must be at least 6 characters long.")
                 return@launch
             }


            try {
                 Log.d("AuthViewModel", "Attempting to change password for $userEmail")
                 // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(userEmail, currentPassword)
                currentUser.reauthenticate(credential).await()
                 Log.d("AuthViewModel", "User re-authenticated successfully.")

                 // Change password
                currentUser.updatePassword(newPassword).await()
                 Log.d("AuthViewModel", "Password changed successfully in Firebase Auth.")
                onSuccess()

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Password change failed", e)
                 val errorMessage = when {
                     e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true -> "Incorrect current password."
                     e.message?.contains("wrong-password", ignoreCase = true) == true -> "Incorrect current password." // Redundant?
                     e.message?.contains("WEAK_PASSWORD", ignoreCase = true) == true -> "New password is too weak (minimum 6 characters)."
                     e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check connection."
                     else -> "Failed to change password: ${e.message}"
                 }
                onError(errorMessage)
            }
        }
    }

    /**
     * Delete the user's account from Firebase Auth and Firestore.
     */
    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(viewModelJob) {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                onError("Cannot delete account: Not logged in.")
                return@launch
            }
            val userId = currentUser.uid
            _authState.value = AuthState.Loading // Indicate process start

            try {
                 Log.d("AuthViewModel", "Starting account deletion for user: $userId")
                 // 1. Delete Firestore data (best effort)
                try {
                    db.collection("users").document(userId).delete().await()
                     Log.d("AuthViewModel", "User document deleted from Firestore.")
                     // Add deletion logic for other user-related collections if needed
                } catch (e: Exception) {
                     Log.e("AuthViewModel", "Failed to delete Firestore data for user $userId during account deletion (will proceed with Auth deletion)", e)
                     // Log this error but proceed with Auth deletion as it's more critical
                }

                 // 2. Delete Firebase Auth account
                 Log.d("AuthViewModel", "Deleting Firebase Auth account for user: $userId")
                currentUser.delete().await()
                 Log.d("AuthViewModel", "Firebase Auth account deleted successfully.")

                 // 3. Clear local state and call success callback
                signOut() // Use signOut to clear all local state consistently
                onSuccess()

            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Account deletion failed", e)
                 val errorMessage = when {
                     e.message?.contains("requires recent login", ignoreCase = true) == true -> "Deletion requires recent login. Please sign out and sign back in."
                     // Add other specific error checks if needed
                     else -> "Failed to delete account: ${e.message}"
                 }
                 _authState.value = AuthState.Error(errorMessage) // Set error state
                onError(errorMessage)
            }
        }
    }

    /**
     * Sign up with email/password (wrapper for createUserWithEmailPassword).
     * Kept for potential compatibility if called elsewhere.
     */
    fun signUpWithEmailPassword(email: String, password: String, fullName: String, phoneNumber: String) {
         Log.d("AuthViewModel", "signUpWithEmailPassword called, forwarding to createUserWithEmailPassword")
        createUserWithEmailPassword(email, password, fullName, phoneNumber)
    }

    /**
     * Set whether to bypass email verification (e.g., for testing).
     */
    fun setBypassVerification(bypass: Boolean) {
        _bypassVerification.value = bypass
         Log.d("AuthViewModel", "Email verification bypass set to: $bypass")
    }

    /**
     * Refresh current user data from Firestore. Updates _currentUser.
     */
    private suspend fun refreshCurrentUser() {
        // Use a try-catch block for safety
        try {
            val firebaseUser = auth.currentUser ?: return // Exit if no auth user
            val userId = firebaseUser.uid
             Log.d("AuthViewModel", "Refreshing current user data from Firestore for $userId")
            val userDoc = db.collection("users").document(userId).get().await()

            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    _currentUser.value = user // Update local state
                     Log.d("AuthViewModel", "Current user refreshed successfully.")
                } else {
                     Log.e("AuthViewModel", "Failed to parse user document during refresh for $userId")
                }
            } else {
                 Log.w("AuthViewModel", "No user document found in Firestore during refresh for $userId")
                 // Maybe sign out or clear local state if Firestore doc is missing?
                 // _currentUser.value = null
            }
        } catch (e: Exception) {
             Log.e("AuthViewModel", "Error refreshing current user from Firestore", e)
        }
    }

    /**
     * Firebase authentication using a Google ID token (e.g., from one-tap sign-in).
     * This is similar to handleGoogleSignInResult but takes only the token.
     */
    fun firebaseAuthWithGoogle(idToken: String) {
        activeSignInJob?.cancel()
        activeSignInJob = viewModelScope.launch(viewModelJob) {
            _authState.value = AuthState.Loading
            try {
                 Log.d("AuthViewModel", "Attempting Firebase authentication with Google ID token.")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                     Log.d("AuthViewModel", "Firebase Auth with Google token successful for ${user.uid}. Fetching/updating profile.")
                    _emailVerified.value = true // Google users are verified

                    // Fetch/Update Firestore Document
                    try {
                        val userDocRef = db.collection("users").document(user.uid)
                        val userDoc = userDocRef.get().await()
                        val appUser: User

                        if (userDoc.exists()) {
                             Log.d("AuthViewModel", "Existing user found in Firestore for direct Google auth.")
                            val existingUser = userDoc.toObject(User::class.java)
                            if (existingUser != null) {
                                // Update last sign-in, provider, verification status
                                val updates = mapOf(
                                    "lastSignInTime" to System.currentTimeMillis(),
                                    "provider" to "google",
                                    "isEmailVerified" to true,
                                    "hasCompletedAppVerification" to true
                                )
                                userDocRef.update(updates).await()
                                appUser = existingUser.copy(
                                    lastSignInTime = System.currentTimeMillis(),
                                    provider = "google",
                                    isEmailVerified = true,
                                    hasCompletedAppVerification = true
                                )
                            } else {
                                 Log.e("AuthViewModel", "Failed to parse existing user document during direct Google auth.")
                                // Fallback: create a basic user object from Auth info
                                appUser = User(
                                    id = user.uid, email = user.email ?: "", fullName = user.displayName ?: "",
                                    provider = "google", isEmailVerified = true, hasCompletedAppVerification = true
                                )
                            }
                        } else {
                             Log.w("AuthViewModel", "User document not found during direct Google auth for ${user.uid}. Creating.")
                             // Create new user document
                            appUser = User(
                                id = user.uid, email = user.email ?: "", fullName = user.displayName ?: "",
                                provider = "google", isEmailVerified = true, hasCompletedAppVerification = true,
                                createdAt = System.currentTimeMillis(), lastSignInTime = System.currentTimeMillis()
                            )
                            userDocRef.set(appUser).await()
                        }
                         _currentUser.value = appUser
                         _authState.value = AuthState.Authenticated(appUser)
                         Log.d("AuthViewModel", "Direct Google authentication successful.")

                    } catch (e: Exception) {
                         Log.e("AuthViewModel", "Error accessing/updating Firestore during direct Google auth: ${e.message}", e)
                         // Fallback: Authenticate with Auth data only
                         val fallbackUser = User(
                            id = user.uid, email = user.email ?: "", fullName = user.displayName ?: "",
                            provider = "google", isEmailVerified = true, hasCompletedAppVerification = true
                         )
                         _currentUser.value = fallbackUser
                         _authState.value = AuthState.Authenticated(fallbackUser)
                         Log.w("AuthViewModel", "Authenticated direct Google user with fallback data due to Firestore error.")
                    }
                } else {
                    Log.e("AuthViewModel", "Firebase user is null after direct Google authentication.")
                    _authState.value = AuthState.Error("Google Authentication failed: User data unavailable.")
                }
            } catch (e: Exception) {
                 Log.e("AuthViewModel", "Direct Google authentication failed", e)
                _authState.value = AuthState.Error("Google Authentication failed: ${e.message}")
            } finally {
                activeSignInJob = null
            }
        }
    }

    // onCleared is now a direct member of the class
    override fun onCleared() {
        super.onCleared()
         Log.d("AuthViewModel", "onCleared called, cancelling viewModelJob.")
        viewModelJob.cancel() // Cancel coroutines started with this job
    }

} // End of AuthViewModel class
