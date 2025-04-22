package com.example.bikerental.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
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
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val status: BookingStatus = BookingStatus.PENDING,
    val totalPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isHourly: Boolean = false // New field to distinguish between hourly and daily bookings
) {
    
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "bikeId" to bikeId,
            "userId" to userId,
            "startDate" to startDate,
            "endDate" to endDate,
            "status" to status.name,
            "totalPrice" to totalPrice,
            "createdAt" to createdAt,
            "isHourly" to isHourly
        )
    }
    
    companion object {
        /**
         * Create a daily booking
         */
        fun createDaily(
            bikeId: String,
            userId: String,
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
                startDate = startDate,
                endDate = endDate,
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
                startDate = startDate,
                endDate = endDate,
                status = BookingStatus.PENDING,
                totalPrice = totalPrice,
                isHourly = true
            )
        }
    }
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
} 