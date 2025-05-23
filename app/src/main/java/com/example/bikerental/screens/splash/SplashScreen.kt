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
import androidx.compose.ui.draw.rotate
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
    
    // Optimized animations with reduced durations for faster startup
    
    // Logo animations - simplify for better performance
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f, // Starting closer to final size
        animationSpec = tween(
            durationMillis = 500, // Reduced from 700ms
            easing = EaseOutQuad
        ), 
        label = "logoScale"
    )
    
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -30f, // Reduced rotation
        animationSpec = tween(
            durationMillis = 600, // Reduced from 800ms
            easing = EaseOutQuad
        ),
        label = "logoRotation"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500 // Reduced from 700ms
        ),
        label = "logoAlpha"
    )
    
    // First text animations - faster fade-in
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500, // Reduced from 700ms
            delayMillis = 200 // Reduced from 300ms
        ),
        label = "titleAlpha"
    )
    
    val titleSlide by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -50f, // Reduced slide distance
        animationSpec = tween(
            durationMillis = 600, // Reduced from 800ms
            delayMillis = 200, // Reduced from 300ms
            easing = EaseOutQuad
        ),
        label = "titleSlide"
    )
    
    // Second text animations - faster fade-in
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500, // Reduced from 700ms
            delayMillis = 300 // Reduced from 500ms
        ),
        label = "subtitleAlpha"
    )
    
    val subtitleSlide by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 50f, // Reduced slide distance
        animationSpec = tween(
            durationMillis = 600, // Reduced from 800ms
            delayMillis = 300, // Reduced from 500ms
            easing = EaseOutQuad
        ),
        label = "subtitleSlide"
    )
    
    // Start animation immediately and don't wait for timer if animations complete
    LaunchedEffect(key1 = true) {
        startAnimation = true
        // No need to call onSplashComplete, will be handled by MainActivity timer
    }

    // Memory efficient background using MaterialTheme
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
        ) {
            // Logo with animations - simplified for performance
            Image(
                painter = painterResource(id = R.drawable.bambikelogo),
                contentDescription = "Bambike Logo",
                modifier = Modifier
                    .size(100.dp) // Reduced from 120dp
                    .scale(logoScale)
                    .rotate(logoRotation)
                    .alpha(logoAlpha)
            )
            
            Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16dp
            
            // Main title with animations
            Text(
                text = "Bambike",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp, // Reduced from 32sp
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(x = titleSlide.dp)
                    .fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp
            
            // Subtitle with animations
            Text(
                text = "REVOLUTION CYCLES",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp, // Reduced from 18sp
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .offset(x = subtitleSlide.dp)
                    .fillMaxWidth()
            )
        }
    }
} 