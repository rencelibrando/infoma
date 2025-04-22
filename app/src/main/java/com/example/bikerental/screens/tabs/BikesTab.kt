package com.example.bikerental.screens.tabs

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bikerental.components.BookingCalendar
import com.example.bikerental.components.RatingBar
import com.example.bikerental.components.ReviewForm
import com.example.bikerental.components.ReviewItem
import com.example.bikerental.components.ReviewSection
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.models.Review
import com.example.bikerental.navigation.Screen
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.LocationManager
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// Use Dark Green color from ColorUtils
private val DarkGreen = ColorUtils.DarkGreen

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
            scope.launch {
                bikeViewModel.fetchBikesFromFirestore()
            }
        }
    )
    
    // Add state for currently selected bike for booking
    var bookingBike by remember { mutableStateOf<Bike?>(null) }
    val isBookingLoading by bikeViewModel.isBookingLoading.collectAsState()
    
    // Get location
    LaunchedEffect(fusedLocationProviderClient) {
        locationManager.getLastLocation(
            onSuccess = { location: LatLng ->
                currentLocation = location
            },
            onFailure = {
                // Handle failure
            }
        )
    }
    
    // Refresh bikes from Firestore when the screen is shown
    LaunchedEffect(Unit) {
        bikeViewModel.fetchBikesFromFirestore()
        bikeViewModel.setupBikesRealtimeUpdates()
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (isLoading && bikes.isEmpty()) {
                // Shimmer loading grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(6) { _ ->
                        ShimmerBikeCard()
                    }
                }
            } else if (error != null && bikes.isEmpty()) {
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
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Error loading bikes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Add debug text to show bike count
                Text(
                    text = "Found ${bikes.size} bikes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Display all bikes in grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(bikes) { index, bike ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(
                                initialOffsetY = { it * (index + 1) / 2 }
                            )
                        ) {
                            BikeCard(
                                bike = bike,
                                currentLocation = currentLocation,
                                onBikeClick = { bikeItem ->
                                    selectedBike = bikeItem
                                    showBottomSheet = true
                                },
                                onBook = { _ ->
                                    // Would navigate to booking flow in a real implementation
                                },
                                onCompleteProfile = {
                                    navController?.navigate("editProfile")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                            )
                        }
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
                    navController?.navigate("bikeDetail/${selectedBike!!.id}")
                },
                onBook = { bikeItem ->
                    // Open the booking calendar dialog
                    bookingBike = bikeItem
                    // Fetch bookings for this bike
                    bikeViewModel.fetchBookingsForBike(bikeItem.id)
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

    // Add the booking calendar dialog
    bookingBike?.let { bike ->
        BookingCalendar(
            isVisible = bookingBike != null,
            onDismiss = { bookingBike = null },
            bike = bike,
            bookedDates = bikeViewModel.getBookedDatesForBike(bike.id),
            isLoading = isBookingLoading,
            onDateSelected = { date: Date ->
                // Optional: Handle single date selection feedback
            },
            onBookingConfirmed = { startDate: Date, endDate: Date ->
                bikeViewModel.createBooking(
                    bikeId = bike.id,
                    startDate = startDate,
                    endDate = endDate,
                    onSuccess = { booking: Booking ->
                        // Hide the calendar
                        bookingBike = null
                        // Show success message
                        Toast.makeText(
                            context,
                            "Booking successful!",
                            Toast.LENGTH_LONG
                        ).show()
                        // Navigate to Bookings tab
                        navController?.navigate("bookings") {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo("home") { saveState = true }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    onError = { errorMessage: String ->
                        // Show error message
                        Toast.makeText(
                            context,
                            "Booking failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        )
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
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            onBikeClick(bike)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Bike Image
            Box {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bike.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = bike.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
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
            }

            // Card content
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Top row with bike name and availability indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bike Name (taking most of the space)
                    Text(
                        text = bike.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Availability indicator
                    Surface(
                        color = if (isAvailable) 
                                    Color(0xFF4CAF50) // Available - Green
                                else 
                                    Color(0xFFE53935), // Unavailable - Red
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(width = 12.dp, height = 12.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {}
                }
                
                // Bike Price
                Text(
                    text = bike.price,
                    fontSize = 14.sp,
                    color = DarkGreen,
                    fontWeight = FontWeight.Bold
                )
                
                // Star rating
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

                // Type and Distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bike Type with availability text
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bike.type,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = if (isAvailable) " • Available" else " • Unavailable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }

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
                
                Button(
                    onClick = { if (isAvailable) onBook(bike) else onCompleteProfile() },
                    modifier = Modifier.weight(1f),
                    enabled = isAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Book Now")
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
private fun ShimmerBikeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            ShimmerLoadingAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerLoadingAnimation(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerLoadingAnimation(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerLoadingAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )
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