package com.example.bikerental.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.components.ResponsiveTextField
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.Screen
import com.example.bikerental.viewmodels.AuthViewModel
import android.util.Log
import android.util.Log.e
import android.util.Patterns
import android.widget.Toast
import com.example.bikerental.navigation.NavigationUtils
import kotlinx.coroutines.delay
import com.google.firebase.FirebaseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AccessAccountScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Collect auth state
    val authState by viewModel.authState.collectAsState()

    // Add a state to track if we've already processed the auth state to prevent duplicate triggers
    var authStateProcessed by remember { mutableStateOf(false) }

    // Add a flag to prevent multiple navigations
    var hasNavigatedToHome by remember { mutableStateOf(false) }

    // Handle auth state changes - make this more robust to prevent unintended redirects
    LaunchedEffect(authState) {
        Log.d("AccessAccountScreen", "Auth state changed: $authState, processed: $authStateProcessed")
        
        // Skip initial auth state updates which could cause redirects when typing
        if (authState is AuthState.Initial) {
            Log.d("AccessAccountScreen", "Ignoring Initial auth state")
            return@LaunchedEffect
        }
        
        // Skip re-processing of the same auth state
        if (authStateProcessed && !(authState is AuthState.Error)) {
            Log.d("AccessAccountScreen", "Ignoring already processed auth state: $authState")
            return@LaunchedEffect
        }
        
        when (val currentState = authState) {
            is AuthState.Authenticated -> {
                Log.d("AccessAccountScreen", "Auth state is Authenticated. Navigation handled by MainActivity.")
                // Ensure flags are potentially reset if needed, although MainActivity now drives navigation
                authStateProcessed = true // Mark as processed to avoid loops if state reappears
                hasNavigatedToHome = true // Assume navigation will happen
            }
            is AuthState.Error -> {
                Log.e("AccessAccountScreen", "Auth Error: ${currentState.message}")
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                authStateProcessed = false // Allow reprocessing after error
            }
            is AuthState.NeedsAdditionalInfo -> {
                Log.d("AccessAccountScreen", "Auth state: NeedsAdditionalInfo")
                authStateProcessed = true
                NavigationUtils.navigateToSignUp(navController)
            }
            is AuthState.NeedsAppVerification -> {
                Log.d("AccessAccountScreen", "Auth state: NeedsAppVerification")
                authStateProcessed = true
                NavigationUtils.navigateToEmailVerification(navController)
            }
            is AuthState.NeedsEmailVerification -> {
                Log.d("AccessAccountScreen", "Auth state: NeedsEmailVerification")
                authStateProcessed = true // Mark as processed
            }
            is AuthState.PasswordResetSent -> {
                Log.d("AccessAccountScreen", "Auth state: PasswordResetSent")
                Toast.makeText(context, "Password reset email sent", Toast.LENGTH_LONG).show()
                showForgotPasswordDialog = false
                authStateProcessed = false // Reset to allow reprocessing
            }
            else -> {
                Log.d("AccessAccountScreen", "Unhandled auth state: $currentState")
                authStateProcessed = false // Reset to allow reprocessing
            }
        }
    }

    fun validateInputs(): Boolean {
        return when {
            email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                false
            }
            password.isBlank() -> {
                Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.primary
            )
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Divider(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(60.dp)
                    .height(4.dp),
                color = colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Form Fields
        ResponsiveTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = "Email",
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            isError = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        ResponsiveTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = { Icon(Icons.Default.Lock, "Password") },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle password visibility"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot Password Link
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { showForgotPasswordDialog = true }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sign In Button
        ResponsiveButton(
            onClick = {
                if (validateInputs()) {
                    viewModel.signInWithEmailPassword(email, password)
                }
            },
            text = "Sign In",
            isLoading = authState is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.primary,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                ResponsiveTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it.trim() },
                    label = "Email",
                    leadingIcon = { Icon(Icons.Default.Email, "Email") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (resetEmail.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                            viewModel.resetPassword(resetEmail)
                        } else {
                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

object FirebaseRateLimitHandler {
    private const val TAG = "FirebaseRateLimit"
    private var lastBackoffTime = 0L
    private var backoffMultiplier = 1
    
    fun handleException(e: Exception, onRetryCallback: () -> Unit) {
        if (e is FirebaseException && (e.message?.contains("Too many attempts") == true ||
                                        e.message?.contains("rate-limited") == true)) {
            Log.w(TAG, "Rate limit hit: ${e.message}")
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastBackoff = currentTime - lastBackoffTime
            
            // If this is a new rate limit (not a continuation of a previous one)
            if (timeSinceLastBackoff > 60000) { // Reset after 1 minute
                backoffMultiplier = 1
            } else {
                // Exponential backoff
                backoffMultiplier = minOf(backoffMultiplier * 2, 16)
            }
            
            val delayMs = 1000L * backoffMultiplier
            lastBackoffTime = currentTime
            
            Log.d(TAG, "Backing off for ${delayMs/1000} seconds")
            
            // Schedule a retry with exponential backoff
            CoroutineScope(Dispatchers.Main).launch {
                delay(delayMs)
                Log.d(TAG, "Retrying after backoff")
                onRetryCallback()
            }
        } else {
            // Different type of error, just log it
            Log.e(TAG, "Firebase error (not rate-limited): ${e.message}")
        }
    }
}























