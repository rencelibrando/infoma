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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.bikerental.ui.theme.BikerentalTheme
import com.example.bikerental.viewmodels.NotificationViewModel
import kotlinx.coroutines.launch

@Composable
fun NotificationsTab(
    navController: NavController? = null,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                onCreateSample = { viewModel.createSampleNotifications() },
                onClearAll = { viewModel.clearAllNotifications() },
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content Section
            if (uiState.isLoading && uiState.notifications.isEmpty()) {
                LoadingState()
            } else if (uiState.notifications.isEmpty()) {
                EmptyState(onCreateSample = { viewModel.createSampleNotifications() })
            } else {
                NotificationsList(
                    notifications = uiState.notifications,
                    onNotificationClick = { notification ->
                        if (!notification.isRead) {
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
    onCreateSample: () -> Unit,
    onClearAll: () -> Unit,
    isLoading: Boolean
) {
    Column {
        // Notifications counter and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Notifications ($notificationCount)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )

            if (notificationCount > 0) {
                Row {
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
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
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

        Text(
            text = "No new notifications",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF6B7280)
            )
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
                        // Icon
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
                                        color = Color(0xFF6B7280)
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
                if (notification.isActionable && notification.actionText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (notification.type) {
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
                if (!notification.isRead) {
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