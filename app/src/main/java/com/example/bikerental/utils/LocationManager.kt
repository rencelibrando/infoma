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