package com.example.bikerental.domain.repository

import com.example.bikerental.data.models.Bike
import kotlinx.coroutines.flow.Flow

interface BikeRepository {
    fun getAvailableBikes(): Flow<List<Bike>>
    suspend fun getBikeById(bikeId: String): Bike?
    suspend fun getBike(bikeId: String): Result<Bike>
    suspend fun updateBikeLocation(bikeId: String, latitude: Double, longitude: Double)
    suspend fun updateBikeAvailability(bikeId: String, isAvailable: Boolean)
    suspend fun updateBikeStatus(bikeId: String, status: String): Result<Unit>
} 