package com.example.bikerental.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 * This centralizes route definitions and prevents hardcoded strings.
 */
sealed class Screen(val route: String) {
    // Main screens
    object Initial : Screen("initial")
    object Home : Screen("home")
    
    // Auth screens
    object SignIn : Screen("signIn")
    object SignUp : Screen("signUp")
    object AccessAccount : Screen("accessAccount")
    object EmailVerification : Screen("emailVerification")
    object IdVerification : Screen("idVerification")
    
    // Profile related screens
    object Profile : Screen("profile")
    object EditProfile : Screen("editProfile")
    object ChangePassword : Screen("changePassword")
    object Help : Screen("help")
    
    // Bike related screens
    object BikeList : Screen("bikeList")
    object BikeDetails : Screen("bikeDetails/{bikeId}") {
        fun createRoute(bikeId: String) = "bikeDetails/$bikeId"
    }
    
    // Admin screens
    object BikeUpload : Screen("bikeUpload")
    
    // Booking related screens
    object Bookings : Screen("bookings")
    object BookingDetails : Screen("bookingDetails/{bookingId}") {
        fun createRoute(bookingId: String) = "bookingDetails/$bookingId"
    }
    object BookingForm : Screen("bookingForm/{bikeId}") {
        fun createRoute(bikeId: String) = "bookingForm/$bikeId"
    }
} 