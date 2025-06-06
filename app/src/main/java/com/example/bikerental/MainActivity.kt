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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.bikerental.screens.help.HelpSupportScreen
import com.example.bikerental.utils.PerformanceMonitor
import com.example.bikerental.services.NotificationService
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    // ViewModel injection using Hilt
    private val authViewModel: AuthViewModel by viewModels()
    private val phoneAuthViewModel: PhoneAuthViewModel by viewModels()
    
    // Service injection
    @Inject
    lateinit var notificationService: NotificationService
    
    // Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    // Auth state listener
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    
    // Simplified scope for background tasks
    private val mainActivityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Start performance tracking
        PerformanceMonitor.startTiming("total_startup")
        PerformanceMonitor.startTiming("main_activity_creation")
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize location services immediately
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates
            }
        }
        
        PerformanceMonitor.endTiming("main_activity_creation")
        
        // Set up UI immediately for better perceived performance
        setContent {
            BikerentalTheme {
                val navController = rememberNavController()
                var showSplash by remember { mutableStateOf(true) }
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                
                // Shorter splash screen for faster startup
                LaunchedEffect(Unit) {
                    // Reduced splash time and simplified
                    kotlinx.coroutines.delay(500) // Reduced from 800ms to 500ms
                    showSplash = false
                    PerformanceMonitor.endTiming("total_startup")
                }
                
                // Simplified navigation logic with better Loading state handling
                LaunchedEffect(showSplash, authState) {
                    Log.d(TAG, "Navigation check - showSplash: $showSplash, authState: $authState")
                    
                    if (!showSplash) {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                Log.d(TAG, "User authenticated, navigating to Home")
                                if (navController.currentDestination?.route != Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            is AuthState.NeedsEmailVerification -> {
                                Log.d(TAG, "User needs email verification")
                                if (navController.currentDestination?.route != Screen.EmailVerification.route) {
                                    navController.navigate(Screen.EmailVerification.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            is AuthState.Loading -> {
                                Log.d(TAG, "Auth state is Loading, waiting for resolution...")
                                // Wait for auth state to resolve, don't navigate yet
                            }
                            is AuthState.NeedsAdditionalInfo -> {
                                Log.d(TAG, "User needs additional info, navigating to setup")
                                if (navController.currentDestination?.route != Screen.SignUp.route) {
                                    navController.navigate(Screen.SignUp.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            is AuthState.NeedsAppVerification -> {
                                Log.d(TAG, "User needs app verification")
                                if (navController.currentDestination?.route != Screen.EmailVerification.route) {
                                    navController.navigate(Screen.EmailVerification.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            is AuthState.PasswordResetSent -> {
                                Log.d(TAG, "Password reset sent, staying on current screen")
                                // Stay on current screen, show success message
                            }
                            is AuthState.VerificationEmailSent -> {
                                Log.d(TAG, "Verification email sent, staying on current screen")
                                // Stay on current screen, show success message
                            }
                            is AuthState.VerificationSuccess -> {
                                Log.d(TAG, "Verification successful, navigating to Home")
                                if (navController.currentDestination?.route != Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            is AuthState.Initial, is AuthState.Error -> {
                                if (navController.currentDestination?.route != Screen.Initial.route) {
                                    Log.d(TAG, "User not authenticated (state: $authState), navigating to initial login")
                                    navController.navigate(Screen.Initial.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Still showing splash screen, auth state: $authState")
                    }
                }
                
                if (showSplash) {
                    SplashScreen(onSplashComplete = { /* Using timer only */ })
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
        
        // Simplified auth state listener setup with notification monitoring
        if (authStateListener == null) {
            authStateListener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                Log.d(TAG, "Auth state changed: user ${if (user != null) "signed in" else "signed out"}")
                
                // Start or stop notification monitoring based on auth state
                if (user != null) {
                    Log.d(TAG, "Starting notification monitoring for user: ${user.uid}")
                    notificationService.startMonitoring()
                } else {
                    Log.d(TAG, "Stopping notification monitoring - user signed out")
                    notificationService.stopMonitoring()
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
        
        // Clean up resources
        try {
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
            
            authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
            
            // Stop notification monitoring
            notificationService.stopMonitoring()
            
            Log.d(TAG, "Resources successfully cleaned up")
        } catch (e: Exception) {
            Log.w(TAG, "Error during cleanup: ${e.message}")
        } finally {
            mainActivityScope.cancel()
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

        // Add our new help screen route to the NavHost
        composable("help") {
            HelpSupportScreen(navController = navController)
        }
    }
}

