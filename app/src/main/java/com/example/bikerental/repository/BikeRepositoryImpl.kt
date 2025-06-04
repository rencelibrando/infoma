package com.example.bikerental.repository

import com.example.bikerental.models.Bike
import com.example.bikerental.utils.ErrorHandler
import com.example.bikerental.utils.FirestoreUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Legacy BikeRepository implementation - Consider using BikeRepositoryImpl in data.repository package instead
 * This class provides basic Firestore operations for bikes
 */
class BikeRepositoryImpl {
    
    private val firestore: FirebaseFirestore = Firebase.firestore

    suspend fun fetchBikesFromFirestore(): List<Bike> {
        return try {
            val result = firestore.collection("bikes").get().await()
            val bikes = mutableListOf<Bike>()
            
            for (doc in result.documents) {
                try {
                    val data = doc.data
                    if (data != null) {
                        val bike = FirestoreUtils.createBikeFromData(doc.id, data)
                        bikes.add(bike)
                    } else {
                        ErrorHandler.logWarning("BikeRepository", "Document ${doc.id} has null data")
                    }
                } catch (e: Exception) {
                    ErrorHandler.logError("BikeRepository", "Error parsing bike document ${doc.id}", e)
                }
            }
            
            ErrorHandler.logInfo("BikeRepository", "Successfully loaded ${bikes.size} bikes from Firestore")
            bikes
        } catch (e: Exception) {
            ErrorHandler.logError("BikeRepository", "Error fetching bikes from Firestore", e)
            emptyList()
        }
    }

    suspend fun getBikeById(bikeId: String): Bike? {
        return try {
            val document = firestore.collection("bikes").document(bikeId).get().await()
            val data = document.data
            if (data != null) {
                FirestoreUtils.createBikeFromData(document.id, data)
            } else {
                ErrorHandler.logWarning("BikeRepository", "Bike document $bikeId not found or has null data")
                null
            }
        } catch (e: Exception) {
            ErrorHandler.logError("BikeRepository", "Error fetching bike $bikeId", e)
            null
        }
    }

    suspend fun updateBikeStatus(bikeId: String, isAvailable: Boolean, isInUse: Boolean): Boolean {
        return try {
            firestore.collection("bikes")
                .document(bikeId)
                .update(
                    mapOf(
                        "isAvailable" to isAvailable,
                        "isInUse" to isInUse,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                ).await()
            
            ErrorHandler.logInfo("BikeRepository", "Successfully updated bike $bikeId status")
            true
        } catch (e: Exception) {
            ErrorHandler.logError("BikeRepository", "Error updating bike $bikeId status", e)
            false
        }
    }
} 