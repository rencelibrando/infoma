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
    
    // Logo animations
    val logoScale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutBack
        )
    )
    
    val logoRotation = animateFloatAsState(
        targetValue = if (startAnimation) 0f else -45f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseOutQuart
        )
    )
    
    val logoAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800
        )
    )
    
    // First text animations
    val titleAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 400
        )
    )
    
    val titleSlide = animateFloatAsState(
        targetValue = if (startAnimation) 0f else -100f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = 400,
            easing = EaseOutQuint
        )
    )
    
    // Second text animations
    val subtitleAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 600
        )
    )
    
    val subtitleSlide = animateFloatAsState(
        targetValue = if (startAnimation) 0f else 100f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = 600,
            easing = EaseOutQuint
        )
    )
    
    // Startup and completion
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500) // Slightly longer to allow animations to complete
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
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
                    .scale(logoScale.value)
                    .rotate(logoRotation.value)
                    .alpha(logoAlpha.value)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main title with slide and fade animations
            Text(
                text = "Bambike",
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(x = titleSlide.value.dp)
                    .fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle with slide and fade animations (from opposite direction)
            Text(
                text = "REVOLUTION CYCLES",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .offset(x = subtitleSlide.value.dp)
                    .fillMaxWidth()
            )
        }
    }
} 