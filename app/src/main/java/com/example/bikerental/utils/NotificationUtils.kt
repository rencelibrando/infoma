package com.example.bikerental.utils

import com.example.bikerental.models.NotificationRequest
import com.example.bikerental.models.NotificationType
import com.example.bikerental.repositories.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationUtils @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    
    /**
     * Send a ride completion notification
     */
    suspend fun sendRideCompletionNotification(
        userId: String,
        duration: String,
        distance: String,
        amount: String
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.RIDE_COMPLETE,
            title = "Ride Completed Successfully!",
            message = "Your $duration ride has ended. Distance: $distance. Amount: $amount. Thank you for choosing Bambike!",
            actionText = "View Receipt",
            actionData = mapOf(
                "type" to "ride_receipt",
                "duration" to duration,
                "distance" to distance,
                "amount" to amount
            )
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send an unpaid payment reminder
     */
    suspend fun sendPaymentReminderNotification(
        userId: String,
        amount: String,
        dueDate: String
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.UNPAID_PAYMENT,
            title = "Payment Reminder",
            message = "You have an outstanding payment of $amount due on $dueDate. Please settle to continue using Bambike.",
            actionText = "Pay Now",
            actionData = mapOf(
                "type" to "payment",
                "amount" to amount,
                "dueDate" to dueDate
            )
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send an admin message notification
     */
    suspend fun sendAdminMessageNotification(
        userId: String,
        title: String,
        message: String,
        actionText: String = "Learn More"
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.ADMIN_MESSAGE,
            title = title,
            message = message,
            actionText = actionText,
            actionData = mapOf(
                "type" to "admin_message"
            )
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send email verification success notification
     */
    suspend fun sendEmailVerificationSuccessNotification(
        userId: String
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.EMAIL_VERIFICATION,
            title = "Email Verified Successfully",
            message = "Your email address has been verified. You now have full access to all Bambike features.",
            actionText = "Continue",
            actionData = mapOf(
                "type" to "email_verified"
            )
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send booking confirmation notification
     */
    suspend fun sendBookingConfirmationNotification(
        userId: String,
        bookingDate: String,
        bookingTime: String,
        bikeModel: String,
        bookingId: String
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.BOOKING_CONFIRMATION,
            title = "Booking Confirmed",
            message = "Your $bikeModel reservation for $bookingDate at $bookingTime has been confirmed. Remember to arrive 5 minutes early.",
            actionText = "View Details",
            actionData = mapOf(
                "type" to "booking_details",
                "bookingId" to bookingId,
                "bikeModel" to bikeModel,
                "date" to bookingDate,
                "time" to bookingTime
            )
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send booking reminder notification
     */
    suspend fun sendBookingReminderNotification(
        userId: String,
        bookingTime: String,
        bikeModel: String,
        location: String,
        bookingId: String
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.BOOKING_REMINDER,
            title = "Upcoming Booking Reminder",
            message = "Your $bikeModel booking is scheduled for $bookingTime at $location. Don't forget!",
            actionText = "View Details",
            actionData = mapOf(
                "type" to "booking_reminder",
                "bookingId" to bookingId,
                "bikeModel" to bikeModel,
                "time" to bookingTime,
                "location" to location
            )
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send multiple notifications to users (for bulk operations)
     */
    suspend fun sendBulkNotifications(
        userIds: List<String>,
        type: NotificationType,
        title: String,
        message: String,
        actionText: String = "",
        actionData: Map<String, Any> = emptyMap()
    ): Result<List<String>> {
        return try {
            val results = mutableListOf<String>()
            userIds.forEach { userId ->
                val request = NotificationRequest(
                    userId = userId,
                    type = type,
                    title = title,
                    message = message,
                    actionText = actionText,
                    actionData = actionData
                )
                val result = notificationRepository.createNotification(request)
                result.getOrNull()?.let { notificationId ->
                    results.add(notificationId)
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Launch a coroutine to send notification without blocking
     */
    fun sendNotificationAsync(
        scope: CoroutineScope,
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        actionText: String = "",
        actionData: Map<String, Any> = emptyMap(),
        onComplete: (Result<String>) -> Unit = {}
    ) {
        scope.launch {
            val request = NotificationRequest(
                userId = userId,
                type = type,
                title = title,
                message = message,
                actionText = actionText,
                actionData = actionData
            )
            val result = notificationRepository.createNotification(request)
            onComplete(result)
        }
    }
} 