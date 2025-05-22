package com.example.bikerental.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.bikerental.navigation.ProfileBackHandler
import com.example.bikerental.navigation.popBackToProfileTab
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // State variables
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    
    // Replace custom BackHandler with ProfileBackHandler
    ProfileBackHandler(navController)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackToProfileTab() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EmailPasswordUserSection(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmNewPassword = confirmNewPassword,
                        isPasswordVisible = isPasswordVisible,
                        onCurrentPasswordChange = { currentPassword = it },
                        onNewPasswordChange = { newPassword = it },
                        onConfirmPasswordChange = { confirmNewPassword = it },
                        onPasswordVisibilityChange = { isPasswordVisible = it },
                        error = error,
                        success = success,
                        onChangePassword = {
                            coroutineScope.launch {
                                changePassword(
                                    currentPassword = currentPassword,
                                    newPassword = newPassword,
                                    confirmNewPassword = confirmNewPassword,
                                    onLoading = { isLoading = it },
                                    onError = { error = it; success = null },
                                    onSuccess = { 
                                        success = "Password changed successfully!" 
                                        error = null
                                        // Clear form fields after successful change
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                        
                                        // Wait a moment then navigate back
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1500)
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Section for email/password users to change their password
 */
@Composable
private fun EmailPasswordUserSection(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    isPasswordVisible: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    error: String?,
    success: String?,
    onChangePassword: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Change Your Password",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Password strength tips card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Password Requirements:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Minimum 8 characters")
                Text("• Include at least one uppercase letter")
                Text("• Include at least one number")
                Text("• Include at least one special character")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Current password
        OutlinedTextField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            label = { Text("Current Password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibilityChange(!isPasswordVisible) }) {
                    Icon(
                        imageVector = if (isPasswordVisible) 
                            Icons.Default.Visibility 
                        else 
                            Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) 
                            "Hide Password" 
                        else 
                            "Show Password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // New password
        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = { Text("New Password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibilityChange(!isPasswordVisible) }) {
                    Icon(
                        imageVector = if (isPasswordVisible) 
                            Icons.Default.Visibility 
                        else 
                            Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) 
                            "Hide Password" 
                        else 
                            "Show Password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Password strength indicator
        if (newPassword.isNotEmpty()) {
            val strength = getPasswordStrength(newPassword)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Password Strength: ${strength.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = strength.color
                )
                LinearProgressIndicator(
                    progress = { strength.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = strength.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        
        // Confirm new password
        OutlinedTextField(
            value = confirmNewPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm New Password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibilityChange(!isPasswordVisible) }) {
                    Icon(
                        imageVector = if (isPasswordVisible) 
                            Icons.Default.Visibility 
                        else 
                            Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) 
                            "Hide Password" 
                        else 
                            "Show Password"
                    )
                }
            },
            isError = confirmNewPassword.isNotEmpty() && confirmNewPassword != newPassword,
            supportingText = {
                if (confirmNewPassword.isNotEmpty() && confirmNewPassword != newPassword) {
                    Text("Passwords do not match")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Error message
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Success message
        if (success != null) {
            Text(
                text = success,
                color = ColorUtils.DarkGreen,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Change password button
        Button(
            onClick = onChangePassword,
            enabled = isPasswordChangeValid(currentPassword, newPassword, confirmNewPassword),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Password")
        }
    }
}

/**
 * Function to change the user's password
 */
private suspend fun changePassword(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    // Validate inputs
    if (currentPassword.isBlank()) {
        onError("Current password is required")
        return
    }
    
    if (newPassword.length < 8) {
        onError("New password must be at least 8 characters")
        return
    }
    
    if (newPassword != confirmNewPassword) {
        onError("New passwords don't match")
        return
    }
    
    // Get current user
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onError("User not authenticated")
        return
    }
    
    onLoading(true)
    
    try {
        // First reauthenticate user
        val credential = com.google.firebase.auth.EmailAuthProvider
            .getCredential(user.email ?: "", currentPassword)
        
        // Reauthenticate and then update password
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                // User has been reauthenticated, now we can change password
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(updateTask.exception?.message ?: "Failed to update password")
                    }
                    onLoading(false)
                }
            } else {
                onError(reauthTask.exception?.message ?: "Authentication failed")
                onLoading(false)
            }
        }
    } catch (e: Exception) {
        onError(e.message ?: "An error occurred")
        onLoading(false)
    }
}

/**
 * Class to represent password strength
 */
private data class PasswordStrength(
    val value: Float,
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

/**
 * Function to calculate password strength
 */
@Composable
private fun getPasswordStrength(password: String): PasswordStrength {
    // Criteria
    val minLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    
    // Calculate score (0.0 to 1.0)
    val criteriaCount = listOf(minLength, hasUppercase, hasLowercase, hasDigit, hasSpecial)
        .count { it }
    
    val strength = when {
        password.length < 4 -> PasswordStrength(
            0.1f, 
            "Very Weak", 
            MaterialTheme.colorScheme.error
        )
        criteriaCount <= 1 -> PasswordStrength(
            0.25f, 
            "Weak", 
            MaterialTheme.colorScheme.error
        )
        criteriaCount == 2 -> PasswordStrength(
            0.5f, 
            "Moderate", 
            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        criteriaCount == 3 -> PasswordStrength(
            0.75f, 
            "Strong", 
            MaterialTheme.colorScheme.tertiary
        )
        else -> PasswordStrength(
            1.0f, 
            "Very Strong", 
            ColorUtils.DarkGreen
        )
    }
    
    return strength
}

/**
 * Function to check if password change is valid
 */
@Composable
private fun isPasswordChangeValid(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String
): Boolean {
    return currentPassword.isNotBlank() && 
           newPassword.length >= 8 && 
           newPassword == confirmNewPassword
}