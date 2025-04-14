package com.example.bikerental.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

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
    // Location can be stored as separate coordinates or as LatLng
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // Additional tracking properties
    val rating: Float = 0f,
    val batteryLevel: Int = 100,
    val isAvailable: Boolean = true,
    val isInUse: Boolean = false,
    val currentRider: String = "",  // User ID of the current rider
    val lastUpdated: Long = 0,      // Timestamp of the last location update
    val stationId: String = "",     // ID of the station where the bike is docked (if applicable)
    val distanceToUser: String = "", // Not stored in Firebase, calculated on client
    val description: String = ""     // Bike description (new field)
) {
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
            "latitude" to latitude,
            "longitude" to longitude,
            "rating" to rating,
            "batteryLevel" to batteryLevel,
            "isAvailable" to isAvailable,
            "isInUse" to isInUse,
            "currentRider" to currentRider,
            "lastUpdated" to lastUpdated,
            "stationId" to stationId,
            "description" to description
        )
    }
    
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)
        
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
                latitude = location.latitude,
                longitude = location.longitude,
                isAvailable = true,
                isInUse = false,
                description = description
            )
        }
    }
} 