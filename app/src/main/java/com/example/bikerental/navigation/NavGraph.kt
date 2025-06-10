package com.example.bikerental.navigation
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.bikerental.screens.BikeDetailScreen
import com.example.bikerental.screens.tabs.BikesTab
import com.example.bikerental.screens.tabs.BookingsTab
import com.example.bikerental.screens.tabs.MapTab
import com.example.bikerental.screens.tabs.ProfileScreen
import com.example.bikerental.screens.tabs.RideHistoryTab
import com.google.android.gms.location.FusedLocationProviderClient
import androidx.lifecycle.viewmodel.compose.viewModel

fun NavGraphBuilder.setUpHomeNavGraph(
    navController: NavHostController,
    fusedLocationProviderClient: FusedLocationProviderClient? = null
) {
    // Home tabs
    composable(route = "bikes") { 
        BikesTab(
            navController = navController,
            fusedLocationProviderClient = fusedLocationProviderClient
        )
    }
    
    composable(route = "map") { 
        MapTab()
    }
    
    composable(route = "history") { 
        RideHistoryTab()
    }
    
    composable(route = "profile") { 
        ProfileScreen(navController = navController, viewModel = viewModel())
    }
    
    // Bookings tab with optional bikeId parameter
    composable(
        route = "bookings?bikeId={bikeId}",
        arguments = listOf(
            navArgument("bikeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val bikeId = backStackEntry.arguments?.getString("bikeId")
        BookingsTab(
            navController = navController,
            bikeId = bikeId
        )
    }
    
    // Bike details screen
    composable(
        route = "bikeDetails/{bikeId}",
        arguments = listOf(
            navArgument("bikeId") {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val bikeId = backStackEntry.arguments?.getString("bikeId") ?: ""
        BikeDetailScreen(
            bikeId = bikeId,
            navController = navController
        )
    }
} 