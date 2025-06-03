package com.example.bikerental.domain.repository

import com.example.bikerental.models.BikeRide
import com.example.bikerental.models.BikeLocation
import kotlinx.coroutines.flow.Flow

interface RideRepository {
    suspend fun createRide(ride: BikeRide): Result<String>
    suspend fun updateRide(rideId: String, updates: Map<String, Any>): Result<Unit>
    suspend fun updateRideStats(
        rideId: String,
        location: BikeLocation,
        distanceTraveled: Double,
        averageSpeed: Double,
        maxSpeed: Double
    ): Result<Unit>
    suspend fun endRide(
        rideId: String,
        endLocation: BikeLocation,
        finalDistance: Double,
        finalCost: Double
    ): Result<Unit>
    suspend fun getRide(rideId: String): Result<BikeRide?>
    suspend fun getCurrentActiveRide(): Result<BikeRide?>
    fun getUserRideHistory(limit: Int = 20): Flow<List<BikeRide>>
    suspend fun addRideToUserHistory(rideId: String): Result<Unit>
    suspend fun getUserRideStats(): Result<Map<String, Any>>
}