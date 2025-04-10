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

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
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
     * Handle Google Sign-In result
     */
    fun handleGoogleSignInResult(
        idToken: String,
        displayName: String?,
        email: String?,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Processing Google sign-in result")
                _authState.value = AuthState.Loading
                
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                
                val user = authResult.user
                if (user != null) {
                    // Check if user exists in our database
                    val userDoc = db.collection("users").document(user.uid).get().await()
                    
                    if (userDoc.exists()) {
                        // User exists in our database, update and authenticate
                        val existingUser = userDoc.toObject(User::class.java)
                        
                        // Update lastSignInTime
                        val updatedUser = existingUser?.copy(lastSignInTime = System.currentTimeMillis())
                        
                        if (updatedUser != null) {
                            db.collection("users").document(user.uid)
                                .update("lastSignInTime", System.currentTimeMillis())
                                .await()
                            
                            _currentUser.value = updatedUser
                            _authState.value = AuthState.Authenticated(updatedUser)
                            Log.d("AuthViewModel", "Existing Google user signed in: ${updatedUser.id}")
                        }
                    } else {
                        // New user, we need additional information
                        if (email.isNullOrEmpty()) {
                            _authState.value = AuthState.Error("Google sign-in failed: No email provided")
                            signOut()
                            return@launch
                        }
                        
                        // Create a new user with available information
                        val newUser = User(
                            id = user.uid,
                            email = email,
                            fullName = displayName ?: "",
                            givenName = user.displayName,
                            displayName = displayName,
                            createdAt = System.currentTimeMillis(),
                            lastSignInTime = System.currentTimeMillis(),
                            provider = "google",
                            googleId = user.uid
                        )
                        
                        // Save to Firestore
                        db.collection("users").document(user.uid)
                            .set(newUser)
                            .await()
                        
                        _currentUser.value = newUser
                        _authState.value = AuthState.Authenticated(newUser)
                        Log.d("AuthViewModel", "New Google user created: ${newUser.id}")
                    }
                } else {
                    _authState.value = AuthState.Error("Google sign-in failed: User is null")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in error", e)
                val errorMessage = when (e) {
                    is ApiException -> {
                        "Google Sign-In failed: ${e.statusCode}"
                    }
                    else -> {
                        "Google Sign-In failed: ${e.message}"
                    }
                }
                _authState.value = AuthState.Error(errorMessage)
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
                            val updatedUser = user.copy(lastSignInTime = System.currentTimeMillis())
                            
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
                        _authState.value = AuthState.Authenticated(newUser)
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
     * Create a new user account with email and password
     */
    fun createUserWithEmailPassword(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String = ""
    ) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Creating user account for email: $email")
                _authState.value = AuthState.Loading
                
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                
                authResult.user?.let { firebaseUser ->
                    // Create user object
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        phoneNumber = phoneNumber,
                        createdAt = System.currentTimeMillis(),
                        isEmailVerified = false,
                        lastSignInTime = System.currentTimeMillis()
                    )
                    
                    // Save user to Firestore
                    db.collection("users").document(firebaseUser.uid)
                        .set(newUser)
                        .await()
                    
                    _currentUser.value = newUser
                    _authState.value = AuthState.Authenticated(newUser)
                    Log.d("AuthViewModel", "User created successfully: ${newUser.id}")
                    
                    // Send email verification
                    firebaseUser.sendEmailVerification().await()
                } ?: run {
                    _authState.value = AuthState.Error("Account creation failed: No user returned")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Create user failed", e)
                _authState.value = AuthState.Error("Account creation failed: ${e.message}")
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Signing out")
                googleSignInClient?.signOut()?.await()
                auth.signOut()
                _currentUser.value = null
                _authState.value = AuthState.Initial
                Log.d("AuthViewModel", "Signed out successfully")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign out failed", e)
                _authState.value = AuthState.Error("Sign out failed: ${e.message}")
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
} 