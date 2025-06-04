package com.example.bikerental.services

import android.util.Log
import com.example.bikerental.models.Bike
import com.example.bikerental.models.BikeLocation
import com.example.bikerental.utils.ErrorHandler
import com.example.bikerental.utils.FirestoreUtils
import com.example.bikerental.utils.QRCodeHelper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Service class for setting up bikes with hardware IDs in Firebase
 * This is useful for testing and initial data setup
 */
class BikeSetupService {
    private val TAG = "BikeSetupService"
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Add hardware IDs to existing bikes that don't have them
     */
    suspend fun addHardwareIdsToExistingBikes(): Result<Int> {
        return try {
            ErrorHandler.logInfo(TAG, "Starting to add hardware IDs to existing bikes")
            
            // Get all bikes from Firestore
            val bikesSnapshot = firestore.collection("bikes").get().await()
            var updatedCount = 0
            
            // Get existing hardware IDs to prevent duplicates
            val existingHardwareIds = mutableSetOf<String>()
            for (document in bikesSnapshot.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val bike = FirestoreUtils.createBikeFromData(document.id, data)
                        if (bike.hardwareId.isNotBlank()) {
                            existingHardwareIds.add(bike.hardwareId)
                        }
                    }
                } catch (e: Exception) {
                    ErrorHandler.logError(TAG, "Error parsing bike document ${document.id} for hardware ID check", e)
                }
            }
            
