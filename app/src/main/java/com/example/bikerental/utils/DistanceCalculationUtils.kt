package com.example.bikerental.utils

import android.location.Location
import com.example.bikerental.models.BikeLocation
import com.google.android.gms.maps.model.LatLng

/**
 * CENTRALIZED Distance Calculation Utility
 * 
 * This is the SINGLE SOURCE OF TRUTH for all distance calculations in the app.
 * All ViewModels, Services, and UI components should use these functions.
 * 
 * MIGRATION PLAN:
 * 1. Replace BikeViewModel.calculateDistance() calls with DistanceCalculationUtils.calculateDistance()
 * 2. Replace MapViewModel.calculateDistance() calls with DistanceCalculationUtils.calculateDistance()
 * 3. Replace LocationManager.calculateDistance() calls with DistanceCalculationUtils.calculateDistance()
 * 4. Replace BikesTab.calculateDistanceOptimized() calls with DistanceCalculationUtils.calculateDistance()
 */
object DistanceCalculationUtils {

    /**
     * Calculate distance between two LatLng points in meters
     * Uses Android's Location.distanceBetween for maximum accuracy
     */
    fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0] // Distance in meters
    }

    /**
     * Calculate distance between two coordinate pairs in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] // Distance in meters
    }

    /**
     * Calculate distance between two BikeLocation points in meters
     */
    fun calculateDistance(start: BikeLocation, end: BikeLocation): Float {
        return calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude)
    }

    /**
     * Calculate distance in kilometers (for display purposes)
     */
    fun calculateDistanceKm(start: LatLng, end: LatLng): Float {
        return calculateDistance(start, end) / 1000f
    }

    /**
     * Calculate distance in kilometers (for display purposes)
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return calculateDistance(lat1, lon1, lat2, lon2) / 1000f
    }
} 