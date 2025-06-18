package com.example.bikerental.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.example.bikerental.models.BikeLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Centralized location manager for tracking user location across the app
 */
class LocationManager(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
        .build()
    
    // Current location that can be observed by multiple components
    private val _currentLocation = mutableStateOf<LatLng?>(null)
    val currentLocation: MutableState<LatLng?> = _currentLocation
    
    // Track if location is currently being updated
    private val _isTracking = mutableStateOf(false)
    val isTracking: MutableState<Boolean> = _isTracking
    
    /**
     * Gets the last known location if permissions are granted
     */
    @SuppressLint("MissingPermission")
    fun getLastLocation(onSuccess: (LatLng) -> Unit, onFailure: () -> Unit) {
        if (hasLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _currentLocation.value = latLng
                    onSuccess(latLng)
                } ?: onFailure()
            }.addOnFailureListener {
                onFailure()
            }
        } else {
            onFailure()
        }
    }
    
    /**
     * Creates a flow of location updates
     */
    @SuppressLint("MissingPermission")
    fun locationFlow(): Flow<BikeLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = latLng
                    
                    val bikeLocation = BikeLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        speed = location.speed
                    )
                    trySend(bikeLocation)
                }
            }
        }
        
        _isTracking.value = true
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        awaitClose {
            _isTracking.value = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    /**
     * Calculate distance between two points
     */
    fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0]
    }
    
    /**
     * Calculate distance between current location and a point
     */
    fun distanceToCurrentLocation(point: LatLng): Float? {
        val current = _currentLocation.value ?: return null
        return calculateDistance(current, point)
    }
    
    /**
     * Checks if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Checks if a given location is within Intramuros, Manila
     * @param location The location to check
     * @return true if the location is inside Intramuros boundaries
     */
    fun isWithinIntramuros(location: LatLng): Boolean {
        // Define Intramuros polygon using coordinates of its boundaries
        // These coordinates define the approximate boundary of Intramuros, Manila
        val intramurosPolygon = listOf(
            LatLng(14.5867, 120.9754), // Northwest corner
            LatLng(14.5875, 120.9790), // Northeast corner
            LatLng(14.5893, 120.9815), // East point
            LatLng(14.5866, 120.9846), // Southeast corner
            LatLng(14.5826, 120.9832), // South point
            LatLng(14.5813, 120.9788), // Southwest corner
            LatLng(14.5826, 120.9758), // West point
            LatLng(14.5867, 120.9754)  // Back to start to close the polygon
        )
        
        // Check if the location is inside the polygon
        return PolyUtil.containsLocation(location, intramurosPolygon, true)
    }
    
    /**
     * Checks if the current location is within Intramuros
     * @return true if current location is inside Intramuros, false otherwise
     * Returns null if current location is not available
     */
    fun isCurrentLocationWithinIntramuros(): Boolean? {
        val current = _currentLocation.value ?: return null
        return isWithinIntramuros(current)
    }
    
    /**
     * Checks if a location is near the boundary of Intramuros (within warning distance but still inside)
     * @param location The location to check
     * @param warningDistance Distance in meters from boundary to trigger warning
     * @return true if location is near the boundary but still inside
     */
    fun isNearIntramurosExit(location: LatLng, warningDistance: Float = 50f): Boolean {
        // First check if the location is inside Intramuros
        if (!isWithinIntramuros(location)) {
            return false // Already outside, so not "approaching" exit
        }
        
        // Define the Intramuros polygon
        val intramurosPolygon = listOf(
            LatLng(14.5867, 120.9754),
            LatLng(14.5875, 120.9790),
            LatLng(14.5893, 120.9815),
            LatLng(14.5866, 120.9846),
            LatLng(14.5826, 120.9832),
            LatLng(14.5813, 120.9788),
            LatLng(14.5826, 120.9758),
            LatLng(14.5867, 120.9754)
        )
        
        // Check distance to each edge of the polygon
        for (i in 0 until intramurosPolygon.size - 1) {
            val distanceToEdge = distanceToLine(
                location,
                intramurosPolygon[i],
                intramurosPolygon[i + 1]
            )
            
            // If we're close to an edge, trigger warning
            if (distanceToEdge <= warningDistance) {
                return true
            }
        }
        
        // Also check the closing edge
        val distanceToClosingEdge = distanceToLine(
            location,
            intramurosPolygon.last(),
            intramurosPolygon.first()
        )
        
        return distanceToClosingEdge <= warningDistance
    }
    
    /**
     * Calculate the shortest distance from a point to a line segment
     */
    private fun distanceToLine(point: LatLng, lineStart: LatLng, lineEnd: LatLng): Float {
        val results = FloatArray(1)
        
        // If start and end are the same point, just calculate distance to that point
        if (lineStart.latitude == lineEnd.latitude && lineStart.longitude == lineEnd.longitude) {
            Location.distanceBetween(
                point.latitude, point.longitude,
                lineStart.latitude, lineStart.longitude,
                results
            )
            return results[0]
        }
        
        // Vector math to calculate distance to line segment
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude - lineStart.latitude
        
        // Calculate projection of point onto line
        val t = ((point.longitude - lineStart.longitude) * dx + 
                 (point.latitude - lineStart.latitude) * dy) / 
                (dx * dx + dy * dy)
        
        // Find the nearest point on the line segment
        val nearestPoint = when {
            t <= 0 -> lineStart // Before start point
            t >= 1 -> lineEnd   // After end point
            else -> LatLng(    // On the line segment
                lineStart.latitude + t * dy,
                lineStart.longitude + t * dx
            )
        }
        
        // Calculate distance to the nearest point
        Location.distanceBetween(
            point.latitude, point.longitude,
            nearestPoint.latitude, nearestPoint.longitude,
            results
        )
        
        return results[0]
    }
    
    companion object {
        const val UPDATE_INTERVAL = 5000L
        const val FASTEST_INTERVAL = 2000L
        
        // Singleton instance
        @Volatile private var instance: LocationManager? = null
        
        fun getInstance(context: Context): LocationManager {
            return instance ?: synchronized(this) {
                instance ?: LocationManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
} 