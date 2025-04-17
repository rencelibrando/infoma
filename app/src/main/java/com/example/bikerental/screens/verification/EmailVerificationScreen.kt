package com.example.bikerental.screens.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.bikerental.BikeRentalApplication
import com.example.bikerental.navigation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val application = context.applicationContext as? BikeRentalApplication
    val user = viewModel.currentUser.collectAsState().value
    val emailVerified by viewModel.emailVerified.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    
    var isCheckingVerification by remember { mutableStateOf(false) }
    var canResendEmail by remember { mutableStateOf(true) }
    var countdown by remember { mutableIntStateOf(0) }
    var errorState by remember { mutableStateOf<String?>(null) }
    var apiErrorCount by remember { mutableIntStateOf(0) }
    var lastCheckTime by remember { mutableLongStateOf(0L) }
    
    // State for email dialog
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    
    // Track if the user verified their email during this session (properly scoped to this composable)
    var verifiedDuringSession by remember { mutableStateOf(false) }
    
    // Email dialog
    if (showEmailDialog) {
        EmailInputDialog(
            emailInput = emailInput,
            onEmailChange = { emailInput = it },
            onDismiss = { showEmailDialog = false },
            onConfirm = {
                showEmailDialog = false
                if (emailInput.isNotEmpty()) {
                    // Update user's email and then send verification
                    coroutineScope.launch {
                        try {
                            FirebaseAuth.getInstance().currentUser?.updateEmail(emailInput)?.await()
                            viewModel.resendEmailVerification()
                        } catch (e: Exception) {
                            Log.e("EmailVerificationScreen", "Failed to update email: ${e.message}")
                            Toast.makeText(context, 
                                "Failed to update email: ${e.message}", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }
    
    // Create a lifecycle-aware verification check
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Check verification status when screen comes into foreground
                    if (System.currentTimeMillis() - lastCheckTime > 5000) { // Avoid too frequent checks
                        isCheckingVerification = true
                        lastCheckTime = System.currentTimeMillis()
                    }
                }
                else -> { /* handle other events if needed */ }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Auto-check verification when the checking flag is set
    LaunchedEffect(isCheckingVerification) {
        if (isCheckingVerification) {
            try {
                delay(500) // Small delay for UI feedback
                
                // REMOVED: Check for application and call to application.checkAndUpdateEmailVerification()
                // ALWAYS use the ViewModel's check now
                Log.d("EmailVerificationScreen", "LaunchedEffect(isCheckingVerification): Calling viewModel.checkEmailVerification()")
                viewModel.checkEmailVerification()
                // The result will be observed via the emailVerified StateFlow

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("EmailVerificationScreen", "Error checking verification: ${e.message}")
                    errorState = "Verification check failed"
                    apiErrorCount++
                }
            } finally {
                isCheckingVerification = false
                lastCheckTime = System.currentTimeMillis()
            }
        }
    }
    
    // Handle countdown for resend button
    LaunchedEffect(canResendEmail) {
        if (!canResendEmail) {
            countdown = 60 // 60 seconds cooldown
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            canResendEmail = true
        }
    }
    
    // Handle email verification state from the ViewModel - improved navigation logic
    LaunchedEffect(emailVerified) {
        if (emailVerified == true) {
            verifiedDuringSession = true
            errorState = null
            
            // Resume navigation to home when verification is detected
            Log.d("EmailVerificationScreen", 
                "Email verification successful, navigating to home")
            try {
                // Use NavigationUtils for consistent navigation
                NavigationUtils.navigateToHome(navController)
            } catch (e: Exception) {
                Log.e("EmailVerificationScreen", 
                    "Error navigating to home after verification: ${e.message}", e)
                // Fallback navigation
                try {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                } catch (e2: Exception) {
                     Log.e("EmailVerificationScreen", "All navigation attempts failed: ${e2.message}", e2)
                     Toast.makeText(context, "Navigation error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            Log.d("EmailVerificationScreen", "Navigation initiated after email verification")
        }
    }
    
    // Add a direct check for Firebase verification status changes
    LaunchedEffect(firebaseUser) {
        if (firebaseUser?.isEmailVerified == true) {
            Log.d("EmailVerificationScreen", "Direct Firebase check: Email is verified, navigating to home")
            verifiedDuringSession = true
            // Update app state
            viewModel.checkEmailVerification()
            // Small delay to let state updates propagate
            delay(500)
            // Ensure we're on the main thread
            withContext(Dispatchers.Main) {
                try {
                    NavigationUtils.navigateToHome(navController)
                } catch (e: Exception) {
                    Log.e("EmailVerificationScreen", "Error in direct Firebase navigation: ${e.message}")
                    // Fallback navigation
                    try {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } catch (e2: Exception) {
                        Log.e("EmailVerificationScreen", "All direct navigation attempts failed: ${e2.message}")
                    }
                }
            }
        }
    }
    
    // Add a periodic check for verification status
    LaunchedEffect(Unit) {
        var checkInterval = 5000L
        verificationLoop@ while (!verifiedDuringSession) {
            delay(checkInterval)
            try {
                withContext(Dispatchers.IO) {
                    FirebaseAuth.getInstance().currentUser?.reload()?.await()
                }
                val currentUser = FirebaseAuth.getInstance().currentUser
                
                if (currentUser?.isEmailVerified == true) {
                    Log.d("EmailVerificationScreen", "Email verified detected")
                    verifiedDuringSession = true
                    viewModel.checkEmailVerification()
                    delay(500)
                    withContext(Dispatchers.Main) {
                        NavigationUtils.navigateToHome(navController)
                    }
                    break@verificationLoop
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("EmailVerificationScreen", "Error: ${e.message}")
                }
            }
            
            if (checkInterval < 30000L) {
                checkInterval = (checkInterval * 1.2).toLong().coerceAtMost(30000L)
            }
        }
    }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (emailVerified == true) {
                    // Email verification successful, navigate to home
                    Log.d("EmailVerificationScreen", "Email verified, navigating to home")
                    NavigationUtils.navigateToHome(navController)
                }
            }
            is AuthState.VerificationEmailSent -> {
                Toast.makeText(context, "Verification email sent!", Toast.LENGTH_LONG).show()
                canResendEmail = false // Start cooldown
                errorState = null // Clear error state on success
            }
            is AuthState.Error -> {
                val message = (authState as AuthState.Error).message
                Log.e("EmailVerificationScreen", "Auth error: $message")
                
                // More specific error messages based on the error type
                errorState = when {
                    message.contains("network", ignoreCase = true) -> 
                        "Network error. Please check your internet connection."
                    message.contains("verification", ignoreCase = true) -> 
                        "Verification failed. Please try again."
                    message.contains("App Check", ignoreCase = true) -> 
                        "Security verification pending. This is normal in development."
                    else -> "Authentication issue. Please try again later."
                }
                
                // Only count non-App Check errors
                if (!message.contains("App Check", ignoreCase = true)) {
                    apiErrorCount++
                }
            }
            else -> {
                // Handle other states or just log them
                Log.d("EmailVerificationScreen", "AuthState: $authState")
            }
        }
    }
    
    // One-time initial check on screen load
    LaunchedEffect(Unit) {
        Log.d("EmailVerificationScreen", "Initial check triggered")
        isCheckingVerification = true
        coroutineScope.launch {
            try {
                // Replace call to application function with ViewModel function
                viewModel.checkEmailVerification()
            } finally {
                isCheckingVerification = false
            }
        }
    }
    
    // Set up lightweight background polling with increasing intervals and failsafe
    LaunchedEffect(Unit) {
        try {
            var checkInterval = 5000L // Start with 5 seconds
            
            while (apiErrorCount < 3 && !verifiedDuringSession) {
                delay(checkInterval)
                
                // Only perform check if not already checking and enough time has passed
                if (!isCheckingVerification && System.currentTimeMillis() - lastCheckTime > checkInterval) {
                    try {
                        isCheckingVerification = true
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            Log.e("EmailVerificationScreen", "Automatic check failed: ${e.message}")
                            apiErrorCount++
                            // Increase interval on failure with exponential backoff
                            checkInterval = (checkInterval * 1.5).toLong().coerceAtMost(30000L) // Max 30 seconds
                        }
                    }
                }
                
                // Progressively increase interval to reduce API load
                if (checkInterval < 30000L) { // Cap at 30 seconds
                    checkInterval = (checkInterval * 1.2).toLong().coerceAtMost(30000L)
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Log.e("EmailVerificationScreen", "Verification check loop stopped: ${e.message}")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Verification") },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Check if user is authenticated before navigating
                        if (verifiedDuringSession) {
                            // If email verified, go to home
                            NavigationUtils.navigateToHome(navController)
                        } else if (firebaseUser != null) {
                            // If authenticated but not verified, go to home
                            // The app will redirect to verification if needed
                            NavigationUtils.navigateToHome(navController)
                        } else {
                            // If not authenticated, go to login
                            NavigationUtils.navigateToLogin(navController)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showEmailDialog = true }) {
                        Text(
                            "Change Email",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show success banner when verified - now moved to be absolutely positioned at the top
            AnimatedVisibility(
                visible = verifiedDuringSession,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f) // Ensure it stays on top of other content
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Email verified successfully! Redirecting to home...",
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Show error banner if we have persistent API errors
            AnimatedVisibility(
                visible = errorState != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (verifiedDuringSession) 90.dp else 0.dp)
                    .zIndex(9f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorState ?: "An error occurred",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .padding(top = if (verifiedDuringSession) 80.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Email icon - conditionally show only if not verified
                if (!verifiedDuringSession) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Verification",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Title
                Text(
                    text = "Verify Your Email",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                Text(
                    text = when {
                        !user?.email.isNullOrEmpty() -> "We've sent a verification email to:\n${user?.email}"
                        !FirebaseAuth.getInstance().currentUser?.email.isNullOrEmpty() -> 
                            "We've sent a verification email to:\n${FirebaseAuth.getInstance().currentUser?.email}"
                        else -> "We've sent a verification email to your registered email address"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Please check your inbox and click the verification link to enable all app features.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                EmailVerificationControls(
                    navController = navController,
                    isLoading = isCheckingVerification,
                    canResendEmail = canResendEmail,
                    onCheckVerification = { isCheckingVerification = true },
                    onResendEmail = {
                        // Check if Firebase Auth has an email before attempting to resend
                        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
                        if (currentEmail.isNullOrEmpty() && user?.email.isNullOrEmpty()) {
                            Log.e("EmailVerificationScreen", "Cannot resend: No email address found")
                            
                            // Show dialog to update email
                            showEmailDialog = true
                            
                            Toast.makeText(
                                context,
                                "No email address found. Please update your profile with an email address.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            viewModel.resendEmailVerification()
                        }
                    },
                    emailVerified = emailVerified,
                    viewModel = viewModel
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Back to app button
                TextButton(
                    onClick = { 
                        // Check authentication state first
                        if (firebaseUser != null) {
                            NavigationUtils.navigateToHome(navController)
                        } else {
                            NavigationUtils.navigateToLogin(navController)
                        }
                    },
                    enabled = !verifiedDuringSession
                ) {
                    Text("Continue Using App")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Warning about restricted features
                Text(
                    text = "Note: Some features may be restricted until your email is verified",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmailVerificationControls(
    navController: NavController,
    isLoading: Boolean,
    canResendEmail: Boolean,
    onCheckVerification: () -> Unit,
    onResendEmail: () -> Unit,
    emailVerified: Boolean?,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Check if email is verified
        if (emailVerified == true) {
            VerifiedSuccessCard(navController)
        } else {
            // Action buttons
            ResponsiveButton(
                text = "I've Verified My Email",
                onClick = {
                    onCheckVerification()
                    coroutineScope.launch {
                        try {
                            Log.d("EmailVerificationScreen", "Performing verification check")
                            withContext(Dispatchers.IO) {
                                FirebaseAuth.getInstance().currentUser?.reload()?.await()
                            }
                            val user = FirebaseAuth.getInstance().currentUser
                            
                            if (user?.isEmailVerified == true) {
                                Log.d("EmailVerificationScreen", "Verification check successful")
                                viewModel.checkEmailVerification()
                                delay(500)
                                withContext(Dispatchers.Main) {
                                    NavigationUtils.navigateToHome(navController)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Email not verified yet. Please check your inbox.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("EmailVerificationScreen", "Error: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error checking verification", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onResendEmail,
                enabled = canResendEmail && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Resend Verification Email")
            }
            
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email
            
            if (!email.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verification email will be sent to: $email",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerifiedSuccessCard(navController: NavController) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Email Verified Successfully!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "You can now access all features of the app",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = {
                    Log.d("EmailVerificationScreen", "Navigating to home")
                    if (firebaseUser != null) {
                        NavigationUtils.navigateToHome(navController)
                    } else {
                        NavigationUtils.navigateToLogin(navController)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Continue to App")
            }
        }
    }
}

@Composable
private fun EmailInputDialog(
    emailInput: String,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isValidEmail = remember(emailInput) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()
    }
    
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Email Required") },
        text = { 
            Column {
                Text("Please enter your email address to receive the verification link")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { 
                        onEmailChange(it)
                        showError = false
                    },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                text = "Please enter a valid email address",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidEmail) {
                        onConfirm()
                    } else {
                        showError = true
                    }
                },
                enabled = emailInput.isNotEmpty()
            ) {
                Text("Save & Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 