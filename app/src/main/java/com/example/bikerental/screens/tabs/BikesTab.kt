package com.example.bikerental.screens.tabs

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.memory.MemoryCache
import coil.disk.DiskCache
import com.example.bikerental.components.BookingCalendar
import com.example.bikerental.components.RatingBar
import com.example.bikerental.components.ReviewForm
import com.example.bikerental.components.ReviewItem
import com.example.bikerental.components.QRScannerDialog
import com.example.bikerental.components.StartRideDialog
import com.example.bikerental.components.BikeUnlockDialog
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.navigation.Screen
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.utils.LocationManager
import com.example.bikerental.utils.ImageLoadingUtils
import com.example.bikerental.utils.PerformanceMonitor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.bikerental.R
import com.example.bikerental.viewmodels.BikeViewModel

// Updated color scheme for white and dark green theme - consistent with PaymentTab and BookingsTab
object BikeColors {
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFF5F5F5)
    val MediumGray = Color(0xFFE0E0E0)
    val DarkGray = Color(0xFF757575)
    val TextGray = Color(0xFF424242)
    val DarkGreen = Color(0xFF1D3C34)
    val MediumGreen = Color(0xFF2D5A4C)
    val LightGreen = Color(0xFF4CAF50)
    val AccentGreen = Color(0xFF10B981)
    val Red = Color(0xFFEF4444)
    val Orange = Color(0xFFFBBF24)
    val Blue = Color(0xFF3B82F6)
    val Success = Color(0xFF059669)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFDC2626)
}

// Performance optimization: Enhanced caching with expiration
private val bikeStatusCache = ConcurrentHashMap<String, BikeStatus>()
private val distanceCache = ConcurrentHashMap<String, CachedDistance>()

// Data class for cached bike status
private data class BikeStatus(
    val isAvailable: Boolean,
    val statusText: String,
    val statusColor: Color,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 5000 // 5 second cache
}

