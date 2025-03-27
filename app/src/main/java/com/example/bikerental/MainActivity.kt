package com.example.bikerental

import GearTickLoginScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import com.example.bikerental.screens.login.AccessAccountScreen
import com.example.bikerental.screens.login.SignUpScreen
import com.example.bikerental.ui.theme.BikerentalTheme
import com.example.bikerental.ui.theme.HomeScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            BikerentalTheme {
                MyAppNavigation(fusedLocationClient)
            }
        }
    }
}

@Composable
fun MyAppNavigation(fusedLocationProviderClient: FusedLocationProviderClient) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "login") {
        composable("login") { GearTickLoginScreen(navController) }
        composable("signIn") { AccessAccountScreen(navController) }
        composable("signUp") { SignUpScreen(navController = navController) }
        composable("home") {
            HomeScreen(
                navController = navController,
                fusedLocationProviderClient = fusedLocationProviderClient
            )
        }
    }
}
