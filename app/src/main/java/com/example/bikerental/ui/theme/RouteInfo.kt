package com.example.bikerental.ui.theme

import com.google.android.gms.maps.model.LatLng

/**
 * Data class for storing route information from the Directions API
 */
data class RouteInfo(
    val distance: String = "",
    val duration: String = "",
    val distanceValue: Int = 0, // distance in meters
    val durationValue: Int = 0, // duration in seconds
    val polylinePoints: List<LatLng> = emptyList(),
    val steps: List<Step> = emptyList()
) {
    /**
     * Data class for storing step information (turn-by-turn directions)
     */
    data class Step(
        val instruction: String,
        val distance: String,
        val distanceValue: Int,
        val duration: String,
        val startLocation: LatLng,
        val endLocation: LatLng,
        val maneuver: String = ""
    )
} 