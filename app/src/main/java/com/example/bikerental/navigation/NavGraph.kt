package com.example.bikerental.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikerental.screens.BikeDetailScreen
import com.example.bikerental.screens.help.HelpSupportScreen
import com.example.bikerental.screens.tabs.BikesTab
import com.example.bikerental.screens.tabs.BookingsTab
import com.example.bikerental.screens.tabs.MapTab
import com.example.bikerental.screens.tabs.ProfileScreen
import com.example.bikerental.screens.tabs.RideHistoryTab
import com.example.bikerental.screens.verification.EmailVerificationScreen
import com.example.bikerental.screens.verification.IdVerificationScreen
import com.example.bikerental.utils.ColorUtils
import com.google.android.gms.location.FusedLocationProviderClient

fun NavHostController.navigate(route: String, popBackStack: Boolean = false) {
    if (popBackStack) {
        this.navigate(route) {
            popUpTo(0) { inclusive = true }
        }
    } else {
        this.navigate(route)
    }
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    fusedLocationProviderClient: FusedLocationProviderClient? = null
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier
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
        
        // Email verification screen
        composable(route = "emailVerification") {
            EmailVerificationScreen(navController = navController)
        }
        
        // ID verification screen
        composable(route = Screen.IdVerification.route) {
            IdVerificationScreen(navController = navController)
        }
        
        // Help and support screen
        composable(route = "help") {
            HelpSupportScreen(navController = navController)
        }
    }
} 