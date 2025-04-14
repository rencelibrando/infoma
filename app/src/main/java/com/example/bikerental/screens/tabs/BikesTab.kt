package com.example.bikerental.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bikerental.components.RestrictedButton
import com.example.bikerental.models.Bike
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.LocationManager
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.*

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
    
    // Collect bikes from Firestore
    val bikes by bikeViewModel.bikes.collectAsState()
    val isLoading by bikeViewModel.isLoading.collectAsState()
    val error by bikeViewModel.error.collectAsState()
    
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
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Available Bikes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading && bikes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null && bikes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error loading bikes: ${error}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bikes) { bike ->
                    BikeCard(
                        bike = bike,
                        currentLocation = currentLocation,
                        onBikeClick = {
                            // Will navigate to bike detail screen
                        },
                        onBook = {
                            // Would navigate to booking flow in a real implementation
                        },
                        onCompleteProfile = {
                            // Navigate to profile completion
                            navController?.navigate("editProfile")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeCard(
    bike: Bike,
    currentLocation: LatLng?,
    onBikeClick: (Bike) -> Unit = {},
    onBook: (Bike) -> Unit,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = { onBikeClick(bike) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Bike Image with error handling and placeholder
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
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
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bike Name
            Text(
                text = bike.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bike Price
            Text(
                text = bike.price,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Type and Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bike Type
                Text(
                    text = bike.type,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
            
            // Bike Description
            Text(
                text = bike.description.ifEmpty { "High quality ${bike.type.lowercase()} bike available for rent in your area." },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Booking button with feature restriction
            RestrictedButton(
                text = "Book Now",
                featureType = "booking", // This requires complete profile & verified phone
                onClick = { onBook(bike) },
                onCompleteProfile = onCompleteProfile,
                modifier = Modifier.fillMaxWidth()
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