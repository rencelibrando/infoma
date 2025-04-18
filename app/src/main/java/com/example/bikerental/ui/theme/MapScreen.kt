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
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import com.example.bikerental.utils.MapPreferencesManager
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun BikeMap(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    onBikeSelected: (BikeMapMarker) -> Unit = {},
    requestLocationUpdate: Boolean = false,
    bikeViewModel: BikeViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mapPreferencesManager = remember { MapPreferencesManager.getInstance(context) }
    
    // Intramuros location (center point) - default fallback position
    val intramurosLocation = remember { LatLng(14.5895, 120.9750) }

    // Define map properties
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED,
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
            )
        )
    }

    // Map UI settings
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false, // We'll use our custom button instead
                compassEnabled = true
            )
        )
    }

    // Track map loading state
    var isMapLoaded by remember { mutableStateOf(false) }

    // Camera position state - initially with default position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(intramurosLocation, 15f)
    }
    
    // Current user location handling
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Collect real-time bike data from ViewModel
    val availableBikes by bikeViewModel.availableBikes.collectAsState()
    val selectedBike by bikeViewModel.selectedBike.collectAsState()
    val activeRide by bikeViewModel.activeRide.collectAsState()
    val bikeLocation by bikeViewModel.bikeLocation.collectAsState()

    // Function to calculate distance between two LatLng points
    fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0] // Distance in meters
    }

    // Convert TrackableBike to BikeMapMarker
    val bikesWithDistance = remember(availableBikes, userLocation) {
        availableBikes.map { bike ->
            BikeMapMarker(
                id = bike.id,
                name = bike.name,
                type = bike.type,
                price = bike.price,
                position = LatLng(bike.latitude, bike.longitude),
                imageRes = bike.imageRes,
                distance = if (userLocation != null) {
                    val distanceInMeters = calculateDistance(userLocation!!, bike.position)
                    when {
                        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
                        else -> String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000)
                    }
                } else ""
            )
        }
    }

    // Route tracking
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    
    // Function to move camera to user's location
    fun moveToUserLocation() {
        if (fusedLocationProviderClient != null &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val newUserLocation = LatLng(it.latitude, it.longitude)
                    userLocation = newUserLocation

                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                newUserLocation,
                                16f
                            ),
                            durationMs = 1000
                        )
                    }
                }
            }
        }
    }

    // If there's an active ride, collect location updates
    LaunchedEffect(activeRide) {
        if (activeRide != null) {
            val bikeId = activeRide?.bikeId ?: return@LaunchedEffect
            bikeViewModel.startTrackingBike(bikeId)
            
            // If user is currently riding, update bike location with user location periodically
            while (isActive && activeRide != null) {
                userLocation?.let { location ->
                    bikeViewModel.updateBikeLocation(bikeId, location)
                }
                delay(5000) // Update every 5 seconds
            }
        }
    }

    // Save camera position when it changes significantly
    LaunchedEffect(cameraPositionState.position) {
        // Only save when map is fully loaded and camera position changes are user-initiated
        if (isMapLoaded && !cameraPositionState.isMoving) {
            // Add a small delay to avoid excessive writes
            delay(500)
            // Save to preferences on IO thread
            withContext(Dispatchers.IO) {
                mapPreferencesManager.saveCameraPosition(cameraPositionState.position)
                Log.d("BikeMap", "Saved camera position: ${cameraPositionState.position}")
            }
        }
    }

    // Load saved camera position
    LaunchedEffect(Unit) {
        // Collect from the flow once
        mapPreferencesManager.getCameraPositionFlow().collect { savedPosition ->
            if (savedPosition != null) {
                Log.d("BikeMap", "Restored camera position: $savedPosition")
                // Use saved position
                cameraPositionState.position = savedPosition
            } else {
                // No saved position, fall back to default behavior
                moveToUserLocation()
            }
            // Only collect once
            return@collect
        }
    }

    // Update route points when bike location changes
    LaunchedEffect(bikeLocation) {
        bikeLocation?.let { location ->
            routePoints = routePoints + location
        }
    }
    
    // Function to start a ride
    fun startRide(bike: BikeMapMarker) {
        userLocation?.let { location ->
            bikeViewModel.startRide(bike.id, location)
        }
    }
    
    // Function to end a ride
    fun endRide() {
        userLocation?.let { location ->
            bikeViewModel.endRide(location)
            routePoints = emptyList() // Clear route
        }
    }

    // Watch for "Locate Me" button press but don't override saved position on first load
    LaunchedEffect(requestLocationUpdate) {
        if (requestLocationUpdate && isMapLoaded) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                moveToUserLocation()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map composable
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = mapProperties,
            uiSettings = uiSettings,
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                isMapLoaded = true
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

                // Draw a circle around user's location (range indicator)
                Circle(
                    center = location,
                    radius = 300.0, // 300 meters radius
                    fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    strokeColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    strokeWidth = 2f
                )
            }

            // Available bikes markers - only show available bikes that aren't in use
            bikesWithDistance
                .filter { bike -> 
                    availableBikes.find { it.id == bike.id }?.isAvailable == true &&
                    availableBikes.find { it.id == bike.id }?.isInUse == false
                }
                .forEach { bike ->
                    MarkerInfoWindow(
                        state = MarkerState(position = bike.position),
                        title = bike.name,
                        snippet = "${bike.type} • ${bike.price}",
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
            
            // Draw the tracked bike if in use
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

            // Bike station markers
            intramurosStations.forEach { station ->
                Marker(
                    state = MarkerState(position = station.position),
                    title = station.name,
                    snippet = "${station.availableBikes} bikes available",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    zIndex = 1f
                )
            }
            
            // Draw ride path if active
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = MaterialTheme.colorScheme.primary,
                    width = 5f
                )
            }

            // Add a polygon outline of Intramuros boundaries for reference
            Polygon(
                points = intramurosOutlinePoints,
                fillColor = Color.Transparent,
                strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                strokeWidth = 3f
            )
        }
        
        // Loading indicator overlay
        if (!isMapLoaded) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}