package com.example.bikerental.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.FAQ
import com.example.bikerental.models.SupportMessage
import com.example.bikerental.repositories.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository
) : ViewModel() {

    // UI State for the Help & Support screen
    data class SupportUiState(
        val isLoading: Boolean = false,
        val faqs: List<FAQ> = emptyList(),
        val userMessages: List<SupportMessage> = emptyList(),
        val errorMessage: String? = null,
        val isMessageSent: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()
    
    init {
        loadFaqs()
        loadUserMessages()
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
} 