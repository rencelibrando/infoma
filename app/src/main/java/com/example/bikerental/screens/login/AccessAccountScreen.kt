package com.example.bikerental.screens.login
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.R
import com.example.bikerental.components.ResponsiveButton
import com.example.bikerental.components.ResponsiveTextField
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.util.Patterns
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.example.bikerental.components.GoogleSignInButton

@Composable
fun AccessAccountScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorVisible by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val colorScheme = MaterialTheme.colorScheme

    // Effect to handle navigation after successful login
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            navController.navigate("home") {
                popUpTo("signIn") { inclusive = true }
            }
        }
    }

    fun validateInputs(): Boolean {
        if (email.isBlank()) {
            errorMessage = "Please enter your email"
            isErrorVisible = true
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = "Please enter a valid email address"
            isErrorVisible = true
            return false
        }
        if (password.isBlank()) {
            errorMessage = "Please enter your password"
            isErrorVisible = true
            return false
        }
        return true
    }

    // Google Sign-In Client
    val googleSignInClient: GoogleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    val googleLauncher: ActivityResultLauncher<Intent> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            isSuccess = true
                        } else {
                            errorMessage = when (task.exception) {
                                is FirebaseAuthInvalidCredentialsException -> "Invalid credentials"
                                else -> "Google sign in failed: ${task.exception?.message}"
                            }
                            isErrorVisible = true
                            isLoading = false
                        }
                    }
            } catch (e: ApiException) {
                errorMessage = "Google sign in failed: ${e.message}"
                isErrorVisible = true
                isLoading = false
            }
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
                text = "Access Account",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colorScheme.primary
            )
            Text(
                text = "Welcome back! Please log in to continue.",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
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

        // Error Message
        if (isErrorVisible && errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Email Field
        ResponsiveTextField(
            value = email,
            onValueChange = { 
                email = it.trim()
                isErrorVisible = false
            },
            label = "Your email address",
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            isError = isErrorVisible && errorMessage?.contains("email") == true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Password Field
        ResponsiveTextField(
            value = password,
            onValueChange = { 
                password = it
                isErrorVisible = false
            },
            label = "Your password",
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
            isError = isErrorVisible && errorMessage?.contains("password") == true
        )

        // Forgot Password Link
        Text(
            text = "Forgot your password?",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
                .clickable { /* TODO: Implement forgot password */ }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Login Button
        ResponsiveButton(
            onClick = {
                if (validateInputs()) {
                    isLoading = true
                    isErrorVisible = false
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                isSuccess = true
                            } else {
                                errorMessage = when (task.exception) {
                                    is FirebaseAuthInvalidUserException -> "No account found with this email"
                                    is FirebaseAuthInvalidCredentialsException -> "Invalid password"
                                    else -> "Login failed: ${task.exception?.message}"
                                }
                                isErrorVisible = true
                                isLoading = false
                            }
                        }
                }
            },
            text = "Sign In",
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(10.dp))

        // Google Sign-In Button
        GoogleSignInButton(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                googleLauncher.launch(signInIntent)
            },
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Sign Up Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 280.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = colorScheme.primary,
                modifier = Modifier
                    .clickable { navController.navigate("signUp") }
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccessAccountScreenPreview() {
    AccessAccountScreen(rememberNavController())
}























