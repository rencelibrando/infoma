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
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.MapStyleOptions
import com.example.bikerental.R
import com.example.bikerental.ui.theme.map.BikeMapMarker
import com.example.bikerental.ui.theme.map.intramurosAvailableBikes
import com.example.bikerental.ui.theme.map.intramurosOutlinePoints
import com.example.bikerental.ui.theme.map.intramurosStations
import java.util.Locale

@Composable
fun BikeMap(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    availableBikes: List<BikeMapMarker> = intramurosAvailableBikes,
    onBikeSelected: (BikeMapMarker) -> Unit = {},
    requestLocationUpdate: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Intramuros location (center point)
    val intramurosLocation = remember{LatLng(14.5895, 120.9750)}

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

    // Bikes with distance information
    var bikesWithDistance by remember { mutableStateOf(availableBikes) }

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

    // Function to update distances to all bikes
    fun updateBikeDistances(currentLocation: LatLng) {
        bikesWithDistance = availableBikes.map { bike ->
            val distanceInMeters = calculateDistance(currentLocation, bike.position)
            val distanceText = when {
                distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
                else -> String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000)
            }

            // Create a new BikeMapMarker with updated distance
            bike.copy(
                // Here you would ideally add a distance field to BikeMapMarker,
                // but we'll handle this externally for now
            )
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
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val newUserLocation = LatLng(it.latitude, it.longitude)
                    userLocation = newUserLocation

                    // Update distances when we get location
                    updateBikeDistances(newUserLocation)

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

        // Available bikes markers
        bikesWithDistance.forEach { bike ->
            MarkerInfoWindow(
                state = MarkerState(position = bike.position),
                title = bike.name,
                snippet = "${bike.type} • ${bike.price}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                onClick = {
                    // Add distance information to the selected bike before passing it
                    userLocation?.let { location ->
                        val distanceInMeters = calculateDistance(location, bike.position)
                        val distanceText = when {
                            distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
                            else -> String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000)
                        }
                        // In a real implementation, you'd attach this to the bike object
                        // Here we're just calling the function
                    }
                    onBikeSelected(bike)
                    true
                },
                onInfoWindowClick = {
                    onBikeSelected(bike)
                },
                zIndex = 1f
            )
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

        // Add a polygon outline of Intramuros boundaries for reference
        Polygon(
            points = intramurosOutlinePoints,
            fillColor = Color.Transparent,
            strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            strokeWidth = 3f
        )
    }
}