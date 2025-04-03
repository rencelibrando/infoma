package com.example.bikerental.models

sealed class PhoneAuthState {
    object Initial : PhoneAuthState()
    object Loading : PhoneAuthState()
    object CodeSent : PhoneAuthState()
    object Success : PhoneAuthState()
    object RecaptchaError : PhoneAuthState()
    data class RateLimited(
        val expireTimeMillis: Long,
        val remainingTime: String = "5 minutes"
    ) : PhoneAuthState()
    object AppCheckError : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
} 