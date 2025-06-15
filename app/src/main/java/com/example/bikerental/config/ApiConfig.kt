package com.example.bikerental.config

import android.content.Context
import android.util.Log
import com.example.bikerental.BuildConfig

/**
 * Central configuration class for managing API keys and other sensitive configuration
 * This avoids hardcoding keys in multiple places and provides a single point for key management
 */
object ApiConfig {
    private const val TAG = "ApiConfig"
    
    // Cache for API keys to avoid repeated resource lookups
    private val apiKeyCache = mutableMapOf<String, String?>()
    
    /**
     * Get the Google Maps API key
     * @param context Application context
     * @return The API key or null if not found
     */
    fun getMapsApiKey(context: Context): String? {
        return getApiKey(context, "maps_api_key", "google_maps_key")
    }
    
    /**
     * Get the Directions API key (same as Maps API key in this case)
     * @param context Application context
     * @return The API key or null if not found
     */
    fun getDirectionsApiKey(context: Context): String? {
        return getMapsApiKey(context)
    }
    
    /**
     * Generic method to get any API key from string resources
     * @param context Application context
     * @param cacheKey Key to use for caching
     * @param resourceName Name of the string resource
     * @return The API key or null if not found
     */
    private fun getApiKey(context: Context, cacheKey: String, resourceName: String): String? {
        // Check cache first
        apiKeyCache[cacheKey]?.let { return it }
        
        return try {
            // Get resource ID dynamically
            val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
            if (resourceId == 0) {
                Log.e(TAG, "Resource not found: $resourceName")
                return null
            }
            
            // Get key from resources
            val apiKey = context.getString(resourceId)
            
            // Cache the key
            apiKeyCache[cacheKey] = apiKey
            
            // Log masked key in debug mode
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Retrieved API key for $cacheKey: ${maskApiKey(apiKey)}")
            }
            
            apiKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get API key for $cacheKey", e)
            null
        }
    }
    
    /**
     * Mask API key for logging purposes
     */
    fun maskApiKey(key: String?): String {
        if (key == null) return "null"
        return if (key.length > 8) {
            key.substring(0, 4) + "..." + key.substring(key.length - 4)
        } else {
            "***"
        }
    }
    
    /**
     * Check if an API key is valid (non-empty and not a placeholder)
     */
    fun isValidApiKey(apiKey: String?): Boolean {
        if (apiKey.isNullOrEmpty()) return false
        
        // Check for common placeholder patterns
        val placeholderPatterns = listOf(
            "YOUR_API_KEY",
            "API_KEY",
            "ENTER_KEY_HERE",
            "REPLACE_WITH",
            "PLACEHOLDER"
        )
        
        return placeholderPatterns.none { apiKey.contains(it, ignoreCase = true) }
    }
} 