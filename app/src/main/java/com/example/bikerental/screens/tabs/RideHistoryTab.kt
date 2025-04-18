package com.example.bikerental.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bikerental.models.BikeRide
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.viewmodels.RideHistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RideHistoryTab() {
    // Initialize the ViewModel
    val viewModel: RideHistoryViewModel = viewModel()
    
    // Collect state from ViewModel
    val rideHistory by viewModel.rideHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Pull to refresh functionality
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.fetchUserRideHistory() }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ride History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ColorUtils.DarkGreen
            )
            
            if (error != null) {
                // Error state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to load ride history",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchUserRideHistory() },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorUtils.DarkGreen)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else if (rideHistory.isEmpty() && !isLoading) {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No ride history yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your completed rides will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Ride History Content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        RideHistoryContent(rideHistory, ColorUtils.blackcol())
                    }
                }
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
private fun RideHistoryContent(rides: List<BikeRide>, textColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (rides.isEmpty()) {
            // This condition shouldn't be reached but just in case
            Text(
                text = "Loading ride history...",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        } else {
            rides.forEachIndexed { index, ride ->
                RideHistoryItem(
                    ride = ride,
                    textColor = textColor
                )
                
                if (index < rides.size - 1) {
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun RideHistoryItem(
    ride: BikeRide,
    textColor: Color
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    // Format the date and time
    val startDate = dateFormat.format(Date(ride.startTime))
    val startTime = timeFormat.format(Date(ride.startTime))
    
    // Calculate duration
    val durationMillis = if (ride.endTime > 0) ride.endTime - ride.startTime else 0
    val durationMinutes = durationMillis / (1000 * 60)
    val durationText = "$durationMinutes minutes"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Bike #${ride.bikeId.takeLast(4)}", // Show just the last 4 chars of the bike ID
            style = MaterialTheme.typography.titleMedium,
            color = textColor
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$startDate at $startTime",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₱${ride.cost}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = ride.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.bodyMedium,
                color = when(ride.status.lowercase()) {
                    "completed" -> Color.Green
                    "cancelled" -> Color.Red
                    "active" -> Color.Blue
                    else -> Color.Gray
                }
            )
        }
    }
} 