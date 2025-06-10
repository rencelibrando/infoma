package com.example.bikerental.models

import java.util.*

/**
 * Model class that combines booking data with bike details for displaying in the UI
 */
data class BookingWithBikeDetails(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val bikeId: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val status: String = "active", // active, completed, cancelled
    val totalPrice: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Bike details
    val bikeName: String = "",
    val bikeType: String? = null,
    val bikeImageUrl: String = "",
    val location: String? = null,
    val bikePricePerHour: Double = 0.0, // Added bike's hourly rate
    
    // Hourly booking info
    val isHourly: Boolean = false
) {
    /**
     * Secondary constructor that accepts Booking and specific bike details
     */
    constructor(
        booking: Booking,
        bikeName: String,
        bikeImage: String,
        bikeType: String,
        bikePricePerHour: Double = 0.0
    ) : this(
        id = booking.id,
        userId = booking.userId,
        userName = booking.userName,
        bikeId = booking.bikeId,
        startDate = booking.startDate,
        endDate = booking.endDate,
        status = booking.status.name,
        totalPrice = booking.totalPrice.toString(),
        createdAt = booking.createdAt,
        bikeName = bikeName,
        bikeType = bikeType,
        bikeImageUrl = bikeImage,
        bikePricePerHour = bikePricePerHour,
        isHourly = booking.isHourly
    )
    
    /**
     * Get a formatted duration string based on booking type
     */
    fun getFormattedDuration(): String {
        val durationInMillis = endDate - startDate
        
        return if (isHourly) {
            // For hourly bookings, show hours and minutes
            val hours = durationInMillis / (1000 * 60 * 60)
            val minutes = (durationInMillis % (1000 * 60 * 60)) / (1000 * 60)
            
            if (minutes > 0) {
                "$hours hr $minutes min"
            } else {
                "$hours ${if (hours == 1L) "hour" else "hours"}"
            }
        } else {
            // For daily bookings, show days
            val days = (durationInMillis / (1000 * 60 * 60 * 24)) + 1
            "$days ${if (days == 1L) "day" else "days"}"
        }
    }
    
    /**
     * Get a formatted time range for an hourly booking
     */
    fun getTimeRange(): String {
        if (!isHourly) return ""
        
        val timeFormat = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
        val startTimeStr = timeFormat.format(Date(startDate))
        val endTimeStr = timeFormat.format(Date(endDate))
        
        return "$startTimeStr - $endTimeStr"
    }

    /**
     * Get formatted hourly rate of the bike
     */
    fun getFormattedHourlyRate(): String {
        return "₱$bikePricePerHour/hr"
    }
} 