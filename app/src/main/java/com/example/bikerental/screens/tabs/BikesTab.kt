package com.example.bikerental.screens.tabs

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bikerental.components.RestrictedButton
import com.example.bikerental.models.Bike
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

@Composable
fun BikesTab(
    fusedLocationProviderClient: FusedLocationProviderClient?,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    var bikes by remember { mutableStateOf(listOf<Bike>()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val locationManager = remember { LocationManager.getInstance(context) }
    
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
    
    // Simulated bike data
    LaunchedEffect(Unit) {
        bikes = listOf(
            Bike(
                id = "1",
                name = "Mountain Bike",
                type = "Mountain",
                price = "₱25/hr",
                priceValue = 25.0,
                imageUrl = "https://example.com/bike1.jpg",
                latitude = 14.5890,
                longitude = 120.9760
            ),
            Bike(
                id = "2",
                name = "Road Bike",
                type = "Road",
                price = "₱30/hr",
                priceValue = 30.0,
                imageUrl = "https://example.com/bike2.jpg",
                latitude = 14.5895,
                longitude = 120.9765
            ),
            Bike(
                id = "3",
                name = "City Cruiser",
                type = "Urban",
                price = "₱20/hr",
                priceValue = 20.0,
                imageUrl = "https://example.com/bike3.jpg",
                latitude = 14.5880,
                longitude = 120.9750
            ),
            Bike(
                id = "4",
                name = "BMX Bike",
                type = "BMX",
                price = "₱35/hr",
                priceValue = 35.0,
                imageUrl = "https://example.com/bike4.jpg",
                latitude = 14.5875,
                longitude = 120.9745
            )
        )
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(bikes) { bike ->
                BikeCard(
                    bike = bike,
                    currentLocation = currentLocation,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeCard(
    bike: Bike,
    currentLocation: LatLng?,
    onBook: (Bike) -> Unit,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = bike.imageUrl,
                contentDescription = bike.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bike.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = bike.price,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bike.type,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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