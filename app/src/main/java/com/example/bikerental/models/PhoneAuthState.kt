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
    
    data class Error(val message: String) : PhoneAuthState()
    
    // Rate limiting with server-based expiry time
    data class RateLimited(
        val expireTimeMillis: Long,
        val displayDuration: String,
        val isServerBased: Boolean = true,
        val isDeviceBlock: Boolean = false,  // Flag for device-level blocks vs rate limits
        val reason: String = "Too many verification attempts" // Reason for the rate limit
    ) : PhoneAuthState()
} 