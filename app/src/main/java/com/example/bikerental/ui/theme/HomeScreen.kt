package com.example.bikerental.ui.theme
import BottomNavigationBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.R
import com.example.bikerental.ui.theme.map.BikeMapMarker
import com.google.android.gms.location.FusedLocationProviderClient

data class Bike(
    val id: String,
    val name: String,
    val type: String,
    val price: String,
    val rating: Float,
    val distance: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    fusedLocationProviderClient: FusedLocationProviderClient? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
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
        ) {
            // Top App Bar with Search and Notifications
            TopAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { newQuery -> searchQuery = newQuery }
            )

            // Display content based on selected tab
            when (selectedTab) {
                0 -> LocationTabContent(fusedLocationProviderClient)
                1 -> BikeListingsTabContent()
                2 -> BookingsTabContent()
                3 -> ProfileTabContent()
            }
        }
    }
}

@Composable
fun LocationTabContent(fusedLocationProviderClient: FusedLocationProviderClient?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Text(
                text = "Find Bikes in Intramuros",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { /* Handle location request */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Locate Me")
            }
        }

        // Placeholder for map area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Map View Placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Legend or Map Options
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
                    MapLegendItem(color = Color(0xFF4CAF50), label = "Available Bikes")
                    MapLegendItem(color = Color(0xFFFF9800), label = "Pickup Stations")
                    MapLegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Your Location")
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
fun BikeListingsTabContent() {
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

        // Featured Bikes Section
        SectionHeader(
            title = "Featured Bikes",
            onSeeAllClick = { /* Navigate to featured bikes page */ }
        )
        FeaturedBikesRow(bikes = featuredBikes)

        Spacer(modifier = Modifier.height(16.dp))

        // Available Bikes Near You Section
        SectionHeader(
            title = "Available Bikes Near You",
            onSeeAllClick = { /* Navigate to nearby bikes page */ }
        )
        AvailableBikesGrid(bikes = availableBikes)
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
fun ProfileTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Profile settings and information will be displayed here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
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
                        text = "• ${bike.distance} away",
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BikerentalTheme {
       HomeScreen(navController = rememberNavController())

    }
}