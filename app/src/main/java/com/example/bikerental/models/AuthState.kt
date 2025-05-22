package com.example.bikerental.models

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User? = User()) : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
    data class NeedsAdditionalInfo(
        val displayName: String,
        val email: String,
        val idToken: String
    ) : AuthState()
    
    // New states for app-specific verification
    data class NeedsAppVerification(val user: User) : AuthState()
    object VerificationEmailSent : AuthState()
    data class VerificationSuccess(val user: User) : AuthState()
    
    // Email verification state
    data class NeedsEmailVerification(val user: User?) : AuthState()
} 