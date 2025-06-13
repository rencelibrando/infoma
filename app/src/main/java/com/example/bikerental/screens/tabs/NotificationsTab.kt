package com.example.bikerental.screens.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.bikerental.models.Notification
import com.example.bikerental.models.NotificationType
import com.example.bikerental.models.NotificationPriority
import com.example.bikerental.ui.theme.BikerentalTheme
import com.example.bikerental.viewmodels.NotificationViewModel
import com.example.bikerental.viewmodels.NotificationFilter
import kotlinx.coroutines.launch

@Composable
fun NotificationsTab(
    navController: NavController? = null,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp)
        ) {
            // Header Section
            NotificationHeader(
                notificationCount = uiState.notifications.size,
                totalCount = uiState.allNotifications.size,
                onCreateSample = { viewModel.createSampleNotifications() },
                onClearAll = { viewModel.clearAllNotifications() },
                onMarkAllRead = { viewModel.markAllAsRead() },
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            NotificationFilters(
                selectedFilter = selectedFilter,
                onFilterSelected = { filter -> viewModel.setFilter(filter) },
                allNotifications = uiState.allNotifications
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content Section
            if (uiState.isLoading && uiState.allNotifications.isEmpty()) {
                LoadingState()
            } else if (uiState.notifications.isEmpty()) {
                EmptyState(
                    filter = selectedFilter,
                    onCreateSample = { viewModel.createSampleNotifications() }
                )
            } else {
                NotificationsList(
                    notifications = uiState.notifications,
                    onNotificationClick = { notification ->
                        if (!notification.read) {
                            viewModel.markAsRead(notification.id)
                        }
                        viewModel.handleNotificationAction(notification)
                    },
                    onNotificationDismiss = { notification ->
                        viewModel.deleteNotification(notification.id)
                    },
                    onActionClick = { notification ->
                        viewModel.handleNotificationAction(notification)
                    }
                )
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun NotificationHeader(
    notificationCount: Int,
    totalCount: Int,
    onCreateSample: () -> Unit,
    onClearAll: () -> Unit,
    onMarkAllRead: () -> Unit,
    isLoading: Boolean
) {
    Column {
        // Notifications counter and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                if (notificationCount != totalCount) {
                    Text(
                        text = "Showing $notificationCount of $totalCount",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }

            if (totalCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notificationCount > 0) {
                        OutlinedButton(
                            onClick = onMarkAllRead,
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF059669)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Mark Read",
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onClearAll,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B7280)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFilters(
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit,
    allNotifications: List<Notification>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NotificationFilter.values()) { filter ->
            val count = when (filter) {
                NotificationFilter.ALL -> allNotifications.size
                NotificationFilter.UNREAD -> allNotifications.count { !it.read }
                NotificationFilter.PAYMENTS -> allNotifications.count { 
                    it.type in listOf(
                        NotificationType.UNPAID_BOOKING,
                        NotificationType.PAYMENT_SUCCESS,
                        NotificationType.PAYMENT_APPROVAL,
                        NotificationType.UNPAID_PAYMENT
                    )
                }
                NotificationFilter.BOOKINGS -> allNotifications.count {
                    it.type in listOf(
                        NotificationType.BOOKING_APPROVAL,
                        NotificationType.BOOKING_CONFIRMATION,
                        NotificationType.BOOKING_REMINDER
                    )
                }
                NotificationFilter.ADMIN -> allNotifications.count {
                    it.type in listOf(
                        NotificationType.ADMIN_REPLY,
                        NotificationType.ADMIN_MESSAGE
                    )
                }
                NotificationFilter.HIGH_PRIORITY -> allNotifications.count {
                    it.priority in listOf(NotificationPriority.HIGH, NotificationPriority.URGENT)
                }
            }
            
            FilterChip(
                onClick = { onFilterSelected(filter) },
                label = { 
                    Text(
                        text = "${filter.displayName} ($count)",
                        fontSize = 12.sp
                    ) 
                },
                selected = selectedFilter == filter,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF059669),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF3F4F6),
                    labelColor = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun EmptyState(
    filter: NotificationFilter,
    onCreateSample: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = Color(0xFFECFDF5),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF10B981),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val emptyMessage = when (filter) {
            NotificationFilter.ALL -> "No notifications yet"
            NotificationFilter.UNREAD -> "No unread notifications"
            NotificationFilter.PAYMENTS -> "No payment notifications"
            NotificationFilter.BOOKINGS -> "No booking notifications"
            NotificationFilter.ADMIN -> "No admin messages"
            NotificationFilter.HIGH_PRIORITY -> "No priority notifications"
        }

        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF6B7280)
            )
        )

        if (filter == NotificationFilter.ALL) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onCreateSample,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF059669)
                )
            ) {
                Text("Create Sample Notifications")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF059669),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading notifications...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun NotificationsList(
    notifications: List<Notification>,
    onNotificationClick: (Notification) -> Unit,
    onNotificationDismiss: (Notification) -> Unit,
    onActionClick: (Notification) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = notifications,
            key = { it.id }
        ) { notification ->
            NotificationCard(
                notification = notification,
                onClick = { onNotificationClick(notification) },
                onDismiss = { onNotificationDismiss(notification) },
                onActionClick = { onActionClick(notification) }
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onActionClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(300, easing = LinearEasing), label = ""
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(700)
        ) + fadeIn(animationSpec = tween(700)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .animateContentSize()
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = notification.type.bgColor
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = notification.type.borderColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
                hoveredElevation = 12.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Icon with priority indicator
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = notification.type.iconBg,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = notification.type.icon,
                                    contentDescription = null,
                                    tint = notification.type.iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Priority indicator
                            if (notification.priority == NotificationPriority.HIGH || 
                                notification.priority == NotificationPriority.URGENT) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = if (notification.priority == NotificationPriority.URGENT) 
                                                Color(0xFFDC2626) else Color(0xFFF59E0B),
                                            shape = CircleShape
                                        )
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Priority",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notification.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = notification.getFormattedTime(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (notification.read) Color(0xFF6B7280) else Color(0xFF1F2937),
                                        fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Medium
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF6B7280),
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }

                    // Dismiss button
                    IconButton(
                        onClick = {
                            isVisible = false
                            // Delay the actual dismiss to allow animation
                            scope.launch {
                                kotlinx.coroutines.delay(300)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Action buttons
                if (notification.actionable && notification.actionText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (notification.type) {
                                    NotificationType.UNPAID_BOOKING -> Color(0xFFDC2626)
                                    NotificationType.PAYMENT_SUCCESS -> Color(0xFF059669)
                                    NotificationType.ADMIN_REPLY -> Color(0xFF2563EB)
                                    NotificationType.PAYMENT_APPROVAL -> Color(0xFF059669)
                                    NotificationType.BOOKING_APPROVAL -> Color(0xFF059669)
                                    NotificationType.RIDE_COMPLETE -> Color(0xFF059669)
                                    NotificationType.UNPAID_PAYMENT -> Color(0xFFDC2626)
                                    NotificationType.ADMIN_MESSAGE -> Color(0xFF2563EB)
                                    else -> Color(0xFF059669)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = notification.actionText,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                isVisible = false
                                scope.launch {
                                    kotlinx.coroutines.delay(300)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6B7280)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Dismiss",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Unread indicator
                if (!notification.read) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = Color(0xFF059669),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsTabPreview() {
    BikerentalTheme {
        NotificationsTab()
    }
} 