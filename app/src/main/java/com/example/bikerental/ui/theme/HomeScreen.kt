package com.example.bikerental.ui.theme
import BottomNavigationBar
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.google.android.gms.maps.model.Gap as MapGap
import com.example.bikerental.components.RequirementsWrapper
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import coil.compose.AsyncImage
import com.example.bikerental.viewmodels.AuthViewModel
import androidx.compose.material3.AlertDialog

data class Bike(
    val id: String,
    val name: String,
    val type: String,
    val price: String,
    val rating: Float,
    val distance: String,
    val imageRes: Int
)

data class BikeLocation(
    val id: String,
    val name: String,
    val type: String,
    val price: String,
    val location: LatLng,
    val isAvailable: Boolean = true
)

data class RouteInfo(
    val distance: String,
    val duration: String,
    val polylinePoints: List<LatLng>,
    val steps: List<String>,
    val isAlternative: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    fusedLocationProviderClient: FusedLocationProviderClient? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val viewModel = remember { AuthViewModel() }
    
    // Check if user is logged in
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            navController.navigate("signin") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    RequirementsWrapper {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                BottomNavigationBar(
                    selectedItem = selectedTab,
                    onItemSelected = { newTab -> selectedTab = newTab }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top App Bar with Search and Notifications
                if (selectedTab != 3) { // Don't show TopAppBar in Profile tab
                    TopAppBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { newQuery -> searchQuery = newQuery }
                    )
                }

                // Display content based on selected tab
                when (selectedTab) {
                    0 -> LocationTabContent(fusedLocationProviderClient)
                    1 -> BikeListingsTabContent(fusedLocationProviderClient)
                    2 -> BookingsTabContent()
                    3 -> ProfileScreen(navController, viewModel)
                }
            }
        }
    }
}

