package com.example.bikerental.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.bikerental.models.BikeRide
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.FormattingUtils
import com.example.bikerental.viewmodels.RideHistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.shape.CircleShape
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RideHistoryTab() {
    // Initialize the ViewModel
    val viewModel: RideHistoryViewModel = viewModel()
    
    // Create dedicated scope for background operations
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    
    // Collect state from ViewModel
    val rideHistory by viewModel.rideHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Pull to refresh functionality
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { 
            ioScope.launch {
                viewModel.fetchUserRideHistory()
            }
        }
    )
    
    // Fetch ride history when component is loaded
    LaunchedEffect(Unit) {
        ioScope.launch {
            viewModel.fetchUserRideHistory()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Header with summary stats
            CompactRideHistoryHeader(rideHistory)
            
            if (error != null) {
                // Error state
                CompactErrorCard(
                    error = error ?: "Unknown error",
                    onRetry = { viewModel.fetchUserRideHistory() }
                )
            } else if (rideHistory.isEmpty() && !isLoading) {
                // Empty state
                CompactEmptyStateCard()
            } else {
                // Ride History Content
                CompactRideHistoryContent(rideHistory)
            }
        }
        
        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = ColorUtils.DarkGreen
        )
    }
}

@Composable
private fun CompactRideHistoryHeader(rides: List<BikeRide>) {
    val totalRides = rides.size
    val completedRides = rides.count { it.status == "completed" }
    val totalDistance = rides.sumOf { it.distanceTraveled }.toFloat()
    val totalCost = rides.sumOf { it.cost }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorUtils.DarkGreen.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ride History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorUtils.DarkGreen
                )
                
                // Live indicator for any active rides
                val hasActiveRides = rides.any { it.status == "active" }
                if (hasActiveRides) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            
            if (totalRides > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Compact summary stats in 2 rows
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompactSummaryStatCard(
                            icon = Icons.Default.DirectionsBike,
                            label = "Rides",
                            value = totalRides.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        CompactSummaryStatCard(
                            icon = Icons.Default.CheckCircle,
                            label = "Done",
                            value = completedRides.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompactSummaryStatCard(
                            icon = Icons.Default.Route,
                            label = "Distance",
                            value = FormattingUtils.formatDistance(totalDistance),
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        CompactSummaryStatCard(
                            icon = Icons.Default.AttachMoney,
                            label = "Cost",
                            value = FormattingUtils.formatCost(totalCost),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorUtils.DarkGreen,
                modifier = Modifier.size(18.dp)
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Unable to load ride history",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CompactEmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBike,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "No rides yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Start your first bike adventure!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompactRideHistoryContent(rides: List<BikeRide>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rides.forEach { ride ->
            CompactRideHistoryItem(ride = ride)
        }
    }
}

@Composable
private fun CompactRideHistoryItem(ride: BikeRide) {
    // Use remember and launched effect to format dates in background
    val coroutineScope = rememberCoroutineScope()
    var formattedDate by remember { mutableStateOf("") }
    var formattedTime by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    
    // Process date formatting in background
    LaunchedEffect(ride) {
        coroutineScope.launch(Dispatchers.Default) {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val dateStr = dateFormat.format(Date(ride.startTime))
            val timeStr = timeFormat.format(Date(ride.startTime))
            
            // Calculate duration
            val durationMillis = if (ride.endTime > 0) ride.endTime - ride.startTime else System.currentTimeMillis() - ride.startTime
            val formattedDuration = FormattingUtils.formatDuration(durationMillis)
            
            // Update UI state on main thread
            withContext(Dispatchers.Main) {
                formattedDate = dateStr
                formattedTime = timeStr
                durationText = formattedDuration
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (ride.status) {
                "active" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                "completed" -> MaterialTheme.colorScheme.surface
                "cancelled" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Compact header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = null,
                        tint = ColorUtils.DarkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Bike #${ride.bikeId.takeLast(4)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorUtils.DarkGreen,
                        fontSize = 12.sp
                    )
                }
                
                CompactStatusChip(status = ride.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Compact info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$formattedDate at $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                    if (ride.status != "active") {
                        Text(
                            text = "Duration: $durationText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Cost display
                Text(
                    text = FormattingUtils.formatCost(ride.cost),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Compact metrics row - Enhanced speed handling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactRideMetricItem(
                    icon = Icons.Default.Route,
                    label = "Distance",
                    value = FormattingUtils.formatDistance(ride.distanceTraveled.toFloat()),
                    modifier = Modifier.weight(1f)
                )
                
                CompactRideMetricItem(
                    icon = Icons.Default.Speed,
                    label = "Max Speed",
                    value = formatSpeedWithFallback(ride.maxSpeed, ride.distanceTraveled, ride.getRideDuration()),
                    modifier = Modifier.weight(1f)
                )
                
                CompactRideMetricItem(
                    icon = Icons.Default.TrendingUp,
                    label = "Avg Speed", 
                    value = formatAverageSpeedWithCalculation(ride.averageSpeed, ride.distanceTraveled, ride.getRideDuration()),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Enhanced speed formatting with fallback calculations
private fun formatSpeedWithFallback(maxSpeed: Double, distance: Double, duration: Long): String {
    return when {
        maxSpeed > 0 -> FormattingUtils.formatSpeed(maxSpeed.toFloat())
        distance > 0 && duration > 0 -> {
            // Estimate max speed as 1.5x average speed if not recorded
            val avgSpeed = (distance / 1000.0) / (duration / 3600000.0) // km/h
            val estimatedMaxSpeed = (avgSpeed * 1.5f).toFloat()
            "~${FormattingUtils.formatSpeed(estimatedMaxSpeed)}"
        }
        else -> "N/A"
    }
}

private fun formatAverageSpeedWithCalculation(averageSpeed: Double, distance: Double, duration: Long): String {
    return when {
        averageSpeed > 0 -> FormattingUtils.formatSpeed(averageSpeed.toFloat())
        distance > 0 && duration > 0 -> {
            // Calculate average speed from distance and duration
            val calculatedSpeed = (distance / 1000.0) / (duration / 3600000.0) // km/h
            FormattingUtils.formatSpeed(calculatedSpeed.toFloat())
        }
        else -> "N/A"
    }
}

@Composable
private fun CompactStatusChip(status: String) {
    val (backgroundColor, contentColor, displayText) = when (status.lowercase()) {
        "active" -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "LIVE"
        )
        "completed" -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            "DONE"
        )
        "cancelled" -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "CANCEL"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface,
            status.uppercase()
        )
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CompactRideMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Helper data class for quadruple values
private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) 