package com.example.bikerental.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.bikerental.models.Review
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.components.ReviewSection
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.maps.model.LatLng

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
    
    // Fetch bike details when screen is displayed
    LaunchedEffect(bikeId) {
        bikeViewModel.getBikeById(bikeId)
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
                        // Would navigate to booking flow in a real implementation
                    },
                    onCompleteProfile = {
                        navController.navigate("editProfile")
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
}

@Composable
fun BikeDetails(
    bike: Bike,
    onBookClick: (Bike) -> Unit,
    onCompleteProfile: () -> Unit
) {
    // View model to get reviews
    val bikeViewModel: BikeViewModel = viewModel()
    val reviews by bikeViewModel.bikeReviews.collectAsState()
    val averageRating by bikeViewModel.averageRating.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    
    // Review form state
    var showReviewForm by remember { mutableStateOf(false) }
    var isSubmittingReview by remember { mutableStateOf(false) }
    
    // Load reviews when the screen is shown
    LaunchedEffect(bike.id) {
        bikeViewModel.fetchReviewsForBike(bike.id)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Large Bike Image
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(bike.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = bike.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bike Name
        Text(
            text = bike.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Price
        Text(
            text = bike.price,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Type
        Text(
            text = "Type: ${bike.type}",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description Title
        Text(
            text = "Description",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description Content
        Text(
            text = bike.description.ifEmpty { "High quality ${bike.type.lowercase()} bike available for rent in your area." },
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bike Specifications
        Text(
            text = "Specifications",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Battery Level (if applicable)
        if (bike.batteryLevel > 0) {
            DetailItem(title = "Battery", value = "${bike.batteryLevel}%")
        }
        
        // Rating
        if (bike.rating > 0) {
            DetailItem(title = "Rating", value = "${bike.rating}/5.0")
        }
        
        // Availability Status
        DetailItem(
            title = "Status", 
            value = if (bike.isAvailable) "Available" else "Currently Unavailable"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reviews Section
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
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Book Now Button
        RestrictedButton(
            text = "Book Now",
            featureType = "booking",
            onClick = { onBookClick(bike) },
            onCompleteProfile = onCompleteProfile,
            modifier = Modifier.fillMaxWidth(),
            containerColor = DarkGreen
        )
    }
}

@Composable
fun DetailItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp
        )
    }
} 