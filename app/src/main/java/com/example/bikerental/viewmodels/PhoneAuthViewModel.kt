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

class PhoneAuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Initial)
    val uiState: StateFlow<PhoneAuthState> = _uiState.asStateFlow()
    
    private val verificationManager = PhoneVerificationManager(_uiState)
    
    fun startPhoneNumberVerification(phoneNumber: String, activity: Activity, senderName: String = "GearTick") {
        verificationManager.startVerification(phoneNumber, activity, senderName)
    }
    
    fun retryWithoutRecaptcha(phoneNumber: String, activity: Activity) {
        verificationManager.retryWithoutRecaptcha(phoneNumber, activity)
    }
    
    fun verifyPhoneNumberWithCode(code: String) {
        verificationManager.verifyCode(code)
    }
    
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
} 