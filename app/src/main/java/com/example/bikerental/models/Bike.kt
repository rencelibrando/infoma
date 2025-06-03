package com.example.bikerental.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.Timestamp

/**
 * Unified Bike model that combines the features of both simple Bike and TrackableBike models.
 * This provides a single source of truth for bike data throughout the app.
 */
@IgnoreExtraProperties
data class Bike(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    // Support different price formats (String for display, Double for calculations)
    val price: String = "",
    val priceValue: Double = 0.0,
    // Support both types of image references
    val imageUrl: String = "",
    val imageRes: Int = 0,
    // Support for multiple images
    val imageUrls: List<String> = emptyList(),
    // Location can be stored as separate coordinates or as LatLng
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // QR Code for bike identification (primary field)
    val qrCode: String = "",
    // Hardware ID for backward compatibility (deprecated - use qrCode)
    @Deprecated("Use qrCode instead")
    val hardwareId: String = "",
    // Additional tracking properties
    val rating: Float = 0f,
    val batteryLevel: Int = 100,
    
    // Status fields - Changed to public vars for Firestore compatibility
    var isAvailable: Boolean = true,
    var isInUse: Boolean = false,
    var isLocked: Boolean = true,
    val currentRider: String = "",  // User ID of the current rider
    
    // New fields that were missing and causing Firestore warnings
    val currentRideId: String = "",  // ID of the current ride
    private val _lastRideEnd: Any? = null,     // Last ride end timestamp
    
    // Timestamp fields
    private val _lastUpdated: Any? = null,      // Can be Long or Timestamp
    private val _updatedAt: Any? = null,        // Can be Long or Timestamp
    private val _lastRideStart: Any? = null,     // Can be Long or Timestamp
    private val _createdAt: Any? = null,          // Can be Long or Timestamp
    
    val stationId: String = "",     // ID of the station where the bike is docked (if applicable)
    val distanceToUser: String = "", // Not stored in Firebase, calculated on client
    val description: String = ""    // Bike description
) {
    
    // Explicit getters and setters for timestamp fields for Firestore compatibility
    var lastRideEnd: Any?
        get() = _lastRideEnd
        set(value) {}
    
    var lastUpdated: Any?
        get() = _lastUpdated
        set(value) {}
    
    var updatedAt: Any?
        get() = _updatedAt
        set(value) {}
    
    var lastRideStart: Any?
        get() = _lastRideStart
        set(value) {}
    
    var createdAt: Any?
        get() = _createdAt
        set(value) {}
    
    // Helper properties for timestamp handling
    @get:Exclude
    val lastRideEndTimestamp: Long
        get() = when (_lastRideEnd) {
            is Long -> _lastRideEnd
            is Timestamp -> _lastRideEnd.toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val lastUpdatedTimestamp: Long
        get() = when (_lastUpdated) {
            is Long -> _lastUpdated
            is Timestamp -> _lastUpdated.toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val updatedAtTimestamp: Long
        get() = when (_updatedAt) {
            is Long -> _updatedAt
            is Timestamp -> _updatedAt.toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val lastRideStartTimestamp: Long
        get() = when (_lastRideStart) {
            is Long -> _lastRideStart
            is Timestamp -> _lastRideStart.toDate().time
            else -> 0L
        }
    
    @get:Exclude
    val createdAtTimestamp: Long
        get() = when (_createdAt) {
            is Long -> _createdAt
            is Timestamp -> _createdAt.toDate().time
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
                latitude = location.latitude,
                longitude = location.longitude,
                isAvailable = true,
                isInUse = false,
                isLocked = true,
                description = description
            )
        }
    }
} 