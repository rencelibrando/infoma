package com.example.bikerental.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.viewmodels.AuthViewModel
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

/**
 * Screen to verify email for Google sign-in users
 */
@Composable
fun GoogleVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val email = firebaseUser?.email
    
    // Check if we're already verified
    LaunchedEffect(firebaseUser) {
        if (firebaseUser?.isEmailVerified == true) {
            NavigationUtils.navigateToHome(navController)
        }
    }
    
    // Process auth state updates
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.VerificationEmailSent -> {
                Toast.makeText(context, "Verification email sent!", Toast.LENGTH_LONG).show()
                isLoading = false
            }
            is AuthState.Authenticated -> {
                NavigationUtils.navigateToHome(navController)
            }
            is AuthState.Error -> {
                val message = (authState as AuthState.Error).message
                errorMessage = message
                isLoading = false
                Log.e("GoogleVerification", "Error: $message")
            }
            else -> {
                // Handle other states
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = "Verification",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To continue, please verify your email address by clicking the link we've sent you.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        if (!email.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verification email will be sent to: $email",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ResponsiveButton(
            text = "Resend Verification Email",
            onClick = {
                isLoading = true
                errorMessage = null
                viewModel.resendEmailVerification()
            },
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ResponsiveButton(
            text = "I've Verified My Email",
            onClick = {
                isLoading = true
                errorMessage = null
                viewModel.checkEmailVerification()
            },
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
} 