// Data class for cached distance calculations
private data class CachedDistance(
    val distance: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 30000 // 30 second cache for distances
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BikesTab(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    bikeViewModel: BikeViewModel = viewModel()
) {
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // Optimized coroutine scope management
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val processingScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Collect bikes from Firestore with performance optimization
    val bikes by bikeViewModel.bikes.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    val error by bikeViewModel.error.collectAsState()
    
    // Performance: Derive filtered bikes to avoid recomputation
    val filteredBikes by remember {
        derivedStateOf {
            bikes.filter { bike ->
                val status = getBikeStatusCached(bike)
                // Only show available bikes by default, can be expanded later
                status.isAvailable || !bike.isInUse
            }
        }
    }
    
    // Bottom sheet state
    var selectedBike by remember { mutableStateOf<Bike?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }

    // Pull-to-refresh state with debounced refresh
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                PerformanceMonitor.startTiming("pull_refresh")
                ioScope.launch {
                    try {
                        PerformanceMonitor.timeOperation("pull_refresh_fetch") {
                            bikeViewModel.fetchBikesFromFirestore()
                        }
                        delay(500) // Minimum refresh time to prevent spam
                    } finally {
                        isRefreshing = false
                        PerformanceMonitor.endTiming("pull_refresh")
                        PerformanceMonitor.logMemoryUsage("BikesTab_Refresh")
                    }
                }
            }
        }
    )
    
    val isBookingLoading by bikeViewModel.isBookingLoading.collectAsState()
    
    // QR Scanning state
    var showStartRideDialog by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
    
    // Bike unlocking states from ViewModel
    val isUnlockingBike by bikeViewModel.isUnlockingBike.collectAsState()
    val unlockError by bikeViewModel.unlockError.collectAsState()
    val unlockSuccess by bikeViewModel.unlockSuccess.collectAsState()
    
    // Performance: Use produceState for location to avoid unnecessary recompositions
    val locationState by produceState<LatLng?>(null, fusedLocationProviderClient) {
        if (fusedLocationProviderClient != null) {
            locationManager.getLastLocation(
                onSuccess = { location -> 
                    value = location
                    currentLocation = location
                },
                onFailure = { /* Handle failure */ }
            )
        }
    }
    
    // Performance monitoring for initial load
    LaunchedEffect(Unit) {
        PerformanceMonitor.startTiming("bikes_tab_init")
        logCachePerformance()
    }
    
    // Initialize data loading with performance monitoring
    LaunchedEffect(Unit) {
        ioScope.launch {
            PerformanceMonitor.timeOperation("fetch_bikes_initial") {
                bikeViewModel.fetchBikesFromFirestore()
            }
            PerformanceMonitor.timeOperation("setup_realtime_updates") {
                bikeViewModel.setupBikesRealtimeUpdates()
            }
        }
        PerformanceMonitor.endTiming("bikes_tab_init")
        PerformanceMonitor.logMemoryUsage("BikesTab_Init")
    }
    
    // Cache cleanup every 2 minutes
    LaunchedEffect(Unit) {
        while (true) {
            delay(120000) // 2 minutes
            cleanupExpiredCaches()
        }
    }

    // Handle successful bike unlock - auto-navigate or show success
    LaunchedEffect(unlockSuccess) {
        if (unlockSuccess) {
            bikeViewModel.resetUnlockStates()
            showQRScanner = false
            showStartRideDialog = false
            Toast.makeText(context, "Bike unlocked successfully! Your ride has started.", Toast.LENGTH_LONG).show()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            // Clear caches when component is disposed
            bikeStatusCache.clear()
            distanceCache.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BikeColors.White)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Optimized header - removed heavy animations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Bikes",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BikeColors.DarkGreen,
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = { showStartRideDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BikeColors.DarkGreen),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUnlockingBike,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Start Ride",
                            modifier = Modifier.size(20.dp),
                            tint = BikeColors.White
                        )
                        Text(
                            text = "Start Ride",
                            color = BikeColors.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Performance optimized content rendering
            when {
                isLoading && bikes.isEmpty() -> {
                    OptimizedShimmerGrid()
                }
                error != null && bikes.isEmpty() -> {
                    ErrorStateContent(
                        error = error,
                        onRetry = {
                            ioScope.launch {
                                bikeViewModel.fetchBikesFromFirestore()
                            }
                        }
                    )
                }
                filteredBikes.isEmpty() -> {
                    EmptyStateContent()
                }
                else -> {
                    // Performance optimized bike grid
                    OptimizedBikeGrid(
                        bikes = filteredBikes,
                        currentLocation = locationState,
                        onBikeClick = { bike ->
                            selectedBike = bike
                            showBottomSheet = true
                        },
                        onBook = { bike ->
                            NavigationUtils.navigateToBookings(navController!!, bike.id)
                        },
                        onCompleteProfile = {
                            navController?.navigate("editProfile")
                        }
                    )
                }
            }
        }

        // Pull refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = BikeColors.White,
            contentColor = BikeColors.DarkGreen,
            scale = true
        )
    }
    
    // Bottom sheet with performance optimizations
    if (showBottomSheet && selectedBike != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false
                selectedBike = null
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 8.dp,
            dragHandle = { 
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            BikeDetailSheet(
                bike = selectedBike!!,
                isVisible = showBottomSheet,
                isAvailable = getBikeStatusCached(selectedBike!!).isAvailable,
                onViewDetails = {
                    navigateToBikeDetails(selectedBike!!.id, navController!!)
                },
                onBook = { bikeItem ->
                    showBottomSheet = false
                    selectedBike = null
                    NavigationUtils.navigateToBookings(navController!!, bikeItem.id)
                },
                onDismiss = {
                    selectedBike = null
                    showBottomSheet = false
                },
                onCompleteProfile = {
                    navController?.navigate("editProfile")
                }
            )
        }
    }
    
    // Dialog components remain the same
    StartRideDialog(
        isVisible = showStartRideDialog,
        onDismiss = { showStartRideDialog = false },
        onStartQRScan = { 
            showStartRideDialog = false
            showQRScanner = true 
        }
    )
    
    QRScannerDialog(
        isVisible = showQRScanner,
        onDismiss = { showQRScanner = false },
        onQRCodeScanned = { qrCode ->
            showQRScanner = false
            fusedLocationProviderClient?.let { locationProvider ->
                getCurrentLocationAndUnlockBike(locationProvider, qrCode, bikeViewModel)
            }
        }
    )
    
    BikeUnlockDialog(
        isVisible = isUnlockingBike,
        onDismiss = { 
            bikeViewModel.resetUnlockStates()
        }
    )
    
    unlockError?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            delay(3000)
            bikeViewModel.resetUnlockStates()
        }
    }
}

