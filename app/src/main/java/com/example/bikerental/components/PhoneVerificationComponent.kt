package com.example.bikerental.components

import android.app.Activity
import android.os.Build
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bikerental.R
import com.example.bikerental.models.PhoneAuthState
import com.example.bikerental.viewmodels.PhoneAuthViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * A reusable phone verification component with robust reCAPTCHA handling
 * for different devices and browsers
 */
@Composable
fun PhoneVerification(
    onVerificationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhoneAuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by viewModel.uiState.collectAsState()
    
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var showRetryPrompt by remember { mutableStateOf(false) }
    var showRecaptchaInfo by remember { mutableStateOf(false) }
    var remainingTime by remember { mutableStateOf("") }
    var verificationMethod by remember { mutableStateOf("auto") } // "auto", "sms", "call"
    
    // Detect device and browser info
    val deviceInfo = remember {
        val userAgent = try {
            WebView(context).settings.userAgentString ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
        
        val browserType = when {
            userAgent.contains("Chrome", ignoreCase = true) && 
            !userAgent.contains("Edg", ignoreCase = true) -> "Chrome"
            userAgent.contains("Firefox", ignoreCase = true) -> "Firefox"
            userAgent.contains("Safari", ignoreCase = true) && 
            !userAgent.contains("Chrome", ignoreCase = true) -> "Safari"
            userAgent.contains("Edg", ignoreCase = true) -> "Edge"
            userAgent.contains("OPR", ignoreCase = true) || 
            userAgent.contains("Opera", ignoreCase = true) -> "Opera"
            userAgent.contains("SamsungBrowser", ignoreCase = true) -> "Samsung Browser"
            else -> "Other Browser"
        }
        
        "$browserType on Android ${Build.VERSION.RELEASE}"
    }
    
    // Format remaining time for rate limiting
    LaunchedEffect(uiState) {
        if (uiState is PhoneAuthState.RateLimited) {
            val rateLimited = uiState as PhoneAuthState.RateLimited
            val expiryTime = rateLimited.expireTimeMillis
            
            // Always start with exactly 3:00 minutes
            remainingTime = "3:00"
            delay(1.seconds)
            
            // Then continue with actual countdown
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            
            while (System.currentTimeMillis() < expiryTime) {
                val diff = expiryTime - System.currentTimeMillis()
                if (diff <= 0) break
                
                val date = Date(diff)
                val formattedTime = dateFormat.format(date)
                
                // Extract minutes and seconds for better display
                val minutes = formattedTime.substring(0, 2).toInt()
                val seconds = formattedTime.substring(3, 5).toInt()
                
                // Ensure we never show more than 3 minutes
                val displayMinutes = minutes.coerceAtMost(3)
                
                // Format as "3:00" style countdown
                remainingTime = if (displayMinutes > 0) {
                    "$displayMinutes:${seconds.toString().padStart(2, '0')}"
                } else {
                    "0:${seconds.toString().padStart(2, '0')}"
                }
                
                delay(1.seconds)
            }
            
            if (System.currentTimeMillis() >= expiryTime) {
                viewModel.resetState()
            }
        }
    }
    
    // Handle verification success
    LaunchedEffect(uiState) {
        if (uiState is PhoneAuthState.Success) {
            onVerificationComplete()
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (uiState) {
            is PhoneAuthState.Initial -> {
                Text(
                    "Phone Verification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Phone number input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                    placeholder = { Text("+63XXXXXXXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    "Please enter your phone number with the +63 country code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Verification method selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "Verification Method",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VerificationMethodOption(
                            title = "Auto",
                            subtitle = "Recommended",
                            icon = Icons.Default.AutoAwesome,
                            selected = verificationMethod == "auto",
                            onClick = { verificationMethod = "auto" },
                            modifier = Modifier.weight(1f)
                        )
                        
                        VerificationMethodOption(
                            title = "SMS",
                            subtitle = "Text message",
                            icon = Icons.Default.Sms,
                            selected = verificationMethod == "sms",
                            onClick = { verificationMethod = "sms" },
                            modifier = Modifier.weight(1f)
                        )
                        
                        VerificationMethodOption(
                            title = "Call",
                            subtitle = "Phone call",
                            icon = Icons.Default.Call,
                            selected = verificationMethod == "call",
                            onClick = { verificationMethod = "call" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // reCAPTCHA information button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRecaptchaInfo = !showRecaptchaInfo }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        "About Google reCAPTCHA verification",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Icon(
                        if (showRecaptchaInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showRecaptchaInfo) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // reCAPTCHA information
                AnimatedVisibility(
                    visible = showRecaptchaInfo,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Phone verification uses Google reCAPTCHA to ensure you're not a robot:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            BulletPoint("You may see a security challenge popup")
                            BulletPoint("Chrome browser works most reliably")
                            BulletPoint("Disable VPNs, proxies, or ad blockers if verification fails")
                            BulletPoint("If using Firefox or Safari, you may need to try multiple times")
                            BulletPoint("For persistent issues, try a different device")
                            
                            // reCAPTCHA logo
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Protected by ",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Text(
                                    "reCAPTCHA",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Browser information
                Text(
                    "Device: $deviceInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                ResponsiveButton(
                    onClick = {
                        val verificationParams = when (verificationMethod) {
                            "sms" -> mapOf("sms" to true, "call" to false)
                            "call" -> mapOf("sms" to false, "call" to true)
                            else -> null // Auto method
                        }
                        viewModel.startPhoneNumberVerification(phoneNumber, activity)
                    },
                    text = "Send Verification Code",
                    enabled = phoneNumber.isNotBlank()
                )
                
                if (showRetryPrompt) {
                    RecaptchaTroubleshootingCard(
                        onRetry = {
                            viewModel.retryWithoutRecaptcha(phoneNumber, activity)
                            showRetryPrompt = false
                        }
                    )
                }
            }
            
            is PhoneAuthState.CodeSent -> {
                Text(
                    "Enter Verification Code",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "A verification code has been sent to $phoneNumber",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Verification code input
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            verificationCode = it
                        }
                    },
                    label = { Text("Verification Code") },
                    placeholder = { Text("6-digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Number")
                    }
                    
                    Button(
                        onClick = { viewModel.verifyPhoneNumberWithCode(verificationCode) },
                        modifier = Modifier.weight(1f),
                        enabled = verificationCode.length == 6
                    ) {
                        Text("Verify")
                    }
                }
                
                TextButton(
                    onClick = { 
                        viewModel.startPhoneNumberVerification(phoneNumber, activity)
                    }
                ) {
                    Text("Resend Code")
                }
                
                Text(
                    "Didn't receive the code? Try checking your SMS inbox or consider using a different verification method.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            is PhoneAuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                
                Text(
                    "Processing your request...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Text(
                    "You may see a security check popup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            is PhoneAuthState.Error -> {
                val errorMessage = (uiState as PhoneAuthState.Error).message
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Error")
                            Text(
                                "Verification Error", 
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (errorMessage.contains("reCAPTCHA", ignoreCase = true) ||
                            errorMessage.contains("verification", ignoreCase = true)) {
                            Text(
                                "This is likely due to security verification issues. " +
                                "Try a different browser or check your internet connection.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { viewModel.resetState() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Try Again")
                }
                
                OutlinedButton(
                    onClick = { 
                        viewModel.resetState()
                        showRetryPrompt = true
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Show Troubleshooting Options")
                }
            }
            
            is PhoneAuthState.RecaptchaError -> {
                Text(
                    "reCAPTCHA Verification Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                RecaptchaTroubleshootingCard(
                    onRetry = {
                        viewModel.retryWithoutRecaptcha(phoneNumber, activity)
                    },
                    showFullOptions = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Number")
                    }
                    
                    Button(
                        onClick = { 
                            viewModel.retryWithoutRecaptcha(phoneNumber, activity)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retry Verification")
                    }
                }
            }
            
            is PhoneAuthState.RateLimited -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Timer",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Please Wait",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Too many verification attempts. Please try again in:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        // Time remaining counter with larger text
                        Text(
                            remainingTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        
                        Text(
                            "For security reasons, we temporarily limit verification attempts to prevent abuse. " +
                            "You can try again when the timer completes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            else -> {} // Handle other states if needed
        }
    }
}

@Composable
private fun VerificationMethodOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (selected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "• ",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RecaptchaTroubleshootingCard(
    onRetry: () -> Unit,
    showFullOptions: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "reCAPTCHA Verification Issues",
                style = MaterialTheme.typography.titleMedium
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
            )
            
            Text(
                "We're having trouble with the security verification (reCAPTCHA). " +
                "This is common with certain browsers or network settings.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (showFullOptions) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Troubleshooting steps:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                BulletPoint("Try using Chrome browser (most reliable)")
                BulletPoint("Disable VPN, proxy or ad blockers")
                BulletPoint("Check that JavaScript is enabled")
                BulletPoint("Try on cellular data instead of WiFi")
                BulletPoint("Clear browser cookies and cache")
                BulletPoint("Try on a different device")
            } else {
                BulletPoint("Check that your phone number is correct")
                BulletPoint("Try a different browser (Chrome works best)")
                BulletPoint("Ensure you're not using a VPN or proxy")
            }
            
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Verification")
            }
        }
    }
} 