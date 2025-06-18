package com.example.bikerental.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bikerental.navigation.Screen
import com.example.bikerental.utils.ColorUtils

/**
 * A card component that displays the current ID verification status
 * and provides relevant actions based on that status
 */
@Composable
fun IdVerificationCard(
    status: String,
    idType: String? = null,
    note: String? = null,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Define colors based on verification status
    val (backgroundColor, contentColor) = when (status) {
        "verified" -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        "pending" -> Pair(Color(0xFFFFF3E0), Color(0xFFF57C00)) // Light orange / Orange
        "rejected" -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon based on status
            Icon(
                imageVector = when (status) {
                    "verified" -> Icons.Default.CheckCircle
                    "pending" -> Icons.Default.HourglassTop
                    "rejected" -> Icons.Default.Error
                    else -> Icons.Default.Badge
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = contentColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title based on status
            Text(
                text = when (status) {
                    "verified" -> "ID Verified"
                    "pending" -> "Verification in Progress"
                    "rejected" -> "Verification Failed"
                    else -> "ID Verification Required"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display ID type if available
            if (status == "verified" && !idType.isNullOrEmpty()) {
                Text(
                    text = "Verified ID: $idType",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Status message
            Text(
                text = when (status) {
                    "verified" -> "Your ID has been verified. You now have full access to all features."
                    "pending" -> "Your ID is being reviewed. This process usually takes 1-2 business days."
                    "rejected" -> note ?: "Your ID verification was rejected. Please submit a clearer image that meets our requirements."
                    else -> "Please verify your identity by uploading a valid government ID to unlock all features."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = contentColor.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action button based on status
            Button(
                onClick = { navController.navigate(Screen.IdVerification.route) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = contentColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when (status) {
                        "verified" -> "View Verification"
                        "pending" -> "Check Status"
                        "rejected" -> "Resubmit ID"
                        else -> "Verify Now"
                    },
                    color = if (status == "pending") Color.White else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * A smaller, more compact version of the ID verification card
 * for use in lists or where space is limited
 */
@Composable
fun CompactIdVerificationCard(
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Define colors based on verification status
    val (backgroundColor, contentColor) = when (status) {
        "verified" -> Pair(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary)
        "pending" -> Pair(Color(0xFFFFF3E0).copy(alpha = 0.3f), Color(0xFFF57C00)) // Light orange / Orange
        "rejected" -> Pair(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.error)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (status) {
                    "verified" -> Icons.Default.CheckCircle
                    "pending" -> Icons.Default.HourglassTop
                    "rejected" -> Icons.Default.Error
                    else -> Icons.Default.Badge
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (status) {
                        "verified" -> "ID Verified"
                        "pending" -> "Verification in Progress"
                        "rejected" -> "Verification Failed"
                        else -> "Verification Required"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                
                Text(
                    text = when (status) {
                        "verified" -> "Your ID has been verified"
                        "pending" -> "Review in progress"
                        "rejected" -> "Action needed"
                        else -> "Please verify your ID"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
} 