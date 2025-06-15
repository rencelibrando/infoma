package com.example.bikerental.utils

import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import com.example.bikerental.utils.RideMetricsUtils.formatSpeed
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*
import com.example.bikerental.ui.theme.RouteInfo

/**
 * CENTRALIZED Formatting Utilities
 * 
 * Consolidates all formatting functions used across the app for consistency.
 * This replaces duplicate formatting in:
 * - RideDashboard.kt
 * - MapTab.kt
 * - RideMetricsUtils.kt
 * - Various other components
 */
object FormattingUtils {

    /**
     * Format distance for display with appropriate units
     */
    fun formatDistance(distanceInMeters: Float): String {
        return when {
            distanceInMeters < 0 -> "0 m"
            !distanceInMeters.isFinite() -> "0 m"
            distanceInMeters < 1000 -> "${distanceInMeters.roundToInt()} m"
            distanceInMeters < 10000 -> String.format(Locale.US, "%.2f km", distanceInMeters / 1000)
            distanceInMeters < 100000 -> String.format(Locale.US, "%.1f km", distanceInMeters / 1000)
            else -> "${(distanceInMeters / 1000).roundToInt()} km"
        }
    }

    /**
     * Format speed for display with validation and edge case handling
     */
    fun formatSpeed(speedInKmh: Float): String {
        return when {
            speedInKmh < 0 -> "0 km/h"
            !speedInKmh.isFinite() -> "0 km/h"
            speedInKmh >= 100 -> "99+ km/h"
            speedInKmh < 0.1 -> "0 km/h"
            speedInKmh < 10 -> String.format(Locale.US, "%.1f km/h", speedInKmh)
            else -> "${speedInKmh.roundToInt()} km/h"
        }
    }

    /**
     * Format duration from milliseconds
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs < 0 || durationMs > TimeUnit.DAYS.toMillis(1)) return "00:00"
        
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        
        return when {
            hours > 23 -> "23:59:59" // Cap at 24 hours max
            hours > 0 -> String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format duration from start time to current time (for live updates)
     */
    fun formatDurationFromStart(startTime: Long): String {
        val currentTime = System.currentTimeMillis()
        
        // Handle edge cases
        if (startTime <= 0 || startTime > currentTime) {
            return "00:00"
        }
        
        val duration = currentTime - startTime
        return formatDuration(duration)
    }

    /**
     * Format cost for display
     */
    fun formatCost(cost: Double, currency: String = "₱"): String {
        return when {
            cost < 0 -> "$currency 0.00"
            !cost.isFinite() -> "$currency 0.00"
            else -> String.format(Locale.US, "$currency %.2f", cost)
        }
    }

    /**
     * Safe float to string conversion with validation
     */
    fun Float.safeToString(decimals: Int = 1): String {
        return when {
            !this.isFinite() -> "0"
            this < 0 -> "0"
            decimals == 0 -> this.roundToInt().toString()
            else -> String.format(Locale.US, "%.${decimals}f", this)
        }
    }

    /**
     * Format coordinates for display
     */
    fun formatCoordinates(lat: Double, lng: Double, precision: Int = 6): String {
        if (!lat.isFinite() || !lng.isFinite()) {
            return "Invalid coordinates"
        }
        return String.format(Locale.US, "%.${precision}f, %.${precision}f", lat, lng)
    }

    /**
     * Calculate and format the Estimated Time of Arrival (ETA) string
     */
    fun calculateEtaString(
        route: RouteInfo,
        currentStepIndex: Int,
        currentLocation: LatLng?
    ): String {
        // If current location is null, return default ETA based on route data
        if (currentLocation == null) {
            return formatEta(route.durationValue.toLong())
        }
        
        // Calculate remaining distance
        val remainingDistanceInCurrentStep = DistanceCalculationUtils.calculateDistance(
            currentLocation,
            route.steps[currentStepIndex].endLocation
        )
        val remainingDistanceInFutureSteps = route.steps.drop(currentStepIndex + 1).sumOf { it.distanceValue }
        val totalRemainingDistance = remainingDistanceInCurrentStep + remainingDistanceInFutureSteps

        // Calculate estimated average speed from the initial route data
        val averageSpeedMps = if (route.durationValue > 0) {
            route.distanceValue.toFloat() / route.durationValue.toFloat()
        } else {
            5f // Default to 5 m/s (18 km/h) if duration is zero
        }

        // Calculate remaining time in seconds
        val remainingTimeSeconds = if (averageSpeedMps > 0) {
            (totalRemainingDistance / averageSpeedMps).toLong()
        } else {
            0L
        }
        
        return formatEta(remainingTimeSeconds)
    }

    /**
     * Format ETA from seconds to a user-friendly string (e.g., "15 min", "< 1 min")
     */
    private fun formatEta(seconds: Long): String {
        return when {
            seconds < 60 -> "< 1 min"
            seconds < 3600 -> "${seconds / 60} min"
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "$hours hr $minutes min"
            }
        }
    }
} 