package com.example.bikerental.utils

import android.util.Log
import org.json.JSONObject
import java.security.SecureRandom
import java.util.*

/**
 * OPTIMIZED: Enhanced QR Code helper with improved validation and performance
 * Supports both JSON and string formats with secure generation
 */
object QRCodeHelper {
    private const val TAG = "QRCodeHelper"
    
    // Characters for secure QR code generation (excluding confusing characters)
    private const val SECURE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val secureRandom = SecureRandom()
    
    // OPTIMIZED: Compiled regex patterns for better performance
    private val BIKE_CODE_REGEX = Regex("^BIKE-[A-Z0-9]{4,8}$")
    private val COLON_FORMAT_REGEX = Regex("^.+:.{6,}$")
    private val BIKE_PREFIX_REGEX = Regex("^bike_.{1,}$")
    
    /**
     * OPTIMIZED: Generate a secure, unique QR code for a bike
     * Format: BIKE-XXXX where X is a secure random character
     */
    fun generateQRCode(bikeId: String): String {
        val code = generateSecureCode(8)
        return "BIKE-$code"
    }
    
    /**
     * OPTIMIZED: Generate cryptographically secure random code
     */
    private fun generateSecureCode(length: Int): String {
        return (1..length)
            .map { SECURE_CHARS[secureRandom.nextInt(SECURE_CHARS.length)] }
            .joinToString("")
    }
    
    /**
     * OPTIMIZED: Enhanced QR code format validation with better performance
     * Supports both JSON from admin dashboard and simple strings
     */
    fun isValidQRCodeFormat(qrCode: String): Boolean {
        if (qrCode.isBlank()) return false
        
        return when {
            // Check if it's JSON format from admin dashboard
            isJsonFormat(qrCode) -> validateJsonFormat(qrCode)
            
            // Check standard formats using pre-compiled regex
            BIKE_CODE_REGEX.matches(qrCode) -> true  // New secure format
            COLON_FORMAT_REGEX.matches(qrCode) -> true  // Legacy colon format
            BIKE_PREFIX_REGEX.matches(qrCode) -> true  // Legacy bike prefix
            qrCode.length >= 6 -> true  // Generic fallback
            
            else -> false
        }
    }
    
    /**
     * OPTIMIZED: Fast JSON format detection
     */
    private fun isJsonFormat(qrCode: String): Boolean {
        val trimmed = qrCode.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }
    
    /**
     * OPTIMIZED: Validate JSON format QR codes
     */
    private fun validateJsonFormat(qrCode: String): Boolean {
        return try {
            val json = JSONObject(qrCode)
            json.has("hardwareId") || json.has("qrCode") || json.has("bikeId")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * OPTIMIZED: Extract bike identifier from QR code with improved error handling
     * Supports multiple formats including JSON from admin dashboard
     */
    fun extractBikeIdentifierFromQRCode(qrCode: String): String? {
        return try {
            when {
                // Handle JSON format from admin dashboard
                isJsonFormat(qrCode) -> extractFromJson(qrCode)
                
                // Handle colon-separated format
                qrCode.contains(":") -> qrCode.split(":").firstOrNull()?.takeIf { it.isNotBlank() }
                
                // Handle direct formats (bike_ prefix or direct codes)
                else -> qrCode.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting bike identifier from QR code", e)
            null
        }
    }
    
    /**
     * OPTIMIZED: Extract identifier from JSON format
     */
    private fun extractFromJson(qrCode: String): String? {
        return try {
            val json = JSONObject(qrCode)
            when {
                json.has("hardwareId") -> json.getString("hardwareId")
                json.has("qrCode") -> json.getString("qrCode")
                json.has("bikeId") -> json.getString("bikeId")
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON QR code", e)
            null
        }
    }
    
    /**
     * OPTIMIZED: Enhanced QR code validation against a specific bike
     * Provides additional security layer
     */
    fun validateQRCodeForBike(qrCode: String, bikeQRCode: String, bikeHardwareId: String): Boolean {
        val extractedId = extractBikeIdentifierFromQRCode(qrCode) ?: return false
        
        return extractedId == bikeQRCode || extractedId == bikeHardwareId
    }
    
    /**
     * OPTIMIZED: Check if two QR codes are equivalent
     * Handles different format representations of the same bike
     */
    fun areQRCodesEquivalent(qrCode1: String, qrCode2: String): Boolean {
        val id1 = extractBikeIdentifierFromQRCode(qrCode1)
        val id2 = extractBikeIdentifierFromQRCode(qrCode2)
        return id1 != null && id2 != null && id1 == id2
    }
    
    /**
     * OPTIMIZED: Generate sample QR codes for testing and development
     * Returns pairs of (bikeId, qrCode)
     */
    fun generateSampleQRCodes(): List<Pair<String, String>> {
        return (1..5).map { index ->
            val bikeId = "bike_%03d".format(index)
            bikeId to generateQRCode(bikeId)
        }
    }
    
    /**
     * Generate a hardware ID for a bike
     * This creates a unique identifier that can be used for QR codes
     */
    fun generateHardwareId(bikeId: String): String {
        return generateQRCode(bikeId)
    }
    
    /**
     * Generate a unique hardware ID with additional entropy to avoid duplicates
     * This version includes timestamp and bike ID to ensure uniqueness
     */
    fun generateUniqueHardwareId(bikeId: String): String {
        val timestamp = System.currentTimeMillis().toString().takeLast(6)
        val bikeIdHash = bikeId.hashCode().toString().takeLast(4).replace("-", "")
        val randomCode = generateSecureCode(4)
        return "BIKE-$bikeIdHash$timestamp$randomCode"
    }
    
    /**
     * Generate sample hardware IDs for testing
     * Returns a map of bikeId to hardwareId
     */
    fun generateSampleHardwareIds(): Map<String, String> {
        return (1..5).associate { index ->
            val bikeId = "bike_%03d".format(index)
            bikeId to generateUniqueHardwareId(bikeId)
        }
    }
} 