package com.example.bikerental.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.MapStyleOptions
import com.example.bikerental.R
import com.example.bikerental.models.TrackableBike
import com.example.bikerental.ui.theme.map.BikeMapMarker
import com.example.bikerental.ui.theme.map.intramurosOutlinePoints
import com.example.bikerental.ui.theme.map.intramurosStations
import com.example.bikerental.viewmodels.BikeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.Job
import androidx.compose.runtime.derivedStateOf
import java.util.concurrent.ConcurrentHashMap

// Cache for distance calculations to avoid repeated calculations
private val distanceCache = ConcurrentHashMap<String, Float>()

// Optimized distance calculation with caching
suspend fun calculateDistanceCached(start: LatLng, end: LatLng): Float {
    val key = "${start.latitude},${start.longitude}-${end.latitude},${end.longitude}"
    return distanceCache.getOrPut(key) {
        withContext(Dispatchers.Default) {
            val results = FloatArray(1)
            Location.distanceBetween(
                start.latitude, start.longitude,
                end.latitude, end.longitude,
                results
            )
            results[0]
        }
    }
}

@Composable
fun BikeMap(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    onBikeSelected: (BikeMapMarker) -> Unit = {},
    requestLocationUpdate: Boolean = false,
    bikeViewModel: BikeViewModel = viewModel()
) {
    val context = LocalContext.current

    // Create singleton coroutine scopes that are properly disposed
    val backgroundScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val computationScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Properly dispose scopes when composable is removed
    DisposableEffect(Unit) {
        onDispose {
            backgroundScope.coroutineContext[Job]?.cancel()
            computationScope.coroutineContext[Job]?.cancel()
        }
    }

    // Intramuros location (center point) - immutable
    val intramurosLocation = remember { LatLng(14.5895, 120.9750) }

    // Define map properties - memoized to prevent recreation
    val mapProperties = remember {
        MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
        )
    }

    // Map UI settings - memoized
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true
        )
    }

    // Camera position state - start centered on Intramuros
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(intramurosLocation, 15f)
    }

    // Current user location handling
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Collect real-time bike data from ViewModel with optimized state management
    val availableBikes by bikeViewModel.availableBikes.collectAsState()
    val selectedBike by bikeViewModel.selectedBike.collectAsState()
    val activeRide by bikeViewModel.activeRide.collectAsState()
    val bikeLocation by bikeViewModel.bikeLocation.collectAsState()

    // Optimized bike markers with distance calculations
    val bikesWithDistance by remember {
        derivedStateOf {
            if (userLocation == null) {
                availableBikes.map { bike ->
                    BikeMapMarker(
                        id = bike.id,
                        name = bike.name,
                        type = bike.type,
                        price = bike.price,
                        position = LatLng(bike.latitude, bike.longitude),
                        imageRes = bike.imageRes,
                        distance = ""
                    )
                }
            } else {
                availableBikes.map { bike ->
                    BikeMapMarker(
                        id = bike.id,
                        name = bike.name,
                        type = bike.type,
                        price = bike.price,
                        position = LatLng(bike.latitude, bike.longitude),
                        imageRes = bike.imageRes,
                        distance = "" // Will be calculated asynchronously
                    )
                }
            }
        }
    }

    // Separate state for distances to avoid blocking UI
    var bikeDistances by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Calculate distances asynchronously only when needed
    LaunchedEffect(availableBikes, userLocation) {
        userLocation?.let { location ->
            computationScope.launch {
                val distances = mutableMapOf<String, String>()
                availableBikes.forEach { bike ->
                    val bikePosition = LatLng(bike.latitude, bike.longitude)
                    val distance = calculateDistanceCached(location, bikePosition)
                    distances[bike.id] = when {
                        distance < 1000 -> "${distance.roundToInt()} m"
                        else -> String.format(Locale.getDefault(), "%.1f km", distance / 1000)
                    }
                }
                withContext(Dispatchers.Main) {
                    bikeDistances = distances
                }
            }
        }
    }

    // Route tracking with optimized memory management
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Optimized location tracking for active rides
    LaunchedEffect(activeRide) {
        activeRide?.let { ride ->
            bikeViewModel.startTrackingBike(ride.bikeId)
            
            // Reduced update frequency for better performance
            while (isActive && activeRide != null) {
                userLocation?.let { location ->
                    backgroundScope.launch {
                        bikeViewModel.updateBikeLocation(ride.bikeId, location)
                    }
                }
                delay(10000) // Update every 10 seconds instead of 5
            }
        }
    }

    // Optimized route points update
    LaunchedEffect(bikeLocation) {
        bikeLocation?.let { location ->
            computationScope.launch {
                val newRoutePoints = routePoints + location
                // Limit route points to prevent memory issues (keep last 1000 points)
                val optimizedRoutePoints = if (newRoutePoints.size > 1000) {
                    newRoutePoints.takeLast(1000)
                } else {
                    newRoutePoints
                }
                
                withContext(Dispatchers.Main) {
                    routePoints = optimizedRoutePoints
                }
            }
        }
    }

    // Optimized location function
    suspend fun moveToUserLocationSuspend() {
        if (fusedLocationProviderClient != null &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val location = withContext(Dispatchers.IO) {
                    fusedLocationProviderClient.lastLocation.await()
                }
                
                location?.let {
                    val newUserLocation = LatLng(it.latitude, it.longitude)
                    userLocation = newUserLocation
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(newUserLocation, 16f),
                        durationMs = 1000
                    )
                }
            } catch (e: Exception) {
                Log.e("BikeMap", "Error getting location: ${e.message}")
            }
        }
    }
    
    // Optimized ride management functions
    fun startRide(bike: BikeMapMarker) {
        userLocation?.let { location ->
            backgroundScope.launch {
                bikeViewModel.startRide(bike.id, location)
            }
        }
    }
    
    fun endRide() {
        userLocation?.let { location ->
            backgroundScope.launch {
                bikeViewModel.endRide(location)
                withContext(Dispatchers.Main) {
                    routePoints = emptyList()
                }
            }
        }
    }

    // Initial location check
    LaunchedEffect(Unit) {
        moveToUserLocationSuspend()
    }

    // Watch for location update requests
    LaunchedEffect(requestLocationUpdate) {
        if (requestLocationUpdate && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            moveToUserLocationSuspend()
        }
    }

    // Memoized filtered bikes to prevent unnecessary recomposition
    val filteredBikes = remember(bikesWithDistance, availableBikes) {
        bikesWithDistance.filter { bike -> 
            availableBikes.find { it.id == bike.id }?.let { 
                it.isAvailable
            } ?: false
        }
    }

    // Map composable
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        properties = mapProperties,
        uiSettings = uiSettings,
        cameraPositionState = cameraPositionState,
        onMapLoaded = {
            Log.d("BikeMap", "Map loaded successfully")
        }
    ) {
        // User's current location marker
        userLocation?.let { location ->
            Marker(
                state = MarkerState(position = location),
                title = "Your Location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                zIndex = 2f
            )

            // Optimized circle with reduced alpha for better performance
            Circle(
                center = location,
                radius = 300.0,
                fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                strokeColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                strokeWidth = 2f
            )
        }

        // Optimized bike markers with cached distances
        filteredBikes.forEach { bike ->
            val distance = bikeDistances[bike.id] ?: ""
            MarkerInfoWindow(
                state = MarkerState(position = bike.position),
                title = bike.name,
                snippet = "${bike.type} • ${bike.price} ${if (distance.isNotEmpty()) "• $distance" else ""}",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (selectedBike?.id == bike.id) 
                        BitmapDescriptorFactory.HUE_GREEN 
                    else 
                        BitmapDescriptorFactory.HUE_RED
                ),
                onClick = {
                    if (activeRide == null) {
                        bikeViewModel.selectBike(bike.id)
                        onBikeSelected(bike)
                    }
                    true
                },
                onInfoWindowClick = {
                    if (activeRide == null) {
                        onBikeSelected(bike)
                    }
                },
                zIndex = 1f
            )
        }
        
        // Tracked bike marker
        bikeLocation?.let { location ->
            if (activeRide != null) {
                Marker(
                    state = MarkerState(position = location),
                    title = selectedBike?.name ?: "Your Bike",
                    snippet = "In use",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    zIndex = 3f
                )
            }
        }

        // Memoized station markers
        intramurosStations.forEach { station ->
            Marker(
                state = MarkerState(position = station.position),
                title = station.name,
                snippet = "${station.availableBikes} bikes available",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                zIndex = 1f
            )
        }
        
        // Optimized route rendering
        if (routePoints.isNotEmpty()) {
            Polyline(
                points = routePoints,
                color = MaterialTheme.colorScheme.primary,
                width = 5f
            )
        }

        // Memoized polygon outline
        Polygon(
            points = intramurosOutlinePoints,
            fillColor = Color.Transparent,
            strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            strokeWidth = 3f
        )
    }
}