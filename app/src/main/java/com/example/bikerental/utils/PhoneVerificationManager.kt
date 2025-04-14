package com.example.bikerental.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.webkit.WebView
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

private const val TAG = "PhoneVerification"

class PhoneVerificationManager(
    private val uiStateFlow: MutableStateFlow<PhoneAuthState>
) {
    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verificationInProgress = false
    
    private var lastAttemptTimestamp = 0L
    private val LOCAL_RETRY_COOLDOWN_MS = 5000L
    
    private var browserInfo: String = "Unknown"
    
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d(TAG, "Verification completed automatically")
            verificationInProgress = false
            updatePhoneCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "Verification failed on $browserInfo", e)
            verificationInProgress = false
            
            val errorMessage = when (e) {
                is FirebaseTooManyRequestsException -> {
                    "Too many requests. Please try again later."
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    if (e.message?.contains("invalid verification code", ignoreCase = true) == true) {
                        "Invalid verification code."
                    } else {
                        "Invalid phone number format. Please use +63XXXXXXXXXX format."
                    }
                }
                else -> {
                    val msg = e.message ?: ""
                    if (msg.contains("reCAPTCHA", ignoreCase = true) || 
                        msg.contains(" SafetyNet", ignoreCase = true) ||
                        msg.contains(" Play Integrity", ignoreCase = true) ||
                        msg.contains("app verification", ignoreCase = true) ||
                        msg.contains("17010")) {
                         uiStateFlow.value = PhoneAuthState.AppCheckError
                         "App verification failed. Please ensure your device passes security checks and try again."
                     } else if (msg.contains("network", ignoreCase = true)) {
                        "Network error. Please check your internet connection and try again."
                    } else {
                        "Verification failed: ${e.message}"
                    }
                }
            }
            
            if (uiStateFlow.value !is PhoneAuthState.AppCheckError) {
                uiStateFlow.value = PhoneAuthState.Error(errorMessage)
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d(TAG, "Code sent successfully")
            verificationInProgress = false
            storedVerificationId = verificationId
            resendToken = token
            uiStateFlow.value = PhoneAuthState.CodeSent
        }
    }

    suspend fun startVerification(phoneNumber: String, activity: Activity, senderName: String = "GearTick") {
        Log.d(TAG, "Starting verification for $phoneNumber")
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAttemptTimestamp < LOCAL_RETRY_COOLDOWN_MS) {
            Log.w(TAG, "Local cooldown active. Ignoring request.")
            return
        }
        
        detectBrowserInfo(activity)
        Log.d(TAG, "Browser detected: $browserInfo")
        
        val formattedNumber = formatPhoneNumber(phoneNumber)
        
        if (verificationInProgress) {
            Log.d(TAG, "Verification already in progress, ignoring request")
            return
        }
        
        if (!validatePhoneNumber(formattedNumber)) {
            uiStateFlow.value = PhoneAuthState.Error(
                "Please enter a valid phone number with +63 prefix"
            )
            return
        }

        uiStateFlow.value = PhoneAuthState.Loading
        verificationInProgress = true

        try {
            val timeoutSeconds = if (browserInfo.contains("Safari", ignoreCase = true) && 
                                    !browserInfo.contains("Chrome", ignoreCase = true)) {
                180L
            } else {
                120L
            }
            
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d(TAG, "Verification request sent with timeout of $timeoutSeconds seconds")
            lastAttemptTimestamp = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting verification", e)
            verificationInProgress = false
            uiStateFlow.value = PhoneAuthState.Error("Failed to start verification: ${e.message}")
        }
    }
    
    private fun detectBrowserInfo(activity: Activity) {
        try {
            val webView = WebView(activity)
            val userAgent = webView.settings.userAgentString ?: "Unknown"
            browserInfo = when {
                userAgent.contains("Chrome", ignoreCase = true) && 
                !userAgent.contains("Edg", ignoreCase = true) -> "Chrome"
                userAgent.contains("Firefox", ignoreCase = true) -> "Firefox"
                userAgent.contains("Safari", ignoreCase = true) && 
                !userAgent.contains("Chrome", ignoreCase = true) -> "Safari"
                userAgent.contains("Edg", ignoreCase = true) -> "Edge"
                userAgent.contains("OPR", ignoreCase = true) || 
                userAgent.contains("Opera", ignoreCase = true) -> "Opera"
                userAgent.contains("SamsungBrowser", ignoreCase = true) -> "Samsung Browser"
                else -> "Unknown: $userAgent"
            }
            
            browserInfo += " on Android ${Build.VERSION.RELEASE}"
            
            webView.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting browser info", e)
            browserInfo = "Detection failed: ${e.message}"
        }
    }
    
    private fun formatPhoneNumber(phoneNumber: String): String {
        var cleaned = phoneNumber.replace(Regex("[^\\d+]"), "")
        
        if (!cleaned.startsWith("+")) {
            if (cleaned.startsWith("63")) {
                cleaned = "+$cleaned"
            } else if (cleaned.startsWith("0")) {
                cleaned = "+63${cleaned.substring(1)}"
            } else {
                cleaned = "+63$cleaned"
            }
        }
        
        return cleaned
    }

    fun verifyCode(code: String) {
        Log.d(TAG, "Verifying code on $browserInfo")
        
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
        Log.d(TAG, "Updating phone credential on $browserInfo")
        
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
        return phoneNumber.startsWith("+") && 
               (phoneNumber.length >= 12 && phoneNumber.length <= 13) &&
               phoneNumber.substring(1).all { it.isDigit() }
    }

    fun reset() {
        Log.d(TAG, "Reset verification state")
        storedVerificationId = null
        verificationInProgress = false
        uiStateFlow.value = PhoneAuthState.Initial
    }
} 