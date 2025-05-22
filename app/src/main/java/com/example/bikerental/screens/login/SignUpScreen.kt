package com.example.bikerental.screens.login
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.components.ResponsiveTextField
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.Screen
import com.example.bikerental.viewmodels.AuthViewModel

/**
 * Formats a Philippine phone number to the international format (+63)
 */
fun formatPhilippinePhoneNumber(phoneNumber: String): String {
    try {
        // Basic validation before formatting
        if (phoneNumber.isBlank()) return phoneNumber
        
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        if (digitsOnly.isEmpty()) return phoneNumber
        
        return when {
            // If it already starts with +63, return as is
            phoneNumber.startsWith("+63") -> phoneNumber
            
            // If it starts with 0, replace with +63
            digitsOnly.startsWith("0") && digitsOnly.length >= 10 -> {
                "+63${digitsOnly.substring(1)}"
            }
            
            // If it's just the 9-digit number (without 0 or +63)
            digitsOnly.startsWith("9") && digitsOnly.length >= 9 -> {
                "+63$digitsOnly"
            }
            
            // If it starts with 63 (without +)
            digitsOnly.startsWith("63") && digitsOnly.length >= 11 -> {
                "+$digitsOnly"
            }
            
            // Otherwise, don't modify (will fail validation)
            else -> phoneNumber
        }
    } catch (e: Exception) {
        android.util.Log.e("SignUpScreen", "Phone formatting error: ${e.message}")
        // Return original on error
        return phoneNumber
    }
}

/**
 * Validates if the input is a valid Philippine phone number
 */
fun isValidPhilippinePhoneNumber(phoneNumber: String): Boolean {
    try {
        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isEmpty()) return false
        
        // Check basic length and pattern before regex
        val digitsOnly = trimmedPhone.replace(Regex("[^0-9+]"), "")
        
        // Basic validation without complex regex
        val isValid = when {
            // +639XXXXXXXXX format
            trimmedPhone.startsWith("+639") && digitsOnly.length == 13 -> true
            // 09XXXXXXXXX format
            trimmedPhone.startsWith("09") && digitsOnly.length == 11 -> true
            // 9XXXXXXXXX format (just numbers)
            trimmedPhone.startsWith("9") && digitsOnly.length == 10 -> true
            // 639XXXXXXXXX format
            trimmedPhone.startsWith("639") && digitsOnly.length == 12 -> true
            // Any other format is invalid
            else -> false
        }
        
        return isValid
    } catch (e: Exception) {
        android.util.Log.e("SignUpScreen", "Phone validation error: ${e.message}")
        return false
    }
}

/**
 * Safely formats a phone number, handling any exceptions internally
 */
