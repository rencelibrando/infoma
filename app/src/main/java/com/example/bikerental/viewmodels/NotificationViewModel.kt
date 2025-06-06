package com.example.bikerental.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.Notification
import com.example.bikerental.models.NotificationType
import com.example.bikerental.models.NotificationPriority
import com.example.bikerental.repositories.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    val selectedFilter: StateFlow<NotificationFilter> = _selectedFilter.asStateFlow()

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
                .map { notifications ->
                    // Sort by priority and timestamp
                    notifications.sortedWith(
                        compareByDescending<Notification> { it.priority.ordinal }
                            .thenByDescending { it.timestamp.toDate() }
                    )
                }
                .collect { notifications ->
                    _uiState.value = _uiState.value.copy(
                        allNotifications = notifications,
                        notifications = filterNotifications(notifications, _selectedFilter.value),
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

    fun setFilter(filter: NotificationFilter) {
        _selectedFilter.value = filter
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            notifications = filterNotifications(currentState.allNotifications, filter)
        )
    }

    private fun filterNotifications(notifications: List<Notification>, filter: NotificationFilter): List<Notification> {
        return when (filter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilter.PAYMENTS -> notifications.filter { 
                it.type in listOf(
                    NotificationType.UNPAID_BOOKING,
                    NotificationType.PAYMENT_SUCCESS,
                    NotificationType.PAYMENT_APPROVAL,
                    NotificationType.UNPAID_PAYMENT
                )
            }
            NotificationFilter.BOOKINGS -> notifications.filter {
                it.type in listOf(
                    NotificationType.BOOKING_APPROVAL,
                    NotificationType.BOOKING_CONFIRMATION,
                    NotificationType.BOOKING_REMINDER
                )
            }
            NotificationFilter.ADMIN -> notifications.filter {
                it.type in listOf(
                    NotificationType.ADMIN_REPLY,
                    NotificationType.ADMIN_MESSAGE
                )
            }
            NotificationFilter.HIGH_PRIORITY -> notifications.filter {
                it.priority in listOf(NotificationPriority.HIGH, NotificationPriority.URGENT)
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
            NotificationType.UNPAID_BOOKING -> {
                // Navigate to payment screen with booking details
                // Extract booking data and initiate payment flow
            }
            NotificationType.PAYMENT_SUCCESS -> {
                // Navigate to receipt/transaction details
                // Show success message and receipt
            }
            NotificationType.ADMIN_REPLY -> {
                // Navigate to support/messages screen
                // Open specific conversation thread
            }
            NotificationType.PAYMENT_APPROVAL -> {
                // Navigate to booking confirmation or next steps
                // Show approval confirmation
            }
            NotificationType.BOOKING_APPROVAL -> {
                // Navigate to booking details or ride preparation
                // Show booking details and next steps
            }
            NotificationType.RIDE_COMPLETE -> {
                // Navigate to ride history or receipt
            }
            NotificationType.UNPAID_PAYMENT -> {
                // Navigate to payment screen
            }
            NotificationType.ADMIN_MESSAGE -> {
                // Navigate to help/support or show detailed message
            }
            NotificationType.BOOKING_CONFIRMATION,
            NotificationType.BOOKING_REMINDER -> {
                // Navigate to bookings tab
            }
            NotificationType.EMAIL_VERIFICATION -> {
                // Navigate to profile or show success message
            }
            NotificationType.GENERAL -> {
                // Default action
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class NotificationUiState(
    val allNotifications: List<Notification> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    PAYMENTS("Payments"),
    BOOKINGS("Bookings"),
    ADMIN("Admin"),
    HIGH_PRIORITY("Priority")
} 