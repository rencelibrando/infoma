package com.example.bikerental.repositories

import android.util.Log
import com.example.bikerental.models.Notification
import com.example.bikerental.models.NotificationPriority
import com.example.bikerental.models.NotificationRequest
import com.example.bikerental.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "NotificationRepository"
        private const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    /**
     * Get real-time notifications for the current user
     */
    fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limit to recent 50 notifications
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching notifications", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { document ->
                    try {
                        // Manually map the document to the Notification object
                        val notification = document.toObject(Notification::class.java)
                        notification?.copy(
                            id = document.id,
                            read = document.getBoolean("read") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Notification", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Fetched ${notifications.size} notifications")
                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get unread notification count
     */
    fun getUnreadCount(): Flow<Int> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching unread count", error)
                    trySend(0)
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                Log.d(TAG, "Unread notifications count: $count")
                trySend(count)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            firestore
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update("read", true)
                .await()

            Log.d(TAG, "Marked notification $notificationId as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for current user
     */
    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val batch = firestore.batch()
            val unreadNotifications = firestore
                .collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("read", false)
                .get()
                .await()

            unreadNotifications.documents.forEach { document ->
                batch.update(document.reference, "read", true)
            }

            batch.commit().await()
            Log.d(TAG, "Marked all notifications as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            firestore
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .await()

            Log.d(TAG, "Deleted notification $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all notifications for current user
     */
    suspend fun clearAllNotifications(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val batch = firestore.batch()
            val userNotifications = firestore
                .collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            userNotifications.documents.forEach { document ->
                batch.delete(document.reference)
            }

            batch.commit().await()
            Log.d(TAG, "Cleared all notifications")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all notifications", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new notification (typically called by admin or system)
     */
    suspend fun createNotification(request: NotificationRequest): Result<String> {
        return try {
            val notificationData = mapOf(
                "userId" to request.userId,
                "type" to request.type,
                "title" to request.title,
                "message" to request.message,
                "actionText" to request.actionText,
                "actionData" to request.actionData,
                "priority" to request.priority,
                "timestamp" to Timestamp.now(),
                "read" to false,
                "actionable" to request.actionText.isNotBlank()
            )

            val documentRef = firestore
                .collection(NOTIFICATIONS_COLLECTION)
                .add(notificationData)
                .await()

            Log.d(TAG, "Created notification with ID: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            Result.failure(e)
        }
    }

    /**
     * Create sample notifications for demo purposes
     */
    suspend fun createSampleNotifications(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val sampleNotifications = listOf(
                // New Priority Notifications
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.UNPAID_BOOKING,
                    title = "Payment Required",
                    message = "Your Bambike Classic booking for Dec 25, 2024 requires payment of ₱180.00. Due: Dec 23, 2024.",
                    actionText = "Pay Now",
                    priority = NotificationPriority.HIGH
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.PAYMENT_SUCCESS,
                    title = "Payment Successful!",
                    message = "Your payment of ₱180.00 has been processed successfully via GCash. Transaction ID: TXN123456789",
                    actionText = "View Receipt",
                    priority = NotificationPriority.NORMAL
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.ADMIN_REPLY,
                    title = "New Reply from Support Team",
                    message = "Thank you for contacting us. We've reviewed your request and here's our response...",
                    actionText = "View Message",
                    priority = NotificationPriority.HIGH
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.PAYMENT_APPROVAL,
                    title = "Payment Approved",
                    message = "Your payment of ₱180.00 has been reviewed and approved by Admin. You can now proceed with your booking.",
                    actionText = "Continue",
                    priority = NotificationPriority.NORMAL
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.BOOKING_APPROVAL,
                    title = "Booking Approved!",
                    message = "Your Bambike Classic booking for Dec 25, 2024 at 2:00 PM has been approved at BGC Station. Get ready for your ride!",
                    actionText = "View Details",
                    priority = NotificationPriority.NORMAL
                ),
                
                // Legacy Notifications (Updated)
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.RIDE_COMPLETE,
                    title = "Ride Completed Successfully!",
                    message = "Your 32-minute ride has ended. Distance: 6.8 km. Thank you for choosing Bambike!",
                    actionText = "View Receipt",
                    priority = NotificationPriority.NORMAL
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.UNPAID_PAYMENT,
                    title = "Payment Reminder",
                    message = "You have an outstanding payment of ₱145.50 for your ride on March 15. Please settle to continue using Bambike.",
                    actionText = "Pay Now",
                    priority = NotificationPriority.HIGH
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.ADMIN_MESSAGE,
                    title = "Service Update",
                    message = "Scheduled maintenance will occur on March 20, 2-4 AM. Some bikes may be temporarily unavailable.",
                    actionText = "Learn More",
                    priority = NotificationPriority.NORMAL
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.BOOKING_CONFIRMATION,
                    title = "Booking Confirmed",
                    message = "Your bike reservation for tomorrow at 9:00 AM has been confirmed. Remember to arrive 5 minutes early.",
                    actionText = "View Details",
                    priority = NotificationPriority.NORMAL
                ),
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.EMAIL_VERIFICATION,
                    title = "Email Verified Successfully",
                    message = "Your email address has been verified. You now have full access to all Bambike features.",
                    actionText = "Continue",
                    priority = NotificationPriority.NORMAL
                ),
                
                // High Priority Urgent Sample
                NotificationRequest(
                    userId = currentUser.uid,
                    type = NotificationType.ADMIN_MESSAGE,
                    title = "🚨 Urgent System Alert",
                    message = "Critical security update required. Please update your password immediately to maintain account security.",
                    actionText = "Update Now",
                    priority = NotificationPriority.URGENT
                )
            )

            sampleNotifications.forEach { request ->
                createNotification(request)
            }

            Log.d(TAG, "Created ${sampleNotifications.size} sample notifications")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sample notifications", e)
            Result.failure(e)
        }
    }
} 