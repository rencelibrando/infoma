package com.example.bikerental.models

/**
 * States for phone verification flow
 */
sealed class PhoneAuthState {
    object Initial : PhoneAuthState()
    object Loading : PhoneAuthState()
    object CodeSent : PhoneAuthState()
    object Success : PhoneAuthState()
    object RecaptchaError : PhoneAuthState()
    object AppCheckError : PhoneAuthState()
    
    data class Error(val message: String) : PhoneAuthState() {
        // Add error accessor for compatibility with ProfileTab code
        val error: Throwable? = null
    }
    
    // Rate limiting - simplified, no server/device distinction needed now
    data class RateLimited(
        val expireTimeMillis: Long, // Keep expiry for potential UI display
        val displayDuration: String // Keep display duration for UI
    ) : PhoneAuthState()
} 