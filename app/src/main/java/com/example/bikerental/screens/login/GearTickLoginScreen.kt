package com.example.bikerental.screens.login

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

@Composable
fun GearTickLoginScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6C63FF)) // Blue background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Icon(
                painter = painterResource(id = R.drawable.cyclist), // Replace with actual logo
                contentDescription = "BikeShare Logo",
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Title
            Text(
                text = "GearTick",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // App Subtitle
            Text(
                text = "Discover convenient bike rental services at your fingertips.",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Log In Button
            Button(
                onClick = { navController.navigate("signIn")},
                modifier = Modifier
                    .width(280.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "Log In",
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Up Button
            OutlinedButton(
                onClick = { navController.navigate("signUp") },
                modifier = Modifier
                    .width(280.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 18.sp,
                    color = Color.White
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