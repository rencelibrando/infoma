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
    private val _isAvailable: Boolean = true,
    private val _isInUse: Boolean = false,
    private val _isLocked: Boolean = true,
    val currentRider: String = "",  // User ID of the current rider
    private val _lastUpdated: Any? = null,      // Can be Long or Timestamp
    val stationId: String = "",     // ID of the station where the bike is docked (if applicable)
    val distanceToUser: String = "", // Not stored in Firebase, calculated on client
    val description: String = "",    // Bike description
    private val _lastRideStart: Any? = null,     // Can be Long or Timestamp
    private val _createdAt: Any? = null          // Can be Long or Timestamp
) {
    
    // Explicit getters and setters for Firestore compatibility
    var isAvailable: Boolean
        get() = _isAvailable
        set(value) {}  // Data class doesn't allow setting, but Firebase needs the setter
    
    var isInUse: Boolean
        get() = _isInUse
        set(value) {}
    
    var isLocked: Boolean
        get() = _isLocked
        set(value) {}
    
    var lastUpdated: Any?
        get() = _lastUpdated
        set(value) {}
    
    var lastRideStart: Any?
        get() = _lastRideStart
        set(value) {}
    
    var createdAt: Any?
        get() = _createdAt
        set(value) {}
    
    // Helper properties for timestamp handling
    @get:Exclude
    val lastUpdatedTimestamp: Long
        get() = when (_lastUpdated) {
            is Long -> _lastUpdated
            is Timestamp -> _lastUpdated.toDate().time
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
            "isAvailable" to _isAvailable,
            "isInUse" to _isInUse,
            "isLocked" to _isLocked,
            "currentRider" to currentRider,
            "lastUpdated" to _lastUpdated,
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
                _isAvailable = true,
                _isInUse = false,
                _isLocked = true,
                description = description
            )
        }
    }
} 