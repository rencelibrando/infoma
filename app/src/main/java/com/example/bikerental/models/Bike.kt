package com.example.bikerental.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.Timestamp

/**
 * Unified Bike model that combines the features of both simple Bike and TrackableBike models.
 * This provides a single source of truth for bike data throughout the app.
 * All fields are properly configured for Firestore compatibility.
 */
@IgnoreExtraProperties
data class Bike(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    // Support different price formats (String for display, Double for calculations)
    var price: String = "",
    var priceValue: Double = 0.0,
    // Support both types of image references
    var imageUrl: String = "",
    var imageRes: Int = 0,
    // Support for multiple images
    var imageUrls: List<String> = emptyList(),
    // Location can be stored as separate coordinates or as LatLng
    // Changed to handle String to Double conversion
    private var _latitude: Any? = 0.0,
    private var _longitude: Any? = 0.0,
    // QR Code for bike identification (primary field)
    var qrCode: String = "",
    // Hardware ID for backward compatibility (deprecated - use qrCode)
    @Deprecated("Use qrCode instead")
    var hardwareId: String = "",
    // Additional tracking properties
    var rating: Float = 0f,
    var batteryLevel: Int = 100,
    
    // Status fields - Changed to public vars for Firestore compatibility
    var isAvailable: Boolean = true,
    var isInUse: Boolean = false,
    var isLocked: Boolean = true,
    var currentRider: String = "",  // User ID of the current rider
    
    // New fields that were missing and causing Firestore warnings
    var currentRideId: String = "",  // ID of the current ride
    private var _lastRideEnd: Any? = null,     // Last ride end timestamp
    
    // Timestamp fields
    private var _lastUpdated: Any? = null,      // Can be Long or Timestamp
    private var _updatedAt: Any? = null,        // Can be Long or Timestamp
    private var _lastRideStart: Any? = null,     // Can be Long or Timestamp
    private var _createdAt: Any? = null,          // Can be Long or Timestamp
    
    var stationId: String = "",     // ID of the station where the bike is docked (if applicable)
    var distanceToUser: String = "", // Not stored in Firebase, calculated on client
    var description: String = ""    // Bike description
) {
    
    // Latitude property with proper type conversion
    var latitude: Double
        get() = when (_latitude) {
            is String -> {
                try {
                    (_latitude as String).toDouble()
                } catch (e: NumberFormatException) {
                    0.0
                }
            }
            is Double -> _latitude as Double
            is Number -> (_latitude as Number).toDouble()
            else -> 0.0
        }
        set(value) {
            _latitude = value
        }
    
    // Longitude property with proper type conversion
    var longitude: Double
        get() = when (_longitude) {
            is String -> {
                try {
                    (_longitude as String).toDouble()
                } catch (e: NumberFormatException) {
                    0.0
                }
            }
            is Double -> _longitude as Double
            is Number -> (_longitude as Number).toDouble()
            else -> 0.0
        }
        set(value) {
            _longitude = value
        }
    
    // Explicit getters and setters for timestamp fields for Firestore compatibility
    var lastRideEnd: Any?
        get() = _lastRideEnd
        set(value) {
            _lastRideEnd = value
        }
    
    var lastUpdated: Any?
        get() = _lastUpdated
        set(value) {
            _lastUpdated = value
        }
    
    var updatedAt: Any?
        get() = _updatedAt
        set(value) {
            _updatedAt = value
        }
    
    var lastRideStart: Any?
        get() = _lastRideStart
        set(value) {
            _lastRideStart = value
        }
    
    var createdAt: Any?
        get() = _createdAt
        set(value) {
            _createdAt = value
        }
    
    // Helper properties for timestamp handling
    @get:Exclude
    val lastRideEndTimestamp: Long
        get() = when (_lastRideEnd) {
            is Long -> _lastRideEnd as Long
            is Timestamp -> (_lastRideEnd as Timestamp).toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val lastUpdatedTimestamp: Long
        get() = when (_lastUpdated) {
            is Long -> _lastUpdated as Long
            is Timestamp -> (_lastUpdated as Timestamp).toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val updatedAtTimestamp: Long
        get() = when (_updatedAt) {
            is Long -> _updatedAt as Long
            is Timestamp -> (_updatedAt as Timestamp).toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val lastRideStartTimestamp: Long
        get() = when (_lastRideStart) {
            is Long -> _lastRideStart as Long
            is Timestamp -> (_lastRideStart as Timestamp).toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val createdAtTimestamp: Long
        get() = when (_createdAt) {
            is Long -> _createdAt as Long
            is Timestamp -> (_createdAt as Timestamp).toDate().time
            else -> 0L
        }

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "type" to type,
            "price" to price,
            "priceValue" to priceValue,
            "imageUrl" to imageUrl,
            "imageRes" to imageRes,
            "imageUrls" to imageUrls,
            "latitude" to latitude,
            "longitude" to longitude,
            "qrCode" to qrCode,
            "hardwareId" to hardwareId, // Keep for backward compatibility
            "rating" to rating,
            "batteryLevel" to batteryLevel,
            "isAvailable" to isAvailable,
            "isInUse" to isInUse,
            "isLocked" to isLocked,
            "currentRider" to currentRider,
            "currentRideId" to currentRideId,
            "lastRideEnd" to _lastRideEnd,
            "lastUpdated" to _lastUpdated,
            "updatedAt" to _updatedAt,
            "stationId" to stationId,
            "description" to description,
            "lastRideStart" to _lastRideStart,
            "createdAt" to _createdAt
        )
    }
    
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)
    
    /**
     * Get the effective QR code, preferring qrCode over hardwareId for backward compatibility
     */
    @get:Exclude
    val effectiveQrCode: String
        get() = if (qrCode.isNotBlank()) qrCode else hardwareId
        
    companion object {
        /**
         * Creates a simplified Bike instance from basic information
         */
        fun createSimple(
            id: String,
            name: String,
            type: String,
            price: Double,
            imageUrl: String,
            location: LatLng,
            description: String = ""
        ): Bike {
            return Bike(
                id = id,
                name = name,
                type = type,
                price = "₱${price}/hr",
                priceValue = price,
                imageUrl = imageUrl,
                imageUrls = listOf(imageUrl),
                _latitude = location.latitude,
                _longitude = location.longitude,
                isAvailable = true,
                isInUse = false,
                isLocked = true,
                description = description
            )
        }
    }
} 