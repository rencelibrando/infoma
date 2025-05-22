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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    init {
        fetchUserBookings()
    }

    /**
     * Fetch all bookings for the current user
     */
    fun fetchUserBookings() {
        // Safety check
        val userId = auth.currentUser?.uid ?: return
        
        _isLoading.value = true
        _error.value = null
        
        // Cancel any existing listener
        bookingsListener?.remove()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching bookings for user: $userId")
                
                // Set up a real-time listener for bookings
                bookingsListener = db.collectionGroup("bookings")
                    .whereEqualTo("userId", userId)
                    .orderBy("startDate", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error fetching bookings (Ask Gemini)", error)
                            _error.value = if (error.message?.contains("requires an index", ignoreCase = true) == true) {
                                "Firebase index error. Please create the required index by following the link in the logs."
                            } else {
                                "Failed to load bookings: ${error.message}"
                            }
                            _isLoading.value = false
                            return@addSnapshotListener
                        }

                        // Process snapshot on background thread
                        viewModelScope.launch(Dispatchers.IO) {
                            if (snapshot == null || snapshot.isEmpty) {
                                Log.d(TAG, "No bookings found for user")
                                _bookings.value = emptyList()
                                _isLoading.value = false
                                return@launch
                            }

                            Log.d(TAG, "Received ${snapshot.documents.size} booking documents")
                            
                            // Create bookings list
                            val bookingsWithBikes = mutableListOf<BookingWithBikeDetails>()
                            val tempBookingMap = ConcurrentHashMap<String, Booking>()
                            
                            // Process booking documents
                            for (document in snapshot.documents) {
                                try {
                                    val booking = document.toObject(Booking::class.java)
                                    if (booking != null && booking.bikeId.isNotEmpty()) {
                                        // Set the ID from the document if not already set
                                        val bookingWithId = if (booking.id.isEmpty()) 
                                            booking.copy(id = document.id) 
                                        else 
                                            booking
                                        tempBookingMap[bookingWithId.id] = bookingWithId
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing booking: ${document.id}", e)
                                }
                            }
                            
                            // Fetch bike details for each booking
                            val bikeDetailsJobs = tempBookingMap.values.map { booking ->
                                viewModelScope.async(Dispatchers.IO) {
                                    try {
                                        val bikeDoc = db.collection("bikes").document(booking.bikeId).get().await()
                                        val bike = bikeDoc.toObject(Bike::class.java)
                                        if (bike != null) {
                                            // Use the first image URL or default to empty string
                                            val imageUrl = if (bike.imageUrls.isNotEmpty()) {
                                                bike.imageUrls.first()
                                            } else {
                                                bike.imageUrl.ifEmpty { "" }
                                            }
                                            
                                            bookingsWithBikes.add(
                                                BookingWithBikeDetails(
                                                    booking = booking,
                                                    bikeName = bike.name,
                                                    bikeImage = imageUrl,
                                                    bikeType = bike.type,
                                                    bikePricePerHour = bike.priceValue
                                                )
                                            )
                                        } else {
                                            // Create a placeholder if bike not found
                                            bookingsWithBikes.add(
                                                BookingWithBikeDetails(
                                                    booking = booking,
                                                    bikeName = "Unknown Bike",
                                                    bikeImage = "",
                                                    bikeType = "Unknown",
                                                    bikePricePerHour = 0.0
                                                )
                                            )
                                            Log.w(TAG, "Bike not found for booking: ${booking.id}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error fetching bike for booking: ${booking.id}", e)
                                        // Add placeholder with error
                                        bookingsWithBikes.add(
                                            BookingWithBikeDetails(
                                                booking = booking,
                                                bikeName = "Error loading bike",
                                                bikeImage = "",
                                                bikeType = "Unknown",
                                                bikePricePerHour = 0.0
                                            )
                                        )
                                    }
                                }
                            }
                            
                            // Wait for all bike details to be loaded
                            bikeDetailsJobs.awaitAll()
                            
                            // Filter out any null entries and sort by start date (newest first)
                            val filteredBookings = bookingsWithBikes.filterNotNull()
                            val sortedBookings = filteredBookings.filter { it.startDate != null }
                                .sortedByDescending { it.startDate }
                            
                            // Update state
                            _bookings.value = sortedBookings
                            _isLoading.value = false
                            
                            Log.d(TAG, "Successfully loaded ${sortedBookings.size} bookings with bike details")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up bookings listener", e)
                _error.value = "Failed to load bookings: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Cancel a booking
     */
    fun cancelBooking(bookingId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                
                // Get current user ID
                val userId = auth.currentUser?.uid ?: run {
                    _isLoading.value = false
                    onError("You must be logged in to cancel a booking")
                    return@launch
                }
                
                // Mark operation as active for this booking
                val alreadyProcessing = activeBookingOperations.putIfAbsent(bookingId, true) ?: false
                if (alreadyProcessing) {
                    Log.w(TAG, "Cancellation already in progress for booking: $bookingId")
                    _isLoading.value = false
                    onError("Cancellation already in progress")
                    return@launch
                }
                
                try {
                    Log.d(TAG, "Starting cancellation for booking: $bookingId")
                    
                    // Try to get the booking from both locations
                    var booking: Booking? = null
                    
                    // First try the user's collection
                    val userBookingDoc = db.collection("users").document(userId)
                        .collection("bookings").document(bookingId).get().await()
                    
                    if (userBookingDoc.exists()) {
                        booking = userBookingDoc.toObject(Booking::class.java)
                        Log.d(TAG, "Found booking in user's collection")
                    }
                    
                    // If not found, try the main bookings collection
                    if (booking == null) {
                        val bookingDoc = db.collection("bookings").document(bookingId).get().await()
                        booking = bookingDoc.toObject(Booking::class.java)
                        Log.d(TAG, "Found booking in main bookings collection")
                    }
                    
                    if (booking == null) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Booking not found")
                        return@launch
                    }
                    
                    // Security check - only allow users to cancel their own bookings
                    if (booking.userId != userId) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("You can only cancel your own bookings")
                        return@launch
                    }
                    
                    // Check if booking is already cancelled
                    if (booking.status == BookingStatus.CANCELLED) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Booking is already cancelled")
                        return@launch
                    }
                    
                    // Check if booking is completed
                    if (booking.status == BookingStatus.COMPLETED) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Cannot cancel a completed booking")
                        return@launch
                    }
                    
                    // Create a batched write to update all booking references atomically
                    val batch = db.batch()
                    
                    // Update booking in the main bookings collection
                    val mainBookingRef = db.collection("bookings").document(bookingId)
                    batch.update(mainBookingRef, "status", BookingStatus.CANCELLED.name)
                    
                    // Update booking in the user's bookings collection
                    val userBookingRef = db.collection("users").document(userId)
                        .collection("bookings").document(bookingId)
                    batch.update(userBookingRef, "status", BookingStatus.CANCELLED.name)
                    
                    // Update booking in the bike's bookings collection if it exists
                    try {
                        // Check if the booking exists in the bike's collection
                        val bikeBookingDoc = db.collection("bikes").document(booking.bikeId)
                            .collection("bookings").document(bookingId).get().await()
                        
                        if (bikeBookingDoc.exists()) {
                            val bikeBookingRef = db.collection("bikes").document(booking.bikeId)
                                .collection("bookings").document(bookingId)
                            batch.update(bikeBookingRef, "status", BookingStatus.CANCELLED.name)
                            Log.d(TAG, "Including bike booking in the batch update")
                        }
                    } catch (e: Exception) {
                        // If the bike booking doesn't exist, just log it and continue
                        Log.w(TAG, "Bike booking reference not found, continuing with cancellation", e)
                    }
                    
                    // Commit the batch
                    batch.commit().await()
                    
                    Log.d(TAG, "Successfully cancelled booking: $bookingId")
                    
                    // Make bike available again if booking was confirmed
                    try {
                        db.collection("bikes").document(booking.bikeId)
                            .update("isAvailable", true)
                            .await()
                        Log.d(TAG, "Made bike ${booking.bikeId} available again")
                    } catch (e: Exception) {
                        // If updating the bike fails, log it but consider the cancel operation successful
                        Log.w(TAG, "Failed to update bike availability, but booking was cancelled", e)
                    }
                    
                    // Refresh bookings to show the updated status
                    fetchUserBookings()
                    
                    // Success operation completed
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        onSuccess()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cancelling booking", e)
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        onError("Failed to cancel booking: ${e.message ?: "Unknown error"}")
                    }
                } finally {
                    // Always remove active operation flag
                    activeBookingOperations.remove(bookingId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in cancelBooking outer block", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    onError("An unexpected error occurred: ${e.message ?: "Unknown error"}")
                }
                activeBookingOperations.remove(bookingId)
            }
        }
    }

    /**
     * Verify if a booking exists in Firestore
     * This is a diagnostic function to help with debugging
     */
    fun verifyBookingExists(bookingId: String, onResult: (exists: Boolean, path: String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verifying if booking exists: $bookingId")
                val userId = auth.currentUser?.uid
                
                if (userId == null) {
                    Log.e(TAG, "Cannot verify booking: User not logged in")
                    withContext(Dispatchers.Main) {
                        onResult(false, null)
                    }
                    return@launch
                }
                
                // First check in user's bookings
                val userBookingPath = "users/$userId/bookings/$bookingId"
                Log.d(TAG, "Checking user booking path: $userBookingPath")
                val userBookingDoc = db.document(userBookingPath).get().await()
                
                if (userBookingDoc.exists()) {
                    Log.d(TAG, "Booking found in user's bookings: $userBookingPath")
                    withContext(Dispatchers.Main) {
                        onResult(true, userBookingPath)
                    }
                    return@launch
                }
                
                // If not found in user's bookings, try a collection group query
                Log.d(TAG, "Booking not found in user path, trying collection group query")
                val bookingsQuery = db.collectionGroup("bookings")
                    .whereEqualTo("id", bookingId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!bookingsQuery.isEmpty) {
                    val docPath = bookingsQuery.documents[0].reference.path
                    Log.d(TAG, "Booking found via collection group query: $docPath")
                    withContext(Dispatchers.Main) {
                        onResult(true, docPath)
                    }
                } else {
                    Log.e(TAG, "Booking not found in Firestore: $bookingId")
                    withContext(Dispatchers.Main) {
                        onResult(false, null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying booking", e)
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }

    /**
     * Create a new booking
     */
    fun createBooking(booking: Booking, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                
                // Get current user ID
                val userId = auth.currentUser?.uid ?: run {
                    _isLoading.value = false
                    onError("You must be logged in to create a booking")
                    return@launch
                }
                
                Log.d(TAG, "Creating booking for bike: ${booking.bikeId}")
                
                // Security check - ensure booking belongs to current user
                val secureBooking = if (booking.userId != userId) {
                    booking.copy(userId = userId)
                } else {
                    booking
                }
                
                // Save booking to Firestore
                val bookingRef = db.collection("bookings").document(secureBooking.id)
                bookingRef.set(secureBooking).await()
                
                // Also save to user's bookings collection for easier querying
                db.collection("users")
                    .document(userId)
                    .collection("bookings")
                    .document(secureBooking.id)
                    .set(secureBooking)
                    .await()
                
                // Update bike availability status
                db.collection("bikes")
                    .document(secureBooking.bikeId)
                    .update("isAvailable", false)
                    .await()
                
                Log.d(TAG, "Successfully created booking: ${secureBooking.id}")
                
                // Success operation completed
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    onSuccess()
                }
                
                // Refresh bookings list
                fetchUserBookings()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating booking", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    onError("Failed to create booking: ${e.message}")
                }
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