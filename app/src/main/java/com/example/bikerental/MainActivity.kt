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
import com.example.bikerental.screens.login.GoogleVerificationScreen
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
import com.google.firebase.auth.GoogleAuthProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isLoading by mutableStateOf(true)
    private val phoneAuthViewModel: PhoneAuthViewModel by viewModels()
    
    // Auth state
    private var isLoggedIn by mutableStateOf(false)
    private var isLoginScreenActive by mutableStateOf(false)
    private var showSplash by mutableStateOf(true)
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    
    companion object {
        private const val TAG = "MainActivity"
        
        // Configure logging to reduce warnings
        init {
            // Configure java.util.logging to be less verbose
            Logger.getLogger("com.google.android.gms").level = Level.WARNING
            
            // Attempt to configure Flogger logging early
            try {
                System.setProperty("flogger.backend_factory", 
                    "com.google.android.gms.flogger.backend.log4j.Log4jBackendFactory#getInstance")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to configure Flogger: ${e.message}")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Configure Google API logging early
        try {
            // GoogleApiAvailability has compatibility issues - use alternative approach
            /*
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            googleApiAvailability.isGooglePlayServicesAvailable(this)
            */
            Log.d(TAG, "Skipping GoogleApiAvailability configuration")
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring Google API: ${e.message}")
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates
            }
        }
        
        // Set up auth state listener
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            isLoggedIn = user != null
            Log.d("MainActivity", "Auth state changed: ${if (isLoggedIn) "logged in" else "logged out"}")
            isLoading = false
        }
        
        // Register auth state listener
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)

        setContent {
            BikerentalTheme {
                // State to track if splash screen has been shown
                var splashScreenComplete by remember { mutableStateOf(false) }
                // State to track if user is logged in
                val isLoggedIn = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

                // Show splash screen first if user is already logged in
                if (!splashScreenComplete && isLoggedIn.value) {
                    SplashScreen(onSplashComplete = { splashScreenComplete = true })
                } else {
                    // Normal app content after splash screen or if not logged in
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Set flag for login screen state tracking
                        var isLoginScreenActive by remember { mutableStateOf(false) }
                        
                        // Set callback to update parent's state from child navigation route
                        MainNavigation(
                            onLoginScreenChange = { isActive -> isLoginScreenActive = isActive },
                            fusedLocationClient = fusedLocationClient
                        )
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
        
        // Remove auth state listener
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
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
fun MainNavigation(
    onLoginScreenChange: (Boolean) -> Unit,
    fusedLocationClient: FusedLocationProviderClient
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Remember last navigation event to prevent losing tab state
    var lastNavigationEvent by remember { mutableStateOf("") }
    var handlingInternalNav by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        val isAuthScreen = when (authState) {
            is AuthState.Initial,
            is AuthState.Loading,
            is AuthState.NeedsEmailVerification,
            is AuthState.NeedsAppVerification,
            is AuthState.Error -> true
            is AuthState.Authenticated -> false
            else -> !navController.currentDestination?.route.equals(Screen.Home.route, ignoreCase = true)
        }
        onLoginScreenChange(isAuthScreen)
        Log.d("MainNavigation", "Auth State: ${authState::class.simpleName}, LoginScreenActive: $isAuthScreen")
    }

    // Track current back stack entry changes
    val currentBackStackEntry by rememberUpdatedState(navController.currentBackStackEntry)
    
    // Handle tab navigation and returnToProfileTab flag
    LaunchedEffect(currentBackStackEntry) {
        val returnToProfileTab = currentBackStackEntry?.savedStateHandle?.get<Boolean>("returnToProfileTab") ?: false
        if (returnToProfileTab && !handlingInternalNav) {
            handlingInternalNav = true
            // Record we're handling profile navigation
            lastNavigationEvent = "profile"
            // Clear flag after handling
            currentBackStackEntry?.savedStateHandle?.remove<Boolean>("returnToProfileTab")
            handlingInternalNav = false
        }
    }

    when (authState) {
        is AuthState.Loading, AuthState.Initial -> {
            LoadingScreen()
        }
        is AuthState.Authenticated -> {
            Log.d("MainNavigation", "Rendering NavHost for Authenticated state, starting at Home")
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) { HomeScreen(navController, fusedLocationClient) }
                composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
                composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController, authViewModel) }
                composable(Screen.Initial.route) { GearTickLoginScreen(navController) }
                composable(Screen.SignIn.route) { AccessAccountScreen(navController) }
                composable(Screen.SignUp.route) { SignUpScreen(navController) }
                composable(Screen.EmailVerification.route) { EmailVerificationScreen(navController) }
            }
        }
        is AuthState.NeedsEmailVerification -> {
            Log.d("MainNavigation", "Rendering NavHost for NeedsEmailVerification state")
            NavHost(
                navController = navController,
                startDestination = Screen.EmailVerification.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.EmailVerification.route) { EmailVerificationScreen(navController) }
                composable(Screen.Initial.route) { GearTickLoginScreen(navController) }
                composable(Screen.SignIn.route) { AccessAccountScreen(navController) }
                composable(Screen.SignUp.route) { SignUpScreen(navController) }
                composable(Screen.Home.route) { HomeScreen(navController, fusedLocationClient) }
            }
        }
        is AuthState.NeedsAppVerification -> {
            Log.d("MainNavigation", "Rendering NavHost for NeedsAppVerification state (e.g., Google Verification)")
            NavHost(
                navController = navController,
                startDestination = Screen.GoogleVerification.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.GoogleVerification.route) { GoogleVerificationScreen(navController) }
                composable(Screen.Initial.route) { GearTickLoginScreen(navController) }
                composable(Screen.SignIn.route) { AccessAccountScreen(navController) }
                composable(Screen.SignUp.route) { SignUpScreen(navController) }
            }
        }
        else -> {
            Log.d("MainNavigation", "Rendering NavHost for Unauthenticated state (State: ${authState::class.simpleName})")
            NavHost(
                navController = navController,
                startDestination = Screen.Initial.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Initial.route) { GearTickLoginScreen(navController) }
                composable(Screen.SignIn.route) { AccessAccountScreen(navController) }
                composable(Screen.SignUp.route) { SignUpScreen(navController) }
                composable(Screen.GoogleVerification.route) { GoogleVerificationScreen(navController) }
                composable(Screen.EmailVerification.route) { EmailVerificationScreen(navController) }
                composable(Screen.Home.route) { HomeScreen(navController, fusedLocationClient) }
            }
        }
    }
}