fun safeFormatPhoneNumber(phoneNumber: String): String {
    return try {
        formatPhilippinePhoneNumber(phoneNumber)
    } catch (e: Exception) {
        android.util.Log.e("SignUpScreen", "Error formatting phone: ${e.message}")
        phoneNumber // Return original on error
    }
}

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var formattedPhoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    // Password validation states
    var hasMinLength by remember { mutableStateOf(false) }
    var hasUppercase by remember { mutableStateOf(false) }
    var hasLowercase by remember { mutableStateOf(false) }
    var hasDigit by remember { mutableStateOf(false) }
    var hasSpecialChar by remember { mutableStateOf(false) }

    // Add an error state to show UI error messages
    var emailError by remember { mutableStateOf<String?>(null) }

    // Update password validation whenever password changes
    LaunchedEffect(password) {
        hasMinLength = password.length >= 8
        hasUppercase = password.any { it.isUpperCase() }
        hasLowercase = password.any { it.isLowerCase() }
        hasDigit = password.any { it.isDigit() }
        hasSpecialChar = password.any { !it.isLetterOrDigit() }
    }

    // Function to check if the password meets all requirements
    fun isPasswordValid(): Boolean {
        return hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    // Collect auth state
    val authState by viewModel.authState.collectAsState()

    // Effect to show appropriate error messages
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            val errorMessage = (authState as AuthState.Error).message
            if (errorMessage.contains("email address is already in use", ignoreCase = true)) {
                emailError = "This email is already registered. Please use a different email or sign in."
            } else {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        } else if (authState is AuthState.Authenticated) {
            // Clear any errors when authenticated
            emailError = null
        } else if (authState is AuthState.NeedsEmailVerification) {
            // Navigate to email verification screen
            navController.navigate(Screen.EmailVerification.route) {
                popUpTo(Screen.SignUp.route) { inclusive = true }
            }
        }
    }

    fun validateInputs(): Boolean {
        // Ensure all inputs are trimmed before validation
        val trimmedFullName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirmPassword = confirmPassword.trim()
        
        // Format phone number before validation
        formattedPhoneNumber = safeFormatPhoneNumber(phone.trim())
        
        return when {
            trimmedFullName.isBlank() -> {
                Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                false
            }
            trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                false
            }
            !isPasswordValid() -> {
                Toast.makeText(context, "Password does not meet all requirements", Toast.LENGTH_SHORT).show()
                false
            }
            trimmedPassword != trimmedConfirmPassword -> {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                false
            }
            !isValidPhilippinePhoneNumber(phone.trim()) -> {
                Toast.makeText(context, "Please enter a valid Philippine phone number", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp), // Add bottom padding for better scrolling
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Spacer(modifier = Modifier.height(48.dp))
        
        // Title Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.primary
            )
            Text(
                text = "Sign up to get started!",
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

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            ResponsiveTextField(
                value = fullName,
                onValueChange = { fullName = it.trim() },
                label = "Full Name",
                leadingIcon = { Icon(Icons.Default.Person, "Name") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ResponsiveTextField(
                value = email,
                onValueChange = { 
                    email = it.trim()
                    // Clear error when user starts typing again
                    emailError = null
                },
                label = "Email",
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                isError = emailError != null,
                errorMessage = emailError
            )

            Spacer(modifier = Modifier.height(16.dp))

            ResponsiveTextField(
                value = password,
                onValueChange = { password = it.trim() },
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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                supportingText = {
                    if (isPasswordFocused || password.isNotEmpty()) {
                        Column {
                            Text("Password must contain:")
                            PasswordRequirementItem(
                                text = "At least 8 characters",
                                isValid = hasMinLength
                            )
                            PasswordRequirementItem(
                                text = "At least one uppercase letter",
                                isValid = hasUppercase
                            )
                            PasswordRequirementItem(
                                text = "At least one lowercase letter",
                                isValid = hasLowercase
                            )
                            PasswordRequirementItem(
                                text = "At least one number",
                                isValid = hasDigit
                            )
                            PasswordRequirementItem(
                                text = "At least one special character",
                                isValid = hasSpecialChar
                            )
                        }
                    }
                },
                onFocusChanged = { focusState ->
                    isPasswordFocused = focusState.isFocused
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ResponsiveTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it.trim() },
                label = "Confirm Password",
                leadingIcon = { Icon(Icons.Default.Lock, "Confirm password") },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ResponsiveTextField(
                value = phone,
                onValueChange = { 
                    // Store raw input without immediate validation
                    phone = it
                    
                    // Only format if the input is not empty and roughly matches a phone pattern
                    if (it.trim().isNotEmpty() && it.trim().length >= 9) {
                        // Basic pattern checking instead of regex for better performance
                        val digitsOnly = it.replace(Regex("[^0-9]"), "")
                        if (digitsOnly.length >= 9) {
                            formattedPhoneNumber = safeFormatPhoneNumber(it.trim())
                        }
                    }
                },
                label = "Phone Number (e.g., 09123456789)",
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                supportingText = {
                    if (phone.isNotEmpty()) {
                        if (isValidPhilippinePhoneNumber(phone.trim())) {
                            Text("Will be saved as: $formattedPhoneNumber")
                        }
                    }
                }
            )

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Up Button
        ResponsiveButton(
            onClick = {
                if (validateInputs()) {
                    // Use trimmed values for registration
                    viewModel.createUserWithEmailPassword(
                        email.trim(), 
                        password.trim(), 
                        fullName.trim(), 
                        formattedPhoneNumber
                    )
                }
            },
            text = "Sign Up",
            isLoading = authState is AuthState.Loading
        )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = colorScheme.primary,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(Screen.SignUp.route)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(rememberNavController())
}

/**
 * A composable for displaying a password requirement with a check mark or X icon
 */
@Composable
fun PasswordRequirementItem(
    text: String,
    isValid: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (isValid) "Requirement met" else "Requirement not met",
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
