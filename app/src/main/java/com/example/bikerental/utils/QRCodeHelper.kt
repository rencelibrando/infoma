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
     * ENHANCED: QR code format validation with detailed logging
     */
    fun isValidQRCodeFormat(qrCode: String): Boolean {
        if (qrCode.isBlank()) {
            Log.d(TAG, "QR code validation failed: Empty or blank")
            return false
        }
        
        Log.d(TAG, "Validating QR code format: '${qrCode.take(50)}...'")
        
        val isValid = when {
            // Check if it's JSON format from admin dashboard
            isJsonFormat(qrCode) -> {
                Log.d(TAG, "Detected JSON format QR code")
                validateJsonFormat(qrCode)
            }
            
            // Check standard formats using pre-compiled regex
            BIKE_CODE_REGEX.matches(qrCode) -> {
                Log.d(TAG, "Matched BIKE-XXXX format")
                true
            }
            
            COLON_FORMAT_REGEX.matches(qrCode) -> {
                Log.d(TAG, "Matched colon-separated format")
                true
            }
            
            BIKE_PREFIX_REGEX.matches(qrCode) -> {
                Log.d(TAG, "Matched bike_ prefix format")
                true
            }
            
            qrCode.length >= 6 -> {
                Log.d(TAG, "Accepted as generic format (length >= 6)")
                true
            }
            
            else -> {
                Log.d(TAG, "QR code format not recognized")
                false
            }
        }
        
        Log.d(TAG, "QR code validation result: $isValid")
        return isValid
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
            val hasValidField = json.has("hardwareId") || json.has("qrCode") || json.has("bikeId")
            Log.d(TAG, "JSON QR code has valid field: $hasValidField")
            hasValidField
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON QR code: ${e.message}")
            false
        }
    }
    
    /**
     * ENHANCED: Extract bike identifier from QR code with detailed logging
     */
    fun extractBikeIdentifierFromQRCode(qrCode: String): String? {
        Log.d(TAG, "Extracting bike identifier from QR code: '${qrCode.take(50)}...'")
        
        return try {
            val identifier = when {
                // Handle JSON format from admin dashboard
                isJsonFormat(qrCode) -> {
                    Log.d(TAG, "Processing JSON format QR code")
                    extractFromJson(qrCode)
                }
                
                // Handle colon-separated format
                qrCode.contains(":") -> {
                    Log.d(TAG, "Processing colon-separated format")
                    val parts = qrCode.split(":")
                    Log.d(TAG, "Split into ${parts.size} parts: ${parts.joinToString(", ")}")
                    parts.firstOrNull()?.takeIf { it.isNotBlank() }
                }
                
                // Handle direct formats (bike_ prefix or direct codes)
                else -> {
                    Log.d(TAG, "Using QR code directly as identifier")
                    qrCode.takeIf { it.isNotBlank() }
                }
            }
            
            Log.d(TAG, "Extracted bike identifier: '$identifier'")
            identifier
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
            val identifier = when {
                json.has("hardwareId") -> {
                    Log.d(TAG, "Found hardwareId in JSON")
                    json.getString("hardwareId")
                }
                json.has("qrCode") -> {
                    Log.d(TAG, "Found qrCode field in JSON")
                    json.getString("qrCode")
                }
                json.has("bikeId") -> {
                    Log.d(TAG, "Found bikeId in JSON")
                    json.getString("bikeId")
                }
                else -> {
                    Log.w(TAG, "No recognized identifier field in JSON")
                    null
                }
            }
            
            Log.d(TAG, "Extracted from JSON: '$identifier'")
            identifier
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON QR code", e)
            null
        }
    }
    
    /**
     * ENHANCED: QR code validation against a specific bike with detailed logging
     */
    fun validateQRCodeForBike(qrCode: String, bikeQRCode: String, bikeHardwareId: String): Boolean {
        Log.d(TAG, "Validating QR code against bike:")
        Log.d(TAG, "  Scanned QR: '${qrCode.take(50)}...'")
        Log.d(TAG, "  Bike QR Code: '$bikeQRCode'")
        Log.d(TAG, "  Bike Hardware ID: '$bikeHardwareId'")
        
        val extractedId = extractBikeIdentifierFromQRCode(qrCode)
        if (extractedId == null) {
            Log.w(TAG, "Failed to extract identifier from QR code")
            return false
        }
        
        val isValid = extractedId == bikeQRCode || extractedId == bikeHardwareId
        Log.d(TAG, "QR code validation result: $isValid")
        
        if (!isValid) {
            Log.w(TAG, "QR code mismatch:")
            Log.w(TAG, "  Extracted: '$extractedId'")
            Log.w(TAG, "  Expected: '$bikeQRCode' OR '$bikeHardwareId'")
        }
        
        return isValid
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
        Log.d(TAG, "Generating sample QR codes for testing")
        val samples = (1..5).map { index ->
            val bikeId = "bike_%03d".format(index)
            val qrCode = generateQRCode(bikeId)
            Log.d(TAG, "Generated sample: $bikeId -> $qrCode")
            bikeId to qrCode
        }
        return samples
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
    
    /**
     * Debug method to log QR code processing details
     */
    fun debugQRCode(qrCode: String): String {
        val debug = StringBuilder()
        debug.appendLine("=== QR Code Debug Information ===")
        debug.appendLine("Raw QR Code: '$qrCode'")
        debug.appendLine("Length: ${qrCode.length}")
        debug.appendLine("Is JSON: ${isJsonFormat(qrCode)}")
        debug.appendLine("Is Valid Format: ${isValidQRCodeFormat(qrCode)}")
        debug.appendLine("Extracted ID: '${extractBikeIdentifierFromQRCode(qrCode)}'")
        debug.appendLine("================================")
        
        val debugString = debug.toString()
        Log.d(TAG, debugString)
        return debugString
    }
} 