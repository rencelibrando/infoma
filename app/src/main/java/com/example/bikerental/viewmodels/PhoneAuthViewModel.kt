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
    
    fun retryWithoutRecaptcha(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            verificationManager.retryWithoutRecaptcha(phoneNumber, activity)
        }
    }
    
    fun verifyPhoneNumberWithCode(code: String) {
        verificationManager.verifyCode(code)
    }
    
    suspend fun checkRateLimitStatus(): Pair<Boolean, Long> {
        return verificationManager.checkRateLimitStatus()
    }
    
    fun checkAndCleanupExpiredRateLimits() {
        viewModelScope.launch {
            verificationManager.cleanupExpiredRateLimits()
        }
    }
    
    fun checkIfDeviceBlockExpired() {
        viewModelScope.launch {
            try {
                val isDeviceStillBlocked = verificationManager.checkIfDeviceStillBlocked()
                if (!isDeviceStillBlocked) {
                    Log.d("PhoneAuthViewModel", "Device block has expired, resetting state")
                    resetState()
                } else {
                    Log.d("PhoneAuthViewModel", "Device is still blocked")
                    val blockInfo = verificationManager.getDeviceBlockInfo()
                    if (blockInfo.first) {
                        setRateLimited(
                            blockInfo.second,
                            "24 hours",
                            isDeviceBlock = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PhoneAuthViewModel", "Error checking device block status: ${e.message}")
            }
        }
    }
    
    fun setRateLimited(expiryTime: Long, displayDuration: String, isDeviceBlock: Boolean = false) {
        _uiState.value = PhoneAuthState.RateLimited(
            expiryTime, 
            displayDuration, 
            isServerBased = true,
            isDeviceBlock = isDeviceBlock
        )
    }
    
    fun resetState() {
        val currentState = _uiState.value
        val isRateLimited = currentState is PhoneAuthState.RateLimited
        val isDeviceBlock = if (currentState is PhoneAuthState.RateLimited) {
            currentState.isDeviceBlock
        } else {
            false
        }
        
        _uiState.value = PhoneAuthState.Initial
        verificationManager.reset()
        
        if (isRateLimited) {
            if (!isDeviceBlock) {
                checkAndCleanupExpiredRateLimits()
            } else {
                checkIfDeviceBlockExpired()
            }
        }
    }
    
    fun updateAuthState(user: FirebaseUser?) {
        if (user != null && !user.phoneNumber.isNullOrEmpty()) {
            _uiState.value = PhoneAuthState.Success
        } else {
            _uiState.value = PhoneAuthState.Initial
        }
    }
} 