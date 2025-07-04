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
    object EmailVerification : Screen("emailVerification")
    
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
    // Booking related screens
    object Bookings : Screen("bookings?bikeId={bikeId}") {
        fun createRoute(bikeId: String? = null): String {
            return if (bikeId != null) "bookings?bikeId=$bikeId" else "bookings?bikeId="
        }
    }
    object BookingDetails : Screen("bookingDetails/{bookingId}") {
        fun createRoute(bookingId: String) = "bookingDetails/$bookingId"
    }
    object IdVerification : Screen("id_verification")
}

object NavigationRoute {
    const val HOME_ROUTE = "home_route"
    const val AUTH_ROUTE = "auth_route"
    const val SPLASH_ROUTE = "splash_route"
} 