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
import com.example.bikerental.components.GoogleSignInButton
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.components.ResponsiveTextField
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.Screen
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.example.bikerental.navigation.NavigationUtils

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

    // Initialize Google Sign-In when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.initializeGoogleSignIn(context)
    }

    // Google Sign-In Launcher
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    viewModel.handleGoogleSignInResult(
                        idToken = token,
                        displayName = account.displayName,
                        email = account.email,
                        context = context
                    )
                } ?: run {
                    Toast.makeText(context, "Failed to get ID token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google Sign-In was cancelled"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error occurred"
                    else -> "Google Sign-In failed: ${e.message}"
                }
                Log.e("AccessAccountScreen", "Google sign in failed", e)
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        
        when (authState) {
            is AuthState.Authenticated -> {
                // Only proceed with navigation if we have a valid authentication
                val authenticated = authState as AuthState.Authenticated
                Log.d("AccessAccountScreen", "Auth state is Authenticated, user: ${authenticated.user}")
                
                if (authenticated.user.id.isNotBlank()) {
                    // Mark as processed before navigating
                    authStateProcessed = true
                    Log.d("AccessAccountScreen", "Auth state processed, navigating...")
                    
                    Log.d("AccessAccountScreen", "User login successful - navigating to Home")
                    NavigationUtils.navigateToHome(navController)
                } else {
                    Log.e("AccessAccountScreen", "Authenticated but user ID is blank, state: $authState")
                }
            }
            is AuthState.Error -> {
                Log.e("AccessAccountScreen", "Auth Error: ${(authState as AuthState.Error).message}")
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                authStateProcessed = false // Allow reprocessing after error
            }
            is AuthState.NeedsAdditionalInfo -> {
                Log.d("AccessAccountScreen", "Auth state: NeedsAdditionalInfo")
                authStateProcessed = true
                NavigationUtils.navigateToSignUp(navController)
            }
            is AuthState.PasswordResetSent -> {
                Log.d("AccessAccountScreen", "Auth state: PasswordResetSent")
                Toast.makeText(context, "Password reset email sent", Toast.LENGTH_LONG).show()
                showForgotPasswordDialog = false
                authStateProcessed = false // Reset to allow reprocessing
            }
            else -> {
                Log.d("AccessAccountScreen", "Unhandled auth state: $authState")
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

        Spacer(modifier = Modifier.height(16.dp))

        // OR Separator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "—————  Or  —————",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In Button
        GoogleSignInButton(
            onClick = {
                try {
                    val googleSignInClient = viewModel.getGoogleSignInClient(context)
                    googleLauncher.launch(googleSignInClient.signInIntent)
                } catch (e: Exception) {
                    Log.e("AccessAccountScreen", "Failed to start Google Sign-In", e)
                    Toast.makeText(
                        context,
                        "Failed to start Google Sign-In. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
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























