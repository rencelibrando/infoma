package com.example.bikerental.data.repository

import com.example.bikerental.domain.repository.RideRepository
import com.example.bikerental.models.BikeRide
import com.example.bikerental.models.BikeLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.bikerental.utils.toBikeRideSafe

@Singleton
class RideRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RideRepository {
    private val ridesCollection = firestore.collection("rides")
    private val usersCollection = firestore.collection("users")

    /**
     * Create a new ride in Firestore
     */
    override suspend fun createRide(ride: BikeRide): Result<String> {
        return try {
            val docRef = ridesCollection.document()
            val rideWithId = ride.copy(id = docRef.id)
            docRef.set(rideWithId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing ride
     */
    override suspend fun updateRide(rideId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updatesWithTimestamp = updates.toMutableMap()
            updatesWithTimestamp["updatedAt"] = System.currentTimeMillis()
            
            ridesCollection.document(rideId)
                .update(updatesWithTimestamp)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update ride statistics (location, speed, distance)
     */
    override suspend fun updateRideStats(
        rideId: String,
        location: BikeLocation,
        distanceTraveled: Double,
        averageSpeed: Double,
        maxSpeed: Double
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "lastLocation" to location,
                "distanceTraveled" to distanceTraveled,
                "averageSpeed" to averageSpeed,
                "maxSpeed" to maxSpeed,
                "updatedAt" to System.currentTimeMillis()
            )
            updateRide(rideId, updates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * End a ride by updating its status and end information
     */
    override suspend fun endRide(
        rideId: String,
        endLocation: BikeLocation,
        finalDistance: Double,
        finalCost: Double
    ): Result<Unit> {
        return try {
            val endTime = System.currentTimeMillis()
            val updates = mapOf(
                "endTime" to endTime,
                "endLocation" to endLocation,
                "distanceTraveled" to finalDistance,
                "cost" to finalCost,
                "status" to "completed",
                "updatedAt" to endTime
            )
            updateRide(rideId, updates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a specific ride by ID
     */
    override suspend fun getRide(rideId: String): Result<BikeRide?> {
        return try {
            val document = ridesCollection.document(rideId).get().await()
            if (document.exists()) {
                val ride = document.toBikeRideSafe()
                Result.success(ride)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the current active ride for the logged-in user
     */
    override suspend fun getCurrentActiveRide(): Result<BikeRide?> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            val query = ridesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .await()

            val ride = query.documents.firstOrNull()?.toBikeRideSafe()
            Result.success(ride)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's ride history
     */
    override fun getUserRideHistory(limit: Int): Flow<List<BikeRide>> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        
        try {
            val query = ridesCollection
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val rides = query.documents.mapNotNull { doc ->
                doc.toBikeRideSafe()
            }
            emit(rides)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * Add ride to user's ride history
     */
    override suspend fun addRideToUserHistory(rideId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            val userDoc = usersCollection.document(userId)
            
            // Get current ride history
            val userSnapshot = userDoc.get().await()
            val currentHistory = userSnapshot.get("rideHistory") as? List<String> ?: emptyList()
            
            // Add new ride to the beginning of the list
            val updatedHistory = listOf(rideId) + currentHistory
            
            userDoc.update("rideHistory", updatedHistory).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get ride statistics for a user
     */
    override suspend fun getUserRideStats(): Result<Map<String, Any>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            val query = ridesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "completed")
                .get()
                .await()

            val rides = query.documents.mapNotNull { doc ->
                doc.toBikeRideSafe()
            }

            val totalRides = rides.size
            val totalDistance = rides.sumOf { it.distanceTraveled }
            val totalCost = rides.sumOf { it.cost }
            val totalDuration = rides.sumOf { it.getRideDuration() }
            val averageSpeed = if (rides.isNotEmpty()) {
                rides.sumOf { it.averageSpeed } / rides.size
            } else 0.0

            val stats = mapOf(
                "totalRides" to totalRides,
                "totalDistance" to totalDistance,
                "totalCost" to totalCost,
                "totalDuration" to totalDuration,
                "averageSpeed" to averageSpeed
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 