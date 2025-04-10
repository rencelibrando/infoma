package com.example.bikerental.ui.theme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.bikerental.components.AppNavigationDrawer
import com.example.bikerental.components.AppTopBar
import com.example.bikerental.components.swipeToOpenDrawer
import kotlinx.coroutines.launch

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
    
    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Function to open drawer
    val openDrawer: () -> Unit = {
        scope.launch {
            drawerState.open()
        }
    }
    
    // Check if user is logged in
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            navController.navigate("signin") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    RequirementsWrapper {
        AppNavigationDrawer(
            drawerState = drawerState,
            selectedItem = selectedTab,
            onItemSelected = { newTab -> 
                selectedTab = newTab
                scope.launch {
                    drawerState.close()
                }
            },
            openDrawer = openDrawer,
            navController = navController,
            viewModel = viewModel,
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content with map
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .swipeToOpenDrawer(openDrawer),
                        topBar = {
                            AppTopBar(
                                onMenuClick = openDrawer
                            )
                        },
                        containerColor = Color.Transparent, // Transparent background for the Scaffold
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
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
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BikerentalTheme {
       HomeScreen(navController = rememberNavController())
    }
}