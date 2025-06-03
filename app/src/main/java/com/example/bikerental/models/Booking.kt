package com.example.bikerental.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

/**
 * Represents a bike booking in the system.
 */
@IgnoreExtraProperties
data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val bikeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    var status: BookingStatus = BookingStatus.PENDING,
    val totalPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isHourly: Boolean = false // New field to distinguish between hourly and daily bookings
) {
    
    // No-argument constructor for Firestore
    constructor() : this(
        id = "",
        bikeId = "",
        userId = "",
        userName = "",
        startDate = System.currentTimeMillis(),
        endDate = System.currentTimeMillis(),
        status = BookingStatus.PENDING,
        totalPrice = 0.0,
        createdAt = System.currentTimeMillis(),
        isHourly = false
    )
    
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "bikeId" to bikeId,
            "userId" to userId,
            "userName" to userName,
            "startDate" to startDate,
            "endDate" to endDate,
            "status" to status.name,
            "totalPrice" to totalPrice,
            "createdAt" to createdAt,
            "isHourly" to isHourly
        )
    }
    
    @Exclude
    fun getFormattedStartDate(): String {
        return Date(startDate).toString()
    }
    
    @Exclude
    fun getFormattedEndDate(): String {
        return Date(endDate).toString()
    }
    
    @Exclude
    fun getDurationInHours(): Long {
        return (endDate - startDate) / (1000 * 60 * 60)
    }
    
    @Exclude
    fun isActive(): Boolean {
        return status == BookingStatus.CONFIRMED && System.currentTimeMillis() in startDate..endDate
    }
    
    @Exclude
    fun canBeCancelled(): Boolean {
        return status in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED) && 
               startDate > System.currentTimeMillis()
    }
    
    companion object {
        /**
         * Helper function to convert Firestore timestamp to Long
         */
        private fun convertTimestampToLong(value: Any?): Long {
            return when (value) {
                is Long -> value
                is Number -> value.toLong()
                is Timestamp -> value.toDate().time
                is Date -> value.time
                else -> System.currentTimeMillis()
            }
        }
        
        /**
         * Create a daily booking
         */
        fun createDaily(
            bikeId: String,
            userId: String,
            userName: String = "",
            startDate: Date,
            endDate: Date,
            pricePerHour: Double
        ): Booking {
            val durationInMillis = endDate.time - startDate.time
            val durationInDays = (durationInMillis / (1000 * 60 * 60 * 24.0)).toInt() + 1
            val totalPrice = durationInDays * pricePerHour * 24 // Daily rate is 24x hourly rate
            
            return Booking(
                bikeId = bikeId,
                userId = userId,
                userName = userName,
                startDate = startDate.time,
                endDate = endDate.time,
                status = BookingStatus.PENDING,
                totalPrice = totalPrice,
                isHourly = false
            )
        }
        
        /**
         * Create an hourly booking
         */
        fun createHourly(
            bikeId: String,
            userId: String,
            userName: String = "",
            startDate: Date,
            endDate: Date,
            pricePerHour: Double
        ): Booking {
            val durationInMillis = endDate.time - startDate.time
            val durationInHours = durationInMillis / (1000.0 * 60 * 60)
            val totalPrice = durationInHours * pricePerHour
            
            return Booking(
                bikeId = bikeId,
                userId = userId,
                userName = userName,
                startDate = startDate.time,
                endDate = endDate.time,
                status = BookingStatus.PENDING,
                totalPrice = totalPrice,
                isHourly = true
            )
        }
        
        /**
         * Create a Booking from a Firestore document, handling status conversion
         */
        fun fromFirestoreDocument(data: Map<String, Any?>): Booking? {
            return try {
                val statusString = data["status"] as? String ?: "PENDING"
                val status = try {
                    BookingStatus.valueOf(statusString.uppercase())
                } catch (e: IllegalArgumentException) {
                    // Fallback for unknown status values
                    when (statusString.lowercase()) {
                        "active", "pending" -> BookingStatus.PENDING
                        "confirmed" -> BookingStatus.CONFIRMED
                        "completed" -> BookingStatus.COMPLETED
                        "cancelled", "canceled" -> BookingStatus.CANCELLED
                        else -> BookingStatus.PENDING
                    }
                }
                
                Booking(
                    id = data["id"] as? String ?: "",
                    bikeId = data["bikeId"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    userName = data["userName"] as? String ?: "",
                    startDate = convertTimestampToLong(data["startDate"]),
                    endDate = convertTimestampToLong(data["endDate"]),
                    status = status,
                    totalPrice = (data["totalPrice"] as? Number)?.toDouble() ?: 0.0,
                    createdAt = convertTimestampToLong(data["createdAt"]),
                    isHourly = data["isHourly"] as? Boolean ?: data["hourly"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
} 