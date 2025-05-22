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
    
    // Logo animations - optimized for better performance
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 700,
            easing = EaseOutQuad
        ), 
        label = "logoScale"
    )
    
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -45f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutQuad
        ),
        label = "logoRotation"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700
        ),
        label = "logoAlpha"
    )
    
    // First text animations
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 300
        ),
        label = "titleAlpha"
    )
    
    val titleSlide by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -100f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 300,
            easing = EaseOutQuad
        ),
        label = "titleSlide"
    )
    
    // Second text animations
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 500
        ),
        label = "subtitleAlpha"
    )
    
    val subtitleSlide by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 100f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 500,
            easing = EaseOutQuad
        ),
        label = "subtitleSlide"
    )
    
    // Startup and completion - shortened duration
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1000) // Shorter duration to complete faster
        onSplashComplete()
    }

    // Simplified background - use MaterialTheme instead of hardcoded color
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
            // Logo with scale, rotation and fade animations
            Image(
                painter = painterResource(id = R.drawable.bambikelogo),
                contentDescription = "Bambike Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
                    .rotate(logoRotation)
                    .alpha(logoAlpha)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main title with slide and fade animations
            Text(
                text = "Bambike",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(x = titleSlide.dp)
                    .fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle with slide and fade animations (from opposite direction)
            Text(
                text = "REVOLUTION CYCLES",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
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