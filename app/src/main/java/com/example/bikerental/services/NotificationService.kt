package com.example.bikerental.services

import android.util.Log
import com.example.bikerental.models.NotificationType
import com.example.bikerental.models.NotificationPriority
import com.example.bikerental.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.example.bikerental.repositories.NotificationRepository
import com.example.bikerental.models.NotificationRequest

/**
 * Service that monitors Firebase collections for real-time events
 * and automatically creates notifications for users
 */
@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationRepository: NotificationRepository
) {
    companion object {
        private const val TAG = "NotificationService"
        private const val USERS_COLLECTION = "users"
        private const val BOOKINGS_COLLECTION = "bookings"
        private const val PAYMENTS_COLLECTION = "payments"
        private const val ADMIN_MESSAGES_COLLECTION = "adminMessages"
        private const val SUPPORT_MESSAGES_COLLECTION = "supportMessages"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val listeners = mutableListOf<ListenerRegistration>()
    private val processedEvents = mutableSetOf<String>()

    /**
     * Start monitoring all relevant collections for the current user
     */
    fun startMonitoring() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found, cannot start monitoring")
            return
        }

        // Clear the cache of processed events for a fresh session
        processedEvents.clear()
        Log.d(TAG, "Starting real-time monitoring for user: ${currentUser.uid}")
        
        // Monitor user document for changes (e.g., email verification)
        monitorUserDocument(currentUser.uid)

        // Monitor bookings for status changes
        monitorBookings(currentUser.uid)
        
        // Monitor payments for status changes
        monitorPayments(currentUser.uid)
        
        // Monitor admin messages/replies
        monitorAdminMessages(currentUser.uid)
        
        // Monitor support message replies
        monitorSupportMessages(currentUser.uid)
    }

    /**
     * Stop all listeners
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping all notification listeners")
        listeners.forEach { it.remove() }
        listeners.clear()
        processedEvents.clear()
    }

    /**
     * Monitor user document for changes like email verification
     */
    private fun monitorUserDocument(userId: String) {
        val listener = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error monitoring user document", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    handleUserDocumentChange(snapshot.data, snapshot.metadata.hasPendingWrites())
                }
            }
        listeners.add(listener)
        Log.d(TAG, "Started monitoring user document for user: $userId")
    }

    /**
     * Monitor booking collection for status changes
     */
    private fun monitorBookings(userId: String) {
        val listener = firestore
            .collection(BOOKINGS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error monitoring bookings", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.MODIFIED -> {
                            handleBookingStatusChange(change.document.data, change.document.id)
                        }
                        DocumentChange.Type.ADDED -> {
                            handleNewBooking(change.document.data, change.document.id)
                        }
                        else -> { /* Handle other types if needed */ }
                    }
                }
            }
        
        listeners.add(listener)
        Log.d(TAG, "Started monitoring bookings for user: $userId")
    }

    /**
     * Monitor payments collection for status changes
     */
    private fun monitorPayments(userId: String) {
        val listener = firestore
            .collection(PAYMENTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error monitoring payments", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.MODIFIED -> {
                            handlePaymentStatusChange(change.document.data, change.document.id)
                        }
                        DocumentChange.Type.ADDED -> {
                            handleNewPayment(change.document.data, change.document.id)
                        }
                        else -> { /* Handle other types if needed */ }
                    }
                }
            }
        
        listeners.add(listener)
        Log.d(TAG, "Started monitoring payments for user: $userId")
    }

    /**
     * Monitor admin messages for new replies
     */
    private fun monitorAdminMessages(userId: String) {
        val listener = firestore
            .collection(ADMIN_MESSAGES_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error monitoring admin messages", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            handleNewAdminMessage(change.document.data, change.document.id)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            handleAdminMessageUpdate(change.document.data, change.document.id)
                        }
                        else -> { /* Handle other types if needed */ }
                    }
                }
            }
        
        listeners.add(listener)
        Log.d(TAG, "Started monitoring admin messages for user: $userId")
    }

    /**
     * Monitor support messages for admin replies
     */
    private fun monitorSupportMessages(userId: String) {
        val listener = firestore
            .collection(SUPPORT_MESSAGES_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error monitoring support messages", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        DocumentChange.Type.MODIFIED -> {
                            handleSupportMessageReply(change.document.data, change.document.id)
                        }
                        else -> { /* Handle other types if needed */ }
                    }
                }
            }
        
        listeners.add(listener)
        Log.d(TAG, "Started monitoring support messages for user: $userId")
    }

    /**
     * Handle booking status changes
     */
    private fun handleBookingStatusChange(data: Map<String, Any>, bookingId: String) {
        scope.launch {
            try {
                val status = data["status"] as? String ?: return@launch
                val userId = data["userId"] as? String ?: return@launch
                
                val eventId = "$bookingId-$status"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate booking event: $eventId")
                    return@launch
                }
                
                when (status.lowercase()) {
                    "approved" -> {
                        val bikeModel = data["bikeModel"] as? String ?: "bike"
                        val bookingDate = data["bookingDate"] as? String ?: ""
                        val bookingTime = data["bookingTime"] as? String ?: ""
                        val location = data["location"] as? String ?: ""
                        
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
                        notificationRepository.createNotification(request)
                        processedEvents.add(eventId)
                        Log.d(TAG, "Sent booking approval notification for booking: $bookingId")
                    }
                    "confirmed" -> {
                        val bikeModel = data["bikeModel"] as? String ?: "bike"
                        val bookingDate = data["bookingDate"] as? String ?: ""
                        val bookingTime = data["bookingTime"] as? String ?: ""

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
                        notificationRepository.createNotification(request)
                        processedEvents.add(eventId)
                        Log.d(TAG, "Sent booking confirmation notification for booking: $bookingId")
                    }
                    "payment_required" -> {
                        val amount = data["amount"] as? String ?: "0"
                        val dueDate = data["dueDate"] as? String ?: ""
                        val bikeModel = data["bikeModel"] as? String ?: "bike"
                        
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
                        notificationRepository.createNotification(request)
                        processedEvents.add(eventId)
                        Log.d(TAG, "Sent unpaid booking notification for booking: $bookingId")
                    }
                    "completed" -> {
                        val duration = data["duration"] as? String ?: "your"
                        val distance = data["distance"] as? String ?: ""
                        val amount = data["amount"] as? String ?: "0"
                        
                        val request = NotificationRequest(
                            userId = userId,
                            type = NotificationType.RIDE_COMPLETE,
                            title = "Ride Completed Successfully!",
                            message = "Your $duration ride has ended.${if(distance.isNotEmpty()) " Distance: $distance." else ""} Amount: $amount. Thank you for choosing Bambike!",
                            actionText = "View Receipt",
                            actionData = mapOf(
                                "type" to "ride_receipt",
                                "duration" to duration,
                                "distance" to distance,
                                "amount" to amount
                            ),
                            priority = NotificationPriority.NORMAL
                        )
                        notificationRepository.createNotification(request)
                        processedEvents.add(eventId)
                        Log.d(TAG, "Sent ride completion notification for booking: $bookingId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling booking status change", e)
            }
        }
    }

    /**
     * Handle new bookings
     */
    private fun handleNewBooking(data: Map<String, Any>, bookingId: String) {
        scope.launch {
            try {
                val userId = data["userId"] as? String ?: return@launch
                val bikeModel = data["bikeModel"] as? String ?: "bike"
                val bookingDate = data["bookingDate"] as? String ?: ""
                val bookingTime = data["bookingTime"] as? String ?: ""

                val eventId = "$bookingId-new"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate new booking event: $eventId")
                    return@launch
                }

                // Send confirmation for new bookings
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
                notificationRepository.createNotification(request)
                processedEvents.add(eventId)
                Log.d(TAG, "Sent booking confirmation for new booking: $bookingId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling new booking", e)
            }
        }
    }

    /**
     * Handle payment status changes
     */
    private fun handlePaymentStatusChange(data: Map<String, Any>, paymentId: String) {
        scope.launch {
            try {
                val status = data["status"] as? String ?: return@launch
                val userId = data["userId"] as? String ?: return@launch
                val amount = data["amount"] as? String ?: "0"
                val transactionId = data["transactionId"] as? String ?: paymentId
                val paymentMethod = data["paymentMethod"] as? String ?: ""
                val bookingId = data["bookingId"] as? String

                val eventId = "$paymentId-$status"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate payment event: $eventId")
                    return@launch
                }

                when (status.lowercase()) {
                    "successful", "completed" -> {
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
                        notificationRepository.createNotification(request)
                        processedEvents.add(eventId)
                        Log.d(TAG, "Sent payment success notification for payment: $paymentId")
                    }
                    "approved" -> {
                        val approvedBy = data["approvedBy"] as? String ?: "Admin"
                        
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
                        notificationRepository.createNotification(request)
                        processedEvents.add(eventId)
                        Log.d(TAG, "Sent payment approval notification for payment: $paymentId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling payment status change", e)
            }
        }
    }

    /**
     * Handle new payments
     */
    private fun handleNewPayment(data: Map<String, Any>, paymentId: String) {
        scope.launch {
            try {
                val status = data["status"] as? String ?: return@launch
                val userId = data["userId"] as? String ?: return@launch

                val eventId = "$paymentId-$status-new"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate new payment event: $eventId")
                    return@launch
                }

                if (status.lowercase() == "pending" || status.lowercase() == "unpaid") {
                    val amount = data["amount"] as? String ?: "0"
                    val dueDate = data["dueDate"] as? String ?: "ASAP"
                    
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
                    notificationRepository.createNotification(request)
                    processedEvents.add(eventId)
                    Log.d(TAG, "Sent payment reminder for new payment: $paymentId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling new payment", e)
            }
        }
    }

    /**
     * Handle new admin messages
     */
    private fun handleNewAdminMessage(data: Map<String, Any>, messageId: String) {
        scope.launch {
            try {
                val userId = data["userId"] as? String ?: return@launch
                val title = data["title"] as? String ?: "New Admin Message"
                val message = data["message"] as? String ?: ""
                val isUrgent = data["isUrgent"] as? Boolean ?: false

                val eventId = "$messageId-new-admin-message"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate new admin message event: $eventId")
                    return@launch
                }

                val request = NotificationRequest(
                    userId = userId,
                    type = NotificationType.ADMIN_MESSAGE,
                    title = title,
                    message = message,
                    actionText = "Learn More",
                    actionData = mapOf(
                        "type" to "admin_message"
                    ),
                    priority = if (isUrgent) NotificationPriority.HIGH else NotificationPriority.NORMAL
                )
                notificationRepository.createNotification(request)
                processedEvents.add(eventId)
                Log.d(TAG, "Sent admin message notification: $messageId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling new admin message", e)
            }
        }
    }

    /**
     * Handle admin message updates
     */
    private fun handleAdminMessageUpdate(data: Map<String, Any>, messageId: String) {
        scope.launch {
            try {
                val userId = data["userId"] as? String ?: return@launch
                val hasReply = data["hasReply"] as? Boolean ?: false
                val adminName = data["repliedBy"] as? String ?: "Support Team"
                val replyMessage = data["replyMessage"] as? String ?: "You have a new reply from our support team."

                val eventId = "$messageId-admin-reply"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate admin reply event: $eventId")
                    return@launch
                }
                
                if (hasReply) {
                    val request = NotificationRequest(
                        userId = userId,
                        type = NotificationType.ADMIN_REPLY,
                        title = "New Reply from $adminName",
                        message = replyMessage.take(100) + if (replyMessage.length > 100) "..." else "",
                        actionText = "View Message",
                        actionData = mapOf(
                            "type" to "admin_reply",
                            "messageId" to messageId,
                            "adminName" to adminName
                        ),
                        priority = NotificationPriority.HIGH
                    )
                    notificationRepository.createNotification(request)
                    processedEvents.add(eventId)
                    Log.d(TAG, "Sent admin reply notification for message: $messageId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling admin message update", e)
            }
        }
    }

    /**
     * Handle support message replies
     */
    private fun handleSupportMessageReply(data: Map<String, Any>, messageId: String) {
        scope.launch {
            try {
                val userId = data["userId"] as? String ?: return@launch
                val hasAdminReply = data["hasAdminReply"] as? Boolean ?: false
                val adminName = data["adminName"] as? String ?: "Support Team"
                val lastReply = data["lastReply"] as? String ?: "You have a new reply from our support team."

                val eventId = "$messageId-support-reply"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate support reply event: $eventId")
                    return@launch
                }

                if (hasAdminReply) {
                    val request = NotificationRequest(
                        userId = userId,
                        type = NotificationType.ADMIN_REPLY,
                        title = "New Reply from $adminName",
                        message = lastReply.take(100) + if (lastReply.length > 100) "..." else "",
                        actionText = "View Message",
                        actionData = mapOf(
                            "type" to "admin_reply",
                            "messageId" to messageId,
                            "adminName" to adminName
                        ),
                        priority = NotificationPriority.HIGH
                    )
                    notificationRepository.createNotification(request)
                    processedEvents.add(eventId)
                    Log.d(TAG, "Sent support reply notification for message: $messageId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling support message reply", e)
            }
        }
    }

    /**
     * Handle user document changes (e.g., email verification)
     */
    private fun handleUserDocumentChange(data: Map<String, Any>?, fromCache: Boolean) {
        // Only trigger on server changes, not local writes
        if (fromCache || data == null) return

        scope.launch {
            try {
                val userId = data["userId"] as? String ?: return@launch
                val emailVerified = data["emailVerified"] as? Boolean ?: false
                
                val eventId = "$userId-email-verified"
                if (processedEvents.contains(eventId)) {
                    Log.d(TAG, "Skipping duplicate email verification event: $eventId")
                    return@launch
                }

                // This logic is simple; could be expanded to check previous state
                // to avoid sending notifications multiple times.
                // For now, we assume this is a one-time event.
                if (emailVerified) {
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
                    notificationRepository.createNotification(request)
                    processedEvents.add(eventId)
                    Log.d(TAG, "Sent email verification notification for user: $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling user document change", e)
            }
        }
    }
} 