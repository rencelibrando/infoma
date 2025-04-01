package com.example.bikerental

import GearTickLoginScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import com.example.bikerental.screens.login.AccessAccountScreen
import com.example.bikerental.screens.login.SignUpScreen
import com.example.bikerental.ui.theme.BikerentalTheme
import com.example.bikerental.ui.theme.HomeScreen
import com.example.bikerental.ui.theme.ProfileScreen
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.example.bikerental.screens.profile.EditProfileScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bikerental.screens.splash.SplashScreen
import com.example.bikerental.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates here if needed
            }
        }

        setContent {
            BikerentalTheme {
                var isLoading by remember { mutableStateOf(true) }
                var showSplash by remember { mutableStateOf(true) }
                var isLoggedIn by remember { mutableStateOf(false) }

                // Add auth state listener
                DisposableEffect(Unit) {
                    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        isLoggedIn = firebaseAuth.currentUser != null
                    }
                    auth.addAuthStateListener(authStateListener)
                    onDispose {
                        auth.removeAuthStateListener(authStateListener)
                    }
                }

                // Initial auth check
                LaunchedEffect(Unit) {
                    isLoggedIn = auth.currentUser != null
                    isLoading = false
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "home" else "initial",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("initial") {
                            GearTickLoginScreen(navController)
                        }
                        
                        composable("signIn") {
                            if (isLoggedIn) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("home") {
                                        popUpTo("initial") { inclusive = true }
                                    }
                                }
                            }
                            AccessAccountScreen(navController)
                        }
                        
                        composable("signUp") {
                            if (isLoggedIn) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("home") {
                                        popUpTo("initial") { inclusive = true }
                                    }
                                }
                            }
                            SignUpScreen(navController)
                        }
                        
                        composable("home") {
                            if (!isLoggedIn) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("initial") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            }
                            if (showSplash && isLoggedIn) {
                                SplashScreen { showSplash = false }
                            } else {
                                HomeScreen(navController, fusedLocationClient)
                            }
                        }
                        
                        composable("profile") {
                            if (!isLoggedIn) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("initial") {
                                        popUpTo("profile") { inclusive = true }
                                    }
                                }
                            }
                            val viewModel = remember { AuthViewModel() }
                            ProfileScreen(navController, viewModel)
                        }
                        
                        composable("editProfile") {
                            if (!isLoggedIn) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("initial") {
                                        popUpTo("editProfile") { inclusive = true }
                                    }
                                }
                            }
                            EditProfileScreen(navController)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
