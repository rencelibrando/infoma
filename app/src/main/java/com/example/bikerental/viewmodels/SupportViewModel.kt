package com.example.bikerental.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.FAQ
import com.example.bikerental.models.SupportMessage
import com.example.bikerental.models.SupportReply
import com.example.bikerental.repositories.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.bikerental.models.OperatingHour
import com.example.bikerental.models.LocationDetails

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository
) : ViewModel() {

    // UI State for the Help & Support screen
    data class SupportUiState(
        val isLoading: Boolean = false,
        val faqs: List<FAQ> = emptyList(),
        val userMessages: List<SupportMessage> = emptyList(),
        val replies: Map<String, List<SupportReply>> = emptyMap(),
        val isSendingReply: Boolean = false,
        val isUploadingImage: Boolean = false,
        val uploadProgress: Float = 0f,
        val errorMessage: String? = null,
        val isMessageSent: Boolean = false,
        val operatingHours: List<OperatingHour> = emptyList(),
        val locationDetails: LocationDetails = LocationDetails()
    )
    
    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()
    
    init {
        refreshContactUsTab()
        loadUserMessages()
    }
    
    /**
     * Refreshes all data for the "Contact Us" tab.
     */
    fun refreshContactUsTab() {
        loadFaqs()
        loadOperatingHours()
        loadLocationDetails()
    }
    
    /**
     * Loads FAQs from the repository
     */
    fun loadFaqs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            supportRepository.getFAQs()
                .onSuccess { faqs ->
                    _uiState.update { it.copy(
                        faqs = faqs,
                        isLoading = false,
                        errorMessage = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load FAQs: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Loads the current user's support messages
     */
    fun loadUserMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            supportRepository.getUserSupportMessages()
                .onSuccess { messages ->
                    _uiState.update { it.copy(
                        userMessages = messages,
                        isLoading = false,
                        errorMessage = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load your messages: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Loads replies for a specific message
     */
    fun loadRepliesForMessage(messageId: String) {
        viewModelScope.launch {
            supportRepository.getReplies(messageId)
                .onSuccess { replies ->
                    _uiState.update {
                        val newRepliesMap = it.replies.toMutableMap()
                        newRepliesMap[messageId] = replies
                        it.copy(replies = newRepliesMap)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        errorMessage = "Failed to load replies: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Sends a reply to a support message
     */
    fun sendReply(messageId: String, replyText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReply = true) }
            supportRepository.sendReply(messageId, replyText)
                .onSuccess {
                    _uiState.update { it.copy(isSendingReply = false) }
                    // Refresh replies for that message
                    loadRepliesForMessage(messageId)
                    // Also refresh the main message list to update status
                    loadUserMessages()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isSendingReply = false,
                        errorMessage = "Failed to send reply: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Sends a reply with an image attachment
     */
    fun sendReplyWithImage(messageId: String, replyText: String, imageUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isSendingReply = true,
                isUploadingImage = imageUri != null
            ) }
            
            Log.d("SupportViewModel", "Sending reply to message $messageId${if (imageUri != null) " with image" else ""}")
            
            supportRepository.sendReplyWithImage(messageId, replyText, imageUri)
                .onSuccess {
                    Log.d("SupportViewModel", "Reply sent successfully")
                    _uiState.update { it.copy(
                        isSendingReply = false,
                        isUploadingImage = false,
                        uploadProgress = 0f
                    ) }
                    // Refresh replies for that message
                    loadRepliesForMessage(messageId)
                    // Also refresh the main message list to update status
                    loadUserMessages()
                }
                .onFailure { error ->
                    Log.e("SupportViewModel", "Failed to send reply", error)
                    _uiState.update { it.copy(
                        isSendingReply = false,
                        isUploadingImage = false,
                        uploadProgress = 0f,
                        errorMessage = "Failed to send reply: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Submits a new support message
     */
    fun submitSupportMessage(subject: String, message: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                isMessageSent = false
            ) }
            
            supportRepository.submitSupportMessage(subject, message, phoneNumber)
                .onSuccess {
                    _uiState.update { it.copy(
                        isLoading = false,
                        isMessageSent = true,
                        errorMessage = null
                    ) }
                    
                    // Reload user messages to include the new one
                    loadUserMessages()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Failed to send message: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Resets the message sent flag
     */
    fun resetMessageSent() {
        _uiState.update { it.copy(isMessageSent = false) }
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Deletes a user support message
     */
    fun deleteUserMessage(messageId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            supportRepository.deleteSupportMessage(messageId)
                .onSuccess {
                    // Update UI state by removing the deleted message
                    val updatedMessages = _uiState.value.userMessages.filter { it.id != messageId }
                    _uiState.update { it.copy(
                        userMessages = updatedMessages,
                        isLoading = false,
                        errorMessage = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete message: ${error.message}"
                    ) }
                }
        }
    }
    
    /**
     * Loads operating hours from the repository
     */
    fun loadOperatingHours() {
        viewModelScope.launch {
            supportRepository.getOperatingHours()
                .onSuccess { hours ->
                    _uiState.update { it.copy(operatingHours = hours) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to load operating hours: ${error.message}") }
                }
        }
    }

    /**
     * Loads location details from the repository
     */
    fun loadLocationDetails() {
        viewModelScope.launch {
            supportRepository.getLocationDetails()
                .onSuccess { details ->
                    _uiState.update { it.copy(locationDetails = details) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to load location details: ${error.message}") }
                }
        }
    }
} 