            for (document in bikesSnapshot.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val bike = FirestoreUtils.createBikeFromData(document.id, data)
                        
                        if (bike.hardwareId.isBlank()) {
                            // Generate a unique hardware ID for this bike
                            var hardwareId: String
                            var attempts = 0
                            do {
                                hardwareId = QRCodeHelper.generateUniqueHardwareId(bike.id)
                                attempts++
                            } while (existingHardwareIds.contains(hardwareId) && attempts < 10)
                            
                            if (attempts >= 10) {
                                ErrorHandler.logWarning(TAG, "Could not generate unique hardware ID for bike ${bike.id} after 10 attempts")
                                continue
                            }
                            
                            // Update the bike document
                            firestore.collection("bikes")
                                .document(bike.id)
                                .update("hardwareId", hardwareId)
                                .await()
                            
                            ErrorHandler.logInfo(TAG, "Added hardware ID '$hardwareId' to bike ${bike.id}")
                            existingHardwareIds.add(hardwareId)
                            updatedCount++
                        }
                    }
                } catch (e: Exception) {
                    ErrorHandler.logError(TAG, "Error processing bike document ${document.id}", e)
                }
            }
            
            ErrorHandler.logInfo(TAG, "Successfully added hardware IDs to $updatedCount bikes")
            Result.success(updatedCount)
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Error adding hardware IDs to bikes", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create sample bikes with hardware IDs for testing
     */
    suspend fun createSampleBikesWithHardwareIds(): Result<List<Bike>> {
        return try {
            ErrorHandler.logInfo(TAG, "Creating sample bikes with hardware IDs")
            
            val sampleBikes = listOf(
                Bike.createSimple(
                    id = "bike_001",
                    name = "City Cruiser 001",
                    type = "Electric",
                    price = 25.0,
                    imageUrl = "https://example.com/bike1.jpg",
                    location = LatLng(14.5890, 120.9760),
                    description = "Perfect for city rides with electric assistance"
                ).copy(hardwareId = QRCodeHelper.generateUniqueHardwareId("bike_001")),
                
                Bike.createSimple(
                    id = "bike_002", 
                    name = "Mountain Explorer 002",
                    type = "Mountain",
                    price = 30.0,
                    imageUrl = "https://example.com/bike2.jpg",
                    location = LatLng(14.5900, 120.9770),
                    description = "Rugged mountain bike for off-road adventures"
                ).copy(hardwareId = QRCodeHelper.generateUniqueHardwareId("bike_002")),
                
                Bike.createSimple(
                    id = "bike_003",
                    name = "Speed Demon 003", 
                    type = "Road",
                    price = 35.0,
                    imageUrl = "https://example.com/bike3.jpg",
                    location = LatLng(14.5910, 120.9780),
                    description = "High-performance road bike for speed enthusiasts"
                ).copy(hardwareId = QRCodeHelper.generateUniqueHardwareId("bike_003")),
                
                Bike.createSimple(
                    id = "bike_004",
                    name = "Comfort Rider 004",
                    type = "Hybrid", 
                    price = 20.0,
                    imageUrl = "https://example.com/bike4.jpg",
                    location = LatLng(14.5920, 120.9790),
                    description = "Comfortable hybrid bike for casual rides"
                ).copy(hardwareId = QRCodeHelper.generateUniqueHardwareId("bike_004")),
                
                Bike.createSimple(
                    id = "bike_005",
                    name = "Electric Pro 005",
                    type = "Electric",
                    price = 40.0, 
                    imageUrl = "https://example.com/bike5.jpg",
                    location = LatLng(14.5930, 120.9800),
                    description = "Premium electric bike with advanced features"
                ).copy(hardwareId = QRCodeHelper.generateUniqueHardwareId("bike_005"))
            )
            
            // Save bikes to Firestore
            for (bike in sampleBikes) {
                firestore.collection("bikes")
                    .document(bike.id)
                    .set(bike.toMap())
                    .await()
                
                ErrorHandler.logInfo(TAG, "Created bike ${bike.id} with hardware ID: ${bike.hardwareId}")
            }
            
            ErrorHandler.logInfo(TAG, "Successfully created ${sampleBikes.size} sample bikes")
            Result.success(sampleBikes)
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Error creating sample bikes", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify that all bikes have valid hardware IDs
     */
    suspend fun verifyBikeHardwareIds(): Result<List<String>> {
        return try {
            ErrorHandler.logInfo(TAG, "Verifying bike hardware IDs")
            
            val bikesSnapshot = firestore.collection("bikes").get().await()
            val issues = mutableListOf<String>()
            
            for (document in bikesSnapshot.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val bike = FirestoreUtils.createBikeFromData(document.id, data)
                        
                        when {
                            bike.hardwareId.isBlank() -> {
                                issues.add("Bike ${bike.id} has no hardware ID")
                            }
                            !QRCodeHelper.isValidQRCodeFormat(bike.hardwareId) -> {
                                issues.add("Bike ${bike.id} has invalid hardware ID format: ${bike.hardwareId}")
                            }
                            else -> {
                                ErrorHandler.logDebug(TAG, "Bike ${bike.id} has valid hardware ID: ${bike.hardwareId}")
                            }
                        }
                    } else {
                        issues.add("Bike ${document.id} has null data")
                    }
                } catch (e: Exception) {
                    ErrorHandler.logError(TAG, "Error parsing bike document ${document.id}", e)
                    issues.add("Bike ${document.id} could not be parsed: ${e.message}")
                }
            }
            
            if (issues.isEmpty()) {
                ErrorHandler.logInfo(TAG, "All bikes have valid hardware IDs")
            } else {
                ErrorHandler.logWarning(TAG, "Found ${issues.size} issues with bike hardware IDs")
            }
            
            Result.success(issues)
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Error verifying bike hardware IDs", e)
            Result.failure(e)
        }
    }
    
    /**
     * Print QR codes for testing (hardware IDs that can be manually entered)
     */
    fun printTestQRCodes() {
        val sampleHardwareIds = QRCodeHelper.generateSampleHardwareIds()
        
        ErrorHandler.logInfo(TAG, "=== TEST QR CODES ===")
        sampleHardwareIds.forEach { (bikeId, hardwareId) ->
            ErrorHandler.logInfo(TAG, "Bike: $bikeId -> QR Code: $hardwareId")
        }
        ErrorHandler.logInfo(TAG, "==================")
    }
    
    /**
     * Check for and resolve duplicate hardware IDs in the database
     */
    suspend fun checkAndResolveDuplicateHardwareIds(): Result<Int> {
        return try {
            ErrorHandler.logInfo(TAG, "Checking for duplicate hardware IDs")
            
            val bikesSnapshot = firestore.collection("bikes").get().await()
            val hardwareIdPairs = mutableListOf<Pair<String, String>>()
            
            // Parse documents safely using FirestoreUtils
            for (document in bikesSnapshot.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val bike = FirestoreUtils.createBikeFromData(document.id, data)
                        if (bike.hardwareId.isNotBlank()) {
                            hardwareIdPairs.add(bike.id to bike.hardwareId)
                        }
                    }
                } catch (e: Exception) {
                    ErrorHandler.logError(TAG, "Error parsing bike document ${document.id}", e)
                }
            }
            
            val hardwareIdGroups = hardwareIdPairs.groupBy { it.second }
            val duplicates = hardwareIdGroups.filter { it.value.size > 1 }
            var resolvedCount = 0
            
            if (duplicates.isEmpty()) {
                ErrorHandler.logInfo(TAG, "No duplicate hardware IDs found")
                return Result.success(0)
            }
            
            ErrorHandler.logWarning(TAG, "Found ${duplicates.size} duplicate hardware IDs")
            
            for ((hardwareId, bikes) in duplicates) {
                ErrorHandler.logWarning(TAG, "Duplicate hardware ID '$hardwareId' found in bikes: ${bikes.map { it.first }}")
                
                // Keep the first bike with this hardware ID, regenerate for others
                for (i in 1 until bikes.size) {
                    val bikeId = bikes[i].first
                    val newHardwareId = QRCodeHelper.generateUniqueHardwareId(bikeId)
                    
                    firestore.collection("bikes")
                        .document(bikeId)
                        .update("hardwareId", newHardwareId)
                        .await()
                    
                    ErrorHandler.logInfo(TAG, "Updated bike '$bikeId' with new hardware ID: '$newHardwareId'")
                    resolvedCount++
                }
            }
            
            ErrorHandler.logInfo(TAG, "Resolved $resolvedCount duplicate hardware IDs")
            Result.success(resolvedCount)
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Error checking/resolving duplicate hardware IDs", e)
            Result.failure(e)
        }
    }
} 