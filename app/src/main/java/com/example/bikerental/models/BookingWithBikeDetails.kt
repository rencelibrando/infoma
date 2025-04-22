package com.example.bikerental.models

import java.util.*

/**
 * Model class that combines booking data with bike details for displaying in the UI
 */
data class BookingWithBikeDetails(
    val id: String = "",
    val userId: String = "",
    val bikeId: String = "",
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val status: String = "active", // active, completed, cancelled
    val totalPrice: String? = null,
    
    // Bike details
    val bikeName: String = "",
    val bikeType: String? = null,
    val bikeImageUrl: String = "",
    val location: String? = null,
    
    // Hourly booking info
    val isHourly: Boolean = false
) {
    /**
     * Get a formatted duration string based on booking type
     */
    fun getFormattedDuration(): String {
        val durationInMillis = endDate.time - startDate.time
        
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
        val startTimeStr = timeFormat.format(startDate)
        val endTimeStr = timeFormat.format(endDate)
        
        return "$startTimeStr - $endTimeStr"
    }
} 