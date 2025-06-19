package com.example.bikerental.ui.theme
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.components.AppNavigationDrawer
import com.example.bikerental.components.AppTopBar
import com.example.bikerental.components.RequirementsWrapper
import com.example.bikerental.components.swipeToOpenDrawer
import com.example.bikerental.screens.tabs.BikesTab
import com.example.bikerental.screens.tabs.BookingsTab
import com.example.bikerental.screens.tabs.MapTab
import com.example.bikerental.screens.tabs.PaymentTab
import com.example.bikerental.screens.tabs.ProfileScreen
import com.example.bikerental.screens.tabs.RideHistoryTab
import com.example.bikerental.screens.tabs.NotificationsTab
import com.example.bikerental.viewmodels.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BikeLocation(
    val id: String,
    val name: String,
    val type: String,
    val price: String,
    val location: LatLng,
    val isAvailable: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    fusedLocationProviderClient: FusedLocationProviderClient? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val viewModel: AuthViewModel = hiltViewModel()
    
    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Function to open drawer
    val openDrawer: () -> Unit = {
        scope.launch {
            drawerState.open()
        }
    }
    
    // Handle return navigation from profile editing
    LaunchedEffect(navController) {
        val navBackStackEntry = navController.currentBackStackEntry
        val returnToProfileTab = navBackStackEntry?.savedStateHandle?.get<Boolean>("returnToProfileTab") == true
            
        if (returnToProfileTab) {
            // Clear the flag to prevent repeated handling
            navBackStackEntry?.savedStateHandle?.remove<Boolean>("returnToProfileTab")
            // Update the selected tab to Profile (index 4)
            selectedTab = 4
        }
    }

    // Only run verification status update if needed - with reduced processing
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
                
                if (currentUser.isEmailVerified) {
                    // Check if Firestore needs update
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                        
                    if (userDoc.exists() && userDoc.getBoolean("isEmailVerified") != true) {
                        Log.d("HomeScreen", "Updating verification status in background")
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.uid)
                            .update(
                                mapOf(
                                    "isEmailVerified" to true,
                                    "hasCompletedAppVerification" to true
                                )
                            )
                            .await()
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Background verification task error: ${e.message}")
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
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .swipeToOpenDrawer(openDrawer),
                        topBar = { AppTopBar(onMenuClick = openDrawer) },
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (selectedTab) {
                                0 -> MapTab(navController = navController)
                                1 -> BikesTab(navController = navController, fusedLocationProviderClient = fusedLocationProviderClient)
                                2 -> BookingsTab(navController = navController)
                                3 -> PaymentTab(navController = navController)
                                4 -> ProfileScreen(navController = navController, viewModel = viewModel)
                                5 -> RideHistoryTab()
                                6 -> NotificationsTab(navController = navController)
                                else -> RideHistoryTab()
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