package com.example.bikerental.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AccessAccount : Screen("accessAccount")
    object SignUp : Screen("signUp")
    object Profile : Screen("profile")
    object BikeList : Screen("bikeList")
    object BikeDetails : Screen("bikeDetails/{bikeId}") {
        fun createRoute(bikeId: String) = "bikeDetails/$bikeId"
    }
} 