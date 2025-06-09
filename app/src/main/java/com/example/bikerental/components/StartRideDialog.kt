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
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon - Smaller
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = "Start Ride",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Title - Smaller font
                    Text(
                        text = "Start Your Ride",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Description - More compact
                    Text(
                        text = "Scan the QR code on your bike to unlock and start riding. Stand next to the bike before scanning.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Buttons - More compact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Scan QR Button
                        Button(
                            onClick = {
                                onDismiss()
                                onStartQRScan()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scan QR",
                                fontSize = 14.sp,
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
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StartRideIcon(isLoading = isLoading)
            
            StartRideContent(isLoading = isLoading)
            
            Spacer(modifier = Modifier.height(4.dp))
            
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
            modifier = Modifier.size(40.dp),
            color = ColorUtils.DarkGreen,
            strokeWidth = 3.dp
        )
    } else {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "QR Scanner",
            modifier = Modifier.size(40.dp),
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
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    
    Text(
        text = description,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isLoading) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text("Cancel", fontSize = 14.sp)
            }
        }
        
        Button(
            onClick = if (isLoading) { {} } else onScanQRCode,
            modifier = Modifier
                .weight(if (isLoading) 2f else 1f)
                .height(36.dp),
            colors = buttonColors,
            enabled = !isLoading
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = Color.White,
                        strokeWidth = 1.5.dp
                    )
                    Text(
                        text = "Processing...",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            } else {
                Text(
                    text = "Scan QR Code",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
} 