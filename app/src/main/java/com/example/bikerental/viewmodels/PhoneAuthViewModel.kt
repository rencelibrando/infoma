package com.example.bikerental.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.PhoneAuthState
import com.example.bikerental.utils.PhoneVerificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.tasks.await
import android.util.Log

class PhoneAuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Initial)
    val uiState: StateFlow<PhoneAuthState> = _uiState.asStateFlow()
    
    private val verificationManager = PhoneVerificationManager(_uiState)
    
    fun startPhoneNumberVerification(phoneNumber: String, activity: Activity, senderName: String = "GearTick") {
        viewModelScope.launch {
            verificationManager.startVerification(phoneNumber, activity, senderName)
        }
    }
    
    fun verifyPhoneNumberWithCode(code: String) {
        verificationManager.verifyCode(code)
    }
    
    fun checkAndCleanupExpiredRateLimits() {
        Log.d("PhoneAuthViewModel", "checkAndCleanupExpiredRateLimits called, but Firestore cleanup is removed.")
    }
    
    fun checkIfDeviceBlockExpired() {
        Log.d("PhoneAuthViewModel", "checkIfDeviceBlockExpired called, but device block logic is removed.")
    }
    
    fun setRateLimited(expiryTime: Long, displayDuration: String) {
        _uiState.value = PhoneAuthState.RateLimited(
            expiryTime, 
            displayDuration
        )
    }
    
    /**
     * Checks if the user is currently rate limited for phone verification
     * @return Pair<Boolean, Long> where first is whether user is rate limited and second is expiry time
     */
    fun checkRateLimitStatus(): Pair<Boolean, Long> {
        // For now, implement a simple version that just checks the current state
        // In a real implementation, this would check Firestore or other persistence
        
        val currentState = _uiState.value
        if (currentState is PhoneAuthState.RateLimited) {
            return Pair(true, currentState.expireTimeMillis)
        }
        
        // Default: not rate limited, with 0 as expiry time
        return Pair(false, 0L)
    }
    
    /**
     * Resets the phone auth state to Initial
     */
    fun resetState() {
        _uiState.value = PhoneAuthState.Initial
        verificationManager.reset()
    }
    
    fun updateAuthState(user: FirebaseUser?) {
        if (user != null && !user.phoneNumber.isNullOrEmpty()) {
            _uiState.value = PhoneAuthState.Success
        } else {
            _uiState.value = PhoneAuthState.Initial
        }
    }
    
    /**
     * Retry verification without reCAPTCHA when the normal flow fails
     * @param phoneNumber The phone number to verify
     * @param activity The activity context
     */
    suspend fun retryWithoutRecaptcha(phoneNumber: String, activity: Activity) {
        Log.d("PhoneAuthViewModel", "Attempting verification without reCAPTCHA for $phoneNumber")
        // In a production app, this might use a different verification method or
        // set special flags for verification. For now, we'll just call the normal method
        // but we could add special handling here in the future.
           startPhoneNumberVerification(phoneNumber, activity, "GearTick")
    }
} 