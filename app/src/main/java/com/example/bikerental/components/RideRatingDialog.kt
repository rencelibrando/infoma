package com.example.bikerental.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header - Smaller
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        
        Text(
            text = "Ride Completed!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Ride Summary - More compact
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Ride Summary",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration:", fontSize = 12.sp)
                    Text(
                        text = formatDuration(rideDuration),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distance:", fontSize = 12.sp)
                    Text(
                        text = formatDistance(distanceTraveled),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Cost:", fontSize = 12.sp)
                    Text(
                        text = "₱${String.format("%.2f", totalCost)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Rating Section
        Text(
            text = "How was your ride?",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        // Star Rating - Smaller
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                IconButton(
                    onClick = { selectedRating = index + 1 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (index < selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star ${index + 1}",
                        tint = if (index < selectedRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
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
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Feedback TextField - More compact
        OutlinedTextField(
            value = feedback,
            onValueChange = { feedback = it },
            label = { Text("Feedback (optional)", fontSize = 12.sp) },
            placeholder = { Text("Tell us about your experience...", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            maxLines = 2,
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )
        
        // Action Buttons - More compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Skip Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                enabled = !isSubmitting
            ) {
                Text("Skip", fontSize = 14.sp)
            }
            
            // Submit Button
            Button(
                onClick = {
                    if (selectedRating > 0) {
                        isSubmitting = true
                        onRatingSubmit(selectedRating, feedback)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                enabled = selectedRating > 0 && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit", fontSize = 14.sp)
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