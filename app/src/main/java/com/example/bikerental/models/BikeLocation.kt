package com.example.bikerental.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * Represents a bike location point with timestamp for tracking
 */
@IgnoreExtraProperties
data class BikeLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float = 0f,
    val speed: Float = 0f
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "accuracy" to accuracy,
            "speed" to speed
        )
    }
    
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)
        
    companion object {
        fun fromLatLng(latLng: LatLng): BikeLocation {
            return BikeLocation(
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        }
    }
} 