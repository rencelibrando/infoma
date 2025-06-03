package com.example.bikerental.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun RideRatingDialog(
    isVisible: Boolean,
    rideDistance: Float,
    rideDuration: Long,
    rideCost: Double,
    onRatingSubmit: (Int, String) -> Unit,
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
            RideRatingContent(
                rideDistance = rideDistance,
                rideDuration = rideDuration,
                rideCost = rideCost,
                onRatingSubmit = onRatingSubmit,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun RideRatingContent(
    rideDistance: Float,
    rideDuration: Long,
    rideCost: Double,
    onRatingSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Ride Completed!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            // Ride Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RideSummaryItem(
                        label = "Distance",
                        value = String.format("%.2f km", rideDistance / 1000)
                    )
                    RideSummaryItem(
                        label = "Duration",
                        value = formatDuration(rideDuration)
                    )
                    RideSummaryItem(
                        label = "Total Cost",
                        value = String.format("$%.2f", rideCost)
                    )
                }
            }
            
            // Rating Section
            Text(
                text = "How was your ride?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Star Rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    Icon(
                        imageVector = if (starIndex <= selectedRating) {
                            Icons.Filled.Star
                        } else {
                            Icons.Outlined.Star
                        },
                        contentDescription = "Star $starIndex",
                        tint = if (starIndex <= selectedRating) {
                            Color(0xFFFFD700) // Gold color
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                selectedRating = starIndex
                            }
                    )
                }
            }
            
            // Rating Labels
            if (selectedRating > 0) {
                Text(
                    text = getRatingLabel(selectedRating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Feedback Section
            Text(
                text = "Additional Feedback (Optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
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
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Skip")
                }
                
                Button(
                    onClick = {
                        if (selectedRating > 0) {
                            onRatingSubmit(selectedRating, feedback)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedRating > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
private fun RideSummaryItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getRatingLabel(rating: Int): String {
    return when (rating) {
        1 -> "Poor"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Very Good"
        5 -> "Excellent"
        else -> ""
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
} 