@Composable
fun LocationTabContent(fusedLocationProviderClient: FusedLocationProviderClient?) {
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
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
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
                val apiKey = "YOUR_GOOGLE_MAPS_API_KEY" // Replace with your API key
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

    // Function to calculate estimated cycling time
    fun calculateCyclingTime(distanceInMeters: Double): String {
        val speedKmH = 13.0 // Average cycling speed
        val timeHours = distanceInMeters / 1000.0 / speedKmH
        val timeMinutes = (timeHours * 60).toInt()
        return when {
            timeMinutes < 1 -> "Less than 1 min"
            timeMinutes < 60 -> "$timeMinutes mins"
            else -> "${timeMinutes / 60}h ${timeMinutes % 60}m"
        }
    }

    // Function to generate route suggestions


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 6.dp)
    ) {
        // Map Title and Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Find Bikes in Intramuros",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                selectedBike?.let { bike ->
                    distanceToSelectedBike?.let { distance ->
                        Text(
                            text = if (distance < 1000) {
                                "Distance to ${bike.name}: ${distance.toInt()} m"
                            } else {
                                "Distance to ${bike.name}: ${"%.1f".format(distance / 1000)} km"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Button(
                onClick = {
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
                },
                modifier = Modifier
                    .height(36.dp)
                    .width(110.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Locate Me",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
        ) {
            // Wrap GoogleMap in a Box to ensure proper composition
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
                        compassEnabled = true,
                        zoomGesturesEnabled = true,
                        rotationGesturesEnabled = true,
                        tiltGesturesEnabled = true,
                        scrollGesturesEnabled = true
                    )
                ) {
                    // Move map content inside the content lambda
                    // Show current location marker
                    currentLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "My Location",
                            snippet = "You are here"
                        )
                    }
                    
                    // Show bike markers
                    intramurosLocations.forEach { bikeLocation ->
                        Marker(
                            state = MarkerState(position = bikeLocation.location),
                            title = "${bikeLocation.name} (${bikeLocation.type})",
                            snippet = "${bikeLocation.price} - Click to select",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (bikeLocation == selectedBike) 
                                    BitmapDescriptorFactory.HUE_GREEN
                                else 
                                    BitmapDescriptorFactory.HUE_RED
                            ),
                            onClick = { marker ->
                                selectedBike = bikeLocation
                                currentLocation?.let { userLocation ->
                                    scope.launch {
                                        val routes = fetchDirections(userLocation, bikeLocation.location)
                                        if (routes.isNotEmpty()) {
                                            availableRoutes = routes
                                            selectedRoute = routes.first()
                                            showRouteSheet = true
                                        }
                                    }
                                }
                                true
                            }
                        )
                    }

                    // Draw route if available
                    if (routePoints.isNotEmpty()) {
                        Polyline(
                            points = routePoints,
                            color = MaterialTheme.colorScheme.primary,
                            width = 5f
                        )
                    }

                    // Draw selected route if available
                    selectedRoute?.let { route ->
                        Polyline(
                            points = route.polylinePoints,
                            color = if (route.isAlternative) 
                                MaterialTheme.colorScheme.secondary 
                            else 
                                MaterialTheme.colorScheme.primary,
                            width = 5f,
                            pattern = if (route.isAlternative) 
                                listOf(Dot(), MapGap(20f))
                            else 
                                null
                        )
                    }
                }
            }

            // Custom zoom controls overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.small
                    )
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.zoomIn(),
                                durationMs = 300
                            )
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.shapes.small
                        )
                ) {
                    Text(
                        "+",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Divider(
                    modifier = Modifier
                        .width(36.dp)
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                IconButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.zoomOut(),
                                durationMs = 300
                            )
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.shapes.small
                        )
                ) {
                    Text(
                        "−",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Legend
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MapLegendItem(color = Color(0xFF4CAF50), label = "Selected Bike")
                    MapLegendItem(color = Color(0xFFFF0000), label = "Available Bikes")
                    MapLegendItem(color = MaterialTheme.colorScheme.primary, label = "Route")
                }
            }
        }

        // Route information sheet
        if (showRouteSheet && selectedBike != null && selectedRoute != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Route to ${selectedBike?.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    availableRoutes.forEach { route ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRoute = route }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (route.isAlternative) "Alternative Route" else "Recommended Route",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selectedRoute == route)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${route.distance} • ${route.duration}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                RadioButton(
                                    selected = selectedRoute == route,
                                    onClick = { selectedRoute = route },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Show turn-by-turn directions
                            if (selectedRoute == route) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Directions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                route.steps.forEach { step ->
                                    Text(
                                        text = "• $step",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = { /* Handle booking */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Book This Bike")
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

@Composable
fun BikeListingsTabContent(fusedLocationProviderClient: FusedLocationProviderClient? = null) {
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            getCurrentLocation(fusedLocationProviderClient) { location ->
                currentLocation = location
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
            }
        }
    }

    // Function to calculate distance between two points
    fun calculateDistance(bikeLocation: LatLng): String {
        return if (currentLocation != null) {
            val startLocation = Location("").apply {
                latitude = currentLocation!!.latitude
                longitude = currentLocation!!.longitude
            }
            
            val endLocation = Location("").apply {
                latitude = bikeLocation.latitude
                longitude = bikeLocation.longitude
            }
            
            val distanceInMeters = startLocation.distanceTo(endLocation)
            if (distanceInMeters < 1000) {
                "${distanceInMeters.toInt()} m"
            } else {
                "${"%.1f".format(distanceInMeters / 1000)} km"
            }
        } else {
            "-- km"
        }
    }

    // Convert intramurosLocations to Bike objects with real distances
    val availableBikesWithDistance = remember(currentLocation) {
        intramurosLocations.map { bikeLocation ->
            Bike(
                id = bikeLocation.id,
                name = bikeLocation.name,
                type = bikeLocation.type,
                price = bikeLocation.price,
                rating = 4.5f, // Default rating
                distance = calculateDistance(bikeLocation.location),
                imageRes = R.drawable.bambike
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Welcome Section
        Text(
            text = "Find your perfect ride",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Featured Bikes Section with real distances
        SectionHeader(
            title = "Featured Bikes",
            onSeeAllClick = { /* Navigate to featured bikes page */ }
        )
        FeaturedBikesRow(bikes = availableBikesWithDistance.take(3))

        Spacer(modifier = Modifier.height(16.dp))

        // Available Bikes Near You Section with real distances
        SectionHeader(
            title = "Available Bikes Near You",
            onSeeAllClick = { /* Navigate to nearby bikes page */ }
        )
        AvailableBikesGrid(bikes = availableBikesWithDistance)
    }
}

@Composable
fun BookingsTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Bookings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You don't have any active bookings at the moment.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    var user by remember { mutableStateOf<FirebaseUser?>(null) }
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navController.navigate("signin") {
                popUpTo("home") { inclusive = true }
            }
            return@LaunchedEffect
        }
        
        // Fetch user profile data from Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user!!.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    profileData = document.data
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProfileDialog = true }
                .animateContentSize(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Picture
                AsyncImage(
                    model = profileData?.get("profilePictureUrl") ?: user?.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.default_profile_picture)
                )

                // Basic Info
                Column {
                    Text(
                        text = profileData?.get("fullName")?.toString() ?: user?.displayName ?: "Not available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = user?.email ?: "Not available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profileData?.get("phoneNumber")?.toString() ?: "No phone number",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Ride History Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Ride History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                RideHistoryContent()
            }
        }

        // Account Settings Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                SettingsContent(navController, viewModel)
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            modifier = Modifier.widthIn(max = 480.dp),
            title = {
                Text(
                    text = "Profile Information",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = profileData?.get("profilePictureUrl") ?: user?.photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.default_profile_picture)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Personal Information
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        InfoRow("Full Name", profileData?.get("fullName")?.toString() ?: user?.displayName ?: "Not available")
                        InfoRow("Email", user?.email ?: "Not available")
                        InfoRow("Phone", profileData?.get("phoneNumber")?.toString() ?: "Not available")
                        InfoRow("Member Since", profileData?.get("memberSince")?.toString() ?: "Not available")
                        
                        // Address Information
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        InfoRow("Street", profileData?.get("street")?.toString() ?: "Not available")
                        InfoRow("Barangay", profileData?.get("barangay")?.toString() ?: "Not available")
                        InfoRow("City", profileData?.get("city")?.toString() ?: "Not available")
                        
                        // Account Information
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        InfoRow("Account Type", profileData?.get("authProvider")?.toString()?.replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase() else it.toString() 
                        } ?: "Email")
                        InfoRow("Email Verified", if (user?.isEmailVerified == true) "Yes" else "No")
                        InfoRow("Last Sign In", user?.metadata?.lastSignInTimestamp?.let { 
                            java.util.Date(it).toString() 
                        } ?: "Not available")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun RideHistoryContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sample ride history items - replace with actual data from your backend
        RideHistoryItem(
            bikeName = "Mountain Bike X1",
            date = "2024-03-15",
            duration = "45 minutes",
            cost = "$12.50",
            status = "Completed"
        )
        Divider()
        RideHistoryItem(
            bikeName = "City Cruiser C2",
            date = "2024-03-10",
            duration = "30 minutes",
            cost = "$8.00",
            status = "Completed"
        )
    }
}

@Composable
private fun RideHistoryItem(
    bikeName: String,
    date: String,
    duration: String,
    cost: String,
    status: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = bikeName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cost,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == "Completed") Color.Green else Color.Red
            )
        }
    }
}

