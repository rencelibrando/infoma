package com.example.bikerental.utils

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

object ErrorHandler {
    
    fun getErrorMessage(throwable: Throwable): String {
        Log.e("ErrorHandler", "Handling error: ${throwable.message}", throwable)
        
        return when (throwable) {
            is FirebaseAuthException -> {
                when (throwable.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "User account not found"
                    "ERROR_WRONG_PASSWORD" -> "Invalid password"
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection"
                    else -> "Authentication error: ${throwable.message}"
                }
            }
            
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                        "Permission denied. Please log in again"
                    FirebaseFirestoreException.Code.UNAVAILABLE -> 
                        "Service temporarily unavailable. Please try again"
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> 
                        "Request timed out. Please try again"
                    else -> "Database error: ${throwable.message}"
                }
            }
            
            is FirebaseException -> {
                "Firebase error: ${throwable.message}"
            }
            
            else -> {
                when {
                    throwable.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timed out. Please try again"
                    throwable.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection"
                    throwable.message?.contains("permission", ignoreCase = true) == true ->
                        "Permission denied. Please log in again"
                    else -> throwable.message ?: "An unexpected error occurred"
                }
            }
        }
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }
} 