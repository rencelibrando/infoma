package com.example.bikerental.ui.theme

import com.google.android.gms.maps.model.LatLng

/**
 * Data class to represent route information from Google Maps Routes API
 */
data class RouteInfo(
    val distance: String,
    val duration: String,
    val distanceValue: Int,
    val durationValue: Int,
    val polylinePoints: List<LatLng>,
    val steps: List<Step>,
    val trafficInfo: Map<Int, Double> = emptyMap() // Map of polyline point index to traffic speed level
) {
    /**
     * Data class to represent a step in the route (a segment with a single navigation instruction)
     */
    data class Step(
        val instruction: String,
        val distance: String,
        val distanceValue: Int,
        val duration: String,
        val startLocation: LatLng,
        val endLocation: LatLng,
        val maneuver: String, // e.g., "turn-right", "turn-left", "straight", etc.
        val polylinePoints: List<LatLng>
    )
} 