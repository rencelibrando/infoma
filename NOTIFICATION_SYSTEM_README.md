# Enhanced Real-Time Notification System

## Overview

The Bike Rental app now features a comprehensive, real-time notification system that provides users with timely updates about their bookings, payments, admin messages, and other important events. The system is built on Firebase Firestore real-time listeners and supports various notification types with priority-based handling.

## Key Features

### 🚀 New Notification Types
- **UNPAID_BOOKING** - High priority notifications for payment requirements
- **PAYMENT_SUCCESS** - Confirmations for successful payments
- **ADMIN_REPLY** - Admin responses to user inquiries
- **PAYMENT_APPROVAL** - Payment approval confirmations
- **BOOKING_APPROVAL** - Booking approval notifications

### 📱 Enhanced UI Features
- **Filter Chips** - Filter notifications by type (All, Unread, Payments, Bookings, Admin, High Priority)
- **Priority Indicators** - Visual indicators for notification importance
- **Smart Sorting** - Notifications sorted by priority and timestamp
- **Real-time Updates** - Live updates without app refresh

### ⚡ Real-Time Monitoring
- **Firebase Listeners** - Automatic monitoring of booking, payment, and message changes
- **Instant Notifications** - Real-time alerts for critical events
- **Battery Efficient** - Optimized listeners that start/stop with user authentication

## Architecture

### Core Components

#### 1. NotificationType Enum
```kotlin
enum class NotificationType(
    val displayName: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val borderColor: Color,
    val iconColor: Color,
    val iconBackground: Color
)
```

**Available Types:**
- `GENERAL` - General app notifications
- `RIDE_COMPLETE` - Ride completion confirmations
- `UNPAID_PAYMENT` - Payment reminders
- `ADMIN_MESSAGE` - Admin announcements
- `BOOKING_CONFIRMATION` - Booking confirmations
- `EMAIL_VERIFICATION` - Email verification status
- `UNPAID_BOOKING` - ⚠️ High priority payment requirements
- `PAYMENT_SUCCESS` - ✅ Payment confirmations
- `ADMIN_REPLY` - 💬 Admin responses
- `PAYMENT_APPROVAL` - ✅ Payment approvals
- `BOOKING_APPROVAL` - ✅ Booking approvals

#### 2. NotificationPriority System
```kotlin
enum class NotificationPriority(val value: Int) {
    LOW(1),      // Non-urgent information
    NORMAL(2),   // Standard notifications
    HIGH(3),     // Important updates
    URGENT(4)    // Critical alerts
}
```

#### 3. Enhanced Notification Model
```kotlin
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionText: String? = null,
    val actionData: Map<String, Any> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.NORMAL
)
```

### Service Layer

#### NotificationService
Real-time monitoring service that watches Firebase collections:

```kotlin
@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationUtils: NotificationUtils
) {
    fun startMonitoring()  // Start real-time listeners
    fun stopMonitoring()   // Clean up listeners
}
```

**Monitored Collections:**
- `bookings` - Booking status changes and new bookings
- `payments` - Payment status updates
- `admin_messages` - New admin messages
- `support_messages` - Support reply notifications

#### NotificationUtils
Utility functions for sending different notification types:

```kotlin
// New notification functions
suspend fun sendUnpaidBookingNotification(userId: String, bookingId: String, amount: Double, dueDate: Long, bikeModel: String)
suspend fun sendPaymentSuccessNotification(userId: String, transactionId: String, amount: Double, paymentMethod: String)
suspend fun sendAdminReplyNotification(userId: String, messageId: String, adminName: String, replyPreview: String)
suspend fun sendPaymentApprovalNotification(userId: String, transactionId: String, approverName: String)
suspend fun sendBookingApprovalNotification(userId: String, bookingId: String, bikeModel: String, bookingDate: String, bookingTime: String, location: String)

// Enhanced functions
suspend fun sendBulkNotifications(userIds: List<String>, type: NotificationType, title: String, message: String, priority: NotificationPriority = NotificationPriority.NORMAL)
suspend fun sendNotificationAsync(userId: String, type: NotificationType, title: String, message: String, priority: NotificationPriority = NotificationPriority.NORMAL, actionText: String? = null, actionData: Map<String, Any> = emptyMap())
```

### UI Layer

#### Enhanced NotificationsTab
- **Filter System** - Filter chips for different notification types
- **Priority Sorting** - Notifications sorted by priority and timestamp
- **Smart Actions** - Context-aware actions based on notification type
- **Real-time Updates** - Live UI updates via StateFlow

#### NotificationViewModel
Enhanced with:
- Priority-based filtering and sorting
- Real-time state management
- Enhanced action handling for new notification types

```kotlin
// New filtering capabilities
fun setFilter(filter: NotificationFilter)
private fun filterNotifications(notifications: List<Notification>, filter: NotificationFilter): List<Notification>

// Enhanced action handling
private fun handleNotificationAction(notification: Notification)
```

## Integration Guide

### 1. Automatic Integration
The notification system automatically starts when a user logs in:

```kotlin
// In MainActivity.kt
override fun onStart() {
    super.onStart()
    
    authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            notificationService.startMonitoring() // ✅ Auto-start
        } else {
            notificationService.stopMonitoring()  // ✅ Auto-stop
        }
    }
}
```

### 2. Manual Notification Sending
```kotlin
// Inject NotificationUtils
@Inject lateinit var notificationUtils: NotificationUtils

// Send specific notification types
notificationUtils.sendUnpaidBookingNotification(
    userId = currentUser.uid,
    bookingId = "booking_123",
    amount = 150.00,
    dueDate = System.currentTimeMillis() + 86400000, // 24 hours
    bikeModel = "Mountain Explorer Pro"
)
```

