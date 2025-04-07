package com.example.bikerental.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.webkit.WebSettings
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

private const val TAG = "PhoneVerification"

class PhoneVerificationManager(
    private val uiStateFlow: MutableStateFlow<PhoneAuthState>
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verificationInProgress = false
    
    // Track retry attempts
    private var retryCount = 0
    private val MAX_RETRIES = 6
    
    // Add a time tracking variable for rate limiting
    private var lastVerificationAttempt = 0L
    private val RATE_LIMIT_DURATION = 180000L // Exactly 3 minutes in milliseconds
    
    // Store browser info for better error handling
    private var browserInfo: String = "Unknown"
    
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d(TAG, "Verification completed automatically")
            verificationInProgress = false
            retryCount = 0
            updatePhoneCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "Verification failed on $browserInfo", e)
            verificationInProgress = false
            
            val errorMessage = when {
                // Add the status code 17010 check for device-level blocks
                e.message?.contains("17010") == true ||
                e.message?.contains("blocked all requests from this device") == true ||
                e.message?.contains("unusual activity") == true -> {
                    // This is a more severe device-level block, not just a temporary rate limit
                    val currentTime = System.currentTimeMillis()
                    // For device-level blocks, use a longer backoff period (24 hours)
                    val backoffDuration = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
                    
                    // Store this as a device-level block in Firestore
                    storeDeviceLevelBlock(currentTime, backoffDuration)
                    
                    val expiryTime = currentTime + backoffDuration
                    
                    // Set a longer display message for device-level blocks
                    uiStateFlow.value = PhoneAuthState.RateLimited(
                        expiryTime, 
                        "24 hours",
                        isServerBased = true,
                        isDeviceBlock = true
                    )
                    return
                }
                e.message?.contains("TOO_MANY_REQUESTS") == true || 
                e.message?.contains("quota_exceeded") == true ||
                e.message?.contains("rate limit") == true ||
                e is FirebaseTooManyRequestsException -> {
                    // Handle regular rate limiting with adaptive timeout
                    val currentTime = System.currentTimeMillis()
                    // Longer backoff for Chrome-based browsers (which tend to have stricter security)
                    val backoffDuration = if (browserInfo.contains("Chrome", ignoreCase = true)) {
                        RATE_LIMIT_DURATION * 2
                    } else {
                        RATE_LIMIT_DURATION
                    }
                    
                    // Store rate limit timestamp in Firestore for this device/user
                    storeRateLimitTimestamp(currentTime, backoffDuration)
                    
                    lastVerificationAttempt = currentTime
                    val expiryTime = currentTime + backoffDuration
                    
                    // Always display exactly 3 minutes (or 6 for Chrome) as initial message
                    val minutesDisplay = if (browserInfo.contains("Chrome", ignoreCase = true)) {
                        "6 minutes"
                    } else {
                        "3 minutes"
                    }
                    
                    uiStateFlow.value = PhoneAuthState.RateLimited(expiryTime, minutesDisplay)
                    return
                }
                e.message?.contains("INVALID_APP_CREDENTIAL") == true ||
                e.message?.contains("Error: [auth/invalid-app-credential]") == true ||
                e.message?.contains("Error: 39") == true ||
                e.message?.contains("reCAPTCHA") == true -> {
                    // reCAPTCHA verification issue - highly browser-dependent
                    if (retryCount < MAX_RETRIES) {
                        retryCount++
                        // Different handling for Safari which often has reCAPTCHA issues
                        if (browserInfo.contains("Safari", ignoreCase = true) && 
                            !browserInfo.contains("Chrome", ignoreCase = true)) {
                            "reCAPTCHA verification failed on Safari. Please try a different browser or retry."
                        } else {
                            uiStateFlow.value = PhoneAuthState.RecaptchaError
                            return
                        }
                    } else {
                        "reCAPTCHA verification failed after multiple attempts. Please try again later or use a different browser."
                    }
                }
                e is FirebaseAuthInvalidCredentialsException -> {
                    if (browserInfo.contains("Firefox", ignoreCase = true)) {
                        "Invalid phone number format. Please use the international format (e.g., +63XXXXXXXXXX)"
                    } else {
                        "Invalid phone number format. Please use +63 format."
                    }
                }
                e.message?.contains("network") == true -> {
                    "Network error. Please check your internet connection and try again."
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

    // Store rate limit timestamp in Firestore
    private fun storeRateLimitTimestamp(timestamp: Long, duration: Long) {
        val expiryTime = timestamp + duration
        
        try {
            // Get device ID for device-specific rate limiting
            val deviceId = android.provider.Settings.Secure.getString(
                auth.app.applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            val rateLimitData = hashMapOf(
                "timestamp" to timestamp,
                "expiryTime" to expiryTime,
                "deviceId" to deviceId,
                "browserInfo" to browserInfo
            )
            
            // Store under current user if logged in
            auth.currentUser?.uid?.let { userId ->
                firestore.collection("rateLimits")
                    .document(userId)
                    .set(rateLimitData)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to store rate limit data", e)
                    }
            } ?: run {
                // If no user is logged in, store using device ID
                firestore.collection("rateLimits")
                    .document(deviceId)
                    .set(rateLimitData)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to store rate limit data for device", e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing rate limit data", e)
        }
    }
    
    // Check if the device or user is rate limited
    private suspend fun checkRateLimit(): Pair<Boolean, Long> {
        return try {
            val deviceId = android.provider.Settings.Secure.getString(
                auth.app.applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            // Try to get rate limit by user ID first if logged in
            val userId = auth.currentUser?.uid
            val document = withContext(Dispatchers.IO) {
                if (userId != null) {
                    firestore.collection("rateLimits").document(userId).get().await()
                } else {
                    firestore.collection("rateLimits").document(deviceId).get().await()
                }
            }
            
            if (document.exists()) {
                val expiryTime = document.getLong("expiryTime") ?: 0L
                val currentTime = System.currentTimeMillis()
                
                if (currentTime < expiryTime) {
                    // User is still rate limited
                    Pair(true, expiryTime)
                } else {
                    // Rate limit has expired, clean up the document
                    withContext(Dispatchers.IO) {
                        if (userId != null) {
                            firestore.collection("rateLimits").document(userId).delete().await()
                        } else {
                            firestore.collection("rateLimits").document(deviceId).delete().await()
                        }
                    }
                    Pair(false, 0L)
                }
            } else {
                Pair(false, 0L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking rate limit", e)
            Pair(false, 0L)
        }
    }

    suspend fun startVerification(phoneNumber: String, activity: Activity, senderName: String = "GearTick") {
        Log.d(TAG, "Starting verification for $phoneNumber")
        
        // Detect browser characteristics
        detectBrowserInfo(activity)
        Log.d(TAG, "Browser detected: $browserInfo")
        
        // Format phone number if needed
        val formattedNumber = formatPhoneNumber(phoneNumber)
        
        // Check for device-level blocks first
        val isDeviceBlocked = checkDeviceBlock()
        if (isDeviceBlocked.first) {
            // Use the actual expiry time from the device block
            uiStateFlow.value = PhoneAuthState.RateLimited(
                isDeviceBlocked.second,
                "24 hours",
                isServerBased = true,
                isDeviceBlock = true
            )
            return
        }
        
        // Check regular rate limit from Firestore
        val (isRateLimited, expiryTime) = checkRateLimit()
        
        if (isRateLimited) {
            // Calculate display duration - always show 3 minutes initially
            val displayDuration = "3 minutes"
            uiStateFlow.value = PhoneAuthState.RateLimited(expiryTime, displayDuration)
            return
        }
        
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
            // Adjust timeout based on browser - Safari needs more time for reCAPTCHA
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
        } catch (e: Exception) {
            Log.e(TAG, "Error starting verification", e)
            verificationInProgress = false
            uiStateFlow.value = PhoneAuthState.Error("Failed to start verification: ${e.message}")
        }
    }
    
    // Detect browser information for better error handling
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
            
            // Add Android version info
            browserInfo += " on Android ${Build.VERSION.RELEASE}"
            
            // Clean up to avoid memory leaks
            webView.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting browser info", e)
            browserInfo = "Detection failed: ${e.message}"
        }
    }
    
    // Format phone number to ensure consistent format
    private fun formatPhoneNumber(phoneNumber: String): String {
        // Strip all non-digit characters except +
        var cleaned = phoneNumber.replace(Regex("[^\\d+]"), "")
        
        // Ensure it has the country code
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
    
    // Use this for direct retry without reCAPTCHA when error 39 occurs
    suspend fun retryWithoutRecaptcha(phoneNumber: String, activity: Activity) {
        Log.d(TAG, "Retrying verification without reCAPTCHA on $browserInfo")
        
        if (verificationInProgress) {
            return
        }
        
        // Check rate limit from Firestore first
        val (isRateLimited, expiryTime) = checkRateLimit()
        
        if (isRateLimited) {
            // Calculate display duration - always show 3 minutes initially
            val displayDuration = "3 minutes"
            uiStateFlow.value = PhoneAuthState.RateLimited(expiryTime, displayDuration)
            return
        }
        
        // Format phone number for consistency
        val formattedNumber = formatPhoneNumber(phoneNumber)
        
        uiStateFlow.value = PhoneAuthState.Loading
        verificationInProgress = true
        
        try {
            // Try to use the resend token if available
            if (resendToken != null) {
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(formattedNumber)
                    .setTimeout(120L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .setForceResendingToken(resendToken!!)
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                Log.d(TAG, "Retry with resend token sent")
            } else {
                // Fall back to regular verification with longer timeout
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(formattedNumber)
                    .setTimeout(180L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                Log.d(TAG, "Retry without resend token sent with longer timeout")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during retry", e)
            verificationInProgress = false
            uiStateFlow.value = PhoneAuthState.Error("Retry failed: ${e.message}")
        }
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
        // Check that the number starts with +63 and has 12-13 characters total
        return phoneNumber.startsWith("+") && 
               (phoneNumber.length >= 12 && phoneNumber.length <= 13) &&
               phoneNumber.substring(1).all { it.isDigit() }
    }

    fun reset() {
        Log.d(TAG, "Reset verification state")
        storedVerificationId = null
        retryCount = 0
        verificationInProgress = false
        // Keep the resendToken in case we need it
        uiStateFlow.value = PhoneAuthState.Initial
    }

    // Add a public method to check rate limit status without attempting verification
    suspend fun checkRateLimitStatus(): Pair<Boolean, Long> {
        return checkRateLimit()
    }
    
    // Add a method to clean up expired rate limits
    suspend fun cleanupExpiredRateLimits() {
        try {
            val deviceId = android.provider.Settings.Secure.getString(
                auth.app.applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            val userId = auth.currentUser?.uid
            val document = withContext(Dispatchers.IO) {
                if (userId != null) {
                    firestore.collection("rateLimits").document(userId).get().await()
                } else {
                    firestore.collection("rateLimits").document(deviceId).get().await()
                }
            }
            
            if (document.exists()) {
                val expiryTime = document.getLong("expiryTime") ?: 0L
                val currentTime = System.currentTimeMillis()
                
                if (currentTime >= expiryTime) {
                    // Rate limit has expired, clean up the document
                    withContext(Dispatchers.IO) {
                        if (userId != null) {
                            firestore.collection("rateLimits").document(userId).delete().await()
                        } else {
                            firestore.collection("rateLimits").document(deviceId).delete().await()
                        }
                        Log.d(TAG, "Cleaned up expired rate limit")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up rate limits", e)
        }
    }

    // Add a method to store device-level blocks
    private fun storeDeviceLevelBlock(timestamp: Long, duration: Long) {
        val expiryTime = timestamp + duration
        
        try {
            // Get device ID for device-specific blocking
            val deviceId = android.provider.Settings.Secure.getString(
                auth.app.applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            val blockData = hashMapOf(
                "timestamp" to timestamp,
                "expiryTime" to expiryTime,
                "deviceId" to deviceId,
                "browserInfo" to browserInfo,
                "isDeviceBlock" to true,
                "reason" to "Too many verification attempts - device level block"
            )
            
            // Store under device block collection
            firestore.collection("deviceBlocks")
                .document(deviceId)
                .set(blockData)
                .addOnSuccessListener {
                    Log.d(TAG, "Device block record saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to store device block data", e)
                }
            
            // Also store under user if logged in
            auth.currentUser?.uid?.let { userId ->
                firestore.collection("userBlocks")
                    .document(userId)
                    .set(blockData)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to store user block data", e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing device block data", e)
        }
    }

    // Add a method to check for device-level blocks
    private suspend fun checkDeviceBlock(): Pair<Boolean, Long> {
        return try {
            val deviceId = android.provider.Settings.Secure.getString(
                auth.app.applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            // Check device blocks collection
            val document = withContext(Dispatchers.IO) {
                firestore.collection("deviceBlocks").document(deviceId).get().await()
            }
            
            if (document.exists()) {
                val expiryTime = document.getLong("expiryTime") ?: 0L
                val currentTime = System.currentTimeMillis()
                
                if (currentTime < expiryTime) {
                    // Device is still blocked
                    Log.d(TAG, "Device is blocked until ${Date(expiryTime)}")
                    Pair(true, expiryTime)
                } else {
                    // Block has expired, clean up the document
                    withContext(Dispatchers.IO) {
                        firestore.collection("deviceBlocks").document(deviceId).delete().await()
                    }
                    Pair(false, 0L)
                }
            } else {
                // Also check user blocks if user is logged in
                auth.currentUser?.uid?.let { userId ->
                    val userBlockDoc = withContext(Dispatchers.IO) {
                        firestore.collection("userBlocks").document(userId).get().await()
                    }
                    
                    if (userBlockDoc.exists()) {
                        val expiryTime = userBlockDoc.getLong("expiryTime") ?: 0L
                        val currentTime = System.currentTimeMillis()
                        
                        if (currentTime < expiryTime) {
                            // User is blocked
                            return Pair(true, expiryTime)
                        } else {
                            // Block has expired, clean up
                            withContext(Dispatchers.IO) {
                                firestore.collection("userBlocks").document(userId).delete().await()
                            }
                        }
                    }
                }
                
                Pair(false, 0L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device block", e)
            Pair(false, 0L)
        }
    }

    // Method to check if device is still blocked
    suspend fun checkIfDeviceStillBlocked(): Boolean {
        val blockInfo = getDeviceBlockInfo()
        return blockInfo.first
    }

    // Method to get device block information
    suspend fun getDeviceBlockInfo(): Pair<Boolean, Long> {
        return try {
            val deviceId = android.provider.Settings.Secure.getString(
                auth.app.applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            // Check device blocks collection
            val document = withContext(Dispatchers.IO) {
                firestore.collection("deviceBlocks").document(deviceId).get().await()
            }
            
            if (document.exists()) {
                val expiryTime = document.getLong("expiryTime") ?: 0L
                val currentTime = System.currentTimeMillis()
                
                if (currentTime < expiryTime) {
                    // Device is still blocked
                    Log.d(TAG, "Device block info: blocked until ${Date(expiryTime)}")
                    Pair(true, expiryTime)
                } else {
                    // Block has expired, clean up the document
                    withContext(Dispatchers.IO) {
                        firestore.collection("deviceBlocks").document(deviceId).delete().await()
                    }
                    Pair(false, 0L)
                }
            } else {
                // Also check user blocks if user is logged in
                auth.currentUser?.uid?.let { userId ->
                    val userBlockDoc = withContext(Dispatchers.IO) {
                        firestore.collection("userBlocks").document(userId).get().await()
                    }
                    
                    if (userBlockDoc.exists()) {
                        val expiryTime = userBlockDoc.getLong("expiryTime") ?: 0L
                        val currentTime = System.currentTimeMillis()
                        
                        if (currentTime < expiryTime) {
                            // User is blocked
                            return Pair(true, expiryTime)
                        } else {
                            // Block has expired, clean up
                            withContext(Dispatchers.IO) {
                                firestore.collection("userBlocks").document(userId).delete().await()
                            }
                        }
                    }
                }
                
                Pair(false, 0L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device block info", e)
            Pair(false, 0L)
        }
    }
} 