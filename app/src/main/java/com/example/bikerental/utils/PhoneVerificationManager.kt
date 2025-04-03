package com.example.bikerental.utils

import android.app.Activity
import android.util.Log
import com.example.bikerental.models.PhoneAuthState
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

private const val TAG = "PhoneVerification"

class PhoneVerificationManager(
    private val uiStateFlow: MutableStateFlow<PhoneAuthState>
) {
    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verificationInProgress = false
    
    // Track retry attempts
    private var retryCount = 0
    private val MAX_RETRIES = 2
    
    // Add a time tracking variable for rate limiting
    private var lastVerificationAttempt = 0L
    private val RATE_LIMIT_DURATION = 5 * 60 * 1000 // 5 minutes in milliseconds (instead of 24 hours)
    
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d(TAG, "Verification completed automatically")
            verificationInProgress = false
            retryCount = 0
            updatePhoneCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "Verification failed", e)
            verificationInProgress = false
            
            val errorMessage = when {
                e.message?.contains("TOO_MANY_REQUESTS") == true || 
                e.message?.contains("quota_exceeded") == true ||
                e.message?.contains("rate limit") == true ||
                e is FirebaseTooManyRequestsException -> {
                    // Handle rate limiting with 5 minute timeout
                    lastVerificationAttempt = System.currentTimeMillis()
                    val expiryTime = lastVerificationAttempt + RATE_LIMIT_DURATION
                    uiStateFlow.value = PhoneAuthState.RateLimited(expiryTime, "5 minutes")
                    return
                }
                e.message?.contains("INVALID_APP_CREDENTIAL") == true ||
                e.message?.contains("Error: [auth/invalid-app-credential]") == true ||
                e.message?.contains("Error: 39") == true -> {
                    // This is the specific error 39 (reCAPTCHA verification issue)
                    if (retryCount < MAX_RETRIES) {
                        retryCount++
                        uiStateFlow.value = PhoneAuthState.RecaptchaError
                        return
                    } else {
                        "reCAPTCHA verification failed. Please try again later."
                    }
                }
                e is FirebaseAuthInvalidCredentialsException -> {
                    "Invalid phone number format. Please use +63 format."
                }
                else -> "Verification failed: ${e.message}"
            }
            
            uiStateFlow.value = PhoneAuthState.Error(errorMessage)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d(TAG, "Code sent successfully")
            verificationInProgress = false
            retryCount = 0
            storedVerificationId = verificationId
            resendToken = token
            uiStateFlow.value = PhoneAuthState.CodeSent
        }
    }

    fun startVerification(phoneNumber: String, activity: Activity, senderName: String = "GearTick") {
        Log.d(TAG, "Starting verification for $phoneNumber")
        
        // Check if we're still within the rate limit period (now 5 minutes)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastVerificationAttempt < RATE_LIMIT_DURATION) {
            // Calculate remaining time in minutes and seconds
            val remainingMillis = RATE_LIMIT_DURATION - (currentTime - lastVerificationAttempt)
            val remainingMinutes = remainingMillis / (60 * 1000)
            val remainingSeconds = (remainingMillis % (60 * 1000)) / 1000
            
            val timeMessage = if (remainingMinutes > 0) {
                "$remainingMinutes min $remainingSeconds sec"
            } else {
                "$remainingSeconds seconds"
            }
            
            // Set the exact expiry time for real-time counting
            val expiryTime = lastVerificationAttempt + RATE_LIMIT_DURATION
            
            uiStateFlow.value = PhoneAuthState.RateLimited(expiryTime, timeMessage)
            return
        }
        
        if (verificationInProgress) {
            Log.d(TAG, "Verification already in progress, ignoring request")
            return
        }
        
        if (!validatePhoneNumber(phoneNumber)) {
            uiStateFlow.value = PhoneAuthState.Error(
                "Please enter a valid phone number with +63 prefix"
            )
            return
        }

        uiStateFlow.value = PhoneAuthState.Loading
        verificationInProgress = true

        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(120L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d(TAG, "Verification request sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting verification", e)
            verificationInProgress = false
            uiStateFlow.value = PhoneAuthState.Error("Failed to start verification: ${e.message}")
        }
    }
    
    // Use this for direct retry without reCAPTCHA when error 39 occurs
    fun retryWithoutRecaptcha(phoneNumber: String, activity: Activity) {
        Log.d(TAG, "Retrying verification without reCAPTCHA")
        
        if (verificationInProgress) {
            return
        }
        
        uiStateFlow.value = PhoneAuthState.Loading
        verificationInProgress = true
        
        try {
            // Try to use the resend token if available
            if (resendToken != null) {
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(120L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .setForceResendingToken(resendToken!!)
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                Log.d(TAG, "Retry with resend token sent")
            } else {
                // Fall back to regular verification
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(120L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                Log.d(TAG, "Retry without resend token sent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during retry", e)
            verificationInProgress = false
            uiStateFlow.value = PhoneAuthState.Error("Retry failed: ${e.message}")
        }
    }

    fun verifyCode(code: String) {
        Log.d(TAG, "Verifying code")
        
        if (code.length != 6 || !code.all { it.isDigit() }) {
            uiStateFlow.value = PhoneAuthState.Error("Please enter a valid 6-digit code")
            return
        }

        val verificationId = storedVerificationId
        if (verificationId == null) {
            uiStateFlow.value = PhoneAuthState.Error("Verification session expired. Please try again")
            return
        }

        uiStateFlow.value = PhoneAuthState.Loading
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            updatePhoneCredential(credential)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying code", e)
            uiStateFlow.value = PhoneAuthState.Error("Invalid verification code")
        }
    }

    private fun updatePhoneCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "Updating phone credential")
        
        auth.currentUser?.updatePhoneNumber(credential)
            ?.addOnSuccessListener {
                Log.d(TAG, "Phone number updated successfully")
                updateUserProfile()
                uiStateFlow.value = PhoneAuthState.Success
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to update phone number", e)
                val message = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> 
                        "Invalid verification code"
                    else -> "Verification failed: ${e.message}"
                }
                uiStateFlow.value = PhoneAuthState.Error(message)
            }
    }

    private fun updateUserProfile() {
        auth.currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "isPhoneVerified" to true,
                        "phoneNumber" to auth.currentUser?.phoneNumber,
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    )
                )
                .addOnSuccessListener {
                    Log.d(TAG, "User profile updated")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update user profile", e)
                }
        } ?: Log.w(TAG, "No user logged in to update profile")
    }

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.startsWith("+") && phoneNumber.length >= 10
    }

    fun reset() {
        Log.d(TAG, "Reset verification state")
        storedVerificationId = null
        retryCount = 0
        verificationInProgress = false
        // Keep the resendToken in case we need it
        uiStateFlow.value = PhoneAuthState.Initial
    }
} 