### 3. Custom Notification Handling
```kotlin
// In your ViewModel or Repository
notificationUtils.sendNotificationAsync(
    userId = userId,
    type = NotificationType.CUSTOM_TYPE,
    title = "Custom Title",
    message = "Custom message",
    priority = NotificationPriority.HIGH,
    actionText = "View Details",
    actionData = mapOf("customId" to "123")
)
```

## Real-Time Event Triggers

### Automatic Notifications
The system automatically sends notifications for:

1. **Booking Events**
   - New booking created → `BOOKING_CONFIRMATION`
   - Booking approved → `BOOKING_APPROVAL`
   - Payment overdue → `UNPAID_BOOKING`

2. **Payment Events**
   - Payment successful → `PAYMENT_SUCCESS`
   - Payment approved → `PAYMENT_APPROVAL`
   - Payment reminder → `UNPAID_PAYMENT`

3. **Admin Events**
   - New admin message → `ADMIN_MESSAGE`
   - Admin reply to support ticket → `ADMIN_REPLY`

### Firebase Collection Structure
```
bookings/{bookingId}
├── userId: string
├── status: string ("pending", "approved", "active", "completed")
├── paymentStatus: string ("pending", "paid", "overdue")
├── bikeModel: string
├── amount: number
└── timestamps: object

payments/{paymentId}
├── userId: string
├── status: string ("pending", "completed", "failed")
├── amount: number
├── method: string
└── timestamps: object

admin_messages/{messageId}
├── userId: string (or "all" for broadcast)
├── title: string
├── message: string
├── priority: string
└── timestamp: number

support_messages/{messageId}
├── userId: string
├── hasAdminReply: boolean
├── lastReply: object
└── timestamp: number
```

## Testing

### Integration Demo
Use the `NotificationIntegrationDemo` class to test all features:

```kotlin
@Inject lateinit var demo: NotificationIntegrationDemo

// Test all notification types
demo.runComprehensiveDemo(viewModelScope)

// Test specific features
demo.demonstrateNewNotificationTypes()
demo.demonstratePrioritySystem()
demo.demonstrateBulkNotifications()
demo.demonstrateRealTimeMonitoring()
```

### Sample Notifications
The system includes comprehensive sample notifications for testing:

```kotlin
// In NotificationRepository.kt
private suspend fun createSampleNotifications() {
    // Creates sample notifications for all types including:
    // - Priority notifications (UNPAID_BOOKING, PAYMENT_SUCCESS, etc.)
    // - Legacy notifications with priority support
    // - High-priority urgent samples
}
```

## Performance Considerations

### Optimizations Implemented
1. **Efficient Listeners** - Listeners start/stop with authentication
2. **Priority Sorting** - Smart sorting reduces UI recomposition
3. **Batch Operations** - Bulk notification support
4. **Memory Management** - Proper cleanup of listeners and coroutines
5. **Battery Efficiency** - Minimal background processing

### Best Practices
- Use appropriate priority levels to avoid notification fatigue
- Implement proper error handling for Firebase operations
- Test with airplane mode to ensure graceful degradation
- Monitor Firebase quota usage for high-traffic scenarios

## Migration Guide

### From Previous Version
The enhanced system is backward compatible. Existing notifications will:
- Automatically get `priority = NORMAL`
- Work with existing UI components
- Maintain current functionality

### New Features Adoption
1. Update notification creation to include priority:
```kotlin
// Old way
notificationUtils.sendGeneralNotification(userId, title, message)

// New way (recommended)
notificationUtils.sendNotificationAsync(userId, type, title, message, priority)
```

2. Use new notification types:
```kotlin
// Replace generic notifications with specific types
notificationUtils.sendPaymentSuccessNotification(userId, transactionId, amount, method)
```

## Troubleshooting

### Common Issues

#### Real-time Updates Not Working
1. Check Firebase authentication status
2. Verify Firestore security rules
3. Ensure NotificationService is properly injected
4. Check network connectivity

#### Notifications Not Appearing
1. Verify user permissions for notifications
2. Check notification filter settings
3. Ensure proper notification type assignment
4. Verify Firebase collection structure

#### Performance Issues
1. Monitor the number of active listeners
2. Check notification frequency and batch operations
3. Verify proper cleanup in onDestroy()
4. Monitor memory usage with large notification lists

### Debug Logging
Enable comprehensive logging:
```kotlin
// In BikeRentalApplication.kt
LogManager.d(TAG, "Notification system status")

// In NotificationService.kt
Log.d(TAG, "Starting monitoring for collections")

// In NotificationViewModel.kt
Log.d(TAG, "Processing notification: ${notification.type}")
```

## Future Enhancements

### Planned Features
- Push notification integration (FCM)
- In-app notification sounds and vibrations
- Notification scheduling and delayed delivery
- Advanced filtering and search capabilities
- Notification analytics and metrics

### Contributing
When adding new notification types:
1. Add to `NotificationType` enum with appropriate styling
2. Create utility function in `NotificationUtils`
3. Add handling in `NotificationViewModel.handleNotificationAction`
4. Update `NotificationIntegrationDemo` with test case
5. Add real-time trigger in `NotificationService` if needed

## Support

For technical issues or questions about the notification system:
1. Check this documentation first
2. Review the integration demo code
3. Test with the provided sample notifications
4. Contact the development team with specific error logs

---

**Version:** 2.0.0  
**Last Updated:** January 2024  
**Compatibility:** Android API 24+, Firebase SDK 32+ 