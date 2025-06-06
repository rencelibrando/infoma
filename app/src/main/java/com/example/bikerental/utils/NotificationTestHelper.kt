package com.example.bikerental.utils

import android.util.Log
import com.example.bikerental.models.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to test notification system functionality
 * Use this to verify that notifications are working correctly in your app
 */
@Singleton
class NotificationTestHelper @Inject constructor(
    private val notificationUtils: NotificationUtils
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "NotificationTestHelper"
    }
    
    /**
     * Test all notification types to ensure the system is working
     */
    fun runNotificationTests(userId: String) {
        scope.launch {
            Log.d(TAG, "Starting notification system tests for user: $userId")
            
            // Test 1: Booking Confirmation
            testBookingConfirmation(userId)
            
            // Test 2: Ride Completion
            testRideCompletion(userId)
            
            // Test 3: Payment Reminder
            testPaymentReminder(userId)
            
            // Test 4: Email Verification Success
            testEmailVerificationSuccess(userId)
            
            // Test 5: Admin Message
            testAdminMessage(userId)
            
            Log.d(TAG, "All notification tests completed")
        }
    }
    
    private suspend fun testBookingConfirmation(userId: String) {
        val result = notificationUtils.sendBookingConfirmationNotification(
            userId = userId,
            bookingDate = "Today",
            bookingTime = "2:00 PM",
            bikeModel = "Mountain Bike",
            bookingId = "test_booking_001"
        )
        
        result.fold(
            onSuccess = { Log.d(TAG, "✅ Booking confirmation test: SUCCESS") },
            onFailure = { Log.e(TAG, "❌ Booking confirmation test: FAILED - ${it.message}") }
        )
    }
    
    private suspend fun testRideCompletion(userId: String) {
        val result = notificationUtils.sendRideCompletionNotification(
            userId = userId,
            duration = "45 minutes",
            distance = "8.2 km",
            amount = "₱125.00"
        )
        
        result.fold(
            onSuccess = { Log.d(TAG, "✅ Ride completion test: SUCCESS") },
            onFailure = { Log.e(TAG, "❌ Ride completion test: FAILED - ${it.message}") }
        )
    }
    
    private suspend fun testPaymentReminder(userId: String) {
        val result = notificationUtils.sendPaymentReminderNotification(
            userId = userId,
            amount = "₱125.00",
            dueDate = "Tomorrow"
        )
        
        result.fold(
            onSuccess = { Log.d(TAG, "✅ Payment reminder test: SUCCESS") },
            onFailure = { Log.e(TAG, "❌ Payment reminder test: FAILED - ${it.message}") }
        )
    }
    
    private suspend fun testEmailVerificationSuccess(userId: String) {
        val result = notificationUtils.sendEmailVerificationSuccessNotification(userId)
        
        result.fold(
            onSuccess = { Log.d(TAG, "✅ Email verification test: SUCCESS") },
            onFailure = { Log.e(TAG, "❌ Email verification test: FAILED - ${it.message}") }
        )
    }
    
    private suspend fun testAdminMessage(userId: String) {
        val result = notificationUtils.sendAdminMessageNotification(
            userId = userId,
            title = "Test System Update",
            message = "New features available! Check out the updated bike selection and improved maps.",
            actionText = "Learn More"
        )
        
        result.fold(
            onSuccess = { Log.d(TAG, "✅ Admin message test: SUCCESS") },
            onFailure = { Log.e(TAG, "❌ Admin message test: FAILED - ${it.message}") }
        )
    }
    
    /**
     * Test bulk notifications for admin features
     */
    fun testBulkNotifications(userIds: List<String>) {
        scope.launch {
            Log.d(TAG, "Testing bulk notifications for ${userIds.size} users")
            
            val result = notificationUtils.sendBulkNotifications(
                userIds = userIds,
                type = NotificationType.ADMIN_MESSAGE,
                title = "Special Promotion Test",
                message = "50% off on all weekend rides! Book now and save big.",
                actionText = "Book Now",
                actionData = mapOf(
                    "discount" to "50",
                    "validUntil" to (System.currentTimeMillis() + 604800000).toString(),
                    "promoCode" to "WEEKEND50"
                )
            )
            
            result.fold(
                onSuccess = { 
                    Log.d(TAG, "✅ Bulk notifications test: SUCCESS - Sent to ${userIds.size} users") 
                },
                onFailure = { 
                    Log.e(TAG, "❌ Bulk notifications test: FAILED - ${it.message}") 
                }
            )
        }
    }
    
    /**
     * Quick test to verify notification system is responsive
     */
    fun quickTest(userId: String) {
        scope.launch {
            val result = notificationUtils.sendAdminMessageNotification(
                userId = userId,
                title = "🚴‍♂️ Notification System Test",
                message = "If you see this, your notification system is working perfectly!",
                actionText = "Great!"
            )
            
            result.fold(
                onSuccess = { 
                    Log.d(TAG, "🎉 Quick test: Notification system is WORKING!")
                },
                onFailure = { 
                    Log.e(TAG, "⚠️ Quick test: Notification system needs attention - ${it.message}")
                }
            )
        }
    }
    
    /**
     * Test booking reminder functionality
     */
    fun testBookingReminder(userId: String) {
        scope.launch {
            val result = notificationUtils.sendBookingReminderNotification(
                userId = userId,
                bookingTime = "3:00 PM",
                bikeModel = "City Bike",
                location = "BGC Station",
                bookingId = "test_reminder_001"
            )
            
            result.fold(
                onSuccess = { Log.d(TAG, "✅ Booking reminder test: SUCCESS") },
                onFailure = { Log.e(TAG, "❌ Booking reminder test: FAILED - ${it.message}") }
            )
        }
    }
} 