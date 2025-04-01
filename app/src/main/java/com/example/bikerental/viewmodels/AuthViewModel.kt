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

    init {
        // Check if user is already signed in
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
                    if (userDoc.exists()) {
                        _currentUser.value = userDoc.toObject(User::class.java)
                        _authState.value = AuthState.Authenticated
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
                            _authState.value = AuthState.Authenticated
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
                            _authState.value = AuthState.Authenticated
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
                        _authState.value = AuthState.Authenticated
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
                        _currentUser.value = userDoc.toObject(User::class.java)
                        _authState.value = AuthState.Authenticated
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
                        _authState.value = AuthState.Authenticated
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
} 