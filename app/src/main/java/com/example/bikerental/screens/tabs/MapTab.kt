package com.example.bikerental.screens.tabs

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bikerental.ui.theme.BikeLocation
import com.example.bikerental.ui.theme.RouteInfo
import com.example.bikerental.utils.ColorUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
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

@Composable
fun MapTab(fusedLocationProviderClient: FusedLocationProviderClient?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // Create dedicated scopes for different types of background operations
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val computeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // State for location permission
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // States for location and route
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedBike by remember { mutableStateOf<BikeLocation?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var distanceToSelectedBike by remember { mutableStateOf<Double?>(null) }
    
    var selectedRoute by remember { mutableStateOf<RouteInfo?>(null) }
    var availableRoutes by remember { mutableStateOf<List<RouteInfo>>(emptyList()) }
    var showRouteSheet by remember { mutableStateOf(false) }
    
    // Default location (Intramuros)
    val defaultLocation = LatLng(14.5890, 120.9760)
    
    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Create OkHttpClient only once and properly in IO scope
    // Store the client in LaunchedEffect to ensure it's created only once
    val httpClient = remember { lazy { OkHttpClient() } }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Move location fetching to IO thread
            ioScope.launch {
                val location = getCurrentLocationSuspend(fusedLocationProviderClient)
                // Update UI state on the main thread
                withContext(Dispatchers.Main) {
                    currentLocation = location
                    // Camera updates need to happen on main thread
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(location, 16f),
                        durationMs = 1000
                    )
                }
            }
        }
    }

    // Request location when component is first loaded
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Move location fetching to IO thread
            ioScope.launch {
                val location = getCurrentLocationSuspend(fusedLocationProviderClient)
                // Update UI state on the main thread
                withContext(Dispatchers.Main) {
                    currentLocation = location
                    // Camera updates need to happen on main thread
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(location, 16f),
                        durationMs = 1000
                    )
                }
            }
        }
    }

    // Function to decode polyline points - ensure this runs on computation thread
    fun decodePoly(encoded: String): List<LatLng> {
        // This is a CPU-intensive operation, make sure it runs in the computation scope
        return computeScope.run {
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
    }

    // Modified function to ensure network and JSON operations run on IO thread
    suspend fun fetchDirections(origin: LatLng, destination: LatLng): List<RouteInfo> {
        // This should definitely run on the IO dispatcher
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
                    .build()

                val response = httpClient.value.newCall(request).execute()
                val jsonData = response.body?.string() ?: ""
                
                // Move JSON parsing to computation thread for better performance
                withContext(Dispatchers.Default) {
                    val routes = mutableListOf<RouteInfo>()

                    val jsonObject = JSONObject(jsonData)
                    if (jsonObject.getString("status") == "OK") {
                        val routesArray = jsonObject.getJSONArray("routes")
                        
                        // Process routes in parallel using async
                        val deferredRoutes = (0 until routesArray.length()).map { i ->
                            async {
                                val route = routesArray.getJSONObject(i)
                                val leg = route.getJSONArray("legs").getJSONObject(0)
                                val steps = mutableListOf<String>()
                                
                                // Extract steps instructions
                                val stepsArray = leg.getJSONArray("steps")
                                for (j in 0 until stepsArray.length()) {
                                    val step = stepsArray.getJSONObject(j)
                                    steps.add(step.getString("html_instructions"))
                                }

                                RouteInfo(
                                    distance = leg.getJSONObject("distance").getString("text"),
                                    duration = leg.getJSONObject("duration").getString("text"),
                                    polylinePoints = decodePoly(route.getJSONObject("overview_polyline").getString("points")),
                                    steps = steps,
                                    isAlternative = i > 0
                                )
                            }
                        }
                        
                        // Collect all processed routes
                        routes.addAll(deferredRoutes.map { it.await() })
                    }
                    routes
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false
            )
        )

        // Bottom Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SCAN Button Row (Previously SOS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { /* Handle QR Scan */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(start = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorUtils.DarkGreen
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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

// Convert the callback-based function to a suspend function for better coroutine integration
@SuppressLint("MissingPermission")
private suspend fun getCurrentLocationSuspend(fusedLocationProviderClient: FusedLocationProviderClient?): LatLng {
    return withContext(Dispatchers.IO) {
        try {
            // If no client, return default location
            if (fusedLocationProviderClient == null) {
                return@withContext LatLng(14.5890, 120.9760)
            }

            // Since we're calling this from a composable context where permission is already checked,
            // and we're using the SuppressLint annotation, we can proceed safely
            val locationTask = fusedLocationProviderClient.lastLocation
            val location = locationTask.await()
            if (location != null) {
                LatLng(location.latitude, location.longitude)
            } else {
                // Default fallback location
                LatLng(14.5890, 120.9760)
            }
        } catch (e: SecurityException) {
            // Handle security exception (permission denied)
            Log.e("MapTab", "Location permission denied: ${e.message}")
            LatLng(14.5890, 120.9760)
        } catch (e: Exception) {
            // Default fallback location for other exceptions
            Log.e("MapTab", "Error getting location: ${e.message}")
            LatLng(14.5890, 120.9760)
        }
    }
}

// Keep the old method for backward compatibility but delegate to the suspend function
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun getCurrentLocation(fusedLocationProviderClient: FusedLocationProviderClient?, onResult: (LatLng) -> Unit) {
    // This function should be called from an IO scope
    fusedLocationProviderClient?.lastLocation
        ?.addOnSuccessListener { location ->
            if (location != null) {
                onResult(LatLng(location.latitude, location.longitude))
            } else {
                // Default fallback location
                onResult(LatLng(14.5890, 120.9760))
            }
        }
        ?.addOnFailureListener {
            // Default fallback location
            onResult(LatLng(14.5890, 120.9760))
        }
} 