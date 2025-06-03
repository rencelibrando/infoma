package com.example.bikerental.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.example.bikerental.models.Booking
import com.example.bikerental.models.BikeRide
import java.util.Date

/**
 * Extension functions for safe Firestore deserialization
 * Handles Timestamp to Long conversions properly
 */

/**
 * Safely converts a Firestore value to Long, handling Timestamp objects
 */
fun Any?.toSafeLong(): Long {
    return when (this) {
        is Long -> this
        is Number -> this.toLong()
        is Timestamp -> this.toDate().time
        is Date -> this.time
        else -> System.currentTimeMillis()
    }
}

/**
 * Safely deserialize a Booking from a Firestore DocumentSnapshot
 */
fun DocumentSnapshot.toBookingSafe(): Booking? {
    return try {
        this.data?.let { Booking.fromFirestoreDocument(it) }
    } catch (e: Exception) {
        null
    }
}

/**
 * Safely deserialize a BikeRide from a Firestore DocumentSnapshot
 */
fun DocumentSnapshot.toBikeRideSafe(): BikeRide? {
    return try {
        this.data?.let { data ->
            BikeRide(
                id = data["id"] as? String ?: this.id,
                bikeId = data["bikeId"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                startTime = data["startTime"].toSafeLong(),
                endTime = data["endTime"].toSafeLong(),
                cost = (data["cost"] as? Number)?.toDouble() ?: 0.0,
                distanceTraveled = (data["distanceTraveled"] as? Number)?.toDouble() ?: 0.0,
                averageSpeed = (data["averageSpeed"] as? Number)?.toDouble() ?: 0.0,
                maxSpeed = (data["maxSpeed"] as? Number)?.toDouble() ?: 0.0,
                duration = data["duration"].toSafeLong(),
                status = data["status"] as? String ?: "active",
                createdAt = data["createdAt"].toSafeLong(),
                updatedAt = data["updatedAt"].toSafeLong()
            )
        }
    } catch (e: Exception) {
        null
    }
} 