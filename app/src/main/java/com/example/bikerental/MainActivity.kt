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

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val auth = FirebaseAuth.getInstance()
    private val phoneAuthViewModel: PhoneAuthViewModel by viewModels()
    private lateinit var authViewModel: AuthViewModel
    
    // Move isLoginScreenActive to be a class property with a default value
    private var isLoginScreenActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AuthViewModel - do this after setting up auth state listener
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

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
                
                // Initial auth check
                LaunchedEffect(Unit) {
                    val currentUser = auth.currentUser
                    isLoggedIn = currentUser != null
                    isLoading = false
                }

                val authState by phoneAuthViewModel.uiState.collectAsState()
                
                val startDestination = if (isLoggedIn) {
                    Screen.Home.route
                } else {
                    Screen.Initial.route
                }
                Log.d("MainActivity", "Calculated start destination: $startDestination")
                
                if (isLoading) {
                    LoadingScreen()
                } else {
                    NavigationContent(
                        isLoggedIn = isLoggedIn,
                        showSplash = showSplash,
                        onSplashComplete = { showSplash = false },
                        fusedLocationClient = fusedLocationClient,
                        onLoginScreenChange = { isActive -> isLoginScreenActive = isActive },
                        onLoginStateChange = { loggedIn -> isLoggedIn = loggedIn }
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
    fusedLocationClient: FusedLocationProviderClient,
    onLoginScreenChange: (Boolean) -> Unit,
    onLoginStateChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val authViewModel = viewModel<AuthViewModel>()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Track login screen state locally
    var isLoginScreenActive by remember { mutableStateOf(false) }
    
    // Update the parent whenever local state changes
    LaunchedEffect(isLoginScreenActive) {
        onLoginScreenChange(isLoginScreenActive)
    }
    
    // Determine start destination based on login state
    val startDestination = if (isLoggedIn) {
        Screen.Home.route
    } else {
        Screen.Initial.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Initial.route) {
            // Update login screen state locally
            isLoginScreenActive = true
            Log.d("Navigation", "Rendering initial login screen")
            GearTickLoginScreen(navController)
        }
        
        composable(Screen.SignIn.route) {
            // Update login screen state locally
            isLoginScreenActive = true
            Log.d("Navigation", "Rendering sign in screen")
            
            if (isLoggedIn) {
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "Already logged in, redirecting to home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Initial.route) { inclusive = true }
                    }
                }
            }
            AccessAccountScreen(navController)
        }
        
        composable(Screen.SignUp.route) {
            // Update login screen state locally
            isLoginScreenActive = true
            Log.d("Navigation", "Rendering sign up screen")
            
            if (isLoggedIn) {
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "Already logged in, redirecting to home")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Initial.route) { inclusive = true }
                    }
                }
            }
            SignUpScreen(navController)
        }
        
        composable(Screen.Home.route) {
            // Update login screen state locally
            isLoginScreenActive = false
            Log.d("Navigation", "Rendering home screen")
            
            if (showSplash && isLoggedIn) {
                SplashScreen(onSplashComplete)
            } else {
                HomeScreen(navController, fusedLocationClient)
            }
        }
        
        composable(Screen.Profile.route) {
            // Update login screen state locally
            isLoginScreenActive = false
            
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "Not logged in, redirecting to initial")
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            }
            val viewModel = remember { AuthViewModel() }
            ProfileScreen(navController, viewModel)
        }
        
        composable(Screen.EditProfile.route) {
            // Update login screen state locally
            isLoginScreenActive = false
            
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "Not logged in, redirecting to initial")
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(Screen.EditProfile.route) { inclusive = true }
                    }
                }
            }
            EditProfileScreen(navController)
        }
        
        composable(Screen.ChangePassword.route) {
            // Update login screen state locally
            isLoginScreenActive = false
            
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    Log.d("Navigation", "Not logged in, redirecting to initial")
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(Screen.ChangePassword.route) { inclusive = true }
                    }
                }
            }
            // Use the existing AuthViewModel from the parent scope
            ChangePasswordScreen(navController, authViewModel)
        }
    }
    
    // Pass navController and the local login screen state to the auth state listener
    AuthStateListener(navController, isLoginScreenActive, onLoginStateChange)
}

@Composable
private fun AuthStateListener(
    navController: NavHostController,
    isLoginScreenActive: Boolean,
    onLoginStateChange: (Boolean) -> Unit
) {
    val authViewModel = viewModel<AuthViewModel>()
    val phoneAuthViewModel = viewModel<PhoneAuthViewModel>()
    
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            Log.d("MainActivityListener", "Auth state changed. Current user: ${currentUser?.uid}, isLoginScreenActive: $isLoginScreenActive")
            
            // Skip auth state changes when on login screen to prevent redirect issues
            if (!isLoginScreenActive) {
                Log.d("MainActivityListener", "Processing auth state change (not on login screen). Current route: ${navController.currentDestination?.route}")
                
                // Update login state
                val isLoggedIn = currentUser != null
                onLoginStateChange(isLoggedIn)
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
