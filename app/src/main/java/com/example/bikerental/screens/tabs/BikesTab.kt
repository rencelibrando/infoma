package com.example.bikerental.screens.tabs

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bikerental.models.Bike
import com.example.bikerental.navigation.Screen
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.LocationManager
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    
    // Get location
    LaunchedEffect(fusedLocationProviderClient) {
        locationManager.getLastLocation(
            onSuccess = { location ->
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
                    items(6) {
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
                                onBikeClick = {
                                    selectedBike = bike
                                    showBottomSheet = true
                                },
                                onBook = {
                                    // Would navigate to booking flow in a real implementation
                                },
                                onCompleteProfile = {
                                    navController?.navigate("editProfile")
                                },
                                modifier = Modifier.fillMaxWidth()
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
                currentLocation = currentLocation,
                onViewDetails = {
                    showBottomSheet = false
                    navController?.navigate(Screen.BikeDetails.createRoute(selectedBike!!.id))
                },
                onBook = {
                    // Handle booking
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
                            location.latitude,
                            location.longitude,
                            bike.latitude,
                            bike.longitude
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
    currentLocation: LatLng?,
    onViewDetails: () -> Unit,
    onBook: (Bike) -> Unit,
    onCompleteProfile: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isAvailable = remember(bike) { bike.isAvailable && !bike.isInUse }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    location.latitude,
                    location.longitude,
                    bike.latitude,
                    bike.longitude
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkGreen)
            ) {
                Text(
                    text = "View Details",
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
        
        // Extra bottom padding for comfortable scrolling
        Spacer(modifier = Modifier.height(32.dp))
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