@Composable
private fun SettingsContent(navController: NavController, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsButton(
            icon = Icons.Default.Edit,
            text = "Edit Profile",
            onClick = { navController.navigate("editProfile") }
        )
        SettingsButton(
            icon = Icons.Default.Lock,
            text = "Change Password",
            onClick = { navController.navigate("changePassword") }
        )
        SettingsButton(
            icon = Icons.Default.Help,
            text = "Help & Support",
            onClick = { navController.navigate("help") }
        )
        Divider()
        SettingsButton(
            icon = Icons.Default.ExitToApp,
            text = "Sign Out",
            onClick = {
                viewModel.signOut()
                navController.navigate("signin") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )
    }
}

@Composable
private fun SettingsButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1.5f)
                    .height(45.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            IconButton(
                onClick = { /* Handle Settings Click */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { /* Handle Notification Click */ },
                modifier = Modifier.size(40.dp)
            ) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text("2")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "See All",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun FeaturedBikesRow(bikes: List<Bike>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(bikes) { bike ->
            FeaturedBikeCard(bike = bike)
        }
    }
}

@Composable
fun FeaturedBikeCard(bike: Bike) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = bike.imageRes),
                    contentDescription = bike.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Rating badge
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bike.rating.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = " ★",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bike.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bike.price,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bike.type,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${bike.distance}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AvailableBikesGrid(bikes: List<Bike>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(280.dp)
    ) {
        items(bikes) { bike ->
            AvailableBikeCard(bike = bike)
        }
    }
}

