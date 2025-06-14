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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF9FAFB),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Header Section
            NotificationHeader(
                notificationCount = uiState.notifications.size,
                totalCount = uiState.allNotifications.size,
                onClearAll = { viewModel.clearAllNotifications() },
                onMarkAllRead = { viewModel.markAllAsRead() },
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            NotificationFilters(
                selectedFilter = selectedFilter,
                onFilterSelected = { filter -> viewModel.setFilter(filter) },
                allNotifications = uiState.allNotifications
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content Section
            if (uiState.isLoading && uiState.allNotifications.isEmpty()) {
                LoadingState()
            } else if (uiState.notifications.isEmpty()) {
                EmptyState(
                    filter = selectedFilter,
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
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun NotificationHeader(
    notificationCount: Int,
    totalCount: Int,
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
                        fontWeight = FontWeight.Bold,
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
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF059669)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Mark Read",
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onClearAll,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B7280)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            maxLines = 1
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
            
            // Custom chip implementation instead of FilterChip
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        color = if (isSelected) Color(0xFF059669) else Color.White
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF059669) else Color(0xFFD1D5DB),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${filter.displayName} ($count)",
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    filter: NotificationFilter
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFECFDF5),
                            Color(0xFFD1FAE5)
                        )
                    ),
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
            NotificationFilter.ALL -> "You're all caught up!"
            NotificationFilter.UNREAD -> "No unread notifications"
            NotificationFilter.PAYMENTS -> "No payment notifications"
            NotificationFilter.BOOKINGS -> "No booking notifications"
            NotificationFilter.ADMIN -> "No admin messages"
            NotificationFilter.HIGH_PRIORITY -> "No priority notifications"
        }

        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "New notifications will appear here.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
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

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (notification.read) Color.White else Color(0xFFF0FDF4)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (notification.read) Color(0xFFE5E7EB) else Color(0xFF86EFAC)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (notification.read) 1.dp else 4.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (notification.read) {
                                listOf(Color.White, Color.White)
                            } else {
                                listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
                            }
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Icon with priority indicator
                    Box(
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            notification.type.iconBg.copy(alpha = 0.9f),
                                            notification.type.iconBg.copy(alpha = 0.7f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = notification.type.iconBg.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = notification.type.icon,
                                contentDescription = null,
                                tint = notification.type.iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        if (notification.priority in listOf(NotificationPriority.HIGH, NotificationPriority.URGENT)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 1.dp, end = 1.dp)
                                    .size(12.dp)
                                    .background(
                                        color = if (notification.priority == NotificationPriority.URGENT)
                                            Color(0xFFDC2626) else Color(0xFFF59E0B),
                                        shape = CircleShape
                                    )
                                    .border(width = 1.5.dp, color = Color.White, shape = CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Content Column
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!notification.read) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = Color(0xFF22C55E),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = notification.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Text(
                                text = notification.getFormattedTime(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val messageText = if (notification.message.contains(" for at ")) {
                            "Your booking has been confirmed. Please check the details for more information."
                        } else {
                            notification.message
                        }

                        Text(
                            text = messageText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF4B5563),
                                lineHeight = 20.sp
                            )
                        )

                        if (notification.actionable && notification.actionText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onActionClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF059669),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 2.dp,
                                        pressedElevation = 0.dp
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Text(
                                        text = notification.actionText, 
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        // Directly call onDismiss without optimistic UI updates
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = Color(0xFFF3F4F6),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        // Directly call onDismiss without optimistic UI updates
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = Color(0xFFF3F4F6),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(16.dp)
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

@Preview(showBackground = true)
@Composable
fun NotificationsTabPreview() {
    BikerentalTheme {
        NotificationsTab()
    }
} 