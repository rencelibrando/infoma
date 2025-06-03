@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.bikerental.screens.tabs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikerental.ui.theme.BikeLocation
import com.example.bikerental.ui.theme.RouteInfo
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.QRCodeHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await
import android.util.Log
import android.annotation.SuppressLint
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import com.example.bikerental.components.QRScannerDialog
import com.example.bikerental.components.StartRideDialog
import com.example.bikerental.components.BikeUnlockDialog
import com.example.bikerental.components.RideRatingDialog
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.bikerental.BuildConfig
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

// Singleton HTTP client for better performance
private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
}

// Cache for route calculations to avoid repeated API calls
private val routeCache = ConcurrentHashMap<String, List<RouteInfo>>()

// Optimized polyline decoder that runs asynchronously
suspend fun decodePoly(encoded: String): List<LatLng> = withContext(Dispatchers.Default) {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(p)
    }
    poly
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapTab(bikeViewModel: BikeViewModel = viewModel()) {
    val context = LocalContext.current
    rememberScrollState()
    
    // Create optimized coroutine scopes with proper lifecycle management
    val backgroundScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val computationScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Properly dispose scopes when composable is removed
    DisposableEffect(Unit) {
        onDispose {
            backgroundScope.coroutineContext[Job]?.cancel()
            computationScope.coroutineContext[Job]?.cancel()
        }
    }
    
    // Permission states
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasLocationPermission = locationPermissionState.status.isGranted
    
    // Notification permission (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    // Multiple permissions launcher for requesting all at once
    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    )
    
    // Permission dialog state
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Location services
    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    // Default location (Manila)
    val defaultLocation = LatLng(14.5890, 120.9760)
    
    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }
    
    // State for Start Ride flow
    var showStartRideDialog by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    
    // Observe bike unlocking states
    val isUnlockingBike by bikeViewModel.isUnlockingBike.collectAsState()
    val unlockError by bikeViewModel.unlockError.collectAsState()
    val unlockSuccess by bikeViewModel.unlockSuccess.collectAsState()
    val activeRide by bikeViewModel.activeRide.collectAsState()
    
    // Handle unlock error
    LaunchedEffect(unlockError) {
        unlockError?.let { error ->
            // Show error snackbar or dialog
            Log.e("MapTab", "Unlock error: $error")
            // Reset error after showing for longer to ensure user sees it
            kotlinx.coroutines.delay(5000) // Show error for 5 seconds
            bikeViewModel.resetUnlockStates()
        }
    }
    
    // Handle successful unlock - navigate to ride screen
    LaunchedEffect(unlockSuccess) {
        if (unlockSuccess) {
            showQRScanner = false
            showStartRideDialog = false
            Log.d("MapTab", "Ride started successfully, resetting UI states")
            bikeViewModel.resetUnlockStates()
        }
    }
    
    // Get current location when permission is granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val location = getCurrentLocationSuspend(fusedLocationProviderClient)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 15f),
                    durationMs = 1000
                )
            } catch (e: Exception) {
                Log.e("MapTab", "Error getting location", e)
            }
        }
    }
    
    // Handle permission requests
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            showPermissionDialog = true
        }
    }

    // Optimized directions fetching with caching
    suspend fun fetchDirections(origin: LatLng, destination: LatLng): List<RouteInfo> {
        val cacheKey = "${origin.latitude},${origin.longitude}-${destination.latitude},${destination.longitude}"
        
        // Check cache first
        routeCache[cacheKey]?.let { return it }
        
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = "AIzaSyASfb-LFSstZrbPUIgPn1rKOqNTFF6mhhk" // Replace with your API key
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origin.latitude},${origin.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&mode=bicycling" +
                        "&alternatives=true" +
                        "&key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "BikeRental/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                val jsonData = response.body?.string() ?: ""
                
                // Process JSON in computation thread
                val routes = withContext(Dispatchers.Default) {
                    val routeList = mutableListOf<RouteInfo>()
                    val jsonObject = JSONObject(jsonData)
                    
                    if (jsonObject.getString("status") == "OK") {
                        val routesArray = jsonObject.getJSONArray("routes")
                        
                        // Process routes in parallel
                        val deferredRoutes = (0 until routesArray.length()).map { i ->
                            async {
                                val route = routesArray.getJSONObject(i)
                                val leg = route.getJSONArray("legs").getJSONObject(0)
                                val steps = mutableListOf<String>()
                                
                                // Extract steps
                                val stepsArray = leg.getJSONArray("steps")
                                for (j in 0 until stepsArray.length()) {
                                    val step = stepsArray.getJSONObject(j)
                                    steps.add(step.getString("html_instructions"))
                                }

                                // Decode polyline asynchronously
                                val polylinePoints = decodePoly(
                                    route.getJSONObject("overview_polyline").getString("points")
                                )

                                RouteInfo(
                                    distance = leg.getJSONObject("distance").getString("text"),
                                    duration = leg.getJSONObject("duration").getString("text"),
                                    polylinePoints = polylinePoints,
                                    steps = steps,
                                    isAlternative = i > 0
                                )
                            }
                        }
                        
                        routeList.addAll(deferredRoutes.map { it.await() })
                    }
                    routeList
                }
                
                // Cache the result
                routeCache[cacheKey] = routes
                routes
                
            } catch (e: Exception) {
                Log.e("MapTab", "Error fetching directions: ${e.message}")
                emptyList()
            }
        }
    }

    // Memoized map properties
    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapType = MapType.NORMAL
        )
    }

    // Memoized UI settings
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        )

        // Bottom Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Show different UI based on ride status
            if (activeRide != null) {
                // Enhanced Ride Dashboard with real-time data
                val rideDistance by bikeViewModel.rideDistance.collectAsState()
                val currentSpeed by bikeViewModel.currentSpeed.collectAsState()
                val activeRideStartTime = activeRide?.startTime ?: System.currentTimeMillis()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header with live indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ride in Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Live indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        
                        // Real-time stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Duration
                            RideStatCard(
                                icon = Icons.Default.Timer,
                                label = "Duration",
                                value = formatDuration(activeRideStartTime),
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Distance
                            RideStatCard(
                                icon = Icons.Default.Route,
                                label = "Distance",
                                value = formatDistance(rideDistance),
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Speed
                            RideStatCard(
                                icon = Icons.Default.Speed,
                                label = "Speed",
                                value = formatSpeed(currentSpeed),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // End Ride Button
                        Button(
                            onClick = {
                                // End the ride using current location
                                if (hasLocationPermission) {
                                    endCurrentRide(fusedLocationProviderClient, bikeViewModel)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "End Ride",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // START RIDE Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { 
                            if (permissionsState.allPermissionsGranted) {
                                showStartRideDialog = true
                            } else {
                                showPermissionDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorUtils.DarkGreen
                        ),
                        shape = RoundedCornerShape(22.dp),
                        enabled = !isUnlockingBike
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = "Start Ride",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "START RIDE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Error Snackbar
        unlockError?.let { error ->
            LaunchedEffect(error) {
                // Show error for a few seconds then clear
                kotlinx.coroutines.delay(3000)
                bikeViewModel.resetUnlockStates()
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ Unlock Failed",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { bikeViewModel.resetUnlockStates() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Dismiss",
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Permissions Required")
                }
            },
            text = {
                Column {
                    Text("To start a bike ride, we need:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Location access for GPS tracking")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Text("• Notification permission for ride updates")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "These permissions ensure your safety and enable real-time tracking during rides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        permissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Start Ride Dialog
    StartRideDialog(
        isVisible = showStartRideDialog,
        onDismiss = { showStartRideDialog = false },
        onStartQRScan = {
            showStartRideDialog = false
            showQRScanner = true
        }
    )
    
    // QR Scanner Dialog
    QRScannerDialog(
        isVisible = showQRScanner,
        onDismiss = { 
            showQRScanner = false
            bikeViewModel.resetUnlockStates()
        },
        onQRCodeScanned = { qrCode ->
            Log.d("MapTab", "QR Code scanned: $qrCode")
            
            // Debug the QR code using the enhanced helper
            QRCodeHelper.debugQRCode(qrCode)
            
            // Validate QR code format first
            if (!QRCodeHelper.isValidQRCodeFormat(qrCode)) {
                Log.w("MapTab", "Invalid QR code format detected")
                Log.e("MapTab", "Invalid QR code format. Please try scanning again.")
                showQRScanner = false
                bikeViewModel.resetUnlockStates()
                return@QRScannerDialog
            }
            
            if (hasLocationPermission) {
                showQRScanner = false
                Log.d("MapTab", "Starting bike unlock process with valid QR code")
                // Get current location and unlock bike
                getCurrentLocationAndUnlockBike(
                    fusedLocationProviderClient = fusedLocationProviderClient,
                    qrCode = qrCode,
                    bikeViewModel = bikeViewModel
                )
            } else {
                Log.w("MapTab", "Location permission not granted")
                Log.e("MapTab", "Location permission is required to unlock bikes")
                bikeViewModel.resetUnlockStates()
                showQRScanner = false
            }
        },
        title = "Scan Bike QR Code"
    )
    
    // Bike Unlock Loading Dialog
    BikeUnlockDialog(
        isVisible = isUnlockingBike,
        onDismiss = { 
            bikeViewModel.resetUnlockStates()
        }
    )
    
    // Ride Rating Dialog
    val showRideRating by bikeViewModel.showRideRating.collectAsState()
    val completedRide by bikeViewModel.completedRide.collectAsState()
    val rideDistance by bikeViewModel.rideDistance.collectAsState()
    
    RideRatingDialog(
        isVisible = showRideRating,
        rideDuration = completedRide?.let { ride ->
            (ride.endTime ?: System.currentTimeMillis()) - ride.startTime
        } ?: 0L,
        distanceTraveled = rideDistance,
        totalCost = completedRide?.cost ?: 0.0,
        onRatingSubmit = { rating, feedback ->
            bikeViewModel.submitRideRating(rating, feedback)
        },
        onDismiss = {
            bikeViewModel.dismissRideRating()
        }
    )
}

@Composable
fun MapLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// OPTIMIZED: Enhanced location retrieval with better error handling and coroutine optimization
@SuppressLint("MissingPermission")
private suspend fun getCurrentLocationSuspend(fusedLocationProviderClient: FusedLocationProviderClient?): LatLng {
    return withContext(Dispatchers.IO) {
        try {
            if (fusedLocationProviderClient == null) {
                Log.w("MapTab", "Location provider is null, using default coordinates")
                return@withContext LatLng(14.5890, 120.9760) // Default coordinates for Philippines
            }

            val location = fusedLocationProviderClient.lastLocation.await()
            if (location != null) {
                Log.d("MapTab", "Location obtained: ${location.latitude}, ${location.longitude}")
                LatLng(location.latitude, location.longitude)
            } else {
                Log.w("MapTab", "Last known location is null, using default coordinates")
                LatLng(14.5890, 120.9760)
            }
        } catch (e: SecurityException) {
            Log.e("MapTab", "Location permission denied: ${e.message}")
            LatLng(14.5890, 120.9760)
        } catch (e: Exception) {
            Log.e("MapTab", "Error getting location: ${e.message}")
            LatLng(14.5890, 120.9760)
        }
    }
}

// OPTIMIZED: Simplified location-based bike unlock with improved coroutine scope
private fun getCurrentLocationAndUnlockBike(
    fusedLocationProviderClient: FusedLocationProviderClient,
    qrCode: String,
    bikeViewModel: BikeViewModel
) {
    // Use proper coroutine scope instead of GlobalScope
    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
        try {
            val location = getCurrentLocationSuspend(fusedLocationProviderClient)
            bikeViewModel.validateQRCodeAndUnlockBike(qrCode, location)
        } catch (e: Exception) {
            Log.e("MapTab", "Error getting location for bike unlock", e)
            bikeViewModel.resetUnlockStates()
        }
    }
}

@Composable
private fun RideStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun formatDuration(startTime: Long): String {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000) // Update every second
        }
    }
    
    val duration = currentTime - startTime
    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
    
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun formatDistance(distanceInMeters: Float): String {
    return if (distanceInMeters < 1000) {
        "${distanceInMeters.roundToInt()} m"
    } else {
        String.format(Locale.US, "%.2f km", distanceInMeters / 1000)
    }
}

private fun formatSpeed(speedInMps: Float): String {
    val speedKmh = speedInMps * 3.6f
    return "${speedKmh.roundToInt()} km/h"
}

// Function to end the current ride
private fun endCurrentRide(
    fusedLocationProviderClient: FusedLocationProviderClient,
    bikeViewModel: BikeViewModel
) {
    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
        try {
            val location = getCurrentLocationSuspend(fusedLocationProviderClient)
            bikeViewModel.endRideWithTracking(location)
        } catch (e: Exception) {
            Log.e("MapTab", "Error getting location for ending ride", e)
            // End ride with last known location or default
            bikeViewModel.endRideWithTracking(LatLng(14.5890, 120.9760))
        }
    }
} 