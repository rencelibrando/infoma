package com.example.bikerental.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Demonstration class showing how the notification system is integrated
 * with all major app features. This is for reference and testing purposes.
 */
@HiltViewModel
class NotificationIntegrationDemo @Inject constructor(
    private val notificationUtils: NotificationUtils
) : ViewModel() {
    
    companion object {
        private const val TAG = "NotificationDemo"
    }
    
    /**
     * Demonstrate ride completion notification
     * This is automatically triggered in RideRepositoryImpl.endRide()
     */
    fun demonstrateRideCompletion(userId: String) {
        viewModelScope.launch {
            val result = notificationUtils.sendRideCompletionNotification(
                userId = userId,
                duration = "45 min",
                distance = "8.2 km",
                amount = "₱180.00"
            )
            
            result.fold(
                onSuccess = { notificationId ->
                    Log.d(TAG, "✅ Ride completion notification sent: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send ride completion notification: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Demonstrate payment reminder notification
     * This is automatically triggered in PaymentViewModel.handleRideCompletion()
     */
    fun demonstratePaymentReminder(userId: String) {
        viewModelScope.launch {
            val result = notificationUtils.sendPaymentReminderNotification(
                userId = userId,
                amount = "₱275.50",
                dueDate = "Dec 25, 2024"
            )
            
            result.fold(
                onSuccess = { notificationId ->
                    Log.d(TAG, "✅ Payment reminder notification sent: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send payment reminder notification: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Demonstrate booking confirmation notification
     * This is automatically triggered in BikeViewModel.createBooking()
     */
    fun demonstrateBookingConfirmation(userId: String) {
        viewModelScope.launch {
            val result = notificationUtils.sendBookingConfirmationNotification(
                userId = userId,
                bookingDate = "Dec 22, 2024",
                bookingTime = "2:00 PM",
                bikeModel = "Bambike Classic",
                bookingId = "booking_123"
            )
            
            result.fold(
                onSuccess = { notificationId ->
                    Log.d(TAG, "✅ Booking confirmation notification sent: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send booking confirmation notification: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Demonstrate email verification success notification
     * This is automatically triggered in AuthViewModel.checkEmailVerification()
     */
    fun demonstrateEmailVerificationSuccess(userId: String) {
        viewModelScope.launch {
            val result = notificationUtils.sendEmailVerificationSuccessNotification(userId)
            
            result.fold(
                onSuccess = { notificationId ->
                    Log.d(TAG, "✅ Email verification success notification sent: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send email verification notification: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Demonstrate admin message notification
     */
    fun demonstrateAdminMessage(userId: String) {
        viewModelScope.launch {
            val result = notificationUtils.sendAdminMessageNotification(
                userId = userId,
                title = "System Maintenance Notice",
                message = "We'll be performing system maintenance on Dec 24, 2024 from 2:00-4:00 AM. Some features may be temporarily unavailable."
            )
            
            result.fold(
                onSuccess = { notificationId ->
                    Log.d(TAG, "✅ Admin message notification sent: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send admin message notification: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Demonstrate bulk notification sending
     */
    fun demonstrateBulkNotifications(userIds: List<String>) {
        viewModelScope.launch {
            val result = notificationUtils.sendBulkNotifications(
                userIds = userIds,
                type = NotificationType.ADMIN_MESSAGE,
                title = "Holiday Special Offer!",
                message = "Get 20% off all bike rentals this Christmas season. Use code XMAS2024 when booking.",
                actionText = "Book Now"
            )
            
            result.fold(
                onSuccess = { notificationIds ->
                    Log.d(TAG, "✅ Bulk notifications sent: ${notificationIds.size} notifications created")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send bulk notifications: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Demonstrate async notification sending
     */
    fun demonstrateAsyncNotification(userId: String) {
        notificationUtils.sendNotificationAsync(
            scope = viewModelScope,
            userId = userId,
            type = NotificationType.BOOKING_REMINDER,
            title = "Upcoming Booking Reminder",
            message = "Your bike rental is scheduled for tomorrow at 3:00 PM. Don't forget!",
            actionText = "View Details"
        ) { result ->
            result.fold(
                onSuccess = { notificationId ->
                    Log.d(TAG, "✅ Async notification sent: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to send async notification: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Summary of all integration points
     */
    fun logIntegrationSummary() {
        Log.d(TAG, """
            
            🎯 NOTIFICATION SYSTEM INTEGRATION SUMMARY:
            
            ✅ RIDE COMPLETION - Integrated with RideRepositoryImpl.endRide()
               → Automatically sends notification when rides are completed
               → Includes duration, distance, and cost information
            
            ✅ PAYMENT REMINDERS - Integrated with PaymentViewModel
               → Sends reminders for unpaid bookings after ride completion
               → Includes amount due and due date
            
            ✅ BOOKING CONFIRMATIONS - Integrated with BikeViewModel.createBooking()
               → Automatically sends confirmation when bookings are created
               → Includes booking date, time, and bike model
            
            ✅ EMAIL VERIFICATION - Integrated with AuthViewModel.checkEmailVerification()
               → Sends success notification when email is verified
               → Triggers after successful verification
            
            ✅ ADMIN MESSAGES - Available via NotificationUtils
               → Can be sent programmatically for announcements
               → Supports custom titles and messages
            
            ✅ BULK NOTIFICATIONS - Available via NotificationUtils
               → Send notifications to multiple users at once
               → Useful for promotions and announcements
            
            ✅ ASYNC NOTIFICATIONS - Available via NotificationUtils
               → Non-blocking notification sending
               → Perfect for background operations
            
            🔧 ALL FEATURES READY FOR PRODUCTION USE!
            
        """.trimIndent())
    }
} 