package com.example.bikerental.data.repository

import android.util.Log
import com.example.bikerental.models.Bike
import com.example.bikerental.domain.repository.BikeRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
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
    private val TAG = "BikeRepositoryImpl"

    override fun getAvailableBikes(): Flow<List<Bike>> {
        return bikesCollection
            .whereEqualTo("isAvailable", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    try {
                        val data = document.data
                        if (data != null) {
                            createBikeFromData(document.id, data)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing bike ${document.id}: ${e.message}")
                        null
                    }
                }
            }
    }

    override suspend fun getBikeById(bikeId: String): Bike? {
        return try {
            val document = bikesCollection.document(bikeId).get().await()
            val data = document.data
            if (data != null) {
                createBikeFromData(document.id, data)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bike $bikeId: ${e.message}")
            null
        }
    }

    override suspend fun getBike(bikeId: String): Result<Bike> {
        return try {
            val document = bikesCollection.document(bikeId).get().await()
            val data = document.data
            if (data != null) {
                val bike = createBikeFromData(document.id, data)
                Result.success(bike)
            } else {
                Result.failure(Exception("Bike not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a Bike object from Firestore document data with proper type handling
     */
    private fun createBikeFromData(documentId: String, data: Map<String, Any>): Bike {
        return Bike(
            id = documentId,
            name = data["name"] as? String ?: "",
            type = data["type"] as? String ?: "",
            price = data["price"] as? String ?: "",
            priceValue = (data["priceValue"] as? Number)?.toDouble() ?: 0.0,
            imageUrl = data["imageUrl"] as? String ?: "",
            imageRes = (data["imageRes"] as? Number)?.toInt() ?: 0,
            imageUrls = data["imageUrls"] as? List<String> ?: emptyList(),
            _latitude = data["latitude"] ?: 0.0,
            _longitude = data["longitude"] ?: 0.0,
            qrCode = data["qrCode"] as? String ?: "",
            hardwareId = data["hardwareId"] as? String ?: "",
            rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
            batteryLevel = (data["batteryLevel"] as? Number)?.toInt() ?: 100,
            isAvailable = data["isAvailable"] as? Boolean ?: true,
            isInUse = data["isInUse"] as? Boolean ?: false,
            isLocked = data["isLocked"] as? Boolean ?: true,
            currentRider = data["currentRider"] as? String ?: "",
            currentRideId = data["currentRideId"] as? String ?: "",
            _lastRideEnd = data["lastRideEnd"],
            _lastUpdated = data["lastUpdated"],
            _updatedAt = data["updatedAt"],
            _lastRideStart = data["lastRideStart"],
            _createdAt = data["createdAt"],
            stationId = data["stationId"] as? String ?: "",
            distanceToUser = "", // This is calculated on client
            description = data["description"] as? String ?: ""
        )
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