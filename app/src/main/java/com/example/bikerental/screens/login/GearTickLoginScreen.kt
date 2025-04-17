package com.example.bikerental.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.bikerental.R
import androidx.compose.foundation.BorderStroke
import com.example.bikerental.navigation.Screen
import android.util.Log
import androidx.compose.runtime.remember
import com.example.bikerental.navigation.NavigationUtils

@Composable
fun GearTickLoginScreen(navController: NavHostController) {
    // Create safe navigation functions
    val safeNavigate = remember(navController) {
        { route: String ->
            try {
                // Check if the NavController is ready
                if (navController.graph.route != null) {
                    navController.navigate(route)
                } else {
                    Log.w("GearTickLoginScreen", "Navigation attempted before graph was ready")
                }
            } catch (e: Exception) {
                Log.e("GearTickLoginScreen", "Navigation error: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Keeping light gray background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.bambikelogo),
                contentDescription = "Bambike Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Title - Changed to black
            Text(
                text = "Bambike",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black // Changed from blue to black
            )

            // App Subtitle - Changed to black
            Text(
                text = "REVOLUTION CYCLES",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black, // Changed from blue to black
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Log In Button - Changed text to black, background to gray
            Button(
                onClick = { 
                    try {
                        NavigationUtils.navigateToSignIn(navController)
                    } catch (e: Exception) {
                        Log.e("GearTickLoginScreen", "Error navigating to sign in: ${e.message}")
                    }
                },
                modifier = Modifier
                    .width(280.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0) // Light gray button
                )
            ) {
                Text(
                    text = "Log In",
                    fontSize = 18.sp,
                    color = Color.Black // Changed from white to black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Up Button - Changed text to black, border to black
            OutlinedButton(
                onClick = { 
                    try {
                        NavigationUtils.navigateToSignUp(navController) 
                    } catch (e: Exception) {
                        Log.e("GearTickLoginScreen", "Error navigating to sign up: ${e.message}")
                    }
                },
                modifier = Modifier
                    .width(280.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black // Changed from blue to black
                ),
                border = BorderStroke(1.dp, Color.Black) // Changed from blue to black
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 18.sp,
                    color = Color.Black // Changed from blue to black
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GearTickLoginScreenPreview() {
    GearTickLoginScreen(rememberNavController())
} 