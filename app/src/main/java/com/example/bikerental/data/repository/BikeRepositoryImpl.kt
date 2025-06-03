package com.example.bikerental.data.repository

import com.example.bikerental.data.models.Bike
import com.example.bikerental.domain.repository.BikeRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BikeRepository {

    private val bikesCollection = firestore.collection("bikes")

    override fun getAvailableBikes(): Flow<List<Bike>> {
        return bikesCollection
            .whereEqualTo("isAvailable", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject<Bike>()?.copy(id = document.id)
                }
            }
    }

    override suspend fun getBikeById(bikeId: String): Bike? {
        return try {
            val document = bikesCollection.document(bikeId).get().await()
            document.toObject<Bike>()?.copy(id = document.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getBike(bikeId: String): Result<Bike> {
        return try {
            val document = bikesCollection.document(bikeId).get().await()
            val bike = document.toObject<Bike>()?.copy(id = document.id)
            if (bike != null) {
                Result.success(bike)
            } else {
                Result.failure(Exception("Bike not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBikeLocation(bikeId: String, latitude: Double, longitude: Double) {
        bikesCollection.document(bikeId).update(
            mapOf(
                "latitude" to latitude,
                "longitude" to longitude
            )
        ).await()
    }

    override suspend fun updateBikeAvailability(bikeId: String, isAvailable: Boolean) {
        bikesCollection.document(bikeId).update("isAvailable", isAvailable).await()
    }

    override suspend fun updateBikeStatus(bikeId: String, status: String): Result<Unit> {
        return try {
            bikesCollection.document(bikeId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 