package com.example.bikerental.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordValid by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Get current user and check auth provider
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    
    // Determine if user signed in with Google
    val isGoogleUser = remember {
        mutableStateOf(false)
    }
    
    LaunchedEffect(Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        isGoogleUser.value = firebaseUser?.providerData?.any { 
            it.providerId == "google.com" 
        } == true
    }
    
    val scrollState = rememberScrollState()
    
    // Password validation
    val validatePasswords = {
        when {
            currentPassword.isBlank() -> {
                isPasswordValid = false
                errorMessage = "Current password is required"
                false
            }
            newPassword.length < 8 -> {
                isPasswordValid = false
                errorMessage = "New password must be at least 8 characters"
                false
            }
            newPassword != confirmNewPassword -> {
                isPasswordValid = false
                errorMessage = "New passwords don't match"
                false
            }
            else -> {
                isPasswordValid = true
                errorMessage = null
                true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ColorUtils.Purple40
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isGoogleUser.value) {
                // Show message for Google users
                GoogleUserPasswordMessage(navController)
            } else {
                // Regular password change UI for email/password users
                PasswordChangeForm(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmNewPassword = confirmNewPassword,
                    isPasswordVisible = isPasswordVisible,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    successMessage = successMessage,
                    onCurrentPasswordChange = { currentPassword = it },
                    onNewPasswordChange = { newPassword = it },
                    onConfirmNewPasswordChange = { confirmNewPassword = it },
                    onPasswordVisibilityChange = { isPasswordVisible = it },
                    onSubmit = {
                        if (validatePasswords()) {
                            isLoading = true
                            viewModel.changePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                onSuccess = {
                                    isLoading = false
                                    successMessage = "Password changed successfully"
                                    // Clear input fields
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GoogleUserPasswordMessage(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Google Account Password",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Your account is linked to Google. To change your password, you'll need to visit your Google Account settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                // Open Google account settings in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/security"))
                navController.context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Go to Google Account Settings")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }
    }
}

@Composable
private fun PasswordChangeForm(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    // Current password field
    OutlinedTextField(
        value = currentPassword,
        onValueChange = onCurrentPasswordChange,
        label = { Text("Current Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { onPasswordVisibilityChange(!isPasswordVisible) }) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                )
            }
        }
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // New password field
    OutlinedTextField(
        value = newPassword,
        onValueChange = onNewPasswordChange,
        label = { Text("New Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { onPasswordVisibilityChange(!isPasswordVisible) }) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                )
            }
        }
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Confirm new password field
    OutlinedTextField(
        value = confirmNewPassword,
        onValueChange = onConfirmNewPasswordChange,
        label = { Text("Confirm New Password") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { onPasswordVisibilityChange(!isPasswordVisible) }) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                )
            }
        }
    )
    
    // Error/Success messages
    errorMessage?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    
    successMessage?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = it,
            color = Color.Green,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Submit button
    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Change Password")
        }
    }
} 