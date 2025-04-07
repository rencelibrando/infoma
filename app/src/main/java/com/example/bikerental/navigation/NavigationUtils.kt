package com.example.bikerental.navigation

import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Utility class for centralized navigation handling throughout the app.
 * This provides consistent navigation patterns and eliminates hardcoded route strings.
 */
object NavigationUtils {

    /**
     * Navigate to a screen with options
     */
    fun navigateTo(
        navController: NavController,
        screen: Screen,
        builder: (NavOptionsBuilder.() -> Unit)? = null
    ) {
        if (builder != null) {
            navController.navigate(screen.route, builder)
        } else {
            navController.navigate(screen.route)
        }
    }

    /**
     * Navigate to home screen and clear back stack
     */
    fun navigateToHome(navController: NavController) {
        navController.navigate(Screen.Home.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    /**
     * Navigate to the initial login screen and clear back stack
     */
    fun navigateToLogin(navController: NavController) {
        Log.d("NavigationUtils", "Navigating to login screen")
        navController.navigate(Screen.Initial.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    /**
     * Navigate to the admin dashboard and clear back stack
     */
    fun navigateToAdminDashboard(navController: NavController) {
        Log.d("NavigationUtils", "Navigating to admin dashboard")
        navController.navigate(Screen.AdminDashboard.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    /**
     * Navigate to sign in screen with options
     */
    fun navigateToSignIn(navController: NavController, popUpRoute: String? = null) {
        navController.navigate(Screen.SignIn.route) {
            if (popUpRoute != null) {
                popUpTo(popUpRoute) { inclusive = true }
            }
        }
    }

    /**
     * Navigate to sign up screen with options
     */
    fun navigateToSignUp(navController: NavController, popUpRoute: String? = null) {
        navController.navigate(Screen.SignUp.route) {
            if (popUpRoute != null) {
                popUpTo(popUpRoute) { inclusive = true }
            }
        }
    }
    
    /**
     * Navigate to profile screen
     */
    fun navigateToProfile(navController: NavController) {
        navController.navigate(Screen.Profile.route)
    }
    
    /**
     * Navigate to bike details with the specified bike ID
     */
    fun navigateToBikeDetails(navController: NavController, bikeId: String) {
        navController.navigate(Screen.BikeDetails.createRoute(bikeId))
    }
    
    /**
     * Navigate to edit profile screen
     */
    fun navigateToEditProfile(navController: NavController) {
        navController.navigate(Screen.EditProfile.route)
    }
    
    /**
     * Navigate to change password screen
     */
    fun navigateToChangePassword(navController: NavController) {
        navController.navigate(Screen.ChangePassword.route)
    }
    
    /**
     * Navigate to help screen
     */
    fun navigateToHelp(navController: NavController) {
        navController.navigate(Screen.Help.route)
    }
    
    /**
     * Navigate to bookings screen
     */
    fun navigateToBookings(navController: NavController) {
        navController.navigate(Screen.Bookings.route)
    }
    
    /**
     * Navigate to booking details with the specified booking ID
     */
    fun navigateToBookingDetails(navController: NavController, bookingId: String) {
        navController.navigate(Screen.BookingDetails.createRoute(bookingId))
    }
} 