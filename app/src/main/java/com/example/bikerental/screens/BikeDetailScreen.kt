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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.bikerental.utils.ProfileRestrictionUtils

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
    
    // Define userData for use in onCompleteProfile
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    // Get context for Toast messages
    val context = LocalContext.current
    
    // Fetch user data when screen is displayed
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.data
                    }
                }
        }
    }
    
    // Fetch bike details when screen is displayed
    LaunchedEffect(bikeId) {
        bikeViewModel.getBikeById(bikeId)
    }
    
    // Create a function to handle profile completion navigation
    val handleProfileCompletion = {
        // Add debug logging to track the navigation flow
        Log.d("BikeDetailScreen", "Starting profile completion check with userData: ${userData?.keys}")
        
        // Navigate to appropriate screen based on verification needs
        if (userData != null) {
            val userDataSnapshot = userData // Create a non-null local copy for safe access
            
            // Log verification states to debug
            Log.d("BikeDetailScreen", "Profile complete: ${userDataSnapshot?.let { ProfileRestrictionUtils.isProfileComplete(it) }}")
            Log.d("BikeDetailScreen", "Email verified: ${userDataSnapshot?.let { ProfileRestrictionUtils.isEmailVerified(it) }}")
            Log.d("BikeDetailScreen", "ID verified: ${userDataSnapshot?.let { ProfileRestrictionUtils.isIdVerified(it) }}")
            Log.d("BikeDetailScreen", "ID status: ${userDataSnapshot?.get("idVerificationStatus")}")
            
            if (userDataSnapshot?.let { !ProfileRestrictionUtils.isProfileComplete(it) } == true) {
                Log.d("BikeDetailScreen", "Navigating to profile completion")
                navController.navigate("editProfile")
                android.widget.Toast.makeText(context, "Please complete your profile first", android.widget.Toast.LENGTH_SHORT).show()
            } else if (userDataSnapshot?.let { !ProfileRestrictionUtils.isEmailVerified(it) } == true) {
                Log.d("BikeDetailScreen", "Navigating to email verification")
                navController.navigate("emailVerification")
                android.widget.Toast.makeText(context, "Please verify your email first", android.widget.Toast.LENGTH_SHORT).show()
            } else if (userDataSnapshot?.let { !ProfileRestrictionUtils.isIdVerified(it) } == true) {
                // Only navigate to ID verification if it's required and not already verified
                Log.d("BikeDetailScreen", "Navigating to ID verification using direct route")
                try {
                    // Try with screen object reference
                    navController.navigate(com.example.bikerental.navigation.Screen.IdVerification.route)
                    android.widget.Toast.makeText(context, "Please verify your ID before booking", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("BikeDetailScreen", "Error navigating with Screen reference: ${e.message}")
                    // Fallback to string-based navigation
                    try {
                        navController.navigate("idVerification")
                        android.widget.Toast.makeText(context, "Please verify your ID before booking", android.widget.Toast.LENGTH_LONG).show()
                    } catch (e2: Exception) {
                        Log.e("BikeDetailScreen", "Failed to navigate to ID verification: ${e2.message}")
                        android.widget.Toast.makeText(context, "Error: Couldn't open ID verification. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // All verification steps are complete, proceed with booking
                Log.d("BikeDetailScreen", "All verifications complete, proceeding to booking")
                navController.navigate("bookingForm/${bikeId}")
            }
        } else {
            // Navigate to profile edit by default
            Log.d("BikeDetailScreen", "No user data, navigating to profile edit")
            navController.navigate("editProfile")
            android.widget.Toast.makeText(context, "Please complete your profile first", android.widget.Toast.LENGTH_SHORT).show()
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
                        // Navigate to booking page
                        navController.navigate("bookingForm/${bikeId}")
                    },
                    onCompleteProfile = handleProfileCompletion,
                    navController = navController
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
    onCompleteProfile: () -> Unit,
    navController: NavController
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
        
        // Book Now Button - With ID Verification Check
        RestrictedButton(
            text = "Book Now",
            featureType = "booking",
            onClick = { onBookClick(bike) },
            onCompleteProfile = onCompleteProfile,
            modifier = Modifier.fillMaxWidth(),
            containerColor = DarkGreen
        )
        
        // Check ID verification status and show appropriate message
        var idVerificationStatus by remember { mutableStateOf("") }
        
        LaunchedEffect(Unit) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            idVerificationStatus = document.data?.get("idVerificationStatus")?.toString() ?: "unverified"
                        }
                    }
            }
        }
        
        // Show status cards based on verification status
        if (idVerificationStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            when (idVerificationStatus) {
                "pending" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFA000).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "ID Verification Pending",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA000)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Your ID verification is under review. You'll be able to book once approved.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(
                                onClick = { navController.navigate("idVerification") }
                            ) {
                                Text("View Status")
                            }
                        }
                    }
                }
                "declined" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "ID Verification Declined",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Your ID verification was declined. Please resubmit with a clearer image.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { navController.navigate("idVerification") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                )
                            ) {
                                Text("Resubmit ID")
                            }
                        }
                    }
                }
            }
        }
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