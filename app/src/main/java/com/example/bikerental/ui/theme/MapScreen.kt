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

@Composable
fun BikeMap(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    onBikeSelected: (BikeMapMarker) -> Unit = {},
    requestLocationUpdate: Boolean = false,
    bikeViewModel: BikeViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Create dedicated scopes for different types of background operations
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val computeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Intramuros location (center point)
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

    // Camera position state - start centered on Intramuros
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

    // Flow for storing processed bike markers with distance calculations
    // Use a StateFlow to handle bikesWithDistance that can be updated from background threads
    val bikesWithDistanceFlow = remember { MutableStateFlow<List<BikeMapMarker>>(emptyList()) }
    val bikesWithDistance by bikesWithDistanceFlow.collectAsState()

    // Move distance calculation to background thread
    LaunchedEffect(availableBikes, userLocation) {
        computeScope.launch {
            val processedBikes = availableBikes.map { bike ->
                val distance = if (userLocation != null) {
                    calculateDistanceSuspend(userLocation!!, bike.position)
                } else null
                
                BikeMapMarker(
                    id = bike.id,
                    name = bike.name,
                    type = bike.type,
                    price = bike.price,
                    position = LatLng(bike.latitude, bike.longitude),
                    imageRes = bike.imageRes,
                    distance = when {
                        distance == null -> ""
                        distance < 1000 -> "${distance.roundToInt()} m"
                        else -> String.format(Locale.getDefault(), "%.1f km", distance / 1000)
                    }
                )
            }
            bikesWithDistanceFlow.value = processedBikes
        }
    }

    // Route tracking
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // If there's an active ride, collect location updates
    LaunchedEffect(activeRide) {
        if (activeRide != null) {
            val bikeId = activeRide?.bikeId ?: return@LaunchedEffect
            bikeViewModel.startTrackingBike(bikeId)
            
            // If user is currently riding, update bike location with user location periodically
            while (isActive && activeRide != null) {
                userLocation?.let { location ->
                    // Process location updates in IO thread
                    ioScope.launch {
                        bikeViewModel.updateBikeLocation(bikeId, location)
                    }
                }
                delay(5000) // Update every 5 seconds
            }
        }
    }

    // Update route points when bike location changes
    LaunchedEffect(bikeLocation) {
        bikeLocation?.let { location ->
            // Append route points in computation thread to avoid UI jank
            computeScope.launch {
                val updatedRoute = routePoints + location
                withContext(Dispatchers.Main) {
                    routePoints = updatedRoute
                }
            }
        }
    }

    // Function to move camera to user's location
    fun moveToUserLocation() {
        if (fusedLocationProviderClient != null &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Move location retrieval to IO thread
            ioScope.launch {
                try {
                    val locationTask = fusedLocationProviderClient.lastLocation
                    val location = locationTask.await()
                    
                    if (location != null) {
                        val newUserLocation = LatLng(location.latitude, location.longitude)
                        
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            userLocation = newUserLocation
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(
                                    newUserLocation,
                                    16f
                                ),
                                durationMs = 1000
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BikeMap", "Error getting location: ${e.message}")
                }
            }
        }
    }
    
    // Function to start a ride
    fun startRide(bike: BikeMapMarker) {
        userLocation?.let { location ->
            // Process ride start in background
            ioScope.launch {
                bikeViewModel.startRide(bike.id, location)
            }
        }
    }
    
    // Function to end a ride
    fun endRide() {
        userLocation?.let { location ->
            // Process ride end in background
            ioScope.launch {
                bikeViewModel.endRide(location)
                withContext(Dispatchers.Main) {
                    routePoints = emptyList() // Clear route on UI thread
                }
            }
        }
    }

    // Initial location check - this gets called once on composition
    LaunchedEffect(true) {
        moveToUserLocation()
    }

    // Watch for "Locate Me" button press
    LaunchedEffect(requestLocationUpdate) {
        if (requestLocationUpdate) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                moveToUserLocation()
            }
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
}

// Convert distance calculation to a suspend function that runs off the main thread
suspend fun calculateDistanceSuspend(start: LatLng, end: LatLng): Float {
    return withContext(Dispatchers.Default) {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        results[0] // Distance in meters
    }
}