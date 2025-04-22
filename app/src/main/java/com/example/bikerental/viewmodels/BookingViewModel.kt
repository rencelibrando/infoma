package com.example.bikerental.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.models.BookingStatus
import com.example.bikerental.models.BookingWithBikeDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * ViewModel for managing booking data and operations
 */
class BookingViewModel : ViewModel() {
    
    private val TAG = "BookingViewModel"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // LiveData for bookings
    private val _bookings = MutableStateFlow<List<BookingWithBikeDetails>>(emptyList())
    val bookings: StateFlow<List<BookingWithBikeDetails>> = _bookings
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Firebase listener registration
    private var bookingsListener: ListenerRegistration? = null
    
    // Thread-safety mechanisms
    private val bookingsLock = ReentrantLock()
    private val activeBookingOperations = ConcurrentHashMap<String, Boolean>()
    
    /**
     * Create a new booking with thread-safety considerations
     */
    fun createBooking(
        bikeId: String,
        startDate: Date,
        endDate: Date,
        isHourly: Boolean = false,
        onSuccess: (Booking) -> Unit,
        onError: (String) -> Unit
    ) {
        // Generate a stable booking ID
        val bookingId = UUID.randomUUID().toString()
        
        // Check if this operation is already in progress
        if (activeBookingOperations.putIfAbsent(bookingId, true) != null) {
            Log.w(TAG, "Booking operation already in progress")
            onError("Another booking operation is in progress")
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                Log.d(TAG, "Creating ${if (isHourly) "hourly" else "daily"} booking for bike: $bikeId, from $startDate to $endDate")
                
                // Get current user
                val currentUser = auth.currentUser
                    ?: throw Exception("User must be logged in to create a booking")
                
                Log.d(TAG, "Creating booking as user: ${currentUser.uid}")
                
                // Get bike details for pricing
                val bikeDoc = withContext(Dispatchers.IO) {
                    db.collection("bikes").document(bikeId).get().await()
                }
                
                val bike = bikeDoc.toObject(Bike::class.java)
                    ?: throw Exception("Bike not found")
                
                // Check bike availability
                if (!bike.isAvailable || bike.isInUse) {
                    Log.e(TAG, "Bike is not available: isAvailable=${bike.isAvailable}, isInUse=${bike.isInUse}")
                    throw Exception("This bike is not available for booking")
                }
                
                // Verify that the requested dates are not already booked
                Log.d(TAG, "Checking existing bookings for $bikeId between $startDate and $endDate")
                val existingBookings = withContext(Dispatchers.IO) {
                    db.collection("bookings")
                        .whereEqualTo("bikeId", bikeId)
                        .whereGreaterThanOrEqualTo("endDate", startDate)
                        .whereLessThanOrEqualTo("startDate", endDate)
                        .get()
                        .await()
                }
                
                if (!existingBookings.isEmpty) {
                    Log.e(TAG, "Bike is already booked: Found ${existingBookings.size()} conflicting bookings")
                    throw Exception("This bike is already booked for the selected dates/times")
                }
                
                // Create the booking record
                val booking = if (isHourly) {
                    Booking.createHourly(
                        bikeId = bikeId,
                        userId = currentUser.uid,
                        startDate = startDate,
                        endDate = endDate,
                        pricePerHour = bike.priceValue
                    )
                } else {
                    Booking.createDaily(
                        bikeId = bikeId,
                        userId = currentUser.uid,
                        startDate = startDate,
                        endDate = endDate,
                        pricePerHour = bike.priceValue
                    )
                }
                
                // Store booking in Firestore with a transaction for atomicity
                Log.d(TAG, "Executing Firestore transaction to create booking: ${booking.id}")
                withContext(Dispatchers.IO) {
                    db.runTransaction { transaction ->
                        // Update the bike's availability status
                        val bikeRef = db.collection("bikes").document(bikeId)
                        
                        // Check if the bike is still available in the transaction
                        val currentBike = transaction.get(bikeRef).toObject(Bike::class.java)
                        if (currentBike == null || !currentBike.isAvailable || currentBike.isInUse) {
                            Log.e(TAG, "Bike availability changed during transaction")
                            throw Exception("This bike is no longer available")
                        }
                        
                        // Main booking document
                        val bookingRef = db.collection("bookings").document(booking.id)
                        transaction.set(bookingRef, booking.toMap())
                        
                        // User's bookings collection
                        val userBookingRef = db.collection("users")
                            .document(currentUser.uid)
                            .collection("bookings")
                            .document(booking.id)
                        transaction.set(userBookingRef, booking.toMap())
                        
                        // Bike's bookings collection
                        val bikeBookingRef = db.collection("bikes")
                            .document(bikeId)
                            .collection("bookings")
                            .document(booking.id)
                        transaction.set(bikeBookingRef, booking.toMap())
                        
                        // Mark bike as in use or update availability
                        transaction.update(bikeRef, "isInUse", true)
                    }.await()
                }
                
                // Call refreshBookings() which now handles its own locking correctly
                Log.d(TAG, "Booking created successfully, refreshing bookings")
                refreshBookings()
                
                onSuccess(booking)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating booking", e)
                _error.value = e.message
                onError("Booking failed: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
                activeBookingOperations.remove(bookingId)
            }
        }
    }
    
