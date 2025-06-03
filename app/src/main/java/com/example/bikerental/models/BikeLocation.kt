package com.example.bikerental.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * Represents a bike location point with timestamp for tracking
 * Enhanced with additional tracking data for real-time navigation
 */
@IgnoreExtraProperties
data class BikeLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val altitude: Double = 0.0,
    val provider: String = "unknown"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "accuracy" to accuracy,
            "speed" to speed,
            "bearing" to bearing,
            "altitude" to altitude,
            "provider" to provider
        )
    }
    
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)
        
    @get:Exclude
    val speedKmh: Float
        get() = speed * 3.6f // Convert m/s to km/h
        
    @get:Exclude
    val isAccurate: Boolean
        get() = accuracy > 0 && accuracy <= 10f // Consider accurate if within 10 meters
        
    @get:Exclude
    val isMoving: Boolean
        get() = speed > 0.5f // Consider moving if speed > 0.5 m/s (1.8 km/h)
        
    companion object {
        fun fromLatLng(latLng: LatLng, speed: Float = 0f, bearing: Float = 0f): BikeLocation {
            return BikeLocation(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                speed = speed,
                bearing = bearing
            )
        }
        
        fun fromLocation(location: android.location.Location): BikeLocation {
            return BikeLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                accuracy = location.accuracy,
                speed = if (location.hasSpeed()) location.speed else 0f,
                bearing = if (location.hasBearing()) location.bearing else 0f,
                altitude = if (location.hasAltitude()) location.altitude else 0.0,
                provider = location.provider ?: "unknown"
            )
        }
    }
} 