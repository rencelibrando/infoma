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
import androidx.compose.material3.Surface
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.GoogleAuthProvider

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
                            isLoggedIn = isLoggedIn.value,
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
    isLoggedIn: Boolean,
    onLoginScreenChange: (Boolean) -> Unit,
    fusedLocationClient: FusedLocationProviderClient
) {
    val navController = rememberNavController()
    val authViewModel = viewModel<AuthViewModel>()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Update the parent whenever local state changes
    LaunchedEffect(isLoggedIn) {
        onLoginScreenChange(!isLoggedIn)
    }
    
    // Get current Firebase user - this is the source of truth
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    
    // Add a state to ensure Firebase has fully initialized
    var firebaseInitialized by remember { mutableStateOf(false) }
    
    // Ensure Firebase Auth is fully initialized before checking user state
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500) // Small delay to ensure auth is fully initialized
        firebaseInitialized = true
        Log.d("MainNavigation", "Firebase Auth initialization completed")
    }
    
    // Always prioritize Firebase Auth status over the isLoggedIn parameter
    val actuallyLoggedIn = firebaseUser != null
    
    // Determine start destination based on login state
    val startDestination = if (actuallyLoggedIn && firebaseInitialized) {
        Log.d("MainNavigation", "User authenticated: ${firebaseUser?.uid}, starting at Home")
        Screen.Home.route
    } else {
        Log.d("MainNavigation", "No authenticated user, starting at Initial")
        Screen.Initial.route
    }
    
    Log.d("MainNavigation", "Start destination: $startDestination, isLoggedIn: $actuallyLoggedIn")
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Initial.route) {
            // Update login screen state locally
            onLoginScreenChange(true)
            Log.d("Navigation", "Rendering initial login screen")
            GearTickLoginScreen(navController)
        }
        
        composable(Screen.SignIn.route) {
            // Update login screen state locally
            onLoginScreenChange(true)
            Log.d("Navigation", "Rendering sign in screen")
            
            // Check if already logged in (could happen if user logs in and presses back)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                LaunchedEffect(currentUser) {
                    Log.d("Navigation", "User provider check: Google provider: true, Firestore provider: google, isGoogleUser: true")
                    Log.d("Navigation", "Already logged in, redirecting to home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                AccessAccountScreen(navController)
            }
        }
        
        composable(Screen.SignUp.route) {
            // Update login screen state locally
            onLoginScreenChange(true)
            Log.d("Navigation", "Rendering sign up screen")
            
            // A flag to disable auth checks when intentionally navigating to sign-up
            val isIntentionalSignUp = remember { mutableStateOf(true) }
            
            // Check if already logged in - but only if not intentionally navigating to sign-up
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && !isIntentionalSignUp.value) {
                LaunchedEffect(currentUser) {
                    Log.d("Navigation", "Already logged in, redirecting to home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                // Reset the flag after initial render
                LaunchedEffect(Unit) {
                    // Delay to ensure the screen renders first
                    kotlinx.coroutines.delay(300)
                    isIntentionalSignUp.value = false
                }
                
                // Show sign-up screen regardless of auth state
                SignUpScreen(navController)
            }
        }
        
        // Add Google Verification screen
        composable(Screen.GoogleVerification.route) {
            // Update login screen state locally
            onLoginScreenChange(true)
            Log.d("Navigation", "Rendering Google verification screen")
            
            GoogleVerificationScreen(navController)
        }
        
        // Add Email Verification screen
        composable(Screen.EmailVerification.route) {
            // Update login screen state locally
            onLoginScreenChange(true)
            Log.d("Navigation", "Rendering Email Verification screen")
            
            EmailVerificationScreen(navController)
        }
        
        composable(Screen.Home.route) {
            // Update login screen state locally
            onLoginScreenChange(false)
            
            // Check authentication - redirect to login if not authenticated
            if (firebaseUser == null) {
                // User not logged in, redirect to login
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "User not logged in, redirecting to sign in")
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) // Empty placeholder while redirecting
            } else {
                // Handle verification requirements if needed
                val emailVerified by authViewModel.emailVerified.collectAsState()
                val authState by authViewModel.authState.collectAsState()
                val bypassVerification = authViewModel.bypassVerification.collectAsState().value
                
                // Check verification status
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "Checking email verification status in Home screen")
                    authViewModel.checkEmailVerification()
                }
                
                // Check for Google Sign-In more reliably
                val isGoogleUser = remember {
                    mutableStateOf(false)
                }
                
                LaunchedEffect(firebaseUser) {
                    // Check if user is signed in with Google
                    val providers = firebaseUser.providerData
                    val googleProvider = providers.any { 
                        it.providerId == "google.com" || it.providerId == GoogleAuthProvider.PROVIDER_ID 
                    }
                    
                    // Also check Firestore for provider info
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(firebaseUser.uid)
                            .get()
                            .await()
                            
                        val providerFromFirestore = userDoc.getString("provider")
                        val isGoogleFromFirestore = providerFromFirestore == "google"
                        
                        // Update the state with the most reliable information
                        isGoogleUser.value = googleProvider || isGoogleFromFirestore
                        
                        // Force the user to be treated as verified if they're a Google user
                        if (googleProvider || isGoogleFromFirestore) {
                            authViewModel.updateEmailVerificationStatus(true)
                        }
                        
                        Log.d("Navigation", "User provider check: Google provider: $googleProvider, " +
                              "Firestore provider: $providerFromFirestore, isGoogleUser: ${isGoogleUser.value}")
                    } catch (e: Exception) {
                        // If Firestore check fails, rely on Firebase Auth providers
                        isGoogleUser.value = googleProvider
                        Log.d("Navigation", "Firestore provider check failed, using Auth provider only: ${isGoogleUser.value}")
                        
                        // Still mark Google users as verified
                        if (googleProvider) {
                            authViewModel.updateEmailVerificationStatus(true)
                        }
                    }
                }
                
                // Handle verification state and redirect if needed
                LaunchedEffect(emailVerified, authState, isGoogleUser.value) {
                    Log.d("Navigation", "Verification state: emailVerified=${emailVerified}, " +
                          "authState=${authState::class.simpleName}, isGoogleUser=${isGoogleUser.value}, " +
                          "bypassVerification=$bypassVerification")
                    
                    // Skip redirection if:
                    // 1. Is a Google user (Google users are auto-verified)
                    // 2. Email is verified
                    // 3. Verification is explicitly bypassed
                    val shouldSkipVerification = isGoogleUser.value || 
                        emailVerified == true || 
                        bypassVerification
                        
                    if (!shouldSkipVerification && 
                        (emailVerified == false || authState is AuthState.NeedsEmailVerification)) {
                        Log.d("Navigation", "Email not verified, redirecting to verification screen")
                        navController.navigate(Screen.EmailVerification.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    } else if (isGoogleUser.value && (emailVerified == false || authState is AuthState.NeedsEmailVerification)) {
                        // For Google users, just update their verification status without redirecting
                        Log.d("Navigation", "Google user detected, bypassing email verification")
                        authViewModel.checkEmailVerification()
                    } else if (bypassVerification) {
                        Log.d("Navigation", "Verification bypassed by user choice")
                    }
                }
                
                // User is authenticated, show the home screen
                HomeScreen(navController, fusedLocationClient)
            }
        }
        
        // Profile-related screens
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController)
        }
        
        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(navController, authViewModel)
        }
        
        // Additional routes for other screens...
        // (Profile, EditProfile, etc. - include all your existing routes)
    }
}