// Performance optimized composables
@Composable
private fun OptimizedShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            ShimmerBikeCard()
        }
    }
}

@Composable
private fun ErrorStateContent(error: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error ?: "Failed to load bikes. Please try again later.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyStateContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No bikes available at the moment",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Pull down to refresh and check again",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OptimizedBikeGrid(
    bikes: List<Bike>,
    currentLocation: LatLng?,
    onBikeClick: (Bike) -> Unit,
    onBook: (Bike) -> Unit,
    onCompleteProfile: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = bikes,
            key = { _, bike -> bike.id } // Performance: Add key for efficient recomposition
        ) { index, bike ->
            OptimizedBikeCard(
                bike = bike,
                currentLocation = currentLocation,
                onBikeClick = onBikeClick,
                onBook = onBook,
                onCompleteProfile = onCompleteProfile
            )
        }
    }
}

// Performance optimized BikeCard
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptimizedBikeCard(
    bike: Bike,
    currentLocation: LatLng?,
    onBikeClick: (Bike) -> Unit,
    onBook: (Bike) -> Unit,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Performance: Cache bike status to avoid recalculation
    val bikeStatus = remember(bike.isAvailable, bike.isInUse, bike.currentRider) { 
        getBikeStatusCached(bike)
    }
    
    // Performance: Cache distance calculation
    val distance = remember(currentLocation, bike.latitude, bike.longitude) {
        if (currentLocation != null) {
            val cacheKey = "${bike.id}_${currentLocation.latitude}_${currentLocation.longitude}"
            val cached = distanceCache[cacheKey]
            
            if (cached != null && !cached.isExpired()) {
                cached.distance
            } else {
                val calculated = calculateDistanceOptimized(
                    lat1 = currentLocation.latitude,
                    lon1 = currentLocation.longitude,
                    lat2 = bike.latitude,
                    lon2 = bike.longitude
                )
                distanceCache[cacheKey] = CachedDistance(calculated)
                calculated
            }
        } else {
            null
        }
    }
    
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = {
            onBikeClick(bike)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Optimized image loading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageLoadingUtils.createThumbnailImageRequest(
                        context = LocalContext.current,
                        imageUrl = bike.imageUrl,
                        bikeId = bike.id
                    ),
                    contentDescription = bike.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    imageLoader = ImageLoadingUtils.getOptimizedImageLoader(LocalContext.current),
                    loading = {
                        ShimmerLoadingAnimation(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bike.name.firstOrNull()?.toString() ?: "B",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                
                // Status badge
                Surface(
                    color = bikeStatus.statusColor.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = bikeStatus.statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Card content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = bike.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = bike.type,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = bike.price,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BikeColors.DarkGreen
                        )
                        
                        if (bike.rating > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "★",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFC107)
                                )
                                Text(
                                    text = String.format("%.1f", bike.rating),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        distance?.let {
                            Text(
                                text = "%.1f km".format(it),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (bike.batteryLevel > 0 && bike.type.contains("E-bike", ignoreCase = true)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "🔋",
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${bike.batteryLevel}%",
                                    fontSize = 10.sp,
                                    color = if (bike.batteryLevel > 20) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Performance helper functions
private fun getBikeStatusCached(bike: Bike): BikeStatus {
    val cacheKey = "${bike.id}_${bike.isAvailable}_${bike.isInUse}_${bike.currentRider}"
    val cached = bikeStatusCache[cacheKey]
    
    if (cached != null && !cached.isExpired()) {
        return cached
    }
    
    val isAvailable = bike.isAvailable && !bike.isInUse && bike.currentRider.isEmpty()
    val statusText = if (isAvailable) "Available" else "In Use"
    val statusColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935)
    
    val status = BikeStatus(isAvailable, statusText, statusColor)
    bikeStatusCache[cacheKey] = status
    return status
}

// Optimized distance calculation using more efficient math
private fun calculateDistanceOptimized(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    // Use more efficient approximation for short distances
    val earthRadius = 6371000.0 // Earth's radius in meters
    
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLatRad = Math.toRadians(lat2 - lat1)
    val deltaLonRad = Math.toRadians(lon2 - lon1)
    
    // Haversine formula optimized for performance
    val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
            cos(lat1Rad) * cos(lat2Rad) *
            sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val distance = earthRadius * c
    
    return (distance / 1000).toFloat() // Convert to kilometers
}

// Cache management functions
private suspend fun cleanupExpiredCaches() {
    withContext(Dispatchers.Default) {
        PerformanceMonitor.timeOperationSync("cache_cleanup") {
            // Clean distance cache
            val distanceIterator = distanceCache.entries.iterator()
            var distanceRemoved = 0
            while (distanceIterator.hasNext()) {
                val entry = distanceIterator.next()
                if (entry.value.isExpired()) {
                    distanceIterator.remove()
                    distanceRemoved++
                }
            }
            
            // Clean bike status cache
            val statusIterator = bikeStatusCache.entries.iterator()
            var statusRemoved = 0
            while (statusIterator.hasNext()) {
                val entry = statusIterator.next()
                if (entry.value.isExpired()) {
                    statusIterator.remove()
                    statusRemoved++
                }
            }
            
            PerformanceMonitor.logCacheCleanup(
                distanceRemoved = distanceRemoved,
                statusRemoved = statusRemoved,
                remainingDistance = distanceCache.size,
                remainingStatus = bikeStatusCache.size
            )
        }
    }
}

// Performance monitoring for cache usage
private fun logCachePerformance() {
    PerformanceMonitor.logCacheUsage(
        distanceCacheSize = distanceCache.size,
        statusCacheSize = bikeStatusCache.size
    )
}

@Composable
fun BikeDetailSheet(
    bike: Bike,
    isVisible: Boolean,
    isAvailable: Boolean,
    onViewDetails: () -> Unit,
    onBook: (Bike) -> Unit,
    onDismiss: () -> Unit,
    onCompleteProfile: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    // Get ViewModel for reviews
    val bikeViewModel: BikeViewModel = viewModel()
    val reviews by bikeViewModel.bikeReviews.collectAsState()
    val averageRating by bikeViewModel.averageRating.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    
    // Review form state
    var showReviewForm by remember { mutableStateOf(false) }
    var isSubmittingReview by remember { mutableStateOf(false) }
    
    // Improved status calculation with real-time synchronization
    val bikeStatus = remember(bike.isAvailable, bike.isInUse, bike.currentRider) {
        when {
            !bike.isAvailable -> "Unavailable"
            bike.isInUse || bike.currentRider.isNotEmpty() -> "In Use"
            else -> "Available"
        }
    }
    
    val statusColor = remember(bikeStatus) {
        when (bikeStatus) {
            "Available" -> Color(0xFF4CAF50)
            "In Use" -> Color(0xFFFF9800)
            else -> Color(0xFFE53935)
        }
    }
    
    // Get current user ID
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid
    
    // State for error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Location state for this component
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    
    // Get current location when component mounts
    LaunchedEffect(Unit) {
        val locationManager = LocationManager.getInstance(context)
        locationManager.getLastLocation(
            onSuccess = { location ->
                currentLocation = location
            },
            onFailure = { /* Handle failure */ }
        )
    }
    
    // Load reviews when the sheet is shown
    LaunchedEffect(bike.id) {
        bikeViewModel.fetchReviewsForBike(bike.id)
    }
    
    // Use Column instead of scroll to avoid nested scrollable containers
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp) // Reduced padding
    ) {
        // This part will be scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Compact image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp) // Reduced from 200dp
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bike.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = bike.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop,
                    loading = {
                        ShimmerLoadingAnimation(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bike.name.firstOrNull()?.toString() ?: "B",
                                style = MaterialTheme.typography.headlineMedium, // Smaller
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                
                // Simplified gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing
            
            // Compact header section with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bike name - more compact
                Text(
                    text = bike.name,
                    fontSize = 18.sp, // Reduced from 24sp
                    fontWeight = FontWeight.Bold,
                    color = BikeColors.DarkGreen,
                    modifier = Modifier.weight(1f)
                )
                
                // Improved status chip with real-time data
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Status indicator dot
                        Surface(
                            color = statusColor,
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        
                        Text(
                            text = bikeStatus,
                            fontSize = 11.sp, // Slightly smaller
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp)) // Reduced spacing
            
            // Compact price and type row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bike.price,
                    fontSize = 18.sp, // Reduced from 20sp
                    fontWeight = FontWeight.Bold,
                    color = BikeColors.DarkGreen
                )
                
                Text(
                    text = bike.type,
                    fontSize = 14.sp, // Reduced from 16sp
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing
            
            // Compact info row with distance and battery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance
                currentLocation?.let { location ->
                    val distance = calculateDistanceOptimized(
                        lat1 = location.latitude,
                        lon1 = location.longitude,
                        lat2 = bike.latitude,
                        lon2 = bike.longitude
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📍",
                            fontSize = 10.sp
                        )
                        Text(
                            text = "%.1f km away".format(distance),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Battery level for e-bikes
                if (bike.batteryLevel > 0 && bike.type.contains("E-bike", ignoreCase = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔋",
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${bike.batteryLevel}% battery",
                            fontSize = 12.sp,
                            color = if (bike.batteryLevel > 20) Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description section
            Text(
                text = "Description",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = bike.description ?: "No description available",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Specifications section
            Text(
                text = "Specifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Specification items
            SpecificationItem("Type", bike.type)
            SpecificationItem("Size", "Medium")
            SpecificationItem("Weight", "12 kg")
            SpecificationItem("Max Speed", "25 km/h")
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reviews Section header only
            Text(
                text = "Reviews",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display full reviews list instead of just summary
            if (isLoading && reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BikeColors.DarkGreen)
                }
                Log.d("BikesTab", "Loading reviews for bike ${bike.id}")
            } else if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reviews yet. Be the first to review!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic
                    )
                }
                Log.d("BikesTab", "No reviews to display for bike ${bike.id}")
            } else {
                // Display average rating first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.1f", averageRating),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RatingBar(
                        rating = averageRating,
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${reviews.size} ${if (reviews.size == 1) "review" else "reviews"})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Log.d("BikesTab", "Reviews snapshot received with ${reviews.size} reviews")
                
                // Display all reviews in a LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp), // Fixed height to prevent layout issues
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reviews) { review ->
                        ReviewItem(
                            review = review,
                            isCurrentUserReview = currentUserId != null && review.userId == currentUserId,
                            onDeleteClick = if (currentUserId != null && review.userId == currentUserId) {
                                {
                                    bikeViewModel.deleteReview(
                                        bikeId = bike.id,
                                        reviewId = review.id,
                                        onSuccess = {
                                            // Review deleted successfully
                                            errorMessage = null
                                        },
                                        onError = { error ->
                                            errorMessage = error
                                            Log.e("BikesTab", "Error deleting review: $error")
                                        }
                                    )
                                }
                            } else null
                        )
                    }
                }
                
                // Show error message if any
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                // Log reviews for debugging
                Log.d("BikesTab", "Bottom sheet showing ${reviews.size} reviews for bike ${bike.id}")
                reviews.take(3).forEachIndexed { index, review ->
                    Log.d("BikesTab", "Review $index: ${review.id} by ${review.userName}, rating: ${review.rating}")
                }
            }
            
            // Write review button
            Button(
                onClick = { showReviewForm = !showReviewForm },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showReviewForm) MaterialTheme.colorScheme.surfaceVariant else BikeColors.DarkGreen,
                    contentColor = if (showReviewForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showReviewForm) "Cancel" else "Write Review")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show review form if requested
            AnimatedVisibility(
                visible = showReviewForm,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                ReviewForm(
                    onSubmit = { rating, comment ->
                        isSubmittingReview = true
                        try {
                            bikeViewModel.submitReview(
                                bikeId = bike.id,
                                rating = rating,
                                comment = comment,
                                onSuccess = {
                                    isSubmittingReview = false
                                    showReviewForm = false
                                },
                                onError = { errorMessage ->
                                    isSubmittingReview = false
                                    Log.e("BikesTab", "Review submission error: $errorMessage")
                                    // In a real app, show an error message to the user
                                }
                            )
                        } catch (e: Exception) {
                            // Catch any unexpected exceptions
                            Log.e("BikesTab", "Unexpected error submitting review", e)
                            isSubmittingReview = false
                        }
                    },
                    isSubmitting = isSubmittingReview
                )
            }
            
            // Extra spacing at bottom
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Action buttons - fixed at bottom
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Replace "View Details" with "Review" button
                OutlinedButton(
                    onClick = { showReviewForm = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BikeColors.DarkGreen)
                ) {
                    Text(
                        text = "Review",
                        color = BikeColors.DarkGreen
                    )
                }
            }
            
            // View Details button separately (full width)
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Full Details")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SpecificationItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ShimmerLoadingAnimation(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerColorShades = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColorShades,
        start = Offset(translateAnim - 1000f, translateAnim - 1000f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun ShimmerBikeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),  // Match the real BikeCard height
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Shimmer image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)  // Match the image height
            ) {
                ShimmerLoadingAnimation(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            
            // Shimmer content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                
                // Type shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                
                // Rating shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                
                // Price and distance row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Price shimmer
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                    )
                    
                    // Distance shimmer
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(12.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

private fun calculateDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val r = 6371 // Earth's radius in kilometers

    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val latDiff = Math.toRadians(lat2 - lat1)
    val lonDiff = Math.toRadians(lon2 - lon1)

    val a = sin(latDiff / 2) * sin(latDiff / 2) +
            cos(lat1Rad) * cos(lat2Rad) *
            sin(lonDiff / 2) * sin(lonDiff / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return r * c
}

// Helper function to format timestamp
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(date)
}

// Navigate to bike details
fun navigateToBikeDetails(bikeId: String, navController: NavController) {
    try {
        // Use Screen's createRoute function to generate the proper route
        val route = Screen.BikeDetails.createRoute(bikeId)
        navController.navigate(route)
        Log.d("BikesTab", "Navigating to bike details for $bikeId")
    } catch (e: Exception) {
        Log.e("BikesTab", "Error navigating to bike details: ${e.message}", e)
    }
}

// Create a helper function to calculate distances in the background
private suspend fun calculateDistanceInBackground(
    bike: Bike,
    currentLocation: LatLng?,
    onResult: (Float) -> Unit
) {
    withContext(Dispatchers.Default) {
        if (currentLocation != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                bike.latitude, bike.longitude,
                results
            )
            withContext(Dispatchers.Main) {
                onResult(results[0])
            }
        }
    }
}

// Helper function to get current location and unlock bike - similar to MapTab
@SuppressLint("MissingPermission")
private suspend fun getCurrentLocationSuspend(fusedLocationProviderClient: FusedLocationProviderClient?): LatLng {
    return withContext(Dispatchers.IO) {
        try {
            if (fusedLocationProviderClient == null) {
                Log.w("BikesTab", "Location provider is null, using default coordinates")
                return@withContext LatLng(14.5890, 120.9760) // Default coordinates for Philippines
            }

            val location = fusedLocationProviderClient.lastLocation.await()
            if (location != null) {
                Log.d("BikesTab", "Location obtained: ${location.latitude}, ${location.longitude}")
                LatLng(location.latitude, location.longitude)
            } else {
                Log.w("BikesTab", "Last known location is null, using default coordinates")
                LatLng(14.5890, 120.9760)
            }
        } catch (e: SecurityException) {
            Log.e("BikesTab", "Location permission denied: ${e.message}")
            LatLng(14.5890, 120.9760)
        } catch (e: Exception) {
            Log.e("BikesTab", "Error getting location: ${e.message}")
            LatLng(14.5890, 120.9760)
        }
    }
}

// Helper function to get location and unlock bike
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
            Log.e("BikesTab", "Error getting location for bike unlock", e)
            bikeViewModel.resetUnlockStates()
        }
    }
}