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
    
    // Dedicated scopes for background tasks
    private val mainActivityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val computeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Initialization state
    private val _initializationComplete = MutableStateFlow(false)
    val initializationComplete: StateFlow<Boolean> = _initializationComplete.asStateFlow()
    
    init {
        // Logging setup - moved to background to avoid blocking main thread initialization
        mainActivityScope.launch {
            try {
                Logger.getLogger("com.example.bikerental").level = Level.ALL
                Log.d(TAG, "Logging configuration complete")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to configure logging: ${e.message}")
            }
        }
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initiate background initialization immediately so it can run concurrently with UI setup
        GlobalScope.launch(Dispatchers.IO) {
            initializeBackgroundComponents()
        }
        
        // Initialize location services - create the client reference but defer actual initialization
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Create location callback - just the definition, not the registration
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates on a background thread
                mainActivityScope.launch {
                    // Process location updates here
                }
            }
        }
        
        // Set up UI immediately for better perceived performance
        setContent {
            BikerentalTheme {
                val navController = rememberNavController()
                var showSplash by remember { mutableStateOf(true) }
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                
                // Shortened splash screen duration for faster startup
                LaunchedEffect(Unit) {
                    launch(Dispatchers.Default) {
                        kotlinx.coroutines.delay(800) // Reduced from 1000ms to 800ms
                        withContext(Dispatchers.Main) {
                            showSplash = false
                        }
                    }
                }
                
                // Navigation logic only runs after splash screen is dismissed
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
    
    // Moved background initialization to a dedicated function
    private suspend fun initializeBackgroundComponents() {
        try {
            Log.d(TAG, "Starting background initialization")
            
            // Check email verification in the background
            authViewModel.checkEmailVerification()
            
            // Pre-fetch any common data
            withContext(Dispatchers.IO) {
                try {
                    // Firebase connection priming - using an optimized read to establish connection
                    FirebaseFirestore.getInstance()
                        .collection("app_config")
                        .document("startup")
                        .get(com.google.firebase.firestore.Source.CACHE)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "Firebase connection initialized")
                            }
                        }
                } catch (e: Exception) {
                    // Non-critical, can continue
                    Log.w(TAG, "Firebase priming failed: ${e.message}")
                }
            }
            
            // Any other background initialization tasks
            // ...
            
            Log.d(TAG, "Background initialization complete")
            _initializationComplete.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Error during background initialization: ${e.message}")
            // Even on error, mark as complete to not block the app
            _initializationComplete.value = true
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        // Set up auth state listener in the background
        mainActivityScope.launch {
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
    }
    
    override fun onStop() {
        super.onStop()
        
        // Remove location updates to save battery - defer to background
        mainActivityScope.launch {
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                    .addOnCompleteListener { task ->
                        Log.d(TAG, "Location updates removed: ${task.isSuccessful}")
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up in the background to avoid blocking UI during teardown
        mainActivityScope.launch {
            try {
                // Clean up location callbacks
                if (::locationCallback.isInitialized) {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
                
                // Remove auth state listener
                authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
                
                Log.d(TAG, "Resources successfully cleaned up")
            } catch (e: Exception) {
                Log.w(TAG, "Error during cleanup: ${e.message}")
            } finally {
                // Cancel all coroutines from our scopes
                computeScope.cancel()
                mainActivityScope.cancel()
            }
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

