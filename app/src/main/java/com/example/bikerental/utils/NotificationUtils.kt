package com.example.bikerental.utils

import com.example.bikerental.models.NotificationRequest
import com.example.bikerental.models.NotificationType
import com.example.bikerental.models.NotificationPriority
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
     * Send an unpaid booking notification
     */
    suspend fun sendUnpaidBookingNotification(
        userId: String,
        bookingId: String,
        amount: String,
        dueDate: String,
        bikeModel: String = ""
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.UNPAID_BOOKING,
            title = "Payment Required",
            message = "Your booking${if (bikeModel.isNotEmpty()) " for $bikeModel" else ""} requires payment of $amount. Due: $dueDate.",
            actionText = "Pay Now",
            actionData = mapOf(
                "type" to "unpaid_booking",
                "bookingId" to bookingId,
                "amount" to amount,
                "dueDate" to dueDate,
                "bikeModel" to bikeModel
            ),
            priority = NotificationPriority.HIGH
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send a payment success notification
     */
    suspend fun sendPaymentSuccessNotification(
        userId: String,
        transactionId: String,
        amount: String,
        bookingId: String? = null,
        paymentMethod: String = ""
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.PAYMENT_SUCCESS,
            title = "Payment Successful!",
            message = "Your payment of $amount has been processed successfully${if (paymentMethod.isNotEmpty()) " via $paymentMethod" else ""}. Transaction ID: $transactionId",
            actionText = "View Receipt",
            actionData = mapOf(
                "type" to "payment_success",
                "transactionId" to transactionId,
                "amount" to amount,
                "bookingId" to (bookingId ?: ""),
                "paymentMethod" to paymentMethod
            ),
            priority = NotificationPriority.NORMAL
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send an admin reply notification
     */
    suspend fun sendAdminReplyNotification(
        userId: String,
        messageId: String,
        adminName: String = "Support Team",
        replyPreview: String
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.ADMIN_REPLY,
            title = "New Reply from $adminName",
            message = replyPreview.take(100) + if (replyPreview.length > 100) "..." else "",
            actionText = "View Message",
            actionData = mapOf(
                "type" to "admin_reply",
                "messageId" to messageId,
                "adminName" to adminName
            ),
            priority = NotificationPriority.HIGH
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send a payment approval notification
     */
    suspend fun sendPaymentApprovalNotification(
        userId: String,
        transactionId: String,
        amount: String,
        approvedBy: String = "Admin"
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.PAYMENT_APPROVAL,
            title = "Payment Approved",
            message = "Your payment of $amount has been reviewed and approved by $approvedBy. You can now proceed with your booking.",
            actionText = "Continue",
            actionData = mapOf(
                "type" to "payment_approval",
                "transactionId" to transactionId,
                "amount" to amount,
                "approvedBy" to approvedBy
            ),
            priority = NotificationPriority.NORMAL
        )
        return notificationRepository.createNotification(request)
    }
    
    /**
     * Send a booking approval notification
     */
    suspend fun sendBookingApprovalNotification(
        userId: String,
        bookingId: String,
        bikeModel: String,
        bookingDate: String,
        bookingTime: String,
        location: String = ""
    ): Result<String> {
        val request = NotificationRequest(
            userId = userId,
            type = NotificationType.BOOKING_APPROVAL,
            title = "Booking Approved!",
            message = "Your $bikeModel booking for $bookingDate at $bookingTime has been approved${if (location.isNotEmpty()) " at $location" else ""}. Get ready for your ride!",
            actionText = "View Details",
            actionData = mapOf(
                "type" to "booking_approval",
                "bookingId" to bookingId,
                "bikeModel" to bikeModel,
                "bookingDate" to bookingDate,
                "bookingTime" to bookingTime,
                "location" to location
            ),
            priority = NotificationPriority.NORMAL
        )
        return notificationRepository.createNotification(request)
    }
    
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
            ),
            priority = NotificationPriority.NORMAL
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
            ),
            priority = NotificationPriority.HIGH
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
            ),
            priority = NotificationPriority.NORMAL
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
            ),
            priority = NotificationPriority.NORMAL
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
            ),
            priority = NotificationPriority.NORMAL
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
            ),
            priority = NotificationPriority.NORMAL
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
        actionData: Map<String, Any> = emptyMap(),
        priority: NotificationPriority = NotificationPriority.NORMAL
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
                    actionData = actionData,
                    priority = priority
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
        priority: NotificationPriority = NotificationPriority.NORMAL,
        onComplete: (Result<String>) -> Unit = {}
    ) {
        scope.launch {
            val request = NotificationRequest(
                userId = userId,
                type = type,
                title = title,
                message = message,
                actionText = actionText,
                actionData = actionData,
                priority = priority
            )
            val result = notificationRepository.createNotification(request)
            onComplete(result)
        }
    }
} 