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

/**
 * Service that monitors Firebase collections for real-time events
 * and automatically creates notifications for users
 */
@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationUtils: NotificationUtils
) {
    companion object {
        private const val TAG = "NotificationService"
        private const val BOOKINGS_COLLECTION = "bookings"
        private const val PAYMENTS_COLLECTION = "payments"
        private const val ADMIN_MESSAGES_COLLECTION = "admin_messages"
        private const val SUPPORT_MESSAGES_COLLECTION = "support_messages"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val listeners = mutableListOf<ListenerRegistration>()

    /**
     * Start monitoring all relevant collections for the current user
     */
    fun startMonitoring() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found, cannot start monitoring")
            return
        }

        Log.d(TAG, "Starting real-time monitoring for user: ${currentUser.uid}")
        
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
                val bikeModel = data["bikeModel"] as? String ?: "bike"
                val bookingDate = data["bookingDate"] as? String ?: ""
                val bookingTime = data["bookingTime"] as? String ?: ""
                val location = data["location"] as? String ?: ""

                when (status.lowercase()) {
                    "approved" -> {
                        notificationUtils.sendBookingApprovalNotification(
                            userId = userId,
                            bookingId = bookingId,
                            bikeModel = bikeModel,
                            bookingDate = bookingDate,
                            bookingTime = bookingTime,
                            location = location
                        )
                        Log.d(TAG, "Sent booking approval notification for booking: $bookingId")
                    }
                    "confirmed" -> {
                        notificationUtils.sendBookingConfirmationNotification(
                            userId = userId,
                            bookingDate = bookingDate,
                            bookingTime = bookingTime,
                            bikeModel = bikeModel,
                            bookingId = bookingId
                        )
                        Log.d(TAG, "Sent booking confirmation notification for booking: $bookingId")
                    }
                    "payment_required" -> {
                        val amount = data["amount"] as? String ?: "0"
                        val dueDate = data["dueDate"] as? String ?: ""
                        
                        notificationUtils.sendUnpaidBookingNotification(
                            userId = userId,
                            bookingId = bookingId,
                            amount = amount,
                            dueDate = dueDate,
                            bikeModel = bikeModel
                        )
                        Log.d(TAG, "Sent unpaid booking notification for booking: $bookingId")
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

                // Send confirmation for new bookings
                notificationUtils.sendBookingConfirmationNotification(
                    userId = userId,
                    bookingDate = bookingDate,
                    bookingTime = bookingTime,
                    bikeModel = bikeModel,
                    bookingId = bookingId
                )
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

                when (status.lowercase()) {
                    "successful", "completed" -> {
                        notificationUtils.sendPaymentSuccessNotification(
                            userId = userId,
                            transactionId = transactionId,
                            amount = amount,
                            bookingId = bookingId,
                            paymentMethod = paymentMethod
                        )
                        Log.d(TAG, "Sent payment success notification for payment: $paymentId")
                    }
                    "approved" -> {
                        val approvedBy = data["approvedBy"] as? String ?: "Admin"
                        
                        notificationUtils.sendPaymentApprovalNotification(
                            userId = userId,
                            transactionId = transactionId,
                            amount = amount,
                            approvedBy = approvedBy
                        )
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
        // For new payments, we might want to send a confirmation
        // This is optional based on your business logic
        Log.d(TAG, "New payment created: $paymentId")
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

                notificationUtils.sendAdminMessageNotification(
                    userId = userId,
                    title = title,
                    message = message
                )
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

                if (hasReply) {
                    notificationUtils.sendAdminReplyNotification(
                        userId = userId,
                        messageId = messageId,
                        adminName = adminName,
                        replyPreview = replyMessage
                    )
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

                if (hasAdminReply) {
                    notificationUtils.sendAdminReplyNotification(
                        userId = userId,
                        messageId = messageId,
                        adminName = adminName,
                        replyPreview = lastReply
                    )
                    Log.d(TAG, "Sent support reply notification for message: $messageId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling support message reply", e)
            }
        }
    }
} 