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
     * Send multiple notifications to users (for bulk operations).
     * This is intended for use by an admin tool.
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
     * Launch a coroutine to send notification without blocking the main thread.
     * Useful for fire-and-forget notifications triggered from a ViewModel.
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