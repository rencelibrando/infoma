package com.example.bikerental.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.AuthState
import com.example.bikerental.models.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import com.example.bikerental.utils.LogManager
import com.example.bikerental.utils.logD
import com.example.bikerental.utils.logE
import com.example.bikerental.utils.logW
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bikerental.utils.PerformanceMonitor
import com.example.bikerental.utils.NetworkUtils
import kotlinx.coroutines.NonCancellable
import com.google.firebase.auth.ktx.actionCodeSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ActionCodeSettings

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
        // Check current auth state immediately without heavy operations
        val currentFirebaseUser = auth.currentUser
        logD("AuthViewModel init - Firebase user: ${currentFirebaseUser?.uid}")
        
        if (currentFirebaseUser == null) {
            _authState.value = AuthState.Initial
            logD("Initialized with no logged in user - setting state to Initial")
        } else {
            // Set loading state briefly, then immediately start background check
            _authState.value = AuthState.Loading
            logD("Found existing user (${currentFirebaseUser.uid}), checking authentication status...")
            
            // Start performance tracking for auth initialization
            PerformanceMonitor.startTiming("auth_initialization")
            
            // Prefetch user from local storage to use as fallback
            val userId = currentFirebaseUser.uid
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Try to load user from local storage first
                    val cachedUser = loadUserFromCache(userId)
                    if (cachedUser != null) {
                        // Update UI with cached user data while we load from network
                        withContext(Dispatchers.Main) {
                            _currentUser.value = cachedUser
                            _authState.value = AuthState.Authenticated(cachedUser)
                            logD("Using cached user data while refreshing from network")
                        }
                    }
                    
                    // Attempt to refresh user data from network with longer timeout
                    kotlinx.coroutines.withTimeout(10000) { // 10 second timeout
                        initializeExistingUser(currentFirebaseUser)
                    }
                    PerformanceMonitor.endTiming("auth_initialization")
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    logW("Authentication initialization timed out after 10s")
                    PerformanceMonitor.endTiming("auth_initialization")
                    
                    // Don't reset to Initial state on timeout if we have cached data
                    withContext(Dispatchers.Main) {
                        if (_currentUser.value == null && _authState.value != AuthState.Authenticated(null)) {
                            // Only reset to Initial if we couldn't load from cache
                            _authState.value = AuthState.Initial
                            logE("Setting auth state to Initial due to timeout and no cached data")
                        } else {
                            logW("Auth refresh timed out, but using cached user data")
                        }
                    }
                } catch (e: Exception) {
                    logE("Error during user initialization", e)
                    // Similar fallback logic for other exceptions
                    withContext(Dispatchers.Main) {
                        if (_currentUser.value == null && _authState.value != AuthState.Authenticated(null)) {
                            _authState.value = AuthState.Initial
                            logE("Setting auth state to Initial due to error: ${e.message}")
                        }
                    }
                    PerformanceMonitor.endTiming("auth_initialization")
                }
            }
        }
    }
    
    private suspend fun initializeExistingUser(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        try {
            logD("initializeExistingUser: Starting for user ${firebaseUser.uid}")
            
            // Quick reload with a single retry for faster startup
            try {
                logD("initializeExistingUser: Reloading Firebase user...")
                NetworkUtils.withRetry(
                    maxAttempts = 2,
                    initialDelayMs = 500,
                    maxDelayMs = 1000
                ) {
                    firebaseUser.reload().await()
                }
                logD("initializeExistingUser: Firebase user reloaded successfully")
            } catch (e: Exception) {
                logW("Failed to reload user, continuing with cached data: ${e.message}")
            }
            
            // Check if user document exists
            logD("initializeExistingUser: Fetching user document from Firestore...")
            val userDoc = try {
                NetworkUtils.withRetry(
                    maxAttempts = 2,
                    initialDelayMs = 500,
                    maxDelayMs = 1000
                ) {
                    db.collection("users").document(firebaseUser.uid).get().await()
                }
            } catch (e: Exception) {
                logW("Failed to fetch user document, checking local cache: ${e.message}")
                
                // Try fetching from cache as fallback
                try {
                    db.collection("users").document(firebaseUser.uid)
                        .get(com.google.firebase.firestore.Source.CACHE).await()
                } catch (innerE: Exception) {
                    logE("Failed to fetch from cache too: ${innerE.message}")
                    null
                }
            }
            
            if (userDoc != null && userDoc.exists()) {
                logD("initializeExistingUser: User document exists: ${userDoc.exists()}")
                val user = userDoc.toObject(User::class.java)
                logD("initializeExistingUser: Parsed user data: ${user?.id}")
                
                if (user != null) {
                    withContext(Dispatchers.Main) {
                        _currentUser.value = user
                        
                        // Simplified email verification check
                        val isEmailVerified = firebaseUser.isEmailVerified || 
                                            firebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                        
                        logD("initializeExistingUser: Email verified - Firebase: ${firebaseUser.isEmailVerified}, Google: ${firebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }}, User doc: ${user.isEmailVerified}")
                        
                        if (isEmailVerified || user.isEmailVerified) {
                            _authState.value = AuthState.Authenticated(user)
                            _emailVerified.value = true
                            logD("User authenticated successfully: ${user.id}")
                        } else {
                            _authState.value = AuthState.NeedsEmailVerification(user)
                            _emailVerified.value = false
                            logD("User needs email verification: ${user.id}")
                        }
                    }
                } else {
                    logE("Failed to parse existing user document: ${firebaseUser.uid}")
                    withContext(Dispatchers.Main) {
                        _authState.value = AuthState.Initial
                        logE("Setting auth state to Initial - failed to parse user document")
                    }
                }
            } else {
                logD("initializeExistingUser: User document doesn't exist, creating new one...")
                
                // Create user document quickly without blocking
                val newUser = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified,
                    createdAt = System.currentTimeMillis()
                )

                logD("initializeExistingUser: Created new user object: ${newUser.id}")

                // Save to Firestore in background (non-blocking but must complete)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        withContext(NonCancellable) {
                            NetworkUtils.withRetry {
                                db.collection("users").document(firebaseUser.uid).set(newUser).await()
                            }
                        }
                        logD("Created new user document in Firestore: ${newUser.id}")
                    } catch (e: Exception) {
                        logE("Failed to create user document: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    _currentUser.value = newUser
                    if (firebaseUser.isEmailVerified) {
                        _authState.value = AuthState.Authenticated(newUser)
                        _emailVerified.value = true
                        logD("New user authenticated: ${newUser.id}")
                    } else {
                        _authState.value = AuthState.NeedsEmailVerification(newUser)
                        _emailVerified.value = false
                        logD("New user needs email verification: ${newUser.id}")
                    }
                }
            }
        } catch (e: Exception) {
            logE("Error during user initialization", e)
            withContext(Dispatchers.Main) {
                // Only reset auth state if we don't have a currentUser already set
                if (_currentUser.value == null) {
                    _authState.value = AuthState.Initial
                    logE("Setting auth state to Initial due to error in initializeExistingUser: ${e.message}")
                } else {
                    logW("Keeping existing user state despite initialization error")
                }
            }
        }
    }

    /**
     * Loads user data from local cache if available
     */
    private suspend fun loadUserFromCache(userId: String): User? {
        return try {
            // Check if we have a locally cached user document
            val userDoc = db.collection("users").document(userId).get(
                com.google.firebase.firestore.Source.CACHE
            ).await()
            
            if (userDoc.exists()) {
                logD("Successfully loaded user from cache: $userId")
                userDoc.toObject(User::class.java)
            } else {
                logD("No cached user data found for: $userId")
                null
            }
        } catch (e: Exception) {
            logE("Failed to load user from cache: ${e.message}")
            null
        }
    }

    /**
     * Reloads the current user from Firebase and checks their email verification status.
     * This updates the internal state and returns the current verification status.
     *
     * @return `true` if the user's email is verified, `false` otherwise.
     */
    suspend fun checkEmailVerificationStatus(): Boolean {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            logD("checkEmailVerificationStatus: No user logged in.")
            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Initial
                _emailVerified.value = null
            }
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                firebaseUser.reload().await()
                val isVerified = firebaseUser.isEmailVerified
                logD("checkEmailVerificationStatus: User reloaded. Verified: $isVerified")
                
                withContext(Dispatchers.Main) {
                    _emailVerified.value = isVerified
                    if (isVerified && _authState.value !is AuthState.Authenticated) {
                        // If verified, ensure the state is Authenticated
                        checkEmailVerification()
                    }
                }
                isVerified
            }
        } catch (e: Exception) {
            logE("checkEmailVerificationStatus: Failed to reload user", e)
            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Error("Failed to check status: ${e.message}")
            }
            false
        }
    }

    // --- All subsequent functions are now Class Members ---

    /**
     * Send app-specific verification email (Placeholder - requires backend/functions)
     */
    private suspend fun sendAppSpecificVerificationEmail(user: User) {
        viewModelScope.launch {
            try {
                logW("sendAppSpecificVerificationEmail called but not implemented.")
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    logE("Cannot send verification email, no current user.")
                    return@launch
                }
                
                // Generate a verification token (this is just a placeholder)
                val verificationToken = UUID.randomUUID().toString()
                logD("Generated verification token for user ${user.id}")
                
                val email = user.email ?: firebaseUser.email ?: return@launch
                logD("[Placeholder] Verification email would be sent to: $email with token: $verificationToken")
            } catch (e: Exception) {
                logE("Error during placeholder verification email setup", e)
            }
        }
    }

    /**
     * Verify email with token (Placeholder - relies on token mechanism)
     */
    fun verifyEmail(token: String) {
         logW("verifyEmail called but relies on unimplemented token sending.")
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
                 logD("Email verification via token successful for user: ${updatedUser.id}")
            } catch (e: Exception) {
                 logE("Token verification failed", e)
                _authState.value = AuthState.Error("Verification failed: ${e.message}")
            }
        }
    }

    /**
     * Verify email with the oobCode from a deep link.
     */
    fun verifyEmailWithCode(code: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                logD("Attempting to verify email with action code.")
                auth.applyActionCode(code).await()
                logD("Email verification successful with action code.")

                // Re-initialize user state to reflect verified email
                auth.currentUser?.let {
                    initializeExistingUser(it)
                }
                onComplete(true, "Email verified successfully! You can now log in.")
            } catch (e: Exception) {
                logE("Email verification failed", e)
                val message = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException,
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "The request is invalid. Please try signing up again."
                    is com.google.firebase.auth.FirebaseAuthActionCodeException -> "Your request to verify your email has expired or the link has already been used. Please try again."
                    else -> "An unknown error occurred: ${e.localizedMessage}"
                }
                onComplete(false, message)
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
                 logD("Email found in Firebase Auth: $email")
                return email
            }

            // 2. Check local ViewModel state (_currentUser)
            email = _currentUser.value?.email
            if (!email.isNullOrEmpty()) {
                 logD("Email found in local state: $email. Updating Firebase Auth.")
                try {
                    firebaseUser.updateEmail(email).await() // Update Firebase Auth
                     logD("Updated Firebase Auth email from local state.")
                    return email
                } catch (e: Exception) {
                     logE("Failed to update Firebase Auth email from local state: ${e.message}")
                     // Proceed to check Firestore, maybe local state was stale but Firestore is correct
                }
            }

            // 3. Check Firestore as a last resort
             logD("Email not in Auth or local state, checking Firestore.")
            try {
                val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                if (userDoc.exists()) {
                    email = userDoc.getString("email")
                    if (!email.isNullOrEmpty()) {
                         logD("Email found in Firestore: $email. Updating Firebase Auth.")
                        try {
                            firebaseUser.updateEmail(email).await() // Update Firebase Auth
                             logD("Updated Firebase Auth email from Firestore.")
                            return email
                        } catch (e: Exception) {
                             logE("Failed to update Firebase Auth email from Firestore: ${e.message}")
                            // If update fails, return the email found in Firestore anyway
                            return email
                        }
                    } else {
                         logW("Email field empty or missing in Firestore for user ${firebaseUser.uid}")
                    }
                } else {
                     logW("User document not found in Firestore for user ${firebaseUser.uid}")
                }
            } catch (e: Exception) {
                 logE("Error reading Firestore during email check: ${e.message}")
            }

             logW("Could not determine user email for ${firebaseUser.uid}")
            return null // Email could not be found/retrieved

        } catch (e: Exception) {
             logE("Unexpected error in ensureUserEmail: ${e.message}", e)
            return null
        }
    }


    /**
     * Check if the current user's email is verified in Firebase Auth.
     * Updates local state and Firestore if necessary.
     */
    fun checkEmailVerification() {
        viewModelScope.launch(Dispatchers.IO) {
            val firebaseUser = auth.currentUser ?: run {
                logD("checkEmailVerification: No user logged in.")
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Initial
                }
                return@launch
            }
            
            try {
                logD("Checking email verification for ${firebaseUser.uid}")
                
                // Quick verification check without reloading for better performance
                val isEmailVerified = firebaseUser.isEmailVerified
                val isGoogleUser = firebaseUser.providerData.any {
                    it.providerId == GoogleAuthProvider.PROVIDER_ID
                }
                val effectiveVerificationStatus = isGoogleUser || isEmailVerified

                logD("Email verification status: Firebase=$isEmailVerified, Google=$isGoogleUser, Effective=$effectiveVerificationStatus")

                // Get our stored user
                val currentUser = _currentUser.value
                
                if (currentUser != null) {
                    // Only update if status actually changed to reduce Firestore writes
                    if (currentUser.isEmailVerified != effectiveVerificationStatus) {
                        try {
                            // Update Firestore with new status
                            db.collection("users")
                                .document(firebaseUser.uid)
                                .update("isEmailVerified", effectiveVerificationStatus)
                                .await()
                            
                            logD("Updated email verification status in Firestore: $effectiveVerificationStatus")
                        } catch (e: Exception) {
                            logE("Failed to update email verification status in Firestore", e)
                            // Continue with local update even if Firestore fails
                        }
                        
                        // Update local state
                        val updatedUser = currentUser.copy(isEmailVerified = effectiveVerificationStatus)
                        withContext(Dispatchers.Main) {
                            _currentUser.value = updatedUser
                            _emailVerified.value = effectiveVerificationStatus
                            
                            if (effectiveVerificationStatus) {
                                _authState.value = AuthState.Authenticated(updatedUser)
                            } else {
                                _authState.value = AuthState.NeedsEmailVerification(updatedUser)
                            }
                        }
                    } else {
                        // Status didn't change, just ensure local state is consistent
                        withContext(Dispatchers.Main) {
                            _emailVerified.value = effectiveVerificationStatus
                            if (effectiveVerificationStatus) {
                                _authState.value = AuthState.Authenticated(currentUser)
                            } else {
                                _authState.value = AuthState.NeedsEmailVerification(currentUser)
                            }
                        }
                    }
                } else {
                    logW("Current user is null, but Firebase user exists. Reloading user data...")
                    // Fallback: reload user data
                    try {
                        val userDoc = db.collection("users")
                            .document(firebaseUser.uid).get().await()
                        
                        if (userDoc.exists()) {
                            val user = userDoc.toObject(User::class.java)
                            if (user != null) {
                                withContext(Dispatchers.Main) {
                                    _currentUser.value = user
                                    _emailVerified.value = effectiveVerificationStatus
                                    if (effectiveVerificationStatus || user.isEmailVerified) {
                                        _authState.value = AuthState.Authenticated(user)
                                    } else {
                                        _authState.value = AuthState.NeedsEmailVerification(user)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logE("Failed to reload user data", e)
                        withContext(Dispatchers.Main) {
                            _authState.value = AuthState.Error("Failed to load user profile")
                        }
                    }
                }
            } catch (e: Exception) {
                logE("Error checking email verification", e)
            }
        }
    }

    /**
     * Manually update email verification status (e.g., for Google sign-in).
     */
    fun updateEmailVerificationStatus(isVerified: Boolean) {
        logD("Manually updating email verification status to: $isVerified")
        _emailVerified.value = isVerified
        
        viewModelScope.launch(Dispatchers.IO) {
            auth.currentUser?.uid?.let { userId ->
                try {
                    db.collection("users").document(userId).update(mapOf(
                        "isEmailVerified" to isVerified,
                        "hasCompletedAppVerification" to isVerified
                    )).await()
                    
                    // Update user safely with null check
                    _currentUser.value?.let { user ->
                        withContext(Dispatchers.Main) {
                            _currentUser.value = user.copy(
                                isEmailVerified = isVerified,
                                hasCompletedAppVerification = isVerified
                            )
                        }
                    }
                } catch (e: Exception) {
                    logE("Error updating verification: ${e.message}")
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

                logD("Attempting to resend verification email to: $email")
                // Reload user first to check current status
                firebaseUser.reload().await()
                if (firebaseUser.isEmailVerified) {
                    logD("User email is already verified.")
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

                val actionCodeSettings = getActionCodeSettings(email)

                // Send the verification email
                firebaseUser.sendEmailVerification(actionCodeSettings).await()
                logD("Verification email resent successfully to $email.")

                // Update Firestore timestamp (optional but good practice)
                try {
                    db.collection("users").document(firebaseUser.uid).update(mapOf(
                        "verificationSentAt" to System.currentTimeMillis(),
                        "verificationAttempts" to FieldValue.increment(1)
                    )).await()
                } catch (e: Exception) {
                     logW("Failed to update verification timestamp in Firestore: ${e.message}")
                }

                 _authState.value = AuthState.VerificationEmailSent

            } catch (e: Exception) {
                 logE("Failed to resend verification email", e)
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
     * Creates and configures the ActionCodeSettings for email verification links.
     */
    private fun getActionCodeSettings(email: String): ActionCodeSettings {
        return actionCodeSettings {
            // URL you want to redirect back to. The domain must be authorized in the Firebase Console.
            url = "https://bike-rental-bc5bd.firebaseapp.com/verify?email=$email"
            // This must be true to be handled by the app.
            handleCodeInApp = true
            setAndroidPackageName(
                "com.example.bikerental",
                true, /* installIfNotAvailable */
                "1"   /* minimumVersion */
            )
            // Optional: iOS bundle ID
            // setIOSBundleId("com.example.bikerental.ios")
        }
    }

    /**
     * Sign in with email and password.
     */
    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch(viewModelJob) {
            _authState.value = AuthState.Loading
            try {
                 logD("Attempting sign in with email: $email")
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) { // Start L741 if
                     logD("Firebase Auth successful for ${firebaseUser.uid}. Checking verification.")
                    firebaseUser.reload().await() // Get latest state

                    if (!firebaseUser.isEmailVerified) {
                         logD("Email not verified for ${firebaseUser.uid}.")
                        // Fetch user data to pass to NeedsEmailVerification state
                        try {
                            val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                            val user = if (userDoc.exists()) userDoc.toObject(User::class.java) else null
                            val userForState = user ?: User(id = firebaseUser.uid, email = email) // Fallback

                            _currentUser.value = userForState // Update local user even if not verified
                            _authState.value = AuthState.NeedsEmailVerification(userForState)
                             logD("Set state to NeedsEmailVerification.")
                            return@launch
                        } catch (e: Exception) {
                             logE("Error fetching user data for NeedsEmailVerification state: ${e.message}")
                            _authState.value = AuthState.Error("Login error: Could not retrieve profile.")
                            return@launch
                        }
                    }

                     // Email is verified, proceed with full login
                     logD("Email verified for ${firebaseUser.uid}. Fetching/updating profile.")
                    try {
                        val userDocRef = db.collection("users").document(firebaseUser.uid)
                        val userDoc = userDocRef.get().await()
                        var user: User?

                        if (userDoc.exists()) {
                            user = userDoc.toObject(User::class.java)
                            if (user != null) {
                                 logD("User profile found in Firestore. Updating last sign-in.")
                                userDocRef.update(
                                    mapOf(
                                        "lastSignInTime" to System.currentTimeMillis(),
                                        "lastUpdated" to com.google.firebase.Timestamp.now()
                                    )
                                ).await()
                                user = user.copy(lastSignInTime = System.currentTimeMillis(), isEmailVerified = true)
                            } else {
                                 logE("Failed to parse user document for ${firebaseUser.uid}")
                                _authState.value = AuthState.Error("Login error: Could not load profile.")
                                return@launch
                            }
                        } else {
                             logW("User profile not found for verified user ${firebaseUser.uid}. Creating.")
                            user = User( // Create user object from Auth data
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: email, // Use provided email as fallback
                                fullName = firebaseUser.displayName ?: "",
                                isEmailVerified = true,
                                createdAt = System.currentTimeMillis(), // Approximated
                                lastSignInTime = System.currentTimeMillis(),
                                lastUpdated = com.google.firebase.Timestamp.now()
                            )
                            userDocRef.set(user).await() // Create document in Firestore
                        }

                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                         logD("User signed in successfully: ${user.email}")

                    } catch (e: Exception) {
                         logE("Error accessing/updating Firestore during sign-in: ${e.message}", e)
                         _authState.value = AuthState.Error("Login error: Could not update profile.")
                    }

                } else { // Else for L741 if (firebaseUser != null)
                    logE("Authentication failed: Firebase user is null after successful call.")
                    _authState.value = AuthState.Error("Authentication failed: Unknown error.")
                } // End else L741

            } catch (e: Exception) {
                 logE("Sign in with email/password failed", e)
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
                 logD("Attempting to create user with email: $email")
                 // 1. Create Firebase Auth user
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                     logD("Firebase Auth user created: ${firebaseUser.uid}")

                     // 2. Send verification email (best effort)
                    try {
                        val actionCodeSettings = getActionCodeSettings(email)
                        firebaseUser.sendEmailVerification(actionCodeSettings).await()
                        logD("Verification email sent to $email.")
                    } catch (e: Exception) {
                        logE("Failed to send verification email during signup", e)
                        // Continue anyway, user can resend later
                    }

                     // 3. Update Firebase Auth Profile (Display Name)
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                         logD("Updated Firebase Auth profile display name.")
                    } catch (e: Exception) {
                         logE("Failed to update Firebase Auth profile name", e)
                    }
                    // Ensure email is set in Auth profile if somehow missing
                     if (firebaseUser.email.isNullOrEmpty()) {
                         try { firebaseUser.updateEmail(email).await() }
                         catch (e: Exception) { logE("Failed to update Firebase Auth email post-creation", e) }
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
                        verificationSentAt = System.currentTimeMillis(), // Record when email was sent
                        lastUpdated = com.google.firebase.Timestamp.now()
                    )
                    try {
                        db.collection("users").document(firebaseUser.uid).set(newUser).await()
                         logD("User document created in Firestore for ${newUser.email}")

                         // 5. Update ViewModel state
                        _currentUser.value = newUser
                        _authState.value = AuthState.NeedsEmailVerification(newUser)
                        _emailVerified.value = false // Ensure verification state is false
                         logD("Set state to NeedsEmailVerification for new user.")

                    } catch (e: Exception) {
                         logE("Failed to create user document in Firestore", e)
                         // Critical failure - Auth user exists but profile doesn't.
                         // Maybe try deleting the Auth user or set a specific error state.
                        _authState.value = AuthState.Error("Failed to save user profile: ${e.message}")
                         // Consider deleting the auth user if Firestore fails critically:
                         // try { firebaseUser.delete().await() } catch (delErr: Exception) { logE("AuthViewModel", "Failed to clean up auth user after Firestore failure", delErr) }
                    }
                } else {
                    // This case should ideally not happen if createUserWithEmailAndPassword succeeds without exception
                     logE("Firebase Auth user creation succeeded but user object is null.")
                    _authState.value = AuthState.Error("Account creation failed: Unknown error.")
                }
            } catch (e: Exception) {
                 logE("Create user with email/password failed", e)
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
             logD("Signing out user...")
            try {
                // Sign out from Firebase Auth
                auth.signOut()

            } catch (e: Exception) {
                 logE("Error during sign out: ${e.message}", e)
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
                 logD("Local state cleared after sign out.")
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
                 logD("Sending password reset email to: $email")
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetSent
                 logD("Password reset email sent successfully.")
            } catch (e: Exception) {
                 logE("Password reset failed", e)
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
                 logD("Updating profile for user: $userId")
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
                     logW("Update profile called with no changes.")
                     onSuccess() // Or onError("No changes submitted")
                     return@launch
                 }

                // Always include lastUpdated with every update
                updates["lastUpdated"] = com.google.firebase.Timestamp.now()
                
                userDocRef.update(updates).await()

                // Update Firebase Auth display name if changed
                 if (updates.containsKey("fullName")) {
                     try {
                         val profileUpdates = UserProfileChangeRequest.Builder()
                             .setDisplayName(updates["fullName"] as String)
                             .build()
                         auth.currentUser?.updateProfile(profileUpdates)?.await()
                     } catch (e: Exception) {
                          logE("Failed to update Firebase Auth display name", e)
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
                 logD("User profile updated successfully in Firestore.")
                onSuccess()

            } catch (e: Exception) {
                 logE("Update profile failed", e)
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
                 logD("Attempting to change password for $userEmail")
                 // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(userEmail, currentPassword)
                currentUser.reauthenticate(credential).await()
                 logD("User re-authenticated successfully.")

                 // Change password
                currentUser.updatePassword(newPassword).await()
                 logD("Password changed successfully in Firebase Auth.")
                onSuccess()

            } catch (e: Exception) {
                 logE("Password change failed", e)
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
                 logD("Starting account deletion for user: $userId")
                 // 1. Delete Firestore data (best effort)
                try {
                    db.collection("users").document(userId).delete().await()
                     logD("User document deleted from Firestore.")
                     // Add deletion logic for other user-related collections if needed
                } catch (e: Exception) {
                     logE("Failed to delete Firestore data for user $userId during account deletion (will proceed with Auth deletion)", e)
                     // Log this error but proceed with Auth deletion as it's more critical
                }

                 // 2. Delete Firebase Auth account
                 logD("Deleting Firebase Auth account for user: $userId")
                currentUser.delete().await()
                 logD("Firebase Auth account deleted successfully.")

                 // 3. Clear local state and call success callback
                signOut() // Use signOut to clear all local state consistently
                onSuccess()

            } catch (e: Exception) {
                 logE("Account deletion failed", e)
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
         logD("signUpWithEmailPassword called, forwarding to createUserWithEmailPassword")
        createUserWithEmailPassword(email, password, fullName, phoneNumber)
    }

    /**
     * Set whether to bypass email verification (e.g., for testing).
     */
    fun setBypassVerification(bypass: Boolean) {
        _bypassVerification.value = bypass
         logD("Email verification bypass set to: $bypass")
    }

    /**
     * Refresh current user data from Firestore. Updates _currentUser.
     */
    private suspend fun refreshCurrentUser() {
        // Use a try-catch block for safety
        try {
            val firebaseUser = auth.currentUser ?: return // Exit if no auth user
            val userId = firebaseUser.uid
             logD("Refreshing current user data from Firestore for $userId")
            val userDoc = db.collection("users").document(userId).get().await()

            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    _currentUser.value = user // Update local state
                     logD("Current user refreshed successfully.")
                } else {
                     logE("Failed to parse user document during refresh for $userId")
                }
            } else {
                 logW("No user document found in Firestore during refresh for $userId")
                 // Maybe sign out or clear local state if Firestore doc is missing?
                 // _currentUser.value = null
            }
        } catch (e: Exception) {
             logE("Error refreshing current user from Firestore", e)
        }
    }

    // onCleared is now a direct member of the class
    override fun onCleared() {
        super.onCleared()
         logD("onCleared called, cancelling viewModelJob.")
        viewModelJob.cancel() // Cancel coroutines started with this job
    }

    fun signUpWithEmailAndPassword(email: String, password: String, fullName: String) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            _authState.value = AuthState.Error("Email, password, and name are required")
            return
        }

        _authState.value = AuthState.Loading
        
        viewModelScope.launch(viewModelJob) {
            try {
                // Check if email already exists before attempting to create
                try {
                    val methods = auth.fetchSignInMethodsForEmail(email).await()
                    val hasSignInMethods = methods != null && methods.toString() != "[]"
                    if (hasSignInMethods) {
                        _authState.value = AuthState.Error("This email address is already in use. Please sign in instead.")
                        logW("Email already in use - prevented registration attempt")
                        return@launch
                    }
                } catch (e: Exception) {
                    // Continue with registration if we can't check (API limit or other issues)
                    logW("Could not check existing email status: ${e.message}")
                }
                
                // Proceed with registration
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    // Update display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    
                    firebaseUser.updateProfile(profileUpdates).await()
                    
                    // Create user in Firestore
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        isEmailVerified = false,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    db.collection("users").document(firebaseUser.uid)
                        .set(newUser)
                        .await()
                    
                    // Send email verification
                    val actionCodeSettings = getActionCodeSettings(email)
                    firebaseUser.sendEmailVerification(actionCodeSettings).await()
                    
                    // Update state
                    _currentUser.value = newUser
                    _authState.value = AuthState.NeedsEmailVerification(newUser)
                    _emailVerified.value = false
                    
                    logD("Successfully created user with email")
                } else {
                    _authState.value = AuthState.Error("User creation failed")
                    logE("Failed to create user with email - null user")
                }
            } catch (e: Exception) {
                // Handle specific auth errors
                val errorMessage = when {
                    e.message?.contains("email address is already in use", ignoreCase = true) == true -> 
                        "This email address is already in use. Please sign in instead."
                    e.message?.contains("password is invalid", ignoreCase = true) == true -> 
                        "Password must be at least 6 characters long."
                    e.message?.contains("network error", ignoreCase = true) == true -> 
                        "Network error. Please check your connection and try again."
                    else -> "Registration failed: ${e.message}"
                }
                
                _authState.value = AuthState.Error(errorMessage)
                logE("Create user with email/password failed", e)
            }
        }
    }

} // End of AuthViewModel class
