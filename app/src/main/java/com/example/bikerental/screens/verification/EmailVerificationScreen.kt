package com.example.bikerental.screens.verification

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.bikerental.BikeRentalApplication
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.navigation.Screen
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Math.pow
import kotlin.math.pow
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
    var resendAttempts by remember { mutableIntStateOf(0) }
    var lastResendTime by remember { mutableLongStateOf(0L) }
    var showExpiredLinkDialog by remember { mutableStateOf(false) }
    
    // State for email dialog
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    
    // Track if the user verified their email during this session
    var verifiedDuringSession by remember { mutableStateOf(false) }
    
    // Rate limiting constants
    val MIN_CHECK_INTERVAL = 10000L // 10 seconds minimum between checks
    val MAX_CHECK_INTERVAL = 60000L // 60 seconds maximum
    val RESEND_COOLDOWN_BASE = 60000L // 1 minute base cooldown
    val MAX_API_ERRORS = 3
    val MAX_RESEND_ATTEMPTS = 5
    
    // Calculate dynamic resend cooldown with exponential backoff
    val getResendCooldown = remember {
        { attempts: Int ->
            RESEND_COOLDOWN_BASE * pow(2.0, attempts.coerceAtMost(4).toDouble()).toLong()
        }
    }
    
    // Check if we should show expired link dialog based on URL intent or error messages
    LaunchedEffect(Unit) {
        // Check if the app was opened with a verification link that expired
        // This would typically come from the intent or navigation arguments
        val hasExpiredLink = navController.currentBackStackEntry?.arguments?.getBoolean("expired_link") ?: false
        if (hasExpiredLink) {
            showExpiredLinkDialog = true
        }
    }
    
    // Expired link dialog
    if (showExpiredLinkDialog) {
        ExpiredLinkDialog(
            onDismiss = { showExpiredLinkDialog = false },
            onSendNewLink = {
                showExpiredLinkDialog = false
                // Automatically send a new verification email
                viewModel.resendEmailVerification()
                Toast.makeText(context, "Sending you a fresh verification link...", Toast.LENGTH_LONG).show()
            }
        )
    }
    
    // Email dialog
    if (showEmailDialog) {
        EmailInputDialog(
            emailInput = emailInput,
            onEmailChange = { emailInput = it },
            onDismiss = { showEmailDialog = false },
            onConfirm = {
                showEmailDialog = false
                if (emailInput.isNotEmpty()) {
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
    
    // Lifecycle-aware verification check with rate limiting
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCheckTime > MIN_CHECK_INTERVAL && 
                        apiErrorCount < MAX_API_ERRORS && 
                        !verifiedDuringSession) {
                        Log.d("EmailVerificationScreen", "ON_RESUME: Triggering verification check")
                        isCheckingVerification = true
                        lastCheckTime = currentTime
                    } else {
                        Log.d("EmailVerificationScreen", "ON_RESUME: Skipping check due to rate limiting")
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
    
    // Controlled verification check with rate limiting
    LaunchedEffect(isCheckingVerification) {
        if (isCheckingVerification && !verifiedDuringSession) {
            try {
                delay(500) // Small delay for UI feedback
                
                Log.d("EmailVerificationScreen", "Performing controlled verification check")
                viewModel.checkEmailVerification()
                
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("EmailVerificationScreen", "Error checking verification: ${e.message}")
                    errorState = when {
                        e.message?.contains("too many", ignoreCase = true) == true -> 
                            "Rate limited. Please wait before checking again."
                        e.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your connection."
                        else -> "Verification check failed. Please try again later."
                    }
                    apiErrorCount++
                }
            } finally {
                isCheckingVerification = false
                lastCheckTime = System.currentTimeMillis()
            }
        }
    }
    
    // Handle countdown for resend button with dynamic cooldown
    LaunchedEffect(canResendEmail, resendAttempts) {
        if (!canResendEmail) {
            val cooldownTime = getResendCooldown(resendAttempts)
            countdown = (cooldownTime / 1000).toInt()
            
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            canResendEmail = true
        }
    }
    
    // Handle email verification state changes
    LaunchedEffect(emailVerified) {
        if (emailVerified == true && !verifiedDuringSession) {
            verifiedDuringSession = true
            errorState = null
            
            Log.d("EmailVerificationScreen", "Email verification successful, navigating to home")
            try {
                delay(1000) // Give user time to see the success state
                NavigationUtils.navigateToHome(navController)
            } catch (e: Exception) {
                Log.e("EmailVerificationScreen", "Error navigating to home: ${e.message}", e)
                Toast.makeText(context, "Navigation error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Single consolidated background verification check with exponential backoff
    LaunchedEffect(Unit) {
        if (verifiedDuringSession) return@LaunchedEffect
        
        var checkInterval = MIN_CHECK_INTERVAL
        var consecutiveErrors = 0
        
        Log.d("EmailVerificationScreen", "Starting background verification loop")
        
        while (!verifiedDuringSession && apiErrorCount < MAX_API_ERRORS) {
            delay(checkInterval)
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCheckTime < MIN_CHECK_INTERVAL) {
                Log.d("EmailVerificationScreen", "Skipping check due to rate limiting")
                continue
            }
            
            try {
                withContext(Dispatchers.IO) {
                    FirebaseAuth.getInstance().currentUser?.reload()?.await()
                }
                val currentUser = FirebaseAuth.getInstance().currentUser
                
                if (currentUser?.isEmailVerified == true) {
                    Log.d("EmailVerificationScreen", "Background check: Email verified detected")
                    verifiedDuringSession = true
                    withContext(Dispatchers.Main) {
                        viewModel.checkEmailVerification()
                    }
                    break
                }
                
                // Reset consecutive errors on success
                consecutiveErrors = 0
                // Gradually increase interval but reset on success
                checkInterval = min(checkInterval * 1.1, MAX_CHECK_INTERVAL.toDouble()).toLong()
                
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    consecutiveErrors++
                    Log.w("EmailVerificationScreen", "Background check error: ${e.message}")
                    
                    if (e.message?.contains("too many", ignoreCase = true) == true) {
                        Log.w("EmailVerificationScreen", "Rate limited, increasing interval significantly")
                        apiErrorCount++
                        checkInterval = MAX_CHECK_INTERVAL
                    } else {
                        // Exponential backoff for other errors
                        checkInterval = min(
                            checkInterval * pow(1.5, consecutiveErrors.toDouble()),
                            MAX_CHECK_INTERVAL.toDouble()
                        ).toLong()
                    }
                }
            }
            
            lastCheckTime = currentTime
        }
        
        if (apiErrorCount >= MAX_API_ERRORS) {
            Log.w("EmailVerificationScreen", "Background verification stopped due to too many errors")
        }
    }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (emailVerified == true && !verifiedDuringSession) {
                    Log.d("EmailVerificationScreen", "Auth state: Email verified, navigating to home")
                    verifiedDuringSession = true
                    NavigationUtils.navigateToHome(navController)
                }
            }
            is AuthState.VerificationEmailSent -> {
                Toast.makeText(context, "Verification email sent!", Toast.LENGTH_LONG).show()
                canResendEmail = false
                resendAttempts++
                lastResendTime = System.currentTimeMillis()
                errorState = null
            }
            is AuthState.Error -> {
                val message = (authState as AuthState.Error).message
                Log.e("EmailVerificationScreen", "Auth error: $message")
                
                errorState = when {
                    message.contains("too many", ignoreCase = true) -> {
                        apiErrorCount++
                        "Too many requests. Please wait ${countdown}s before trying again."
                    }
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
                Log.d("EmailVerificationScreen", "AuthState: $authState")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Verification") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (verifiedDuringSession) {
                            NavigationUtils.navigateToHome(navController)
                        } else if (firebaseUser != null) {
                            NavigationUtils.navigateToHome(navController)
                        } else {
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
            // Show success banner when verified
            AnimatedVisibility(
                visible = verifiedDuringSession,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
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
            
            // Show error banner with rate limiting info
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
                        Column {
                            Text(
                                text = errorState ?: "An error occurred",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (apiErrorCount >= MAX_API_ERRORS) {
                                Text(
                                    text = "Automatic checking disabled due to rate limiting",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
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
                if (!verifiedDuringSession) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Verification",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                Text(
                    text = "Verify Your Email",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                    text = "Please check your inbox and click the verification link to enable all app features.\n\nIf your verification link has expired, simply request a new one using the button below.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                EmailVerificationControls(
                    navController = navController,
                    isLoading = isCheckingVerification,
                    canResendEmail = canResendEmail && resendAttempts < MAX_RESEND_ATTEMPTS,
                    countdown = countdown,
                    onCheckVerification = { 
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastCheckTime > MIN_CHECK_INTERVAL && apiErrorCount < MAX_API_ERRORS) {
                            isCheckingVerification = true
                            lastCheckTime = currentTime
                        } else {
                            Toast.makeText(
                                context, 
                                "Please wait before checking again", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onResendEmail = {
                        val currentTime = System.currentTimeMillis()
                        if (resendAttempts >= MAX_RESEND_ATTEMPTS) {
                            Toast.makeText(
                                context,
                                "Maximum resend attempts reached. Please contact support.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@EmailVerificationControls
                        }
                        
                        if (currentTime - lastResendTime < getResendCooldown(resendAttempts)) {
                            Toast.makeText(
                                context,
                                "Please wait ${countdown} seconds before resending",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@EmailVerificationControls
                        }
                        
                        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
                        if (currentEmail.isNullOrEmpty() && user?.email.isNullOrEmpty()) {
                            Log.e("EmailVerificationScreen", "Cannot resend: No email address found")
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
                    viewModel = viewModel,
                    apiErrorCount = apiErrorCount,
                    maxErrors = MAX_API_ERRORS
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = { 
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
    countdown: Int,
    onCheckVerification: () -> Unit,
    onResendEmail: () -> Unit,
    emailVerified: Boolean?,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    apiErrorCount: Int = 0,
    maxErrors: Int = 3
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
        if (emailVerified == true) {
            VerifiedSuccessCard(navController)
        } else {
            ResponsiveButton(
                text = if (isLoading) "Checking..." else "I've Verified My Email",
                onClick = {
                    onCheckVerification()
                    coroutineScope.launch {
                        try {
                            Log.d("EmailVerificationScreen", "Manual verification check")
                            withContext(Dispatchers.IO) {
                                FirebaseAuth.getInstance().currentUser?.reload()?.await()
                            }
                            val user = FirebaseAuth.getInstance().currentUser
                            
                            if (user?.isEmailVerified == true) {
                                Log.d("EmailVerificationScreen", "Manual check successful")
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
                            Log.e("EmailVerificationScreen", "Manual check error: ${e.message}")
                            withContext(Dispatchers.Main) {
                                val message = if (e.message?.contains("too many", ignoreCase = true) == true) {
                                    "Too many requests. Please wait before checking again."
                                } else {
                                    "Error checking verification"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isLoading && apiErrorCount < maxErrors,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onResendEmail,
                enabled = canResendEmail && !isLoading && apiErrorCount < maxErrors,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    if (canResendEmail) "Get Fresh Verification Link" 
                    else "Get Fresh Link (${countdown}s)"
                )
            }
            
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email
            
            if (!email.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fresh verification link will be sent to: $email",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Add helpful text about expired links
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡 Tip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If your verification link says it's expired or already used, just click 'Get Fresh Verification Link' above to receive a new one.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (apiErrorCount >= maxErrors) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Automatic verification disabled due to rate limiting. Please check your email manually.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
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
private fun ExpiredLinkDialog(
    onDismiss: () -> Unit,
    onSendNewLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Verification Link Expired")
            }
        },
        text = { 
            Column {
                Text("Your email verification link has expired or has already been used.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Would you like us to send you a fresh verification link?")
            }
        },
        confirmButton = {
            Button(
                onClick = onSendNewLink,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Send New Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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