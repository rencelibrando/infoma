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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.components.ResponsiveTextField
import com.example.bikerental.components.GoogleSignInButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.util.Patterns
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikerental.models.AuthState
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.bikerental.navigation.Screen

/**
 * Formats a Philippine phone number to the international format (+63)
 */
fun formatPhilippinePhoneNumber(phoneNumber: String): String {
    val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
    
    return when {
        // If it already starts with +63, return as is
        phoneNumber.startsWith("+63") -> phoneNumber
        
        // If it starts with 0, replace with +63
        digitsOnly.startsWith("0") && digitsOnly.length >= 11 -> {
            "+63${digitsOnly.substring(1)}"
        }
        
        // If it's just the 9-digit number (without 0 or +63)
        digitsOnly.length >= 9 && !digitsOnly.startsWith("0") && !digitsOnly.startsWith("63") -> {
            "+63$digitsOnly"
        }
        
        // If it starts with 63 (without +)
        digitsOnly.startsWith("63") && digitsOnly.length >= 12 -> {
            "+$digitsOnly"
        }
        
        // Otherwise, don't modify (will fail validation)
        else -> phoneNumber
    }
}

/**
 * Validates if the input is a valid Philippine phone number
 */
fun isValidPhilippinePhoneNumber(phoneNumber: String): Boolean {
    val trimmedPhone = phoneNumber.trim()
    
    // Check if it matches the pattern of a Philippine phone number
    // Valid formats: +639XXXXXXXXX, 09XXXXXXXXX, 9XXXXXXXXX, 639XXXXXXXXX
    return trimmedPhone.matches(Regex("^(\\+63|0|)9\\d{9}$")) || 
           trimmedPhone.matches(Regex("^63\\d{10}$"))
}

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var formattedPhoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Password validation states
    var hasMinLength by remember { mutableStateOf(false) }
    var hasUppercase by remember { mutableStateOf(false) }
    var hasLowercase by remember { mutableStateOf(false) }
    var hasDigit by remember { mutableStateOf(false) }
    var hasSpecialChar by remember { mutableStateOf(false) }

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

    // Initialize Google Sign-In
    LaunchedEffect(Unit) {
        viewModel.initializeGoogleSignIn(context)
    }
    
    // Add a direct Firebase Auth listener as backup
    val currentFirebaseUser = remember { mutableStateOf<FirebaseUser?>(null) }
    
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null && user != currentFirebaseUser.value) {
                Log.d("SignUpScreen", "Firebase Auth state changed - user is now logged in: ${user.uid}")
                currentFirebaseUser.value = user
            }
        }
        
        // Register the listener
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        
        // Clean up on dispose
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        }
    }

    // Google Sign-In Launcher
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("SignUpScreen", "Google Sign-In result received: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                Log.d("SignUpScreen", "Google Sign-In successful, account: ${account?.email}")
                account?.idToken?.let { token ->
                    Log.d("SignUpScreen", "Handling Google Sign-In result")
                    viewModel.handleGoogleSignInResult(
                        idToken = token,
                        displayName = account.displayName,
                        email = account.email,
                        context = context
                    )
                } ?: run {
                    Log.e("SignUpScreen", "Failed to get ID token")
                    Toast.makeText(context, "Failed to get ID token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google Sign-In was cancelled by user"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error occurred. Please check your internet connection"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid Google account selected"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign-In is required"
                    else -> "Google Sign-In failed: ${e.message}"
                }
                Log.e("SignUpScreen", "Google sign in failed: $errorMessage", e)
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    }
                            } else {
            Log.d("SignUpScreen", "Google Sign-In cancelled by user")
            Toast.makeText(context, "Sign in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    fun validateInputs(): Boolean {
        // Format phone number before validation
        formattedPhoneNumber = formatPhilippinePhoneNumber(phone.trim())
        
        return when {
            fullName.trim().isBlank() -> {
                Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                false
            }
            email.trim().isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                false
            }
            !isPasswordValid() -> {
                Toast.makeText(context, "Password does not meet all requirements", Toast.LENGTH_SHORT).show()
                false
            }
            password != confirmPassword -> {
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
                onValueChange = { email = it.trim() },
                label = "Email",
                leadingIcon = { Icon(Icons.Default.Email, "Email") }
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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                supportingText = {
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
            )

            Spacer(modifier = Modifier.height(16.dp))

            ResponsiveTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
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
                    // Store raw input
                    phone = it
                    
                    // Show formatted preview if valid
                    if (isValidPhilippinePhoneNumber(it.trim())) {
                        formattedPhoneNumber = formatPhilippinePhoneNumber(it.trim())
                    }
                },
                label = "Phone Number (e.g., 09123456789)",
                leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                supportingText = {
                    if (phone.isNotEmpty() && isValidPhilippinePhoneNumber(phone.trim())) {
                        Text("Will be saved as: $formattedPhoneNumber")
                    }
                }
            )

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Up Button
        ResponsiveButton(
            onClick = {
                if (validateInputs()) {
                    // Use the formatted phone number for registration
                    viewModel.createUserWithEmailPassword(
                        email.trim(), 
                        password, 
                        fullName.trim(), 
                        formattedPhoneNumber
                    )
                }
            },
            text = "Sign Up",
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
                        Log.d("SignUpScreen", "Starting Google Sign-In process")
                        val googleSignInClient = viewModel.getGoogleSignInClient(context)
                        
                        // Check for existing Google Sign-In
                        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
                        if (lastSignedInAccount != null) {
                            Log.d("SignUpScreen", "Found existing Google Sign-In, signing out first")
                            googleSignInClient.signOut().addOnCompleteListener {
                                Log.d("SignUpScreen", "Previous sign-in cleared, launching new sign-in")
                                try {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    Log.e("SignUpScreen", "Failed to launch sign-in intent after signout", e)
                                    Toast.makeText(
                                        context,
                                        "Failed to start Google Sign-In: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Log.d("SignUpScreen", "No existing sign-in found, launching sign-in directly")
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("SignUpScreen", "Failed to initialize Google Sign-In", e)
                        Toast.makeText(
                            context,
                            "Failed to initialize Google Sign-In: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                isLoading = authState is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

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
