package com.example.bikerental.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bikerental.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Simplified animations to reduce main thread blocking
    // Using only fade-in animations, no complex transformations
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300, // Much faster animation
            easing = LinearEasing // Simple linear easing
        ), 
        label = "contentAlpha"
    )
    
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f, // Minimal scaling
        animationSpec = tween(
            durationMillis = 400, // Reduced duration
            easing = LinearOutSlowInEasing // Simpler easing
        ),
        label = "logoScale"
    )
    
    // Start animation immediately for better perceived performance
    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    // Simplified layout without complex transformations
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .alpha(contentAlpha) // Single alpha animation for whole content
        ) {
            // Logo with minimal animation
            Image(
                painter = painterResource(id = R.drawable.bambikelogo),
                contentDescription = "Bambike Logo",
                modifier = Modifier
                    .size(80.dp) // Smaller size for faster rendering
                    .scale(logoScale)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Static text without complex animations
            Text(
                text = "Bambike",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp, // Smaller for faster rendering
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "REVOLUTION CYCLES",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp, // Smaller for faster rendering
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 