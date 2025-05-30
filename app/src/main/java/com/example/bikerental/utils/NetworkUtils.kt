package com.example.bikerental.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseNetworkException
import com.example.bikerental.utils.LogManager
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Utility class to handle network operations with retries
 */
object NetworkUtils {
    private val TAG = "NetworkUtils"
    
    /**
     * Executes a suspending operation with retry logic using exponential backoff
     * 
     * @param maxAttempts Maximum number of attempts (defaults to 3)
     * @param initialDelayMs Initial delay in milliseconds before first retry (defaults to 1000ms)
     * @param maxDelayMs Maximum delay between retries in milliseconds (defaults to 5000ms)
     * @param factor Exponential backoff factor (defaults to 2.0)
     * @param retryOnNetworkException Only retry on network-related exceptions (defaults to true)
     * @param block The suspending function to execute with retries
     * @return The result of the suspending function
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 5000,
        factor: Double = 2.0,
        retryOnNetworkException: Boolean = true,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                
                val shouldRetry = when {
                    attempt + 1 >= maxAttempts -> false
                    retryOnNetworkException -> isNetworkRelated(e)
                    else -> true
                }
                
                if (!shouldRetry) throw e
                
                LogManager.w(TAG, "Operation failed (attempt ${attempt + 1}/$maxAttempts), retrying in ${currentDelay}ms: ${e.message}")
                
                // Delay using exponential backoff
                kotlinx.coroutines.delay(currentDelay)
                
                // Calculate next delay with exponential backoff, capped at maxDelayMs
                currentDelay = min(currentDelay * factor.toFloat().toDouble(), maxDelayMs.toDouble()).toLong()
            }
        }
        
        // If we've exhausted all attempts, throw the last exception
        throw lastException ?: RuntimeException("Unknown error in withRetry after $maxAttempts attempts")
    }
    
    /**
     * Check if the exception is related to network issues
     */
    private fun isNetworkRelated(e: Exception): Boolean {
        return e is FirebaseNetworkException || 
               e.cause is FirebaseNetworkException ||
               e.message?.contains("network", ignoreCase = true) == true ||
               e.message?.contains("timeout", ignoreCase = true) == true ||
               e.message?.contains("connection", ignoreCase = true) == true
    }
    
    /**
     * Check if the device has an active network connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
} 