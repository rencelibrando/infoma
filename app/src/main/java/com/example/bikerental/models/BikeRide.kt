package com.example.bikerental.models

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Represents a bike ride record with tracking information
 */
@IgnoreExtraProperties
data class BikeRide(
    val id: String = "",
    val bikeId: String = "",
    val userId: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val startLocation: BikeLocation = BikeLocation(),
    val endLocation: BikeLocation = BikeLocation(),
    val cost: Double = 0.0,
    val distanceTraveled: Double = 0.0,
    val status: String = "active", // active, completed, cancelled
    val path: List<BikeLocation> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "bikeId" to bikeId,
            "userId" to userId,
            "startTime" to startTime,
            "endTime" to endTime,
            "startLocation" to startLocation,
            "endLocation" to endLocation,
            "cost" to cost,
            "distanceTraveled" to distanceTraveled,
            "status" to status,
            "path" to path
        )
    }
} 