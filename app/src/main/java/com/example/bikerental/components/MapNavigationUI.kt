package com.example.bikerental.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bikerental.ui.theme.RouteInfo
import com.example.bikerental.utils.FormattingUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay

/**
 * A composable that displays turn-by-turn navigation instructions during a ride
 * with updated UI design to match the DestinationSearchDialog
 */
@Composable
fun MapNavigationUI(
    isVisible: Boolean,
    currentRoute: RouteInfo?,
    currentLocation: LatLng?,
    currentSpeed: Float,
    currentStep: Int,
    distanceToNextTurn: Int,
    timeRemaining: String,
    onStepChanged: (Int) -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible && currentRoute != null && currentLocation != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -200 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -200 })
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top navigation card with updated styling
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                var showDetails by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Get current step data
                    val currentStepData = currentRoute?.steps?.getOrNull(currentStep)
                    
                    if (currentStepData != null) {
                        // Header with navigation status and expand/collapse
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDetails = !showDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Navigation icon
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getManeuverIcon(currentStepData.maneuver),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Column {
                                    Text(
                                        text = currentStepData.instruction,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Text(
                                        text = "In ${formatDistance(distanceToNextTurn)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Estimated time
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                
                                Text(
                                    text = timeRemaining,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Icon(
                                    imageVector = if (showDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = if (showDetails) "Show less" else "Show more",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // Expandable details
                        AnimatedVisibility(
                            visible = showDetails,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                
                                // Progress to next turn
                                NavigationProgressBar(
                                    currentStep = currentStep,
                                    totalSteps = currentRoute.steps.size,
                                    distanceToNextTurn = distanceToNextTurn,
                                    currentStepDistance = currentStepData.distanceValue
                                )
                                
                                // Navigation metrics in card row format
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        NavigationMetricCard(
                                            icon = Icons.Default.DirectionsBike,
                                            value = "${currentSpeed.toInt()} km/h",
                                            label = "Speed"
                                        )
                                    }
                                    
                                    item {
                                        NavigationMetricCard(
                                            icon = Icons.Default.Schedule,
                                            value = currentRoute.duration,
                                            label = "Total"
                                        )
                                    }
                                    
                                    item {
                                        NavigationMetricCard(
                                            icon = Icons.Default.NearMe,
                                            value = currentRoute.distance,
                                            label = "Distance"
                                        )
                                    }
                                }
                                
                                // Steps preview (showing next steps)
                                if (currentRoute.steps.size > 1) {
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    Text(
                                        text = "Upcoming Steps",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    UpcomingSteps(
                                        steps = currentRoute.steps,
                                        currentStep = currentStep,
                                        onStepSelected = onStepChanged
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingSteps(
    steps: List<RouteInfo.Step>,
    currentStep: Int,
    onStepSelected: (Int) -> Unit
) {
    val displaySteps = steps.drop(currentStep).take(3) // Show current + 2 next steps
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(displaySteps) { index, step ->
            val stepIndex = currentStep + index
            val isCurrentStep = index == 0
            
            OutlinedCard(
                modifier = Modifier.width(140.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isCurrentStep) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isCurrentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentStep) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getManeuverIcon(step.maneuver),
                            contentDescription = null,
                            tint = if (isCurrentStep) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isCurrentStep) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = step.distance,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationMetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.width(100.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Custom progress bar for navigation
 */
@Composable
fun NavigationProgressBar(
    currentStep: Int,
    totalSteps: Int,
    distanceToNextTurn: Int,
    currentStepDistance: Int
) {
    val progress = remember(distanceToNextTurn, currentStepDistance) {
        if (currentStepDistance <= 0) 0f else
            1f - (distanceToNextTurn.toFloat() / currentStepDistance.toFloat()).coerceIn(0f, 1f)
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "NavigationProgress"
    )
    
    Column {
        // Text indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

/**
 * Get icon for navigation maneuver
 */
@Composable
fun getManeuverIcon(maneuver: String): ImageVector {
    return when (maneuver) {
        "turn-left" -> Icons.Default.TurnLeft
        "turn-right" -> Icons.Default.TurnRight
        "slight-left" -> Icons.Default.TurnSlightLeft
        "slight-right" -> Icons.Default.TurnSlightRight
        "arrive" -> Icons.Default.Close
        "uturn" -> Icons.Default.ArrowForward
        "sharp-left" -> Icons.Default.TurnLeft
        "sharp-right" -> Icons.Default.TurnRight
        "roundabout" -> Icons.Default.ArrowForward
        "straight" -> Icons.Default.ArrowForward
        else -> Icons.Default.ArrowForward
    }
}

/**
 * Format distance in meters to a human-readable string
 */
private fun formatDistance(meters: Int): String {
    return when {
        meters < 50 -> "now"
        meters < 1000 -> "$meters meters"
        else -> "${String.format("%.1f", meters / 1000f)} km"
    }
} 