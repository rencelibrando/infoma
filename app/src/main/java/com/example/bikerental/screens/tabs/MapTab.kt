package com.example.bikerental.screens.tabs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bikerental.ui.theme.BikeLocation
import com.example.bikerental.ui.theme.RouteInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.google.android.gms.maps.model.Gap as MapGap
import com.google.android.gms.maps.model.Dot
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.bikerental.utils.ColorUtils

@Composable
fun MapTab(fusedLocationProviderClient: FusedLocationProviderClient?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
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

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            getCurrentLocation(fusedLocationProviderClient) { location ->
                currentLocation = location
                scope.launch {
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
            getCurrentLocation(fusedLocationProviderClient) { location ->
                currentLocation = location
                scope.launch {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(location, 16f),
                        durationMs = 1000
                    )
                }
            }
        }
    }

    val client = remember { OkHttpClient() }

    // Function to decode polyline points
    fun decodePoly(encoded: String): List<LatLng> {
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
        return poly
    }

    // Function to fetch directions from Google Maps API
    suspend fun fetchDirections(origin: LatLng, destination: LatLng): List<RouteInfo> {
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

                val response = client.newCall(request).execute()
                val jsonData = JSONObject(response.body?.string() ?: "")
                val routes = mutableListOf<RouteInfo>()

                if (jsonData.getString("status") == "OK") {
                    val routesArray = jsonData.getJSONArray("routes")
                    for (i in 0 until routesArray.length()) {
                        val route = routesArray.getJSONObject(i)
                        val leg = route.getJSONArray("legs").getJSONObject(0)
                        val steps = mutableListOf<String>()
                        
                        // Extract steps instructions
                        val stepsArray = leg.getJSONArray("steps")
                        for (j in 0 until stepsArray.length()) {
                            val step = stepsArray.getJSONObject(j)
                            steps.add(step.getString("html_instructions"))
                        }

                        routes.add(
                            RouteInfo(
                                distance = leg.getJSONObject("distance").getString("text"),
                                duration = leg.getJSONObject("duration").getString("text"),
                                polylinePoints = decodePoly(route.getJSONObject("overview_polyline").getString("points")),
                                steps = steps,
                                isAlternative = i > 0
                            )
                        )
                    }
                }
                routes
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
                        containerColor = ColorUtils.Purple40
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

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun getCurrentLocation(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    onLocationReceived: (LatLng) -> Unit
) {
    fusedLocationProviderClient?.lastLocation?.addOnSuccessListener { location ->
        location?.let {
            onLocationReceived(LatLng(it.latitude, it.longitude))
        }
    }
} 