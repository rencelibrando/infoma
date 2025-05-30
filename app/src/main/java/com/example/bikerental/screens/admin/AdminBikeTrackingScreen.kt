package com.example.bikerental.screens.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikerental.models.Bike
import com.example.bikerental.models.BikeRide
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBikeTrackingScreen(
    bikeViewModel: BikeViewModel = viewModel()
) {
    val context = LocalContext.current
    val bikes by bikeViewModel.bikes.collectAsState()
    val activeRides by bikeViewModel.activeRides.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    
    var showMapView by remember { mutableStateOf(true) }
    var selectedBike by remember { mutableStateOf<Bike?>(null) }
    var selectedRide by remember { mutableStateOf<BikeRide?>(null) }
    
    // Track map instance
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    
    // Fetch data on launch
    LaunchedEffect(Unit) {
        bikeViewModel.fetchBikesFromFirestore()
        bikeViewModel.fetchActiveRides()
        
        // Set up real-time updates
        while (true) {
            delay(30000) // Update every 30 seconds
            bikeViewModel.fetchActiveRides()
        }
    }
    
    // Update map markers when data changes
    LaunchedEffect(bikes, activeRides) {
        googleMap?.let { map ->
            updateMapMarkers(map, bikes, activeRides)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        AdminTrackingHeader(
            showMapView = showMapView,
            onToggleView = { showMapView = !showMapView },
            totalBikes = bikes.size,
            activeBikes = bikes.count { it.isInUse },
            activeRides = activeRides.size
        )
        
        if (showMapView) {
            // Map View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            onCreate(null)
                            getMapAsync { map ->
                                googleMap = map
                                setupGoogleMap(map)
                                updateMapMarkers(map, bikes, activeRides)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Floating action button for centering map
                FloatingActionButton(
                    onClick = {
                        centerMapOnActiveBikes(googleMap, bikes, activeRides)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = ColorUtils.DarkGreen
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on active rides",
                        tint = Color.White
                    )
                }
            }
        } else {
            // List View
            AdminBikeTrackingList(
                bikes = bikes,
                activeRides = activeRides,
                isLoading = isLoading,
                onBikeSelected = { selectedBike = it },
                onRideSelected = { selectedRide = it }
            )
        }
    }
    
    // Bottom sheet for selected bike/ride details
    selectedBike?.let { bike ->
        BikeDetailsBottomSheet(
            bike = bike,
            activeRide = activeRides.find { it.bikeId == bike.id },
            onDismiss = { selectedBike = null }
        )
    }
    
    selectedRide?.let { ride ->
        RideDetailsBottomSheet(
            ride = ride,
            bike = bikes.find { it.id == ride.bikeId },
            onDismiss = { selectedRide = null }
        )
    }
}

@Composable
private fun AdminTrackingHeader(
    showMapView: Boolean,
    onToggleView: () -> Unit,
    totalBikes: Int,
    activeBikes: Int,
    activeRides: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bike Tracking Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // View toggle button
                Button(
                    onClick = onToggleView,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorUtils.DarkGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (showMapView) Icons.Default.List else Icons.Default.Map,
                        contentDescription = if (showMapView) "List View" else "Map View",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showMapView) "List" else "Map")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Statistics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    icon = Icons.Default.DirectionsBike,
                    label = "Total Bikes",
                    value = totalBikes.toString(),
                    color = ColorUtils.Info
                )
                
                StatCard(
                    icon = Icons.Default.PlayArrow,
                    label = "Active Bikes",
                    value = activeBikes.toString(),
                    color = ColorUtils.Success
                )
                
                StatCard(
                    icon = Icons.Default.Person,
                    label = "Active Rides",
                    value = activeRides.toString(),
                    color = ColorUtils.Warning
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminBikeTrackingList(
    bikes: List<Bike>,
    activeRides: List<BikeRide>,
    isLoading: Boolean,
    onBikeSelected: (Bike) -> Unit,
    onRideSelected: (BikeRide) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorUtils.DarkGreen)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active rides section
            if (activeRides.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Rides (${activeRides.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(activeRides) { ride ->
                    ActiveRideCard(
                        ride = ride,
                        bike = bikes.find { it.id == ride.bikeId },
                        onClick = { onRideSelected(ride) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // All bikes section
            item {
                Text(
                    text = "All Bikes (${bikes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(bikes) { bike ->
                BikeTrackingCard(
                    bike = bike,
                    activeRide = activeRides.find { it.bikeId == bike.id },
                    onClick = { onBikeSelected(bike) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveRideCard(
    ride: BikeRide,
    bike: Bike?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorUtils.Success.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(ColorUtils.Success)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bike?.name ?: "Unknown Bike",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rider: ${ride.userId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Started: ${formatTime(ride.startTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikeTrackingCard(
    bike: Bike,
    activeRide: BikeRide?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (bike.isInUse) ColorUtils.Success else ColorUtils.LightText
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bike.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (bike.isInUse) "In Use" else "Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bike.isInUse) ColorUtils.Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Location: ${bike.latitude}, ${bike.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (activeRide != null) {
                Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorUtils.Success,
                    modifier = Modifier
                        .background(
                            ColorUtils.Success.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikeDetailsBottomSheet(
    bike: Bike,
    activeRide: BikeRide?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = bike.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bike status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (bike.isInUse) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Status",
                    tint = if (bike.isInUse) ColorUtils.Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (bike.isInUse) "Currently in use" else "Available for rent",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bike details
            DetailRow("Type", bike.type)
            DetailRow("Location", "${bike.latitude}, ${bike.longitude}")
            DetailRow("Battery Level", "${bike.batteryLevel}%")
            DetailRow("Coordinates", "${bike.latitude}, ${bike.longitude}")
            
            activeRide?.let { ride ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Active Ride Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Rider", ride.userId)
                DetailRow("Start Time", formatTime(ride.startTime))
                DetailRow("Duration", "${(System.currentTimeMillis() - ride.startTime) / 60000} minutes")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideDetailsBottomSheet(
    ride: BikeRide,
    bike: Bike?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Ride Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow("Bike", bike?.name ?: "Unknown")
            DetailRow("Rider", ride.userId)
            DetailRow("Start Time", formatTime(ride.startTime))
            DetailRow("Duration", "${(System.currentTimeMillis() - ride.startTime) / 60000} minutes")
            
            bike?.let {
                Spacer(modifier = Modifier.height(16.dp))
                DetailRow("Current Location", "${it.latitude}, ${it.longitude}")
                DetailRow("Battery Level", "${it.batteryLevel}%")
                DetailRow("Coordinates", "${it.latitude}, ${it.longitude}")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun setupGoogleMap(map: GoogleMap) {
    map.apply {
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isCompassEnabled = true
        uiSettings.isMapToolbarEnabled = true
        
        // Set initial camera position to Philippines
        val philippines = LatLng(14.5995, 120.9842)
        moveCamera(CameraUpdateFactory.newLatLngZoom(philippines, 10f))
    }
}

private fun updateMapMarkers(
    map: GoogleMap,
    bikes: List<Bike>,
    activeRides: List<BikeRide>
) {
    map.clear()
    
    bikes.forEach { bike ->
        val isActive = activeRides.any { it.bikeId == bike.id }
        val markerColor = if (isActive) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_BLUE
        
        map.addMarker(
            MarkerOptions()
                .position(LatLng(bike.latitude, bike.longitude))
                .title(bike.name)
                .snippet(if (isActive) "Active ride" else "Available")
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
        )
    }
}

private fun centerMapOnActiveBikes(
    map: GoogleMap?,
    bikes: List<Bike>,
    activeRides: List<BikeRide>
) {
    map?.let {
        val activeBikes = bikes.filter { bike -> activeRides.any { it.bikeId == bike.id } }
        
        if (activeBikes.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            activeBikes.forEach { bike ->
                builder.include(LatLng(bike.latitude, bike.longitude))
            }
            val bounds = builder.build()
            val padding = 100
            it.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
} 