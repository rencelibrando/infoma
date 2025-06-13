package com.example.bikerental.screens.verification

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val isEmailVerified by viewModel.emailVerified.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    
    var showSuccessBanner by remember { mutableStateOf(false) }
    var errorState by remember { mutableStateOf<String?>(null) }
    
    var canResendEmail by remember { mutableStateOf(true) }
    var countdown by remember { mutableIntStateOf(0) }

    LaunchedEffect(isEmailVerified) {
        if (isEmailVerified == true) {
            Log.d("EmailVerificationScreen", "Email verification successful, showing banner and navigating.")
            showSuccessBanner = true
            delay(2000)
            NavigationUtils.navigateToHome(navController)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("EmailVerificationScreen", "ON_RESUME: Triggering verification check.")
                coroutineScope.launch {
                    viewModel.checkEmailVerificationStatus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.VerificationEmailSent -> {
                Toast.makeText(context, "Verification email sent!", Toast.LENGTH_LONG).show()
                errorState = null
            }
            is AuthState.Error -> {
                val message = state.message
                Log.e("EmailVerificationScreen", "Auth error: $message")
                errorState = when {
                    message.contains("too many", ignoreCase = true) -> "Too many requests. Please wait a moment."
                    message.contains("network", ignoreCase = true) -> "Network error. Please check your connection."
                    else -> "An error occurred. Please try again."
                }
            }
            else -> {
                errorState = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Verification") },
                navigationIcon = {
                    IconButton(onClick = { NavigationUtils.navigateToLogin(navController) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Login")
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
            AnimatedVisibility(
                visible = showSuccessBanner,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
                        Text("Email Verified! Redirecting...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            AnimatedVisibility(
                visible = errorState != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).zIndex(9f)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Text(errorState ?: "An error occurred", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                    contentDescription = "Email Icon",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Verify Your Email",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "We've sent a verification email to:\n${user?.email ?: "your registered address"}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Please check your inbox (and spam folder) and click the link to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                
                ResponsiveButton(
                    text = "I've Verified My Email",
                    onClick = {
                        coroutineScope.launch {
                            Log.d("EmailVerificationScreen", "Checking verification status...")
                            val isVerified = viewModel.checkEmailVerificationStatus()
                            if (!isVerified) {
                                Toast.makeText(context, "Email is not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = (authState !is AuthState.Loading),
                    isLoading = (authState is AuthState.Loading),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        if (canResendEmail) {
                            Log.d("EmailVerificationScreen", "Resending verification email.")
                            viewModel.resendEmailVerification()
                            canResendEmail = false
                            countdown = 60

                    coroutineScope.launch {
                                while (countdown > 0) {
                                    delay(1000)
                                    countdown--
                                }
                                canResendEmail = true
                            }
                                } else {
                            Toast.makeText(context, "Please wait before sending another email.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = canResendEmail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(if (canResendEmail) "Resend Verification Email" else "Resend available in $countdown s")
                }
            }
        }
    }
} 