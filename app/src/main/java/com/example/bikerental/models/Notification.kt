package com.example.bikerental.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Notification model representing different types of notifications in the Bambike app
 */
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val title: String = "",
    val message: String = "",
    val actionText: String = "",
    val actionData: Map<String, Any> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val isActionable: Boolean = true,
    val priority: NotificationPriority = NotificationPriority.NORMAL
) {
    constructor() : this(
        id = "",
        userId = "",
        type = NotificationType.GENERAL,
        title = "",
        message = "",
        actionText = "",
        actionData = emptyMap(),
        timestamp = Timestamp.now(),
        isRead = false,
        isActionable = true,
        priority = NotificationPriority.NORMAL
    )
    
    /**
     * Get formatted timestamp for display
     */
    fun getFormattedTime(): String {
        val now = Date()
        val notificationTime = timestamp.toDate()
        val diffInMillis = now.time - notificationTime.time
        
        return when {
            diffInMillis < 60_000 -> "Just now"
            diffInMillis < 3600_000 -> "${diffInMillis / 60_000} min ago"
            diffInMillis < 86400_000 -> "${diffInMillis / 3600_000} hour${if (diffInMillis / 3600_000 > 1) "s" else ""} ago"
            diffInMillis < 604800_000 -> "${diffInMillis / 86400_000} day${if (diffInMillis / 86400_000 > 1) "s" else ""} ago"
            else -> "${diffInMillis / 604800_000} week${if (diffInMillis / 604800_000 > 1) "s" else ""} ago"
        }
    }
}

/**
 * Enum class representing notification priority levels
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Enum class representing different notification types
 */
enum class NotificationType(
    val displayName: String,
    val icon: ImageVector,
    val bgColor: Color,
    val borderColor: Color,
    val iconColor: Color,
    val iconBg: Color
) {
    // User Journey Notifications
    UNPAID_BOOKING(
        displayName = "Unpaid Booking",
        icon = Icons.Default.Warning,
        bgColor = Color.White,
        borderColor = Color(0xFFEF4444),
        iconColor = Color(0xFFDC2626),
        iconBg = Color(0xFFFEF2F2)
    ),
    PAYMENT_SUCCESS(
        displayName = "Payment Successful",
        icon = Icons.Default.Payment,
        bgColor = Color.White,
        borderColor = Color(0xFF10B981),
        iconColor = Color(0xFF059669),
        iconBg = Color(0xFFECFDF5)
    ),
    ADMIN_REPLY(
        displayName = "Admin Reply",
        icon = Icons.Default.Reply,
        bgColor = Color.White,
        borderColor = Color(0xFF3B82F6),
        iconColor = Color(0xFF2563EB),
        iconBg = Color(0xFFEFF6FF)
    ),
    PAYMENT_APPROVAL(
        displayName = "Payment Approved",
        icon = Icons.Default.Verified,
        bgColor = Color.White,
        borderColor = Color(0xFF10B981),
        iconColor = Color(0xFF059669),
        iconBg = Color(0xFFECFDF5)
    ),
    BOOKING_APPROVAL(
        displayName = "Booking Approved",
        icon = Icons.Default.EventAvailable,
        bgColor = Color.White,
        borderColor = Color(0xFF10B981),
        iconColor = Color(0xFF059669),
        iconBg = Color(0xFFECFDF5)
    ),
    
    // Legacy and General Notifications
    RIDE_COMPLETE(
        displayName = "Ride Complete",
        icon = Icons.Default.CheckCircle,
        bgColor = Color.White,
        borderColor = Color(0xFF10B981),
        iconColor = Color(0xFF059669),
        iconBg = Color(0xFFECFDF5)
    ),
    UNPAID_PAYMENT(
        displayName = "Payment Due",
        icon = Icons.Default.Warning,
        bgColor = Color.White,
        borderColor = Color(0xFFEF4444),
        iconColor = Color(0xFFDC2626),
        iconBg = Color(0xFFFEF2F2)
    ),
    ADMIN_MESSAGE(
        displayName = "Admin Message",
        icon = Icons.Default.Shield,
        bgColor = Color.White,
        borderColor = Color(0xFF3B82F6),
        iconColor = Color(0xFF2563EB),
        iconBg = Color(0xFFEFF6FF)
    ),
    EMAIL_VERIFICATION(
        displayName = "Email Verification",
        icon = Icons.Default.Email,
        bgColor = Color.White,
        borderColor = Color(0xFF10B981),
        iconColor = Color(0xFF059669),
        iconBg = Color(0xFFECFDF5)
    ),
    BOOKING_CONFIRMATION(
        displayName = "Booking Confirmed",
        icon = Icons.Default.Bookmark,
        bgColor = Color.White,
        borderColor = Color(0xFF10B981),
        iconColor = Color(0xFF059669),
        iconBg = Color(0xFFECFDF5)
    ),
    BOOKING_REMINDER(
        displayName = "Booking Reminder",
        icon = Icons.Default.Schedule,
        bgColor = Color.White,
        borderColor = Color(0xFFF59E0B),
        iconColor = Color(0xFFD97706),
        iconBg = Color(0xFFFEF3C7)
    ),
    GENERAL(
        displayName = "General",
        icon = Icons.Default.DirectionsBike,
        bgColor = Color.White,
        borderColor = Color(0xFF6B7280),
        iconColor = Color(0xFF4B5563),
        iconBg = Color(0xFFF9FAFB)
    )
}

/**
 * Data class for creating new notifications
 */
data class NotificationRequest(
    val userId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val actionText: String = "",
    val actionData: Map<String, Any> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.NORMAL
) 