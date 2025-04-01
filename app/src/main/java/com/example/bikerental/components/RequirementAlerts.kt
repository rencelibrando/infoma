package com.example.bikerental.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikerental.utils.ConnectionUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.LocationOff

@Composable
fun RequirementsWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var showNoInternetAlert by remember { mutableStateOf(false) }
    var showNoGpsAlert by remember { mutableStateOf(false) }
    
    val hasInternet = ConnectionUtils.isInternetAvailable(context)
    val hasGps = ConnectionUtils.isGpsEnabled(context)
    
    if (!hasInternet || !hasGps) {
        RequirementsBlocker(
            hasInternet = hasInternet,
            hasGps = hasGps,
            onInternetClick = { showNoInternetAlert = true },
            onGpsClick = { showNoGpsAlert = true }
        )
    } else {
        content()
    }
    
    if (showNoInternetAlert) {
        NoInternetAlert(onDismiss = { showNoInternetAlert = false })
    }
    
    if (showNoGpsAlert) {
        NoGpsAlert(onDismiss = { showNoGpsAlert = false })
    }
}

@Composable
private fun RequirementsBlocker(
    hasInternet: Boolean,
    hasGps: Boolean,
    onInternetClick: () -> Unit,
    onGpsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "App Features Disabled",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please enable the following to use the app:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!hasInternet) {
                RequirementButton(
                    text = "Enable Internet Connection",
                    icon = Icons.Default.WifiOff,
                    onClick = onInternetClick
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (!hasGps) {
                RequirementButton(
                    text = "Enable Location Services",
                    icon = Icons.Default.LocationOff,
                    onClick = onGpsClick
                )
            }
        }
    }
}

@Composable
private fun RequirementButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(text = text)
        }
    }
}

@Composable
fun NoInternetAlert(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "No Internet Connection",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "This app requires an active internet connection to function properly. " +
                    "Please check your WiFi or mobile data connection and try again."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun NoGpsAlert(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Location Services Disabled",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "This app requires GPS location services to be enabled to show nearby bikes " +
                    "and provide navigation features. Please enable location services to continue."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            ) {
                Text("Enable GPS")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun RequirementsNotMetBanner(
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = message)
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(actionLabel)
            }
        }
    }
} 