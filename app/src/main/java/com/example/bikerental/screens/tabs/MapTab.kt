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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.FloatingActionButton
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.PatternItem
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
import com.example.bikerental.components.SOSDialog
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.bikerental.utils.FormattingUtils.formatDistance
import com.example.bikerental.utils.FormattingUtils.formatDuration
import com.example.bikerental.utils.FormattingUtils
import com.example.bikerental.utils.RideMetricsUtils.formatSpeed
import com.example.bikerental.components.MapNavigationUI
import com.example.bikerental.components.NavigationProgressBar
import com.example.bikerental.components.RideProgressDialog
import com.example.bikerental.utils.RoutesApiService
import kotlinx.coroutines.flow.update
import androidx.compose.foundation.BorderStroke
import com.example.bikerental.components.DestinationSearchDialog
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableIntStateOf
import com.example.bikerental.utils.FormattingUtils.calculateEtaString

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapTab(bikeViewModel: BikeViewModel = viewModel()) {
    val context = LocalContext.current
    rememberScrollState()
    
    // SharedPreferences for ride session persistence
    val prefs = remember { 
        context.getSharedPreferences("ride_session", android.content.Context.MODE_PRIVATE) 
    }
    
    // SharedPreferences for camera state persistence
    val cameraPrefs = remember {
        context.getSharedPreferences("camera_state", android.content.Context.MODE_PRIVATE)
    }
    
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
    
    // Camera position state with POV configuration
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 18f) // Higher zoom for POV
    }
    
    // State for Start Ride flow
    var showStartRideDialog by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    var showSOSDialog by remember { mutableStateOf(false) }
    
    // Observe bike unlocking states
    val isUnlockingBike by bikeViewModel.isUnlockingBike.collectAsState()
    val unlockError by bikeViewModel.unlockError.collectAsState()
    val unlockSuccess by bikeViewModel.unlockSuccess.collectAsState()
    val activeRide by bikeViewModel.activeRide.collectAsState()
    
    // Enhanced ride tracking states
    val ridePath by bikeViewModel.ridePath.collectAsState()
    val userBearing by bikeViewModel.userBearing.collectAsState()
    val currentLocation by bikeViewModel.currentLocation.collectAsState()
    
    // POV Navigation states with SharedPreferences persistence
    var isPOVMode by remember { mutableStateOf(prefs.getBoolean("isPOVMode", false)) }
    var showRouteOverview by remember { mutableStateOf(false) }
    var isRideResumed by remember { mutableStateOf(false) }
    var isRestoringCamera by remember { mutableStateOf(false) }
    
    // New navigation states for the Directions API
    var selectedDestination by remember { mutableStateOf<LatLng?>(null) }
    var isRoutingMode by remember { mutableStateOf(false) }
    var showDestinationSearch by remember { mutableStateOf(false) }
    var currentRoutes by remember { mutableStateOf<List<RouteInfo>>(emptyList()) }
    var selectedRouteIndex by remember { mutableIntStateOf(0) }
    var currentNavigationStep by remember { mutableIntStateOf(0) }
    var distanceToNextTurn by remember { mutableIntStateOf(0) }
    var isNavigationActive by remember { mutableStateOf(false) }
    var timeRemainingString by remember { mutableStateOf("0 min") }
    
    // Calculate the currently selected route
    val selectedRoute = remember(currentRoutes, selectedRouteIndex) {
        if (currentRoutes.isNotEmpty() && selectedRouteIndex < currentRoutes.size) {
            currentRoutes[selectedRouteIndex]
        } else null
    }
    
    // Calculate ETA and time remaining
    LaunchedEffect(selectedRoute, currentLocation) {
        if (selectedRoute != null && isNavigationActive && currentLocation != null) {
            // Update distance to next turn
            val nextInstruction = RoutesApiService.getNextNavigationInstruction(
                currentLocation = currentLocation,
                route = selectedRoute,
                currentStepIndex = currentNavigationStep
            )
            distanceToNextTurn = nextInstruction.second
            
            // Check if we need to advance to the next navigation step
            if (distanceToNextTurn < 20 && currentNavigationStep < selectedRoute.steps.size - 1) {
                currentNavigationStep++
            }
            
            // Calculate remaining time for the entire route
            timeRemainingString = calculateEtaString(
                route = selectedRoute,
                currentStepIndex = currentNavigationStep,
                currentLocation = currentLocation
            )
        }
    }
    
    // Initialize RoutesApiService on first composition
    LaunchedEffect(Unit) {
        try {
            Log.d("MapTab", "Initializing RoutesApiService")
            RoutesApiService.initialize(context)
            Log.d("MapTab", "RoutesApiService initialized successfully")
        } catch (e: Exception) {
            Log.e("MapTab", "Failed to initialize RoutesApiService", e)
            
            // Show a toast message to inform the user
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context, 
                    "Navigation features may be limited. Please check your internet connection.", 
                    Toast.LENGTH_LONG
                ).show()
            }
            
            // Try initialization again after delay
            delay(3000)
            try {
                RoutesApiService.initialize(context)
                Log.d("MapTab", "RoutesApiService initialization retry successful")
            } catch (e: Exception) {
                Log.e("MapTab", "Failed to initialize RoutesApiService on retry", e)
            }
        }
    }
    
    // Function to calculate route using Routes API v2
    val calculateRoute = remember {
        { origin: LatLng, destination: LatLng ->
            backgroundScope.launch {
                try {
                    Log.d("MapTab", "Calculating route from $origin to $destination")
                    
                    // Show loading toast
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Calculating route...", Toast.LENGTH_SHORT).show()
                    }
                    
                    val result = RoutesApiService.getRoutes(
                        origin = origin,
                        destination = destination,
                        mode = "BICYCLE", // Explicitly use BICYCLE mode for proper road-based routes
                        alternatives = true,
                        useMockData = false // Use mock data only if API fails
                    )
                    
                    result.onSuccess { routes ->
                        Log.d("MapTab", "Route calculation successful, received ${routes.size} routes")
                        if (routes.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                currentRoutes = routes
                                selectedRouteIndex = 0
                                currentNavigationStep = 0
                                showRouteOverview = true
                                isRoutingMode = true
                                
                                // Show success toast
                                Toast.makeText(context, "Route found", Toast.LENGTH_SHORT).show()
                                
                                // Center camera on route overview
                                if (routes[0].polylinePoints.isNotEmpty()) {
                                    Log.d("MapTab", "Centering camera on route overview")
                                    // Calculate bounds to include origin, destination and route
                                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                                        .include(origin)
                                        .include(destination)
                                    
                                    // Add some points from the route (not all to avoid performance issues)
                                    val routePoints = routes[0].polylinePoints
                                    val stride = maxOf(1, routePoints.size / 10)
                                    for (i in routePoints.indices step stride) {
                                        bounds.include(routePoints[i])
                                    }
                                    
                                    try {
                                        val padding = 100 // pixels
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngBounds(bounds.build(), padding),
                                            durationMs = 1000
                                        )
                                    } catch (e: Exception) {
                                        Log.e("MapTab", "Failed to animate camera to route bounds", e)
                                    }
                                }
                            }
                        } else {
                            // No routes returned
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "No routes found - using fallback route", Toast.LENGTH_SHORT).show()
                                
                                // Create a fallback route even if no routes were returned
                                val fallbackRoutes = RoutesApiService.getRoutes(
                                    origin = origin,
                                    destination = destination,
                                    mode = "BICYCLE",
                                    alternatives = false,
                                    useMockData = true // Explicitly use mock data
                                ).getOrNull() ?: emptyList()
                                
                                if (fallbackRoutes.isNotEmpty()) {
                                    currentRoutes = fallbackRoutes
                                    selectedRouteIndex = 0
                                    currentNavigationStep = 0
                                    showRouteOverview = true
                                    isRoutingMode = true
                                }
                            }
                        }
                    }.onFailure { error ->
                        Log.e("MapTab", "Failed to get routes: ${error.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Using fallback route: ${error.message}", Toast.LENGTH_SHORT).show()
                            
                            // Always generate fallback route on failure
                            val fallbackRoutes = RoutesApiService.getRoutes(
                                origin = origin,
                                destination = destination,
                                mode = "BICYCLE",
                                alternatives = false,
                                useMockData = true // Explicitly use mock data
                            ).getOrNull() ?: emptyList()
                            
                            if (fallbackRoutes.isNotEmpty()) {
                                currentRoutes = fallbackRoutes
                                selectedRouteIndex = 0
                                currentNavigationStep = 0
                                showRouteOverview = true
                                isRoutingMode = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MapTab", "Error calculating route", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Using fallback route due to error", Toast.LENGTH_SHORT).show()
                        
                        // Generate fallback route on any exception
                        val fallbackRoutes = RoutesApiService.getRoutes(
                            origin = origin,
                            destination = destination,
                            mode = "BICYCLE",
                            alternatives = false,
                            useMockData = true // Explicitly use mock data
                        ).getOrNull() ?: emptyList()
                        
                        if (fallbackRoutes.isNotEmpty()) {
                            currentRoutes = fallbackRoutes
                            selectedRouteIndex = 0
                            currentNavigationStep = 0
                            showRouteOverview = true
                            isRoutingMode = true
                        }
                    }
                }
            }
        }
    }
    
    // Persistent camera state for POV restoration
    var lastKnownPOVLocation: LatLng? by remember { 
        mutableStateOf(
            if (cameraPrefs.contains("last_lat") && cameraPrefs.contains("last_lng")) {
                LatLng(
                    cameraPrefs.getFloat("last_lat", 0f).toDouble(),
                    cameraPrefs.getFloat("last_lng", 0f).toDouble()
                )
            } else null
        )
    }
    var lastKnownBearing by remember { mutableStateOf(cameraPrefs.getFloat("last_bearing", 0f)) }
    var lastKnownZoom by remember { mutableStateOf(cameraPrefs.getFloat("last_zoom", 19f)) }
    var lastKnownTilt by remember { mutableStateOf(cameraPrefs.getFloat("last_tilt", 60f)) }
    
    // Function to save camera state for POV restoration
    val saveCameraState = remember {
        { location: LatLng, bearing: Float, zoom: Float, tilt: Float ->
            cameraPrefs.edit().apply {
                putFloat("last_lat", location.latitude.toFloat())
                putFloat("last_lng", location.longitude.toFloat())
                putFloat("last_bearing", bearing)
                putFloat("last_zoom", zoom)
                putFloat("last_tilt", tilt)
                apply()
            }
            lastKnownPOVLocation = location
            lastKnownBearing = bearing
            lastKnownZoom = zoom
            lastKnownTilt = tilt
        }
    }
    
    // Function to clear camera state when ride ends
    val clearCameraState = remember {
        {
            cameraPrefs.edit().clear().apply()
            lastKnownPOVLocation = null
            lastKnownBearing = 0f
            lastKnownZoom = 19f
            lastKnownTilt = 60f
        }
    }
    
    // ENHANCED: Auto-enable POV mode when ride starts OR is resumed
    LaunchedEffect(activeRide) {
        val currentRide = activeRide
        if (currentRide != null) {
            Log.d("MapTab", "Active ride detected: ${currentRide.id}")
            val wasAlreadyInPOV = prefs.getBoolean("isPOVMode", false)
            isPOVMode = true
            isRideResumed = !wasAlreadyInPOV // Only mark as resumed if POV wasn't already enabled
            
            // Save POV state to SharedPreferences
            prefs.edit().putBoolean("isPOVMode", true).apply()
            
            // Start location tracking if ride is active
            bikeViewModel.startLocationTracking()
            
            // If resuming ride and we have saved camera state, restore it immediately
            if (isRideResumed && lastKnownPOVLocation != null) {
                Log.d("MapTab", "Resuming ride - restoring camera state to saved position")
                isRestoringCamera = true
                
                val restoredPosition = CameraPosition.Builder()
                    .target(lastKnownPOVLocation!!)
                    .zoom(lastKnownZoom)
                    .bearing(lastKnownBearing)
                    .tilt(lastKnownTilt)
                    .build()
                
                withContext(Dispatchers.Main) {
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(restoredPosition),
                            durationMs = 2500 // Slightly longer animation for smooth restoration
                        )
                        Log.d("MapTab", "Camera state restored successfully")
                        
                        // Brief delay before allowing normal updates
                        delay(2500)
                        isRestoringCamera = false
                    } catch (e: Exception) {
                        Log.e("MapTab", "Error restoring camera state", e)
                        isRestoringCamera = false
                    }
                }
            }
            
            Log.d("MapTab", "POV mode enabled and location tracking started")
        } else {
            isPOVMode = false
            showRouteOverview = false
            isRideResumed = false
            isRestoringCamera = false
            
            // Clear POV state and camera state when ride ends
            prefs.edit().putBoolean("isPOVMode", false).apply()
            clearCameraState()
            
            Log.d("MapTab", "No active ride - POV mode disabled and camera state cleared")
        }
    }
    
    // ENHANCED: Update camera position for POV mode with better handling and state persistence
    LaunchedEffect(currentLocation, userBearing, isPOVMode, isRideResumed, isRestoringCamera) {
        // Skip updates during camera restoration to avoid conflicts
        if (isRestoringCamera) {
            Log.d("MapTab", "Skipping camera update during restoration")
            return@LaunchedEffect
        }
        
        if (isPOVMode && currentLocation != null && activeRide != null) {
            val newZoom = 19f
            val newTilt = 60f
            
            val newPosition = CameraPosition.Builder()
                .target(currentLocation!!)
                .zoom(newZoom) // Close zoom for navigation
                .bearing(userBearing) // Follow user's heading
                .tilt(newTilt) // Tilted view for POV effect
                .build()
            
            // Ensure we're on main thread for Google Maps API
            withContext(Dispatchers.Main) {
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(newPosition),
                        durationMs = if (isRideResumed) 2000 else 1000 // Slower animation on resume
                    )
                    
                    // Save current camera state for persistence
                    saveCameraState(currentLocation!!, userBearing, newZoom, newTilt)
                    
                    if (isRideResumed) {
                        Log.d("MapTab", "Camera position updated after ride resume")
                        // Reset the flag after first position update
                        isRideResumed = false
                    }else{
                        Log.d("MapTab", "Camera position updated")
                    }
                } catch (e: Exception) {
                    Log.e("MapTab", "Error animating camera to POV position", e)
                }
            }
        }
    }
    
    // Handle unlock error
    LaunchedEffect(unlockError) {
        unlockError?.let { error ->
            Log.e("MapTab", "Unlock error: $error")
            kotlinx.coroutines.delay(5000)
            bikeViewModel.resetUnlockStates()
        }
    }
    
    // Handle successful unlock
    LaunchedEffect(unlockSuccess) {
        if (unlockSuccess) {
            showQRScanner = false
            showStartRideDialog = false
            Log.d("MapTab", "Ride started successfully, enabling POV mode")
            bikeViewModel.resetUnlockStates()
        }
    }
    
    // Get current location when permission is granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val location = getCurrentLocationSuspend(fusedLocationProviderClient)
                if (!isPOVMode) {
                    // Fix: Ensure we're on main thread for Google Maps API
                    withContext(Dispatchers.Main) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(location, 15f),
                            durationMs = 1000
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MapTab", "Error getting location", e)
            }
        }
    }
    
    // Fallback restoration: If we have saved POV state but no current location yet
    LaunchedEffect(hasLocationPermission, isPOVMode, activeRide) {
        if (hasLocationPermission && isPOVMode && activeRide != null && 
            currentLocation == null && lastKnownPOVLocation != null && !isRestoringCamera) {
            
            Log.d("MapTab", "Fallback restoration: Using saved location while waiting for GPS")
            
            val fallbackPosition = CameraPosition.Builder()
                .target(lastKnownPOVLocation!!)
                .zoom(lastKnownZoom)
                .bearing(lastKnownBearing)
                .tilt(lastKnownTilt)
                .build()
            
            withContext(Dispatchers.Main) {
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(fallbackPosition),
                        durationMs = 1500
                    )
                    Log.d("MapTab", "Fallback camera position applied successfully")
                } catch (e: Exception) {
                    Log.e("MapTab", "Error applying fallback camera position", e)
                }
            }
        }
    }
    
    // Handle permission requests
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            showPermissionDialog = true
        }
    }

    // Memoized map properties with POV optimizations
    val mapProperties = remember(hasLocationPermission, isPOVMode) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission && !isPOVMode, // Hide default location button in POV
            mapType = MapType.NORMAL,
            isTrafficEnabled = false // Enable traffic for navigation
        )
    }

    // Enhanced UI settings for POV navigation
    val uiSettings = remember(isPOVMode) {
        MapUiSettings(
            zoomControlsEnabled = isPOVMode,
            myLocationButtonEnabled = isPOVMode,
            mapToolbarEnabled = false,
            compassEnabled = isPOVMode, // Show compass in POV mode
            rotationGesturesEnabled = isPOVMode, // Disable rotation in POV
            scrollGesturesEnabled = isPOVMode, // Limit scrolling in POV
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
                    onMapLongClick = { latLng ->
            // Long press to set destination when in active ride but not navigating
            if (activeRide != null && currentLocation != null && !isUnlockingBike && !isNavigationActive) {
                Log.d("MapTab", "Map long click - setting destination at $latLng")
                selectedDestination = latLng
                // Calculate route from current location to selected destination
                calculateRoute(currentLocation!!, latLng)
            }
        }
        ) {
            // Render ride path as polyline
            if (ridePath.isNotEmpty()) {
                Polyline(
                    points = ridePath,
                    color = Color(0xFF2E7D32), // Green path color
                    width = 8f,
                    pattern = null,
                    geodesic = true
                )
            }
            
            // Current location marker with direction indicator
            currentLocation?.let { location ->
                if (activeRide != null) {
                    Marker(
                        state = MarkerState(position = location),
                        title = "You are here",
                        snippet = "Current location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                        rotation = userBearing,
                        flat = true
                    )
                }
            }
            
            // Show destination marker when selected
            selectedDestination?.let { destination ->
                Marker(
                    state = MarkerState(position = destination),
                    title = "Destination",
                    snippet = "Selected destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
            
            // Enhanced route visualization with improved colors and effects
            if (selectedRoute != null && currentLocation != null) {
                // First, render the full route as an underlayer if we have the full polyline
                if (selectedRoute.polylinePoints.isNotEmpty()) {
                    // Draw route background/outline for better visibility
                    Polyline(
                        points = selectedRoute.polylinePoints,
                        color = Color(0xFFFFFFFF), // White outline
                        width = 12f,
                        geodesic = true,
                        zIndex = 0f
                    )
                    
                    // Draw main route line with optimized styling
                    Polyline(
                        points = selectedRoute.polylinePoints,
                        color = Color(0xFF1A73E8).copy(alpha = 0.8f), // Google Maps blue
                        width = 8f,
                        geodesic = true,
                        zIndex = 1f
                    )
                }
                
                // Then, render each step with appropriate colors to show progress
                if (selectedRoute.steps.isNotEmpty()) {
                    selectedRoute.steps.forEachIndexed { index, step ->
                        if (step.polylinePoints.isNotEmpty()) {
                            val (color, width, zIndex) = when {
                                index < currentNavigationStep -> Triple(
                                    Color(0xFF9E9E9E).copy(alpha = 0.7f), // Gray for traveled
                                    6f,
                                    2f
                                )
                                index == currentNavigationStep -> Triple(
                                    Color(0xFF1A73E8), // Bright blue for current
                                    10f,
                                    3f
                                )
                                else -> Triple(
                                    Color(0xFF757575).copy(alpha = 0.5f), // Darker gray with transparency for upcoming
                                    5f,
                                    1f
                                )
                            }
                            
                            // Draw the outer stroke first for emphasis on current segment
                            if (index == currentNavigationStep) {
                                Polyline(
                                    points = step.polylinePoints,
                                    color = Color.White,
                                    width = width + 4f,
                                    geodesic = true,
                                    zIndex = zIndex - 0.1f
                                )
                            }
                            
                            // Draw the actual step polyline
                            Polyline(
                                points = step.polylinePoints,
                                color = color,
                                width = width,
                                geodesic = true,
                                zIndex = zIndex
                            )
                        }
                    }
                    
                    // Add decision points and maneuver indicators
                    selectedRoute.steps.forEachIndexed { index, step ->
                        if (step.maneuver.isNotEmpty() && step.maneuver != "straight") {
                            // More prominent visualization for turn points
                            Circle(
                                center = step.startLocation,
                                radius = if (index == currentNavigationStep) 8.0 else 5.0,
                                fillColor = if (index == currentNavigationStep) 
                                    Color(0xFF1A73E8) else Color(0xFF757575),
                                strokeColor = Color.White,
                                strokeWidth = 2f,
                                zIndex = 4f
                            )
                        }
                    }
                }

                // Show start and destination markers
                if (selectedRoute.polylinePoints.isNotEmpty()) {
                    // Start marker
                    Marker(
                        state = MarkerState(position = selectedRoute.polylinePoints.first()),
                        title = "Start",
                        snippet = "Starting point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                    
                    // Destination marker
                    Marker(
                        state = MarkerState(position = selectedRoute.polylinePoints.last()),
                        title = "Destination",
                        snippet = "End point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                } else {
                    // Fallback markers if no polyline points
                    selectedDestination?.let { destination ->
                        Marker(
                            state = MarkerState(position = destination),
                            title = "Destination",
                            snippet = "Selected destination",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }
                    
                    currentLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Start",
                            snippet = "Current location",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )
                        
                        // Draw simple straight line as absolute fallback
                        selectedDestination?.let { destination ->
                            Polyline(
                                points = listOf(location, destination),
                                color = Color(0xFF1E88E5), // Blue
                                width = 8f,
                                geodesic = true,
                                pattern = listOf(Dash(20f))
                            )
                        }
                    }
                }
            }
        }

        // Navigation UI components - only show during active navigation
        // Collect the state properly using collectAsState()
        val currentSpeed by bikeViewModel.currentSpeed.collectAsState()
        MapNavigationUI(
            isVisible = isNavigationActive && selectedRoute != null && currentLocation != null,
            currentRoute = selectedRoute,
            currentLocation = currentLocation,
            currentSpeed = currentSpeed,
            currentStep = currentNavigationStep,
            distanceToNextTurn = distanceToNextTurn,
            timeRemaining = timeRemainingString,
            onStepChanged = { step -> currentNavigationStep = step }
        )

        // POV Mode Controls (Top)
        if (activeRide != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // POV Toggle
                    FloatingActionButton(
                        onClick = { 
                            isPOVMode = !isPOVMode
                            // Save POV state to SharedPreferences
                            prefs.edit().putBoolean("isPOVMode", isPOVMode).apply()
                            
                            // Clear camera state if exiting POV mode
                            if (!isPOVMode) {
                                clearCameraState()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (isPOVMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = if (isPOVMode) "Exit POV" else "Enter POV",
                            tint = if (isPOVMode) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Center on location
                    FloatingActionButton(
                        onClick = {
                            currentLocation?.let { location ->
                                val newPosition = if (isPOVMode) {
                                    val newZoom = 19f
                                    val newTilt = 60f
                                    val position = CameraPosition.Builder()
                                        .target(location)
                                        .zoom(newZoom)
                                        .bearing(userBearing)
                                        .tilt(newTilt)
                                        .build()
                                    
                                    // Save camera state when manually centering in POV mode
                                    saveCameraState(location, userBearing, newZoom, newTilt)
                                    position
                                } else {
                                    CameraPosition.fromLatLngZoom(location, 15f)
                                }
                                
                                backgroundScope.launch {
                                    // Ensure we're on the main thread for Google Maps animations
                                    withContext(Dispatchers.Main) {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(newPosition),
                                            durationMs = 1000
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Center on location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Enhanced SOS Button (Left side during active ride)
        if (activeRide != null) {
            FloatingActionButton(
                onClick = { showSOSDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = "Emergency SOS",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Route Selection UI - show when routes are calculated but not actively navigating
        if (showRouteOverview && selectedRoute != null && !isNavigationActive) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 120.dp, top = 16.dp) // Position above ride progress
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Route header with distance and time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Route to Destination",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Text(
                                text = selectedRoute.duration,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = selectedRoute.distance,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Alternative routes selector
                    if (currentRoutes.size > 1) {
                        Text(
                            text = "Route Options",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentRoutes.forEachIndexed { index, route ->
                                val isSelected = selectedRouteIndex == index
                                OutlinedButton(
                                    onClick = { selectedRouteIndex = index },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                        else 
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(
                                        text = "Route ${index + 1}",
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Start Navigation Button
                    Button(
                        onClick = {
                            // Start active navigation with selected route
                            isNavigationActive = true
                            showRouteOverview = false
                            
                            // Enter POV mode automatically
                            isPOVMode = true
                            prefs.edit().putBoolean("isPOVMode", true).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null
                            )
                            Text(
                                text = "Start Navigation",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cancel button
                    TextButton(
                        onClick = {
                            // Clear route data
                            selectedDestination = null
                            currentRoutes = emptyList()
                            showRouteOverview = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Bottom Action Buttons
        if (activeRide != null) {
            // Get ride data from the view model
            val rideDistance by bikeViewModel.rideDistance.collectAsState()
            val currentSpeed by bikeViewModel.currentSpeed.collectAsState()
            val maxSpeed by bikeViewModel.maxSpeed.collectAsState()
            
            // Position the ride progress UI at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Use the enhanced RideProgressDialog with integrated route information
                RideProgressDialog(
                    isVisible = true,
                    activeRide = activeRide,
                    rideDistance = rideDistance,
                    currentSpeed = currentSpeed,
                    maxSpeed = maxSpeed,
                    onDismiss = { /* Dialog stays visible while ride is active */ },
                    onEndRide = {
                        if (hasLocationPermission) {
                            // End active navigation if it's running
                            isNavigationActive = false
                            selectedDestination = null
                            currentRoutes = emptyList()
                            
                            endCurrentRide(fusedLocationProviderClient, bikeViewModel)
                        }
                    },
                    onShowSOS = { showSOSDialog = true },
                    onSetDestination = {
                        if (currentLocation != null) {
                            showDestinationSearch = true
                        }
                    },
                    // Pass route information to the dialog
                    selectedRoute = selectedRoute,
                    timeRemainingString = timeRemainingString,
                    isNavigationActive = isNavigationActive,
                    onShowRouteOptions = {
                        showRouteOverview = true
                        isNavigationActive = false
                    }
                )
            }
        } else {
            // Bottom Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
        
        // Emergency State Feedback
        val emergencyState by bikeViewModel.emergencyState.collectAsState()
        emergencyState?.let { state ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            state.contains("success") -> MaterialTheme.colorScheme.primaryContainer
                            state.contains("Failed") -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                state.contains("success") -> "🚨 Emergency Alert Sent"
                                state.contains("Sending") -> "📡 Sending Emergency Alert..."
                                state.contains("Failed") -> "❌ Emergency Alert Failed"
                                else -> "🚨 Emergency Alert"
                            },
                            color = when {
                                state.contains("success") -> MaterialTheme.colorScheme.onPrimaryContainer
                                state.contains("Failed") -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                state.contains("success") -> "Emergency services have been notified with your location"
                                state.contains("Sending") -> "Sending your location to emergency services..."
                                state.contains("Failed") -> "Failed to send emergency alert. Please try again."
                                else -> state
                            },
                            color = when {
                                state.contains("success") -> MaterialTheme.colorScheme.onPrimaryContainer
                                state.contains("Failed") -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        if (state.contains("success") || state.contains("Failed")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { 
                                    // Emergency state auto-clears, but button provides user acknowledgment
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        state.contains("success") -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                ),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "OK",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Show destination search dialog
        DestinationSearchDialog(
            isVisible = showDestinationSearch,
            onDismiss = { showDestinationSearch = false },
            onDestinationSelected = { destination ->
                selectedDestination = destination
                // Calculate route from current location to selected destination
                currentLocation?.let { origin ->
                    Log.d("MapTab", "Destination selected: $destination, calculating route from $origin")
                    calculateRoute(origin, destination)
                    
                    // Show a toast indicating route calculation is starting
                    Toast.makeText(context, "Calculating route to destination...", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Log.w("MapTab", "Current location is null, cannot calculate route")
                    Toast.makeText(context, "Unable to get your location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
                    Text("• Location access for GPS tracking and navigation")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Text("• Notification permission for ride updates")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "These permissions enable POV navigation and emergency features during rides.",
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
    
    // SOS Dialog
    SOSDialog(
        isVisible = showSOSDialog,
        onDismiss = { showSOSDialog = false },
        onConfirmEmergency = { emergencyType ->
            // Handle emergency - send location and alert
            if (hasLocationPermission && activeRide != null) {
                handleEmergency(
                    context = context,
                    emergencyType = emergencyType,
                    bikeViewModel = bikeViewModel,
                    fusedLocationProviderClient = fusedLocationProviderClient
                )
            }
            showSOSDialog = false
        }
    )
    
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
            
            QRCodeHelper.debugQRCode(qrCode)
            
            if (!QRCodeHelper.isValidQRCodeFormat(qrCode)) {
                Log.w("MapTab", "Invalid QR code format detected")
                showQRScanner = false
                bikeViewModel.resetUnlockStates()
                return@QRScannerDialog
            }
            
            if (hasLocationPermission) {
                showQRScanner = false
                Log.d("MapTab", "Starting bike unlock process with valid QR code")
                getCurrentLocationAndUnlockBike(
                    fusedLocationProviderClient = fusedLocationProviderClient,
                    qrCode = qrCode,
                    bikeViewModel = bikeViewModel
                )
            } else {
                Log.w("MapTab", "Location permission not granted")
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

// These functions are now part of the RideProgressDialog component

// Enhanced location retrieval with better error handling
@SuppressLint("MissingPermission")
private suspend fun getCurrentLocationSuspend(fusedLocationProviderClient: FusedLocationProviderClient?): LatLng {
    return withContext(Dispatchers.IO) {
        try {
            if (fusedLocationProviderClient == null) {
                Log.w("MapTab", "Location provider is null, using default coordinates")
                return@withContext LatLng(14.5890, 120.9760)
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

// Location-based bike unlock
private fun getCurrentLocationAndUnlockBike(
    fusedLocationProviderClient: FusedLocationProviderClient,
    qrCode: String,
    bikeViewModel: BikeViewModel
) {
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
            bikeViewModel.endRideWithTracking(LatLng(14.5890, 120.9760))
        }
    }
}

// Enhanced emergency handling
private fun handleEmergency(
    context: android.content.Context,
    emergencyType: String,
    bikeViewModel: BikeViewModel,
    fusedLocationProviderClient: FusedLocationProviderClient
) {
    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
        try {
            val location = getCurrentLocationSuspend(fusedLocationProviderClient)
            
            // Send emergency alert with location
            bikeViewModel.sendEmergencyAlert(emergencyType, location)
            
            // Also trigger system emergency if needed
            when (emergencyType) {
                "Medical Emergency", "Accident" -> {
                    // Consider launching emergency dialer
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:911") // or local emergency number
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            }
            
        } catch (e: Exception) {
            Log.e("MapTab", "Error handling emergency", e)
        }
    }
}

