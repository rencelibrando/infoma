package com.example.bikerental.screens.tabs

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
import com.example.bikerental.components.BookingCalendar
import com.example.bikerental.components.RatingBar
import com.example.bikerental.components.ReviewForm
import com.example.bikerental.components.ReviewItem
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.navigation.Screen
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.utils.LocationManager
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


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
    
    // Introduce a dedicated IO scope for background operations
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    
    // Create a processing scope for computation-heavy tasks
    val processingScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Collect bikes from Firestore
    val bikes by bikeViewModel.bikes.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    val error by bikeViewModel.error.collectAsState()
    
    // Bottom sheet state
    var selectedBike by remember { mutableStateOf<Bike?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }

    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            // Use IO scope for Firestore operations
            ioScope.launch {
                bikeViewModel.fetchBikesFromFirestore()
            }
        }
    )
    
    // Remove bookingBike state - we'll navigate instead
    val isBookingLoading by bikeViewModel.isBookingLoading.collectAsState()
    
    // Get location using the IO scope
    LaunchedEffect(fusedLocationProviderClient) {
        locationManager.getLastLocation(
            onSuccess = { location: LatLng ->
                // Update UI state on main thread
                scope.launch {
                    currentLocation = location
                }
            },
            onFailure = {
                    // Handle failure
            }
        )

    }
    
    // Refresh bikes from Firestore when the screen is shown - use IO scope
    LaunchedEffect(Unit) {
        ioScope.launch {
            bikeViewModel.fetchBikesFromFirestore()
            bikeViewModel.setupBikesRealtimeUpdates()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Animated header
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "Available Bikes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Content based on loading state
            if (isLoading && bikes.isEmpty()) {
                // Loading state - show shimmer placeholders
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
            } else if (error != null && bikes.isEmpty()) {
                // Error state
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
                            onClick = {
                                ioScope.launch {
                                    bikeViewModel.fetchBikesFromFirestore()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            } else if (bikes.isEmpty()) {
                // Empty state
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
            } else {
                // Bike grid with filtered results
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(bikes) { index, bike ->
                        BikeCard(
                            bike = bike,
                            currentLocation = currentLocation,
                            onBikeClick = {
                                selectedBike = it
                                showBottomSheet = true
                            },
                            onBook = { bikeItem ->
                                // Navigate to Bookings tab with the bike ID
                                NavigationUtils.navigateToBookings(navController!!, bikeItem.id)
                            },
                            onCompleteProfile = {
                                navController?.navigate("editProfile")
                            },
                            modifier = Modifier
                        )
                    }
                }
            }
        }

        // Pull refresh indicator with custom styling
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = DarkGreen,
            scale = true
        )
    }
    
    // Half-screen bottom sheet
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
                isAvailable = true, // You might want to dynamically determine this
                onViewDetails = {
                    navigateToBikeDetails(selectedBike!!.id, navController!!)
                },
                onBook = { bikeItem ->
                    // Navigate to Bookings tab with the bike ID
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeCard(
    bike: Bike,
    currentLocation: LatLng?,
    onBikeClick: (Bike) -> Unit,
    onBook: (Bike) -> Unit,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine bike availability based on database fields
    val isAvailable = remember(bike) { 
        bike.isAvailable && !bike.isInUse 
    }
    
    // Haptic feedback
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),  // Fixed height for all cards
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            onBikeClick(bike)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Bike Image - fixed height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)  // Fixed height for all images
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bike.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = bike.name,
                    modifier = Modifier.fillMaxSize(),
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
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                
                // Availability badge on top of image
                Surface(
                    color = if (isAvailable) 
                        Color(0xFF4CAF50).copy(alpha = 0.9f) // Available - Green
                    else 
                        Color(0xFFE53935).copy(alpha = 0.9f), // Unavailable - Red
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isAvailable) "Available" else "Unavailable",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Card content - fixed padding and layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Content top section
                Column {
                    // Bike Name - fixed height
                    Text(
                        text = bike.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Bike Type
                    Text(
                        text = bike.type,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Middle section - Rating
                if (bike.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        RatingBar(
                            rating = bike.rating,
                            modifier = Modifier.height(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", bike.rating),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Bottom section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bike Price
                    Text(
                        text = bike.price,
                        fontSize = 14.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.Bold
                    )

                    // Distance from user
                    currentLocation?.let { location ->
                        val distance = calculateDistance(
                            lat1 = location.latitude,
                            lon1 = location.longitude,
                            lat2 = bike.latitude,
                            lon2 = bike.longitude
                        )
                        Text(
                            text = "%.1f km".format(distance),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // This part will be scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Image with parallax effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bike.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = bike.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                
                // Add gradient overlay for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header section with name and availability status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bike name
                Text(
                    text = bike.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    modifier = Modifier.weight(1f)
                )
                
                // Availability status chip
                Surface(
                    color = if (isAvailable) Color(0xFFEDF7ED) else Color(0xFFFDEDED),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Status indicator dot
                        Surface(
                            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        
                        Text(
                            text = if (isAvailable) "Available" else "Unavailable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Price with larger text
            Text(
                text = bike.price,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Type and distance info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bike Type with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown, // Replace with appropriate icon
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = bike.type,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Distance
                currentLocation?.let { location ->
                    val distance = calculateDistance(
                        lat1 = location.latitude,
                        lon1 = location.longitude,
                        lat2 = bike.latitude,
                        lon2 = bike.longitude
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown, // Replace with appropriate icon
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f km away".format(distance),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    CircularProgressIndicator(color = DarkGreen)
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
                    containerColor = if (showReviewForm) MaterialTheme.colorScheme.surfaceVariant else DarkGreen,
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
                    border = BorderStroke(1.dp, DarkGreen)
                ) {
                    Text(
                        text = "Review",
                        color = DarkGreen
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