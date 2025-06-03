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
    val lastLocation: BikeLocation = BikeLocation(),
    val cost: Double = 0.0,
    val distanceTraveled: Double = 0.0,
    val averageSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val duration: Long = 0, // in milliseconds
    val status: String = "active", // active, completed, cancelled
    val path: List<BikeLocation> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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
            "lastLocation" to lastLocation,
            "cost" to cost,
            "distanceTraveled" to distanceTraveled,
            "averageSpeed" to averageSpeed,
            "maxSpeed" to maxSpeed,
            "duration" to duration,
            "status" to status,
            "path" to path,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
    
    /**
     * Calculate ride duration based on start and end times
     */
    fun getRideDuration(): Long {
        return if (endTime > startTime) endTime - startTime else 0
    }
    
    /**
     * Check if ride is currently active
     */
    fun isActive(): Boolean = status == "active"
    
    /**
     * Check if ride is completed
     */
    fun isCompleted(): Boolean = status == "completed"
} 