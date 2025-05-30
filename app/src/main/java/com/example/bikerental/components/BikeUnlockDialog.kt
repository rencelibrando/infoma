package com.example.bikerental.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun BikeUnlockDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = { /* Prevent dismissal during unlock process */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Title
                    Text(
                        text = "Unlocking Bike",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Animated status message
                    UnlockStatusMessage()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Info text
                    Text(
                        text = "Please wait while we verify the QR code and unlock your bike...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockStatusMessage() {
    val messages = listOf(
        "Verifying QR code...",
        "Checking bike availability...",
        "Connecting to bike...",
        "Unlocking bike...",
        "Starting ride tracking..."
    )
    
    var currentMessageIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500) // Change message every 1.5 seconds
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
        }
    }
    
    Text(
        text = messages[currentMessageIndex],
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
    )
} 