@Composable
private fun AuthStateListener(
    navController: NavHostController,
    isLoginScreenActive: Boolean,
    onLoginStateChange: (Boolean) -> Unit
) {
    val authViewModel = viewModel<AuthViewModel>()
    val phoneAuthViewModel = viewModel<PhoneAuthViewModel>()
    val currentRoute = navController.currentDestination?.route
    
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            Log.d("MainActivityListener", "Auth state changed. Current user: ${currentUser?.uid}, isLoginScreenActive: $isLoginScreenActive")
            
            // Skip auth state changes when on login screen to prevent redirect issues
            if (!isLoginScreenActive) {
                Log.d("MainActivityListener", "Processing auth state change (not on login screen). Current route: ${navController.currentDestination?.route}")
                
                // Update login state - prevent excessive updates by checking if state actually changed
                val newLoggedInState = currentUser != null
                onLoginStateChange(newLoggedInState)
                
                if (currentUser != null) {
                    phoneAuthViewModel.updateAuthState(currentUser)
                }
            }
        }
        
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(authStateListener)
        onDispose {
            Log.d("MainActivityListener", "Removing auth state listener")
            auth.removeAuthStateListener(authStateListener)
        }
    }
}

// Create a composable to observe auth state and handle navigation
@Composable
fun AuthStateObserver(navController: NavHostController, authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.authState.collectAsState()
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Initial -> {
                // When auth state is Initial (user signed out), navigate to sign in
                Log.d("AuthStateObserver", "AuthState is Initial, navigating to sign in")
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
            else -> { /* Other states handled elsewhere */ }
        }
    }
}

