package com.example.bikerental.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.example.bikerental.utils.ColorUtils

@Composable
fun StartRideDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onStartQRScan: () -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = "Start Ride",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Title
                    Text(
                        text = "Start Your Ride",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Description
                    Text(
                        text = "Scan the QR code on your bike to unlock it and start your ride. Make sure you're standing next to the bike you want to use.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Scan QR Button
                        Button(
                            onClick = {
                                onDismiss()
                                onStartQRScan()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scan QR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartRideDialogContent(
    onDismiss: () -> Unit,
    onScanQRCode: () -> Unit,
    isLoading: Boolean
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardColors = CardDefaults.cardColors(containerColor = surfaceColor)
    
    val darkGreenColor = ColorUtils.DarkGreen
    val buttonColors = ButtonDefaults.buttonColors(containerColor = darkGreenColor)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StartRideIcon(isLoading = isLoading)
            
            StartRideContent(isLoading = isLoading)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            StartRideActions(
                onDismiss = onDismiss,
                onScanQRCode = onScanQRCode,
                isLoading = isLoading,
                buttonColors = buttonColors
            )
        }
    }
}

@Composable
private fun StartRideIcon(isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = ColorUtils.DarkGreen,
            strokeWidth = 4.dp
        )
    } else {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "QR Scanner",
            modifier = Modifier.size(64.dp),
            tint = ColorUtils.DarkGreen
        )
    }
}

@Composable
private fun StartRideContent(isLoading: Boolean) {
    val title = remember(isLoading) {
        if (isLoading) "Starting Your Ride..." else "Start Your Bike Ride"
    }
    
    val description = remember(isLoading) {
        if (isLoading) {
            "Please wait while we unlock your bike and set up your ride session."
        } else {
            "Scan the QR code on the bike to unlock and start your ride. Make sure you're near the bike before scanning."
        }
    }
    
    Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    
    Text(
        text = description,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
}

@Composable
private fun StartRideActions(
    onDismiss: () -> Unit,
    onScanQRCode: () -> Unit,
    isLoading: Boolean,
    buttonColors: ButtonColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isLoading) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
        
        Button(
            onClick = if (isLoading) { {} } else onScanQRCode,
            modifier = Modifier.weight(if (isLoading) 2f else 1f),
            colors = buttonColors,
            enabled = !isLoading
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Processing...",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "Scan QR Code",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 