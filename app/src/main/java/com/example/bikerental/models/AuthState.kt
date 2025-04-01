package com.example.bikerental.models

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
    data class NeedsAdditionalInfo(
        val displayName: String,
        val email: String,
        val idToken: String
    ) : AuthState()
} 