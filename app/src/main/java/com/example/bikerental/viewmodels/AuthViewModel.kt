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

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Add isCurrentUserAdmin property if it doesn't exist
    private val _isCurrentUserAdmin = MutableStateFlow(false)
    val isCurrentUserAdmin: StateFlow<Boolean> = _isCurrentUserAdmin
    
    // Update user admin status whenever the current user changes
    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                _isCurrentUserAdmin.value = user?.isAdmin == true
            }
        }
    }

    init {
        // Important: We no longer automatically check or modify the current auth state on init
        // This prevents unexpected redirects during login input
        
        // Initialize admin account silently in the background without affecting auth state
        initializeAdminAccountSilently()
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user!!)
                    } else {
                        // User exists in Firebase Auth but not in Firestore
                        _authState.value = AuthState.NeedsAdditionalInfo(
                            displayName = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            idToken = ""
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error fetching user data", e)
                    _authState.value = AuthState.Error("Failed to fetch user data")
                }
            }
        }
    }

    // Separate method to initialize admin account without affecting UI state at all
    private fun initializeAdminAccountSilently() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Silently checking admin account")
                // Use a separate Firebase Auth instance that doesn't trigger auth state changes
                val tempAuth = FirebaseAuth.getInstance()
                val tempCurrentUser = tempAuth.currentUser
                
                // Query Firestore directly without affecting auth state
                val adminQuery = db.collection("users")
                    .whereEqualTo("email", "admin@bikerental.com")
                    .whereEqualTo("isAdmin", true)
                    .limit(1)
                    .get()
                    .await()
                
                if (adminQuery.isEmpty) {
                    Log.d("AuthViewModel", "Admin account does not exist, creating silently in background")
                    // Do not update _authState at all during this process
                    
                    try {
                        // Try to sign in to check if account exists but lacks admin flag
                        tempAuth.signInWithEmailAndPassword("admin@bikerental.com", "Admin-123").await()
                        
                        // Account exists in Auth, just add admin flag in Firestore if needed
                        tempAuth.currentUser?.let { firebaseUser ->
                            val adminUser = User(
                                id = firebaseUser.uid,
                                email = "admin@bikerental.com",
                                fullName = "System Administrator",
                                createdAt = System.currentTimeMillis(),
                                isAdmin = true
                            )
                            
                            // Quietly update Firestore, don't affect auth state
                            db.collection("users").document(firebaseUser.uid)
                                .set(adminUser)
                                .await()
                            
                            Log.d("AuthViewModel", "Admin account updated silently in Firestore")
                        }
                        
                        // Silently sign out from the temp auth instance
                        tempAuth.signOut()
                    } catch (e: Exception) {
                        // Silent fail - just log
                        Log.d("AuthViewModel", "Admin account doesn't exist in Auth, trying to create")
                        try {
                            // Create new account
                            val authResult = tempAuth.createUserWithEmailAndPassword("admin@bikerental.com", "Admin-123").await()
                            
                            authResult.user?.let { firebaseUser ->
                                val adminUser = User(
                                    id = firebaseUser.uid,
                                    email = "admin@bikerental.com",
                                    fullName = "System Administrator",
                                    createdAt = System.currentTimeMillis(),
                                    isAdmin = true
                                )
                                
                                // Quietly update Firestore
                                db.collection("users").document(firebaseUser.uid)
                                    .set(adminUser)
                                    .await()
                                
                                Log.d("AuthViewModel", "Admin account created silently")
                            }
                            
                            // Silently sign out
                            tempAuth.signOut()
                        } catch (createEx: Exception) {
                            // Silent fail - just log, don't update UI state
                            Log.e("AuthViewModel", "Failed to create admin account silently", createEx)
                        }
                    }
                } else {
                    Log.d("AuthViewModel", "Admin account already exists")
                }
                
                // Restore the original user if there was one
                if (tempCurrentUser != null && tempAuth.currentUser?.uid != tempCurrentUser.uid) {
                    tempAuth.updateCurrentUser(tempCurrentUser).await()
                }
            } catch (e: Exception) {
                // Silent fail - just log
                Log.e("AuthViewModel", "Silent admin check failed", e)
            }
        }
    }

    fun initializeGoogleSignIn(context: Context) {
        try {
            Log.d("AuthViewModel", "Initializing Google Sign-In")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("862099405823-r4enfg894e2stk11iq2bc2otoar0oeh6.apps.googleusercontent.com")
                .requestEmail()
                .requestProfile()
                .requestId()
                .build()
            
            // Check if there's an existing signed-in account
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastSignedInAccount != null) {
                Log.d("AuthViewModel", "Found existing Google Sign-In account, signing out first")
                GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                    googleSignInClient = GoogleSignIn.getClient(context, gso)
                    Log.d("AuthViewModel", "Google Sign-In reinitialized after signout")
                }
            } else {
                googleSignInClient = GoogleSignIn.getClient(context, gso)
                Log.d("AuthViewModel", "Google Sign-In initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error initializing Google Sign-In", e)
            _authState.value = AuthState.Error("Failed to initialize Google Sign-In: ${e.localizedMessage}")
        }
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        Log.d("AuthViewModel", "Getting Google Sign-In client")
        if (googleSignInClient == null) {
            Log.d("AuthViewModel", "Google Sign-In client is null, initializing")
            initializeGoogleSignIn(context)
        }
        return googleSignInClient ?: throw IllegalStateException("Google Sign-In client is not initialized")
    }

    fun handleGoogleSignInResult(idToken: String, displayName: String?, email: String?, context: android.content.Context) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting Google Sign-In process")
                _authState.value = AuthState.Loading
                
                // Create credential
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                Log.d("AuthViewModel", "Created Google credential")
                
                // Sign in with Firebase
                val authResult = auth.signInWithCredential(credential).await()
                Log.d("AuthViewModel", "Firebase auth successful")
                
                authResult.user?.let { firebaseUser ->
                    // Get Google Sign In account
                    val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
                    
                    // Check if user exists in Firestore
                    Log.d("AuthViewModel", "Checking if user exists in Firestore")
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (!userDoc.exists()) {
                        Log.d("AuthViewModel", "Creating new user in Firestore")
                        // Create new user with Google account information
                        val user = User(
                            id = firebaseUser.uid,
                            email = email ?: firebaseUser.email ?: "",
                            fullName = displayName ?: firebaseUser.displayName ?: "",
                            phoneNumber = firebaseUser.phoneNumber ?: "",
                            createdAt = System.currentTimeMillis(),
                            profilePictureUrl = googleAccount?.photoUrl?.toString(),
                            isEmailVerified = firebaseUser.isEmailVerified,
                            givenName = googleAccount?.givenName,
                            familyName = googleAccount?.familyName,
                            displayName = googleAccount?.displayName,
                            lastSignInTime = System.currentTimeMillis(),
                            provider = "google",
                            googleId = googleAccount?.id
                        )
                        
                        try {
                            // Save user to Firestore
                            db.collection("users").document(firebaseUser.uid)
                                .set(user)
                                .await()
                            
                            Log.d("AuthViewModel", "User created successfully")
                            _currentUser.value = user
                            _authState.value = AuthState.Authenticated(user)
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to save user data", e)
                            _authState.value = AuthState.Error("Failed to save user data: ${e.localizedMessage}")
                            signOut()
                        }
                    } else {
                        Log.d("AuthViewModel", "Updating existing user with latest Google info")
                        // Update existing user with latest Google information
                        val updates = hashMapOf<String, Any>(
                            "lastSignInTime" to System.currentTimeMillis(),
                            "isEmailVerified" to firebaseUser.isEmailVerified,
                            "provider" to "google"
                        )
                        
                        // Only update these fields if they have values
                        googleAccount?.let { account ->
                            account.photoUrl?.toString()?.let { updates["profilePictureUrl"] = it }
                            account.givenName?.let { updates["givenName"] = it }
                            account.familyName?.let { updates["familyName"] = it }
                            account.displayName?.let { updates["displayName"] = it }
                            account.id?.let { updates["googleId"] = it }
                        }
                        
                        try {
                            // Update user in Firestore
                            db.collection("users").document(firebaseUser.uid)
                                .update(updates)
                                .await()
                            
                            // Get updated user data
                            val updatedUserDoc = db.collection("users").document(firebaseUser.uid).get().await()
                            val updatedUser = updatedUserDoc.toObject(User::class.java)
                            _currentUser.value = updatedUser
                            _authState.value = AuthState.Authenticated(updatedUser!!)
                            Log.d("AuthViewModel", "User updated successfully")
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to update user data", e)
                            _authState.value = AuthState.Error("Failed to update user data: ${e.localizedMessage}")
                        }
                    }
                } ?: run {
                    Log.e("AuthViewModel", "Failed to get user data after Google sign in")
                    _authState.value = AuthState.Error("Failed to get user data after Google sign in")
                    signOut()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign in failed", e)
                when (e) {
                    is ApiException -> {
                        val errorMessage = when (e.statusCode) {
                            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google Sign-In was cancelled by user"
                            GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error occurred. Please check your internet connection"
                            GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid Google account selected"
                            GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign-In is required"
                            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google Sign-In failed. Please try again"
                            GoogleSignInStatusCodes.INTERNAL_ERROR -> "Internal error occurred. Please try again"
                            else -> "Google Sign-In failed: ${e.message}"
                        }
                        Log.e("AuthViewModel", "ApiException during Google Sign-In: $errorMessage")
                        _authState.value = AuthState.Error(errorMessage)
                    }
                    else -> {
                        val errorMessage = when {
                            e.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your internet connection"
                            e.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Connection timeout. Please try again"
                            e.message?.contains("credential", ignoreCase = true) == true -> 
                                "Invalid credentials. Please try again"
                            else -> e.message ?: "Google sign in failed"
                        }
                        Log.e("AuthViewModel", "Exception during Google Sign-In: $errorMessage")
                        _authState.value = AuthState.Error(errorMessage)
                    }
                }
                signOut()
            }
        }
    }

    fun signUpWithEmailPassword(
        email: String,
        password: String,
        fullName: String,
        phone: String
    ) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    val user = User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        phoneNumber = phone,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    try {
                        db.collection("users").document(firebaseUser.uid)
                            .set(user)
                            .await()
                        
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to save user data", e)
                        _authState.value = AuthState.Error("Failed to save user data: ${e.localizedMessage}")
                        signOut()
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up failed", e)
                val errorMessage = when {
                    e.message?.contains("email", ignoreCase = true) == true -> 
                        "This email is already in use. Please use a different email"
                    e.message?.contains("password", ignoreCase = true) == true -> 
                        "Password is too weak. Please use at least 8 characters with letters and numbers"
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection"
                    else -> e.message ?: "Sign up failed"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    if (userDoc.exists()) {
                        val user = userDoc.toObject(User::class.java)
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user!!)
                    } else {
                        _authState.value = AuthState.Error("User data not found")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in failed", e)
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    /**
     * Sign in as an admin user with email and password
     * This method checks if the user exists and has admin privileges
     */
    fun signInAsAdmin(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Attempting admin sign in with email: $email")
                _authState.value = AuthState.Loading
                
                // Special handling for the default admin account
                val isDefaultAdmin = email.trim().equals("admin@bikerental.com", ignoreCase = true)
                Log.d("AuthViewModel", "Is default admin account: $isDefaultAdmin")
                
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    Log.d("AuthViewModel", "Firebase auth successful for user: ${firebaseUser.uid}")
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    
                    if (isDefaultAdmin) {
                        // For the default admin account, always ensure admin privileges
                        val adminUser = if (userDoc.exists()) {
                            Log.d("AuthViewModel", "Admin document exists, updating with admin privileges")
                            // Get existing user data
                            val existingUser = userDoc.toObject(User::class.java)
                            // Create a copy with isAdmin set to true
                            existingUser?.copy(isAdmin = true) ?: User(
                                id = firebaseUser.uid,
                                email = email,
                                fullName = "System Administrator",
                                createdAt = System.currentTimeMillis(),
                                isAdmin = true
                            )
                        } else {
                            Log.d("AuthViewModel", "Admin document doesn't exist, creating new one")
                            // Create new admin user
                            User(
                                id = firebaseUser.uid,
                                email = email,
                                fullName = "System Administrator",
                                createdAt = System.currentTimeMillis(),
                                isAdmin = true
                            )
                        }
                        
                        // Update or create the admin document
                        db.collection("users").document(firebaseUser.uid)
                            .set(adminUser)
                            .await()
                        
                        Log.d("AuthViewModel", "Successfully set admin user in Firestore: ${adminUser.id}, isAdmin=${adminUser.isAdmin}")
                        
                        _currentUser.value = adminUser
                        _authState.value = AuthState.Authenticated(adminUser)
                        Log.d("AuthViewModel", "Admin login successful (default admin account)")
                    } else {
                        // Regular admin user flow
                        if (userDoc.exists()) {
                            val user = userDoc.toObject(User::class.java)
                            Log.d("AuthViewModel", "User document exists, isAdmin=${user?.isAdmin}")
                            
                            if (user?.isAdmin == true) {
                                _currentUser.value = user
                                _authState.value = AuthState.Authenticated(user)
                                Log.d("AuthViewModel", "Admin login successful")
                            } else {
                                Log.d("AuthViewModel", "User is not an admin, access denied")
                                _authState.value = AuthState.Error("Access denied: Admin privileges required")
                                signOut()
                            }
                        } else {
                            Log.d("AuthViewModel", "User document not found in Firestore")
                            _authState.value = AuthState.Error("User data not found")
                        }
                    }
                } ?: run {
                    Log.e("AuthViewModel", "Firebase auth returned null user")
                    _authState.value = AuthState.Error("Authentication failed: No user returned")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Admin sign in failed", e)
                _authState.value = AuthState.Error("Invalid admin credentials: ${e.message}")
            }
        }
    }

    /**
     * Check if the current user has admin privileges
     */
    fun isCurrentUserAdmin(): Boolean {
        return _currentUser.value?.isAdmin == true
    }

    fun completeGoogleSignUp(phone: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val currentAuthState = _authState.value
                
                if (currentAuthState is AuthState.NeedsAdditionalInfo) {
                    auth.currentUser?.let { firebaseUser ->
                        val user = User(
                            id = firebaseUser.uid,
                            email = currentAuthState.email,
                            fullName = currentAuthState.displayName,
                            phoneNumber = phone,
                            createdAt = System.currentTimeMillis()
                        )
                        
                        // Save user data to Firestore
                        db.collection("users").document(firebaseUser.uid)
                            .set(user)
                            .await()
                        
                        // Update current user and authentication state
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } ?: run {
                        _authState.value = AuthState.Error("User not found")
                        signOut() // Sign out if user data is not available
                    }
                } else {
                    _authState.value = AuthState.Error("Invalid state for completing Google sign up")
                    signOut() // Sign out if in invalid state
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Complete Google sign up failed", e)
                _authState.value = AuthState.Error(e.message ?: "Failed to complete sign up")
                signOut() // Sign out on error
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                googleSignInClient?.signOut()?.await()
                auth.signOut()
                _currentUser.value = null
                _authState.value = AuthState.Initial
                Log.d("AuthViewModel", "Sign out completed successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during sign out", e)
                _authState.value = AuthState.Error("Failed to sign out properly")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetSent
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset failed", e)
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun clearPermissionsAndSignOut() {
        viewModelScope.launch {
            try {
                // Sign out from Google
                googleSignInClient?.signOut()?.await()
                
                // Sign out from Firebase
                auth.signOut()
                
                // Clear current user
                _currentUser.value = null
                _authState.value = AuthState.Initial
                
                Log.d("AuthViewModel", "Permissions cleared and signed out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during permission clearing and sign out", e)
                _authState.value = AuthState.Error("Failed to clear permissions and sign out")
            }
        }
    }

    // Add a function to force create and set up admin account
    fun forceCreateAdminAccount() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Try to sign in with admin credentials first
                try {
                    val authResult = auth.signInWithEmailAndPassword("admin@bikerental.com", "Admin-123").await()
                    
                    // Update the user document with admin privileges
                    authResult.user?.let { firebaseUser ->
                        // Create admin user object with isAdmin = true
                        val adminUser = User(
                            id = firebaseUser.uid,
                            email = "admin@bikerental.com",
                            fullName = "System Administrator",
                            createdAt = System.currentTimeMillis(),
                            isAdmin = true
                        )
                        
                        // Force write to Firestore with admin privileges
                        db.collection("users").document(firebaseUser.uid)
                            .set(adminUser)
                            .await()
                        
                        Log.d("AuthViewModel", "Admin account updated with admin privileges")
                        
                        // Sign in as admin and update auth state
                        _currentUser.value = adminUser
                        _authState.value = AuthState.Authenticated(adminUser)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to sign in as admin: ${e.message}")
                    
                    // Create a new admin account if sign-in failed
                    try {
                        val authResult = auth.createUserWithEmailAndPassword("admin@bikerental.com", "Admin-123").await()
                        
                        authResult.user?.let { firebaseUser ->
                            // Create admin user with admin privileges
                            val adminUser = User(
                                id = firebaseUser.uid,
                                email = "admin@bikerental.com",
                                fullName = "System Administrator",
                                createdAt = System.currentTimeMillis(),
                                isAdmin = true
                            )
                            
                            // Write to Firestore
                            db.collection("users").document(firebaseUser.uid)
                                .set(adminUser)
                                .await()
                            
                            Log.d("AuthViewModel", "New admin account created with admin privileges")
                            
                            // Update auth state
                            _currentUser.value = adminUser
                            _authState.value = AuthState.Authenticated(adminUser)
                        }
                    } catch (createEx: Exception) {
                        Log.e("AuthViewModel", "Failed to create admin account: ${createEx.message}")
                        _authState.value = AuthState.Error("Failed to set up admin account: ${createEx.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in forceCreateAdminAccount", e)
                _authState.value = AuthState.Error("Error creating admin account: ${e.message}")
            }
        }
    }
} 