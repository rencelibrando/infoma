package com.example.bikerental.ui.theme
import com.example.bikerental.components.BottomNavigationBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.example.bikerental.components.RequirementsWrapper
import com.example.bikerental.viewmodels.AuthViewModel
import com.example.bikerental.screens.tabs.BikesTab
import com.example.bikerental.screens.tabs.BookingsTab
import com.example.bikerental.screens.tabs.MapTab
import com.example.bikerental.screens.tabs.ProfileScreen

data class BikeLocation(
    val id: String,
    val name: String,
    val type: String,
    val price: String,
    val location: LatLng,
    val isAvailable: Boolean = true
)

data class RouteInfo(
    val distance: String,
    val duration: String,
    val polylinePoints: List<LatLng>,
    val steps: List<String>,
    val isAlternative: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    fusedLocationProviderClient: FusedLocationProviderClient? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val viewModel = remember { AuthViewModel() }
    
    // Check if user is logged in
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            navController.navigate("signin") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    RequirementsWrapper {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
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
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top App Bar with Search and Notifications - only shown for Bikes and Bookings tabs
                if (selectedTab == 1 || selectedTab == 2) {
                    TopAppBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { newQuery -> searchQuery = newQuery }
                    )
                }

                // Display content based on selected tab
                when (selectedTab) {
                    0 -> MapTab(fusedLocationProviderClient)
                    1 -> BikesTab(fusedLocationProviderClient)
                    2 -> BookingsTab()
                    3 -> ProfileScreen(navController, viewModel)
                }
            }
        }
    }
}

@Composable
fun TopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
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
                    .height(50.dp),
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
}
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BikerentalTheme {
       HomeScreen(navController = rememberNavController())

    }
}