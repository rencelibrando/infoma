package com.example.bikerental.utils

import android.location.Location
import com.example.bikerental.models.BikeLocation
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.*

/**
 * Shared utility functions for ride metrics calculations
 * Ensures consistency across ViewModels, Services, and UI components
 */
object RideMetricsUtils {

    // Constants for validation
    const val MAX_REALISTIC_SPEED_KMH = 100f // 100 km/h maximum for bikes
    const val MIN_ACCURACY_THRESHOLD = 50f // 50 meters minimum accuracy
    const val MAX_GPS_JUMP_DISTANCE = 100000f // 100km maximum jump between GPS points (in meters)

    /**
     * Calculate distance between two LatLng points
     */
    fun calculateDistanceInMeters(start: LatLng, end: LatLng): Float {
        return DistanceCalculationUtils.calculateDistance(start, end)
    }

    /**
     * Calculate distance between two BikeLocation points
     */
    fun calculateDistanceInMeters(start: BikeLocation, end: BikeLocation): Float {
        return DistanceCalculationUtils.calculateDistance(start, end)
    }

    /**
     * Validate GPS location data quality
     */
    fun isValidGPSLocation(location: BikeLocation): Boolean {
        return location.latitude != 0.0 && 
               location.longitude != 0.0 &&
               Math.abs(location.latitude) <= 90.0 &&
               Math.abs(location.longitude) <= 180.0 &&
               location.accuracy <= MIN_ACCURACY_THRESHOLD
    }

    /**
     * Validate if distance between two points is realistic (filters GPS jumps)
     */
    fun isRealisticDistance(distanceMeters: Float, timeIntervalMs: Long): Boolean {
        if (timeIntervalMs <= 0) return true // Can't validate without time data
        
        val timeHours = timeIntervalMs / 3600000.0 // Convert to hours
        val speedKmh = (distanceMeters / 1000.0) / timeHours
        
        return speedKmh < MAX_REALISTIC_SPEED_KMH && distanceMeters < MAX_GPS_JUMP_DISTANCE
    }

    /**
     * Validate speed value
     */
    fun isRealisticSpeed(speedKmh: Float): Boolean {
        return speedKmh >= 0f && speedKmh < MAX_REALISTIC_SPEED_KMH
    }

    /**
     * Calculate average speed from a list of BikeLocation points
     */
    fun calculateAverageSpeed(path: List<BikeLocation>): Float {
        if (path.size < 2) return 0f
        
        val validSpeeds = path.mapNotNull { location ->
            val speed = location.speedKmh
            if (isRealisticSpeed(speed)) speed else null
        }
        
        return if (validSpeeds.isNotEmpty()) {
            validSpeeds.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculate maximum speed from a list of BikeLocation points
     */
    fun calculateMaxSpeed(path: List<BikeLocation>): Float {
        if (path.isEmpty()) return 0f
        
        return path.mapNotNull { location ->
            val speed = location.speedKmh
            if (isRealisticSpeed(speed)) speed else null
        }.maxOrNull() ?: 0f
    }

    /**
     * Calculate total distance from a path of BikeLocation points with GPS noise filtering
     */
    fun calculateTotalDistance(path: List<BikeLocation>): Float {
        if (path.size < 2) return 0f
        
        var totalDistance = 0f
        
        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]
            
            // Validate both locations
            if (isValidGPSLocation(prev) && isValidGPSLocation(curr)) {
                val segmentDistance = calculateDistanceInMeters(prev, curr)
                val timeInterval = curr.timestamp - prev.timestamp
                
                // Only add distance if it's realistic
                if (isRealisticDistance(segmentDistance, timeInterval)) {
                    totalDistance += segmentDistance
                }
            }
        }
        
        return totalDistance
    }

    /**
     * Format distance for display
     */
    fun formatDistance(distanceInMeters: Float): String {
        return when {
            distanceInMeters < 0 -> "0 m"
            distanceInMeters < 1000 -> "${distanceInMeters.roundToInt()} m"
            distanceInMeters < 10000 -> String.format(Locale.US, "%.2f km", distanceInMeters / 1000)
            else -> String.format(Locale.US, "%.1f km", distanceInMeters / 1000)
        }
    }

    /**
     * Format speed for display
     */
    fun formatSpeed(speedInKmh: Float): String {
        return when {
            speedInKmh < 0 -> "0 km/h"
            speedInKmh >= MAX_REALISTIC_SPEED_KMH -> "99+ km/h"
            speedInKmh < 10 -> String.format(Locale.US, "%.1f km/h", speedInKmh)
            else -> "${speedInKmh.roundToInt()} km/h"
        }
    }

    /**
     * Format duration for display
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs < 0) return "0:00"
        
        val hours = durationMs / 3600000
        val minutes = (durationMs % 3600000) / 60000
        val seconds = (durationMs % 60000) / 1000
        
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
} 