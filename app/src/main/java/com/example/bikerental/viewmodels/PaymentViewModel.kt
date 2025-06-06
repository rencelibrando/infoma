package com.example.bikerental.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.data.repository.PaymentSettings
import com.example.bikerental.data.repository.PaymentSettingsRepository
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.models.BookingStatus
import com.example.bikerental.models.Payment
import com.example.bikerental.models.PaymentRequest
import com.example.bikerental.models.PaymentStatus
import com.example.bikerental.models.UnpaidBooking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

sealed class PaymentUiState {
    object Idle : PaymentUiState()
    object Loading : PaymentUiState()
    object Success : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentSettingsRepository: PaymentSettingsRepository
) : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()
    
    private val _userPayments = MutableStateFlow<List<Payment>>(emptyList())
    val userPayments: StateFlow<List<Payment>> = _userPayments.asStateFlow()
    
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()
    
    private val _currentPaymentRequest = MutableStateFlow<PaymentRequest?>(null)
    val currentPaymentRequest: StateFlow<PaymentRequest?> = _currentPaymentRequest.asStateFlow()
    
    private val _paymentSettings = MutableStateFlow(PaymentSettings())
    val paymentSettings: StateFlow<PaymentSettings> = _paymentSettings.asStateFlow()
    
    // New state for unpaid bookings
    private val _unpaidBookings = MutableStateFlow<List<UnpaidBooking>>(emptyList())
    val unpaidBookings: StateFlow<List<UnpaidBooking>> = _unpaidBookings.asStateFlow()
    
    private val _isLoadingUnpaidBookings = MutableStateFlow(false)
    val isLoadingUnpaidBookings: StateFlow<Boolean> = _isLoadingUnpaidBookings.asStateFlow()
    
    // State for selected unpaid booking for payment
    private val _selectedUnpaidBooking = MutableStateFlow<UnpaidBooking?>(null)
    val selectedUnpaidBooking: StateFlow<UnpaidBooking?> = _selectedUnpaidBooking.asStateFlow()
    
    // Firebase listeners
    private var unpaidBookingsListener: ListenerRegistration? = null
    
    init {
        loadPaymentSettings()
        loadUnpaidBookings()
    }
    
    private fun loadPaymentSettings() {
        viewModelScope.launch {
            paymentSettingsRepository.getPaymentSettingsFlow().collect { settings ->
                _paymentSettings.value = settings
            }
        }
    }
    
    /**
     * Load unpaid bookings from Firebase
     */
    fun loadUnpaidBookings() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                _isLoadingUnpaidBookings.value = true
                
                // Cancel any existing listener
                unpaidBookingsListener?.remove()
                
                // Set up real-time listener for unpaid bookings
                unpaidBookingsListener = firestore.collection("bookings")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereIn("status", listOf("CONFIRMED", "COMPLETED"))
                    .orderBy("startDate", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _isLoadingUnpaidBookings.value = false
                            return@addSnapshotListener
                        }
                        
                        viewModelScope.launch {
                            if (snapshot == null || snapshot.isEmpty) {
                                _unpaidBookings.value = emptyList()
                                _isLoadingUnpaidBookings.value = false
                                return@launch
                            }
                            
                            // Process bookings and filter unpaid ones
                            val bookingJobs = snapshot.documents.mapNotNull { document ->
                                try {
                                    val booking = document.data?.let { Booking.fromFirestoreDocument(it) }
                                    if (booking != null) {
                                        async {
                                            // Check if this booking has been paid
                                            val hasPayment = checkIfBookingIsPaid(booking.id)
                                            if (!hasPayment) {
                                                // Fetch bike details
                                                val bike = fetchBikeDetails(booking.bikeId)
                                                UnpaidBooking(
                                                    bookingId = booking.id,
                                                    bikeId = booking.bikeId,
                                                    bikeName = bike?.name ?: "Unknown Bike",
                                                    bikeType = bike?.type ?: "Unknown Type",
                                                    bikeImageUrl = bike?.imageUrls?.firstOrNull() ?: bike?.imageUrl ?: "",
                                                    startDate = booking.startDate,
                                                    endDate = booking.endDate,
                                                    totalPrice = booking.totalPrice,
                                                    duration = if (booking.isHourly) {
                                                        val hours = (booking.endDate - booking.startDate) / (1000 * 60 * 60)
                                                        if (hours <= 1) "1 hour" else "$hours hours"
                                                    } else {
                                                        val days = ((booking.endDate - booking.startDate) / (1000 * 60 * 60 * 24)) + 1
                                                        if (days <= 1) "1 day" else "$days days"
                                                    },
                                                    status = booking.status,
                                                    isHourly = booking.isHourly,
                                                    createdAt = booking.createdAt
                                                )
                                            } else {
                                                null
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            // Wait for all jobs to complete and filter out nulls
                            val unpaidBookingsList = bookingJobs.awaitAll().filterNotNull()
                            
                            _unpaidBookings.value = unpaidBookingsList
                            _isLoadingUnpaidBookings.value = false
                        }
                    }
                
            } catch (e: Exception) {
                _isLoadingUnpaidBookings.value = false
            }
        }
    }
    
    /**
     * Check if a booking has been paid by looking for payment records
     */
    private suspend fun checkIfBookingIsPaid(bookingId: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            
            val paymentQuery = firestore.collection("payments")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("referenceNumber", bookingId)
                .whereEqualTo("status", PaymentStatus.CONFIRMED.name)
                .limit(1)
                .get()
                .await()
            
            !paymentQuery.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Fetch bike details for a booking
     */
    private suspend fun fetchBikeDetails(bikeId: String): Bike? {
        return try {
            val bikeDoc = firestore.collection("bikes").document(bikeId).get().await()
            bikeDoc.toObject(Bike::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Mark a booking as paid after successful payment
     */
    fun markBookingAsPaid(bookingId: String) {
        viewModelScope.launch {
            try {
                // Create a payment record for this booking
                val currentUser = auth.currentUser ?: return@launch
                
                // Find the unpaid booking
                val unpaidBooking = _unpaidBookings.value.find { it.bookingId == bookingId }
                    ?: return@launch
                
                // This would typically be called after a successful payment submission
                // The payment record would already be created by submitPayment
                // This is just to refresh the unpaid bookings list
                loadUnpaidBookings()
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Handle post-ride completion - check for unpaid bookings
     */
    fun handleRideCompletion(bookingId: String) {
        viewModelScope.launch {
            try {
                // Update booking status to completed
                firestore.collection("bookings").document(bookingId)
                    .update("status", "COMPLETED")
                    .await()
                
                // Refresh unpaid bookings to include this completed booking if not paid
                loadUnpaidBookings()
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Get unpaid booking by ID for payment processing
     */
    fun getUnpaidBookingById(bookingId: String): UnpaidBooking? {
        return _unpaidBookings.value.find { it.bookingId == bookingId }
    }
    
    fun setCurrentStep(step: Int) {
        _currentStep.value = step
    }
    
    fun updatePaymentRequest(request: PaymentRequest) {
        _currentPaymentRequest.value = request
    }
    
    /**
     * Set selected unpaid booking for payment
     */
    fun selectUnpaidBookingForPayment(booking: UnpaidBooking) {
        _selectedUnpaidBooking.value = booking
    }
    
    /**
     * Clear selected unpaid booking
     */
    fun clearSelectedUnpaidBooking() {
        _selectedUnpaidBooking.value = null
    }
    
    /**
     * Get payment details for selected unpaid booking
     */
    fun getPaymentDetailsForSelectedBooking(): Triple<Double, String, String>? {
        val booking = _selectedUnpaidBooking.value ?: return null
        return Triple(
            booking.totalPrice,
            booking.bikeType,
            booking.getFormattedDuration()
        )
    }
    
    fun submitPayment(
        mobileNumber: String,
        referenceNumber: String,
        amount: Double,
        bikeType: String,
        duration: String,
        screenshotUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = PaymentUiState.Loading
                
                val currentUser = auth.currentUser
                    ?: throw Exception("User not authenticated")
                
                var screenshotUrl = ""
                
                // Upload screenshot if provided
                screenshotUri?.let { uri ->
                    screenshotUrl = uploadScreenshot(uri, currentUser.uid)
                }
                
                // Get current payment settings
                val settings = _paymentSettings.value
                
                // If paying for an unpaid booking, use booking ID as reference
                val finalReferenceNumber = _selectedUnpaidBooking.value?.bookingId ?: referenceNumber
                
                // Create payment object
                val payment = Payment(
                    userId = currentUser.uid,
                    mobileNumber = mobileNumber,
                    referenceNumber = finalReferenceNumber,
                    amount = amount,
                    screenshotUrl = screenshotUrl,
                    bikeType = bikeType,
                    duration = duration,
                    status = PaymentStatus.PENDING,
                    businessName = settings.businessName,
                    gcashNumber = settings.gcashNumber
                )
                
                // Log payment details for debugging
                println("PaymentViewModel: Submitting payment - ID: ${payment.id}, User: ${payment.userId}, Amount: ${payment.amount}, Reference: ${payment.referenceNumber}")
                
                // Save to Firestore
                firestore.collection("payments")
                    .document(payment.id)
                    .set(payment)
                    .await()
                
                println("PaymentViewModel: Payment successfully saved to Firestore")
                
                _uiState.value = PaymentUiState.Success
                
                // Clear selected unpaid booking after successful payment
                _selectedUnpaidBooking.value = null
                
                // Refresh user payments and unpaid bookings
                loadUserPayments()
                loadUnpaidBookings()
                
            } catch (e: Exception) {
                println("PaymentViewModel: Error submitting payment - ${e.message}")
                e.printStackTrace()
                _uiState.value = PaymentUiState.Error("Failed to submit payment: ${e.message}")
            }
        }
    }
    
    private suspend fun uploadScreenshot(uri: Uri, userId: String): String {
        val fileName = "payment_screenshots/${userId}/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)
        
        val uploadTask = storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }
    
    fun loadUserPayments() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                val payments = firestore.collection("payments")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Payment::class.java) }
                
                _userPayments.value = payments
                
            } catch (e: Exception) {
                _uiState.value = PaymentUiState.Error("Failed to load payments: ${e.message}")
            }
        }
    }
    
    fun resetUiState() {
        _uiState.value = PaymentUiState.Idle
    }
    
    fun resetPaymentFlow() {
        _currentStep.value = 1
        _currentPaymentRequest.value = null
        _selectedUnpaidBooking.value = null
        _uiState.value = PaymentUiState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        unpaidBookingsListener?.remove()
    }
} 