    /**
     * Calculate booking price based on duration
     */
    private fun calculatePrice(startDate: Date, endDate: Date, pricePerHour: Double, isHourly: Boolean): Double {
        val durationInMillis = endDate.time - startDate.time
        
        return if (isHourly) {
            // Hourly rate - calculate exact hours
            val durationInHours = durationInMillis / (1000 * 60 * 60.0)
            durationInHours * pricePerHour
        } else {
            // Daily rate - calculate days and multiply by 24 hour rate
            val durationInDays = (durationInMillis / (1000 * 60 * 60 * 24.0)).toInt() + 1
            durationInDays * pricePerHour * 24
        }
    }
    
    /**
     * Refresh bookings from Firestore with thread safety
     */
    fun refreshBookings() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _error.value = "You must be logged in to view bookings"
                    _isLoading.value = false
                    Log.e(TAG, "Cannot load bookings: User not logged in")
                    return@launch
                }
                
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "Started loading bookings for user: $userId")
                
                try {
                    // Perform Firestore operations outside the lock
                    val bookingsSnapshot = withContext(Dispatchers.IO) {
                        Log.d(TAG, "Querying 'bookings' collection for userId: $userId")
                        db.collection("bookings")
                            .whereEqualTo("userId", userId)
                            .get()
                            .await()
                    }
                    
                    Log.d(TAG, "Retrieved ${bookingsSnapshot.documents.size} booking documents")
                    
                    // Process bookings and fetch bike details
                    val bookingsWithDetails = mutableListOf<BookingWithBikeDetails>()
                    
                    for (bookingDoc in bookingsSnapshot.documents) {
                        try {
                            val booking = bookingDoc.toObject(Booking::class.java)
                            
                            if (booking != null) {
                                Log.d(TAG, "Processing booking: ${bookingDoc.id} for bike: ${booking.bikeId}")
                                
                                // Create a copy with the ID from Firestore instead of reassigning
                                val bookingWithId = booking.copy(id = bookingDoc.id)
                                
                                // Get bike details - outside of the lock
                                try {
                                    val bikeDoc = withContext(Dispatchers.IO) {
                                        db.collection("bikes")
                                            .document(bookingWithId.bikeId)
                                            .get()
                                            .await()
                                    }
                                    
                                    val bike = bikeDoc.toObject(Bike::class.java)
                                    
                                    if (bike != null) {
                                        // Create booking with bike details
                                        val bookingWithDetails = BookingWithBikeDetails(
                                            id = bookingWithId.id,
                                            userId = bookingWithId.userId,
                                            bikeId = bookingWithId.bikeId,
                                            startDate = bookingWithId.startDate,
                                            endDate = bookingWithId.endDate,
                                            status = bookingWithId.status.name,
                                            totalPrice = String.format("$%.2f", bookingWithId.totalPrice),
                                            location = "${bike.latitude},${bike.longitude}",
                                            bikeName = bike.name,
                                            bikeType = bike.type,
                                            bikeImageUrl = bike.imageUrl,
                                            isHourly = bookingWithId.isHourly
                                        )
                                        
                                        bookingsWithDetails.add(bookingWithDetails)
                                        Log.d(TAG, "Added booking with bike details for ${bike.name}")
                                    } else {
                                        Log.e(TAG, "Bike details missing for ID: ${bookingWithId.bikeId}")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error fetching bike details for ID ${bookingWithId.bikeId}: ${e.message}", e)
                                }
                            } else {
                                Log.e(TAG, "Failed to parse booking document: ${bookingDoc.id}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing booking doc ${bookingDoc.id}: ${e.message}", e)
                        }
                    }
                    
                    // Sort bookings by startDate (newest first) manually since we removed the orderBy from the query
                    val sortedBookings = bookingsWithDetails.sortedByDescending { it.startDate }
                    
                    // Only lock when updating the shared state
                    Log.d(TAG, "Updating bookings state with ${sortedBookings.size} bookings")
                    bookingsLock.withLock {
                        _bookings.value = sortedBookings
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "Unknown error"
                    if (errorMsg.contains("index")) {
                        // This is a Firestore index error
                        Log.e(TAG, "Firestore index error: $errorMsg", e)
                        _error.value = "Firebase index required. Please contact the app administrator."
                    } else {
                        Log.e(TAG, "Error fetching bookings: $errorMsg", e)
                        _error.value = "Error fetching bookings: $errorMsg"
                    }
                } finally {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Top level error in refreshBookings: ${e.message}", e)
                _error.value = "Unexpected error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Set up realtime updates for user bookings
     */
    fun setupBookingsRealtimeUpdates() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "Cannot setup realtime updates: User not logged in")
                _error.value = "You must be logged in to view bookings"
                return
            }
            
            Log.d(TAG, "Setting up realtime updates for user bookings: $userId")
            
            // Remove any existing listener
            bookingsListener?.remove()
            
            // Set up new listener with modified query to avoid index requirements
            bookingsListener = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        val errorMsg = e.message ?: "Unknown error"
                        if (errorMsg.contains("index")) {
                            // This is a Firestore index error
                            Log.e(TAG, "Firestore index error: $errorMsg", e)
                            _error.value = "Firebase index required. Please contact the app administrator."
                        } else {
                            Log.e(TAG, "Listen failed: $errorMsg", e)
                            _error.value = "Failed to listen for updates: $errorMsg"
                        }
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        Log.d(TAG, "Received booking update with ${snapshot.documents.size} documents")
                        viewModelScope.launch {
                            // Refresh all bookings to get the latest data with bike details
                            refreshBookings()
                        }
                    } else {
                        Log.d(TAG, "Received null snapshot for bookings")
                    }
                }
            
            Log.d(TAG, "Realtime updates listener setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up realtime updates: ${e.message}", e)
            _error.value = "Failed to setup updates: ${e.message}"
        }
    }
    
    /**
     * Cancel a booking by ID with thread safety
     */
    fun cancelBooking(bookingId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Check if this operation is already in progress
        if (activeBookingOperations.putIfAbsent(bookingId, true) != null) {
            onError("Another operation is in progress for this booking")
            return
        }
        
        viewModelScope.launch {
            try {
                // Use a transaction to ensure atomicity - keep this outside the lock
                withContext(Dispatchers.IO) {
                    db.runTransaction { transaction ->
                        // Get the booking document
                        val bookingRef = db.collection("bookings").document(bookingId)
                        val bookingDoc = transaction.get(bookingRef)
                        val booking = bookingDoc.toObject(Booking::class.java)
                            ?: throw Exception("Booking not found")
                        
                        // Update booking status
                        transaction.update(bookingRef, "status", BookingStatus.CANCELLED.name)
                        
                        // Update user's booking copy
                        val userBookingRef = db.collection("users")
                            .document(booking.userId)
                            .collection("bookings")
                            .document(bookingId)
                        transaction.update(userBookingRef, "status", BookingStatus.CANCELLED.name)
                        
                        // Update bike's booking copy
                        val bikeBookingRef = db.collection("bikes")
                            .document(booking.bikeId)
                            .collection("bookings")
                            .document(bookingId)
                        transaction.update(bikeBookingRef, "status", BookingStatus.CANCELLED.name)
                        
                        // Mark the bike as available
                        val bikeRef = db.collection("bikes").document(booking.bikeId)
                        transaction.update(bikeRef, "isInUse", false)
                    }.await()
                }
                
                onSuccess()
                
                // Call refreshBookings() which now handles its own locking correctly
                refreshBookings()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling booking: ${e.message}", e)
                onError("Failed to cancel booking: ${e.message}")
            } finally {
                activeBookingOperations.remove(bookingId)
            }
        }
    }
    
    /**
     * Check if a specific date range is available for a bike
     */
    fun checkBikeAvailability(
        bikeId: String,
        startDate: Date,
        endDate: Date,
        onAvailable: () -> Unit,
        onUnavailable: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existingBookings = withContext(Dispatchers.IO) {
                    db.collection("bookings")
                        .whereEqualTo("bikeId", bikeId)
                        .whereEqualTo("status", BookingStatus.PENDING.name)
                        .whereGreaterThanOrEqualTo("endDate", startDate)
                        .whereLessThanOrEqualTo("startDate", endDate)
                        .get()
                        .await()
                }
                
                if (existingBookings.isEmpty) {
                    onAvailable()
                } else {
                    onUnavailable("This bike is already booked for the selected dates/times")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking bike availability", e)
                onUnavailable("Failed to check availability: ${e.message}")
            }
        }
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
    }
} 