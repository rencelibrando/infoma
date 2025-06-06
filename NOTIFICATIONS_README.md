# Bambike Notification System

This document provides a comprehensive guide to the notification system implemented in the Bambike mobile app using Jetpack Compose and Firebase.

## Overview

The notification system provides real-time, in-app notifications for various user activities and admin messages. It's designed with a modern UI that matches the provided React.js design specifications.

## Features

### ✅ Supported Notification Types

1. **Ride Complete** - Notifications when a bike ride is finished
2. **Unpaid Payment** - Payment reminders for outstanding amounts
3. **Admin Messages** - System announcements and service updates
4. **Email Verification** - Success notifications for email verification
5. **Booking Confirmation** - Confirmations for bike reservations
6. **Booking Reminders** - Upcoming booking alerts
7. **General** - Miscellaneous notifications

### ✅ Key Features

- **Real-time Updates** - Live notification feed using Firebase Firestore listeners
- **Badge Counts** - Unread notification indicators in navigation
- **Modern UI** - Smooth animations and responsive design
- **Interactive Actions** - Contextual action buttons for each notification type
- **Mark as Read** - Individual and bulk read status management
- **Dismiss & Delete** - Remove notifications with smooth animations
- **Sample Data** - Generate test notifications for development

## Architecture

### Components

```
📁 models/
├── Notification.kt              # Data model for notifications
└── NotificationType.kt         # Enum defining notification categories

📁 repositories/
└── NotificationRepository.kt   # Firebase operations and data management

📁 viewmodels/
└── NotificationViewModel.kt    # UI state management and business logic

📁 screens/tabs/
└── NotificationsTab.kt        # Main notification UI composable

📁 utils/
├── NotificationUtils.kt       # Helper functions for creating notifications
└── firestore-notifications-rules.txt  # Security rules for Firestore

📁 di/
└── RepositoryModule.kt        # Dependency injection setup
```

### Data Flow

```
Firebase Firestore ↔ NotificationRepository ↔ NotificationViewModel ↔ NotificationsTab
                                    ↕
                            NotificationUtils (for creating notifications)
```

## Setup Instructions

### 1. Firebase Firestore Rules

Add the notification rules to your `firestore.rules` file:

```javascript
// Allow users to read/update/delete their own notifications
match /notifications/{notificationId} {
  allow read, update, delete: if request.auth != null && 
                              request.auth.uid == resource.data.userId;
  allow create: if request.auth != null; // Adjust for production
}
```

### 2. Navigation Integration

The notifications tab is integrated into the app navigation:

- **Tab Index**: 6 in the navigation drawer
- **Route**: Available at `Screen.Notifications`
- **Badge**: Shows unread count in real-time

### 3. Dependency Injection

The system uses Hilt for dependency injection. The `NotificationRepository` is automatically provided through the `RepositoryModule`.

## Usage

### For Users

1. **Access Notifications**: Tap the notification icon in the navigation drawer
2. **View Details**: Tap any notification to mark as read and trigger actions
3. **Take Actions**: Use action buttons (Pay Now, View Receipt, etc.)
4. **Dismiss**: Swipe or tap dismiss to remove notifications
5. **Clear All**: Use the "Clear All" button to remove all notifications

### For Developers

#### Creating Notifications

```kotlin
// Using NotificationUtils
notificationUtils.sendRideCompletionNotification(
    userId = currentUserId,
    duration = "32 minutes",
    distance = "6.8 km",
    amount = "₱145.50"
)

// Using Repository directly
val request = NotificationRequest(
    userId = userId,
    type = NotificationType.BOOKING_CONFIRMATION,
    title = "Booking Confirmed",
    message = "Your bike reservation is confirmed.",
    actionText = "View Details"
)
notificationRepository.createNotification(request)
```

#### Listening to Notifications

```kotlin
// In a ViewModel or Composable
notificationRepository.getNotifications()
    .collect { notifications ->
        // Handle notification updates
    }

// Get unread count
notificationRepository.getUnreadCount()
    .collect { count ->
        // Update badge
    }
```

## UI Components

### NotificationsTab

The main screen component with:
- Header with Bambike branding
- Notification counter
- Empty state with sample data generation
- Scrollable notification list
- Loading states

### NotificationCard

Individual notification items featuring:
- Type-specific icons and colors
- Title, message, and timestamp
- Action buttons (when applicable)
- Dismiss functionality
- Read status indicators
- Smooth animations

## Customization

### Adding New Notification Types

1. Add new enum to `NotificationType`:
```kotlin
NEW_TYPE(
    displayName = "New Type",
    icon = Icons.Default.NewIcon,
    bgColor = Color.White,
    borderColor = Color(0xFF10B981),
    iconColor = Color(0xFF059669),
    iconBg = Color(0xFFECFDF5)
)
```

2. Add handling in `NotificationViewModel.handleNotificationAction()`

3. Create helper function in `NotificationUtils`

### Styling

Colors and styling are defined in the `NotificationType` enum. Modify the enum values to change:
- Background colors
- Border colors
- Icon colors
- Icon backgrounds

## Integration Points

### With Other App Features

1. **Ride Completion**: Automatically triggered when rides end
2. **Payment System**: Sends reminders for unpaid amounts
3. **Booking System**: Confirmations and reminders
4. **User Authentication**: Email verification success
5. **Admin Panel**: Service announcements

### Example Integration

```kotlin
// In ride completion logic
viewModelScope.launch {
    notificationUtils.sendRideCompletionNotification(
        userId = currentUser.uid,
        duration = rideDuration,
        distance = rideDistance,
        amount = totalAmount
    )
}
```

## Testing

### Sample Data Generation

Use the "Create Sample Notifications" feature to generate test data:

```kotlin
viewModel.createSampleNotifications()
```

This creates 5 different notification types for testing UI and functionality.

### Manual Testing Checklist

- [ ] Notification appears in real-time
- [ ] Badge count updates correctly
- [ ] Mark as read functionality works
- [ ] Action buttons trigger correct behavior
- [ ] Dismiss animations work smoothly
- [ ] Empty state displays correctly
- [ ] Loading states work properly

## Performance Considerations

1. **Pagination**: Limited to 50 most recent notifications
2. **Real-time Updates**: Uses Firestore listeners for efficiency
3. **Memory Management**: Proper listener cleanup in ViewModels
4. **Animations**: Optimized for smooth performance

## Security

- Users can only access their own notifications
- Read/update permissions restricted to notification owners
- Create permissions can be restricted to admin/system users
- All operations require authentication

## Future Enhancements

1. **Push Notifications**: Integrate with FCM for background notifications
2. **Notification Categories**: Add filtering and categorization
3. **Sound & Vibration**: Audio feedback for new notifications
4. **Rich Media**: Support for images and attachments
5. **Scheduling**: Delayed and recurring notifications
6. **Analytics**: Track notification engagement metrics

## Troubleshooting

### Common Issues

1. **Notifications not appearing**: Check Firestore rules and user authentication
2. **Badge count incorrect**: Verify unread count query and listener setup
3. **Actions not working**: Ensure proper navigation handling in ViewModel
4. **Performance issues**: Check listener cleanup and pagination limits

### Debug Tools

- Enable Firestore logging for detailed operation tracking
- Use Android Studio's Firebase debugger
- Check network connectivity for real-time updates

## Contact

For questions or support regarding the notification system, please refer to the development team or create an issue in the project repository. 