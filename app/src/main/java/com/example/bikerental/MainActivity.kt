package com.example.bikerental

import com.example.bikerental.screens.login.GearTickLoginScreen
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
import com.example.bikerental.screens.profile.ChangePasswordScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.bikerental.navigation.Screen
import androidx.lifecycle.ViewModelProvider
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bikerental.models.AuthState
import java.util.logging.Level
import java.util.logging.Logger
import com.example.bikerental.screens.verification.EmailVerificationScreen
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikerental.components.ResponsiveButton
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.Surface
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.bikerental.screens.BikeDetailScreen
import com.example.bikerental.screens.tabs.BikesTab
import com.example.bikerental.screens.tabs.BookingsTab
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    // ViewModel injection using Hilt
    private val authViewModel: AuthViewModel by viewModels()
    private val phoneAuthViewModel: PhoneAuthViewModel by viewModels()
    
    // Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    // Auth state listener
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    
    // Dedicated scope for background initialization tasks
    private val mainActivityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // Logging setup
        try {
            Logger.getLogger("com.example.bikerental").level = Level.ALL
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure logging: ${e.message}")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Use a dedicated scope for initialization to prevent blocking main thread
        mainActivityScope.launch {
            try {
                Log.d(TAG, "Initializing Google API services on background thread")
                
                // Only initialize location services on the main thread
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        // Handle location updates on a background thread
                        mainActivityScope.launch {
                            // Process location updates here
                        }
                    }
                }
                
                // Check email verification in the background
                authViewModel.checkEmailVerification()
            } catch (e: Exception) {
                Log.w(TAG, "Error during background initialization: ${e.message}")
            }
        }

        setContent {
            BikerentalTheme {
                val navController = rememberNavController()
                var showSplash by remember { mutableStateOf(true) }
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                
                LaunchedEffect(Unit) {
                    launch {
                        kotlinx.coroutines.delay(1000)
                        showSplash = false
                    }
                }
                
                LaunchedEffect(showSplash, authState) {
                    if (!showSplash) {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                Log.d(TAG, "User authenticated, navigating to Home")
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is AuthState.NeedsEmailVerification -> {
                                Log.d(TAG, "User needs email verification")
                                navController.navigate(Screen.EmailVerification.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            is AuthState.Initial, is AuthState.Error -> {
                                if (navController.currentDestination?.route != Screen.Initial.route) {
                                    Log.d(TAG, "User not authenticated, navigating to initial login")
                                    navController.navigate(Screen.Initial.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            else -> {
                                // Keep current screen for other states
                            }
                        }
                    }
                }
                
                if (showSplash) {
                    SplashScreen(onSplashComplete = { /* Ignore manual completion, using timer */ })
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            navController = navController,
                            authViewModel = authViewModel,
                            fusedLocationClient = fusedLocationClient
                        )
                    }
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        // Set up auth state listener if needed
        if (authStateListener == null) {
            authStateListener = FirebaseAuth.AuthStateListener { auth ->
                // Handle auth state changes in a background thread
                mainActivityScope.launch {
                    val user = auth.currentUser
                    Log.d(TAG, "Auth state changed: user ${if (user != null) "signed in" else "signed out"}")
                    
                    // Additional auth state processing can be done here
                }
            }
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)
        }
    }
    
    override fun onStop() {
        super.onStop()
        
        // Remove location updates to save battery
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up location callbacks
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        
        // Remove auth state listener
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        
        // Cancel all coroutines from our scope
        mainActivityScope.cancel()
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
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    fusedLocationClient: FusedLocationProviderClient
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Initial.route,
        modifier = Modifier.fillMaxSize()
    ) {
        // Auth routes
        composable(Screen.Initial.route) { GearTickLoginScreen(navController) }
        composable(Screen.SignIn.route) { AccessAccountScreen(navController) }
        composable(Screen.SignUp.route) { SignUpScreen(navController) }
        composable(Screen.EmailVerification.route) { EmailVerificationScreen(navController) }
        
        // Main app routes
        composable(Screen.Home.route) { HomeScreen(navController, fusedLocationClient) }
        composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
        composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController, authViewModel) }
        
        // Add missing routes
        composable(Screen.BikeList.route) { BikesTab(fusedLocationClient) }
        composable(Screen.Bookings.route) { BookingsTab() }
        
        // Bike detail route
        composable(
            route = Screen.BikeDetails.route,
            arguments = listOf(navArgument("bikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId") ?: ""
            BikeDetailScreen(bikeId = bikeId, navController = navController)
        }
        
        // Booking detail route
        composable(
            route = Screen.BookingDetails.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            // BookingDetailScreen(bookingId = bookingId, navController = navController)
            // Placeholder until you implement the booking detail screen
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
    }
}

