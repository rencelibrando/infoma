package com.example.bikerental.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bikerental.components.RestrictedButton
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.models.Review
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.components.ReviewSection
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.maps.model.LatLng
import com.example.bikerental.components.BookingCalendar
import java.util.Date
import androidx.compose.foundation.BorderStroke

// Use Dark Green color from ColorUtils
private val DarkGreen = ColorUtils.DarkGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    bikeId: String,
    navController: NavController,
    bikeViewModel: BikeViewModel = viewModel()
) {
    val bike by bikeViewModel.selectedBike.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    val error by bikeViewModel.error.collectAsState()
    
    // Booking calendar state
    var showBookingCalendar by remember { mutableStateOf(false) }
    val isBookingLoading by bikeViewModel.isBookingLoading.collectAsState()
    val bookedDates by bikeViewModel.bikeBookings.collectAsState()
    
    // Fetch bike details when screen is displayed
    LaunchedEffect(bikeId) {
        bikeViewModel.getBikeById(bikeId)
    }
    
    // Fetch bookings for this bike when screen is shown
    LaunchedEffect(bike?.id) {
        bike?.id?.let {
            bikeViewModel.fetchBookingsForBike(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bike Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = DarkGreen
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${error}",
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                    ) {
                        Text("Go Back")
                    }
                }
            } else if (bike != null) {
                BikeDetails(
                    bike = bike!!,
                    onBookClick = {
                        // Show booking calendar
                        showBookingCalendar = true
                    },
                    onCompleteProfile = {
                        navController.navigate("editProfile")
                    },
                    onBookingSuccess = {
                        // Show success message
                    }
                )
            } else {
                Text(
                    text = "Bike not found",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .wrapContentSize(Alignment.Center)
                )
            }
        }
    }
    
    // Booking calendar dialog
    bike?.let {
        BookingCalendar(
            isVisible = showBookingCalendar,
            onDismiss = { showBookingCalendar = false },
            bike = it,
            bookedDates = bikeViewModel.getBookedDatesForBike(it.id),
            isLoading = isBookingLoading,
            onDateSelected = { date ->
                // Optional: Handle single date selection feedback
            },
            onBookingConfirmed = { startDate, endDate ->
                bikeViewModel.createBooking(
                    bikeId = it.id,
                    startDate = startDate,
                    endDate = endDate,
                    onSuccess = { booking: Booking ->
                        showBookingCalendar = false
                        // Show success message
                    },
                    onError = { errorMessage ->
                        // We should show an error message here
                        Log.e("BikeDetails", "Booking error: $errorMessage")
                    }
                )
            }
        )
    }
}

@Composable
fun BikeDetails(
    bike: Bike,
    onBookClick: (Bike) -> Unit,
    onCompleteProfile: () -> Unit,
    onBookingSuccess: () -> Unit = {}
) {
    // View model to get reviews
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
    
    // Load reviews when the screen is shown
    LaunchedEffect(bike.id) {
        bikeViewModel.fetchReviewsForBike(bike.id)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp) // Reduced from 16dp
    ) {
        // Compact Bike Image
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(bike.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = bike.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), // Reduced from 250dp
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkGreen)
                }
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
        
        Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16dp
        
        // Compact header with name and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bike Name - more compact
            Text(
                text = bike.name,
                fontSize = 20.sp, // Reduced from 24sp
                fontWeight = FontWeight.Bold,
                color = DarkGreen,
                modifier = Modifier.weight(1f)
            )
            
            // Real-time status chip
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, statusColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Text(
                        text = bikeStatus,
                        fontSize = 12.sp,
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
                color = DarkGreen
            )
            
            Text(
                text = bike.type,
                fontSize = 14.sp, // Reduced from 16sp
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16dp
        
        // Compact specifications in a card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Specifications",
                    fontSize = 14.sp, // Reduced from 18sp
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                
                // Battery Level (if applicable)
                if (bike.batteryLevel > 0) {
                    CompactDetailItem(title = "Battery", value = "${bike.batteryLevel}%")
                }
                
                // Rating
                if (bike.rating > 0) {
                    CompactDetailItem(title = "Rating", value = "${bike.rating}/5.0")
                }
                
                // Last Updated (for status synchronization)
                if (bike.lastUpdatedTimestamp > 0) {
                    val timeAgo = getTimeAgo(bike.lastUpdatedTimestamp)
                    CompactDetailItem(title = "Last Updated", value = timeAgo)
                }
            }
        }
        
        // Compact description
        if (bike.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Description",
                fontSize = 14.sp, // Reduced from 18sp
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            
            Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp
            
            Text(
                text = bike.description,
                fontSize = 14.sp, // Reduced from 16sp
                lineHeight = 20.sp // Reduced from 24sp
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Description",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "High quality ${bike.type.lowercase()} bike available for rent in your area.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp)) // Reduced from 24dp
        Divider()
        Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16dp
        
        // Reviews Section - more compact
        ReviewSection(
            bikeId = bike.id,
            showForm = showReviewForm,
            reviews = reviews,
            averageRating = averageRating,
            isLoading = isLoading && reviews.isEmpty(),
            isSubmitting = isSubmittingReview,
            onToggleForm = { showReviewForm = !showReviewForm },
            onSubmitReview = { rating, comment ->
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
                            // Show error message to user - we should add a proper error display here
                            Log.e("BikeDetails", "Review submission error: $errorMessage")
                        }
                    )
                } catch (e: Exception) {
                    // Catch any unexpected exceptions
                    Log.e("BikeDetails", "Unexpected error submitting review", e)
                    isSubmittingReview = false
                }
            }
        )
        
        // Note: "Book Now" button removed as requested
        Spacer(modifier = Modifier.height(16.dp)) // Bottom spacing
    }
}

@Composable
fun CompactDetailItem(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp, // Reduced from 16sp
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp, // Reduced from 16sp
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper function to show time ago for status updates
private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "Over a week ago"
    }
} 