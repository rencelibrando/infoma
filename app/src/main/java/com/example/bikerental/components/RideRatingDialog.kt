package com.example.bikerental.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideRatingDialog(
    isVisible: Boolean,
    rideDuration: Long,
    distanceTraveled: Float,
    totalCost: Double,
    onRatingSubmit: (rating: Int, feedback: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                RideRatingContent(
                    rideDuration = rideDuration,
                    distanceTraveled = distanceTraveled,
                    totalCost = totalCost,
                    onRatingSubmit = onRatingSubmit,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun RideRatingContent(
    rideDuration: Long,
    distanceTraveled: Float,
    totalCost: Double,
    onRatingSubmit: (rating: Int, feedback: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "Ride Completed!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Ride Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ride Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration:")
                    Text(
                        text = formatDuration(rideDuration),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distance:")
                    Text(
                        text = formatDistance(distanceTraveled),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Cost:")
                    Text(
                        text = "₱${String.format("%.2f", totalCost)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Rating Section
        Text(
            text = "How was your ride?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Star Rating
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { index ->
                IconButton(
                    onClick = { selectedRating = index + 1 },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (index < selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star ${index + 1}",
                        tint = if (index < selectedRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Rating Text
        if (selectedRating > 0) {
            Text(
                text = when (selectedRating) {
                    1 -> "Poor"
                    2 -> "Fair"
                    3 -> "Good"
                    4 -> "Very Good"
                    5 -> "Excellent"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Feedback TextField
        OutlinedTextField(
            value = feedback,
            onValueChange = { feedback = it },
            label = { Text("Additional feedback (optional)") },
            placeholder = { Text("Tell us about your experience...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Skip Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !isSubmitting
            ) {
                Text("Skip")
            }
            
            // Submit Button
            Button(
                onClick = {
                    if (selectedRating > 0) {
                        isSubmitting = true
                        onRatingSubmit(selectedRating, feedback)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedRating > 0 && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit")
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatDistance(distanceInMeters: Float): String {
    return if (distanceInMeters < 1000) {
        "${distanceInMeters.toInt()} m"
    } else {
        String.format("%.2f km", distanceInMeters / 1000)
    }
} 