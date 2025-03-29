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
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import com.example.bikerental.screens.login.AccessAccountScreen
import com.example.bikerental.screens.login.SignUpScreen
import com.example.bikerental.ui.theme.BikerentalTheme
import com.example.bikerental.ui.theme.HomeScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.bikerental.screens.profile.EditProfileScreen

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If user is logged in and tries to go back, don't allow navigation to login screens
                if (auth.currentUser != null) {
                    // Only finish activity if we're on the home screen
                    if (isTaskRoot) {
                        finish()
                    }
                } else {
                    // Normal back navigation for non-logged-in users
                    if (isEnabled) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

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
    val auth = FirebaseAuth.getInstance()

    // Check initial auth state
    LaunchedEffect(Unit) {
        val startDestination = if (auth.currentUser != null) "home" else "signIn"
        navController.navigate(startDestination) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(navController, startDestination = "signIn") {
        composable("login") { 
            // Prevent access to login screen if already logged in
            LaunchedEffect(Unit) {
                if (auth.currentUser != null) {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            GearTickLoginScreen(navController) 
        }
        composable("signIn") { 
            // Prevent access to sign in screen if already logged in
            LaunchedEffect(Unit) {
                if (auth.currentUser != null) {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            AccessAccountScreen(navController) 
        }
        composable("signUp") { 
            // Prevent access to sign up screen if already logged in
            LaunchedEffect(Unit) {
                if (auth.currentUser != null) {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            SignUpScreen(navController = navController) 
        }
        composable("home") {
            // Prevent access to home screen if not logged in
            LaunchedEffect(Unit) {
                if (auth.currentUser == null) {
                    navController.navigate("signIn") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            HomeScreen(
                navController = navController,
                fusedLocationProviderClient = fusedLocationProviderClient
            )
        }
        composable("editProfile") {
            // Prevent access to edit profile if not logged in
            LaunchedEffect(Unit) {
                if (auth.currentUser == null) {
                    navController.navigate("signIn") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            EditProfileScreen(navController = navController)
        }
    }
}
