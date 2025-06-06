package com.example.bikerental.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.Notification
import com.example.bikerental.repositories.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotifications()
                .onStart { 
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load notifications"
                    )
                }
                .collect { notifications ->
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount()
                .catch { /* Handle silently for badge count */ }
                .collect { count ->
                    _unreadCount.value = count
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to mark as read: ${exception.message}"
                    )
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            notificationRepository.markAllAsRead()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to mark all as read: ${exception.message}"
                    )
                }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete notification: ${exception.message}"
                    )
                }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            notificationRepository.clearAllNotifications()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to clear notifications: ${exception.message}"
                    )
                }
        }
    }

    fun createSampleNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            notificationRepository.createSampleNotifications()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create sample notifications: ${exception.message}"
                    )
                }
        }
    }

    fun handleNotificationAction(notification: Notification) {
        // Mark as read when action is performed
        markAsRead(notification.id)
        
        // Handle specific action based on notification type
        when (notification.type) {
            com.example.bikerental.models.NotificationType.RIDE_COMPLETE -> {
                // Navigate to ride history or receipt
            }
            com.example.bikerental.models.NotificationType.UNPAID_PAYMENT -> {
                // Navigate to payment screen
            }
            com.example.bikerental.models.NotificationType.ADMIN_MESSAGE -> {
                // Navigate to help/support or show detailed message
            }
            com.example.bikerental.models.NotificationType.BOOKING_CONFIRMATION,
            com.example.bikerental.models.NotificationType.BOOKING_REMINDER -> {
                // Navigate to bookings tab
            }
            com.example.bikerental.models.NotificationType.EMAIL_VERIFICATION -> {
                // Navigate to profile or show success message
            }
            com.example.bikerental.models.NotificationType.GENERAL -> {
                // Default action
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) 