@Composable
fun AvailableBikeCard(bike: Bike) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Image(
                painter = painterResource(id = bike.imageRes),
                contentDescription = bike.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = bike.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bike.price,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bike.rating.toString(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " ★",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Sample Data - Keep the same data
val featuredBikes = listOf(
    Bike(
        id = "bike1",
        name = "Mountain Explorer",
        type = "Mountain Bike",
        price = "₱12/hr",
        rating = 4.8f,
        distance = "0.8 km",
        imageRes = R.drawable.bambike // Replace with actual images
    ),
    Bike(
        id = "bike2",
        name = "Road Master",
        type = "Road Bike",
        price = "₱10/hr",
        rating = 4.6f,
        distance = "1.2 km",
        imageRes = R.drawable.bambike // Replace with actual images
    ),
    Bike(
        id = "bike3",
        name = "City Cruiser",
        type = "Hybrid Bike",
        price = "₱8/hr",
        rating = 4.5f,
        distance = "1.5 km",
        imageRes = R.drawable.bambike // Replace with actual images
    )
)

val availableBikes = listOf(
    Bike(
        id = "bike4",
        name = "Urban Commuter",
        type = "City Bike",
        price = "₱9/hr",
        rating = 4.3f,
        distance = "0.5 km",
        imageRes = R.drawable.bambike // Replace with actual images
    ),
    Bike(
        id = "bike5",
        name = "Trail Blazer",
        type = "Mountain Bike",
        price = "₱11/hr",
        rating = 4.7f,
        distance = "1.8 km",
        imageRes = R.drawable.bambike // Replace with actual images
    ),
    Bike(
        id = "bike6",
        name = "Speed Demon",
        type = "Road Bike",
        price = "₱13/hr",
        rating = 4.9f,
        distance = "2.2 km",
        imageRes = R.drawable.bambike // Replace with actual images
    ),
    Bike(
        id = "bike7",
        name = "Comfort Ride",
        type = "Cruiser",
        price = "₱7/hr",
        rating = 4.2f,
        distance = "0.7 km",
        imageRes = R.drawable.bambike // Replace with actual images
    )
)

// Add Intramuros bike locations
val intramurosLocations = listOf(
    BikeLocation(
        id = "bike1",
        name = "Mountain Bike 1",
        type = "Mountain Bike",
        price = "₱12/hr",
        location = LatLng(14.5891, 120.9767) // Near Manila Cathedral
    ),
    BikeLocation(
        id = "bike2",
        name = "City Bike 1",
        type = "City Bike",
        price = "₱10/hr",
        location = LatLng(14.5895, 120.9757) // Near Fort Santiago
    ),
    BikeLocation(
        id = "bike3",
        name = "Road Bike 1",
        type = "Road Bike",
        price = "₱8/hr",
        location = LatLng(14.5879, 120.9749) // Near Baluarte de San Diego
    ),
    BikeLocation(
        id = "bike4",
        name = "Mountain Bike 2",
        type = "Mountain Bike",
        price = "₱12/hr",
        location = LatLng(14.5883, 120.9775) // Near San Agustin Church
    ),
    BikeLocation(
        id = "bike5",
        name = "City Bike 2",
        type = "City Bike",
        price = "₱10/hr",
        location = LatLng(14.5902, 120.9763) // Near Plaza Roma
    )
)

@SuppressLint("MissingPermission")
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BikerentalTheme {
       HomeScreen(navController = rememberNavController())

    }
}