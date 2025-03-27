package com.example.bikerental.screens.login
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun SignUpScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()

    fun saveUserToFirestore(uid: String, fullName: String, email: String, phone: String) {
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phone,
            "authProvider" to "email",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                println("User successfully added to Firestore!")
            }
            .addOnFailureListener { e ->
                println("Error adding user to Firestore: $e")
            }
    }

    // Google Sign-In Client
    val googleSignInClient: GoogleSignInClient= remember {
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
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navController.navigate("home") // Replace with your home route
                        }
                    }
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
        Text(
            text = "Sign up to get started!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Full Name Field
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Your email address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter your password") },
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
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Phone Number Field
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Your phone number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sign-Up Button
        Button(
            onClick = {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            user?.let {
                                saveUserToFirestore(it.uid, fullName, email, phone)
                                navController.navigate("home")  // Navigate to home screen on success
                            }
                        } else {
                            println("Sign-up failed: ${task.exception?.message}")
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
        ) {
            Text("Sign Up", color = Color.White, fontSize = 16.sp)
        }


        Spacer(modifier = Modifier.height(15.dp))

        // OR Separator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "—————  Or  —————", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Social Sign-In Buttons
        SocialLoginButton(
            text = "Continue with Google",
            borderColor = Color(0xFFEA4335),
            googleSignInClient = googleSignInClient,  // Pass from parent
            googleLauncher = googleLauncher
        )
        Spacer(modifier = Modifier.height(10.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // Navigate to Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Already have an account?", color = Color.Gray)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Log In",
                color = Color(0xFF6750A4),
                modifier = Modifier.clickable { navController.navigate("signIn") }
            )
        }
    }
}

@Composable
fun SocialLoginButton(text: String,
                      borderColor: Color,
                      googleSignInClient: GoogleSignInClient,  // Add parameter
                      googleLauncher: ActivityResultLauncher<Intent>) {

    OutlinedButton(
        onClick = {val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)},
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = borderColor
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(text, color = borderColor, fontSize = 16.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(rememberNavController())
}
