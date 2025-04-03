package com.example.bikerental.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class TrackableBike(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val price: String = "",
    val rating: Float = 0f,
    val imageRes: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val batteryLevel: Int = 100,
    val isAvailable: Boolean = true,
    val isInUse: Boolean = false,
    val currentRider: String = "",  // User ID of the current rider
    val lastUpdated: Long = 0,      // Timestamp of the last location update
    val stationId: String = "",     // ID of the station where the bike is docked (if applicable)
    val distanceToUser: String = "" // Not stored in Firebase, calculated on client
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "type" to type,
            "price" to price,
            "rating" to rating,
            "imageRes" to imageRes,
            "latitude" to latitude,
            "longitude" to longitude,
            "batteryLevel" to batteryLevel,
            "isAvailable" to isAvailable,
            "isInUse" to isInUse,
            "currentRider" to currentRider,
            "lastUpdated" to lastUpdated,
            "stationId" to stationId
        )
    }
    
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)
}

// Bike ride tracking data
data class BikeRide(
    val rideId: String = "",
    val bikeId: String = "",
    val userId: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val startLocation: BikeLocation = BikeLocation(),
    val endLocation: BikeLocation = BikeLocation(),
    val route: List<BikeLocation> = emptyList(),
    val distance: Double = 0.0,
    val cost: Double = 0.0,
    val status: String = "active" // active, completed, cancelled
)

// Location point for tracking
data class BikeLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0
) {
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)
} 