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
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.example.bikerental.screens.profile.EditProfileScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bikerental.screens.splash.SplashScreen
import com.example.bikerental.screens.tabs.ProfileScreen
import com.example.bikerental.viewmodels.AuthViewModel
import com.example.bikerental.viewmodels.PhoneAuthViewModel
import androidx.activity.viewModels
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val auth = FirebaseAuth.getInstance()
    private val phoneAuthViewModel: PhoneAuthViewModel by viewModels()

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

        // Handle auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { user ->
                phoneAuthViewModel.updateAuthState(user)
            }
        }

        setContent {
            BikerentalTheme {
                var isLoading by remember { mutableStateOf(true) }
                var showSplash by remember { mutableStateOf(true) }
                var isLoggedIn by remember { mutableStateOf(false) }

                // Handle auth state changes
                DisposableEffect(Unit) {
                    val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        isLoggedIn = firebaseAuth.currentUser != null
                        firebaseAuth.currentUser?.let { user ->
                            phoneAuthViewModel.updateAuthState(user)
                        }
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

                val authState by phoneAuthViewModel.uiState.collectAsState()

                if (isLoading) {
                    LoadingScreen()
                } else {
                    NavigationContent(
                        isLoggedIn = isLoggedIn,
                        showSplash = showSplash,
                        onSplashComplete = { showSplash = false },
                        fusedLocationClient = fusedLocationClient
                    )
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

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NavigationContent(
    isLoggedIn: Boolean,
    showSplash: Boolean,
    onSplashComplete: () -> Unit,
    fusedLocationClient: FusedLocationProviderClient
) {
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
                SplashScreen(onSplashComplete)
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
