package com.example.bikerental.utils

import android.content.Context
import android.app.Application
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class to handle reCAPTCHA Enterprise functionality
 */
object RecaptchaHelper {
    private const val TAG = "RecaptchaHelper"
    @Volatile
    private var recaptchaClient: RecaptchaClient? = null
    private const val RECAPTCHA_TIMEOUT_MS = 10000L // 10 seconds
    
    /**
     * Initialize reCAPTCHA to avoid errors in logs
     */
    suspend fun initializeRecaptcha(context: Context, siteKey: String) {
        if (recaptchaClient != null) return
        
        try {
            withContext(Dispatchers.IO) {
                withTimeout(RECAPTCHA_TIMEOUT_MS) {
                    // The reCAPTCHA API requires an Application instance
                    val application = when {
                        context is Application -> context
                        context.applicationContext is Application -> context.applicationContext as Application
                        else -> {
                            LogManager.e(TAG, "Context is not an Application instance")
                            return@withTimeout
                        }
                    }
                    
                    // Use getClient with await() since fetchClient may not be available
                    Recaptcha.getClient(application, siteKey)
                        .onSuccess { client ->
                            recaptchaClient = client
                            LogManager.d(TAG, "reCAPTCHA client initialized successfully")
                        }
                        .onFailure { exception ->
                            LogManager.w(TAG, "Failed to initialize reCAPTCHA client", exception)
                        }
                }
            }
        } catch (e: Exception) {
            // Log but don't crash - app can function without reCAPTCHA
            LogManager.w(TAG, "Failed to initialize reCAPTCHA client", e)
        }
    }
    
    /**
     * Execute reCAPTCHA check for a specific action with error handling
     * @return Token or null if error
     */
    suspend fun executeReCaptchaCheck(action: String): String? {
        val client = recaptchaClient ?: return null
        
        return try {
            withContext(Dispatchers.IO) {
                withTimeout(RECAPTCHA_TIMEOUT_MS) {
                    val recaptchaAction = when (action) {
                        "login" -> RecaptchaAction.LOGIN
                        "signup" -> RecaptchaAction.SIGNUP
                        else -> RecaptchaAction.custom(action)
                    }
                    
                    // Execute and handle Result object
                    client.execute(recaptchaAction).getOrNull()
                }
            }
        } catch (e: ApiException) {
            // If we get a "Too many attempts" error, back off
            if (e.message?.contains("Too many attempts") == true) {
                LogManager.w(TAG, "reCAPTCHA rate limited, backing off", e)
                // Sleep to prevent immediate retry
                delay(5000)
            } else {
                LogManager.e(TAG, "Error executing reCAPTCHA check", e)
            }
            null
        } catch (e: Exception) {
            LogManager.e(TAG, "Error executing reCAPTCHA check", e)
            null
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        recaptchaClient = null
    }
} 