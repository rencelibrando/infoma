package com.example.bikerental.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.BuildConfig
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import java.util.UUID

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
                Log.d(TAG, "Current user email: ${auth.currentUser?.email}")
                Log.d(TAG, "Current user display name: ${auth.currentUser?.displayName}")
                
                // Fetch bookings from main collection - temporarily remove orderBy to test index issue
                bookingsListener = db.collection("bookings")
                    .whereEqualTo("userId", userId)
                    .orderBy("startDate", Query.Direction.DESCENDING) // Re-enabled for proper sorting
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error fetching bookings", error)
                            Log.e(TAG, "Error code: ${error.code}")
                            Log.e(TAG, "Error message: ${error.message}")
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
                                Log.d(TAG, "No bookings found for user: $userId")
                                
                                // Try a simple query without ordering to see if there are any bookings at all
                                viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        val simpleQuery = db.collection("bookings")
                                            .whereEqualTo("userId", userId)
                                            .get()
                                            .await()
                                        
                                        Log.d(TAG, "Simple query found ${simpleQuery.documents.size} bookings for user: $userId")
                                        if (simpleQuery.documents.isNotEmpty()) {
                                            Log.d(TAG, "Sample booking data: ${simpleQuery.documents.first().data}")
                                        }
                                        
                                        // Also check if there are any bookings in the collection at all
                                        val allBookingsQuery = db.collection("bookings")
                                            .limit(10)
                                            .get()
                                            .await()
                                        
                                        Log.d(TAG, "Total bookings in collection: ${allBookingsQuery.documents.size}")
                                        allBookingsQuery.documents.forEach { doc ->
                                            val docUserId = doc.getString("userId")
                                            val docStatus = doc.getString("status")
                                            Log.d(TAG, "Booking ${doc.id}: userId='$docUserId', status='$docStatus'")
                                            Log.d(TAG, "User ID match: ${docUserId == userId}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error in diagnostic queries", e)
                                    }
                                }
                                
                                _bookings.value = emptyList()
                                _isLoading.value = false
                                return@launch
                            }

                            Log.d(TAG, "Received ${snapshot.documents.size} booking documents from main collection")
                            
                            // Process booking documents and fetch bike details
                            val bookingJobs = snapshot.documents.mapNotNull { document ->
                                try {
                                    // Log the raw document data for debugging
                                    Log.d(TAG, "Raw document data for ${document.id}: ${document.data}")
                                    
                                    // Use custom deserializer to handle status field properly
                                    val booking = document.data?.let { Booking.fromFirestoreDocument(it) }
                                    if (booking != null && booking.bikeId.isNotEmpty()) {
                                        // Set the ID from the document if not already set
                                        val bookingWithId = if (booking.id.isEmpty()) 
                                            booking.copy(id = document.id) 
                                        else 
                                            booking
                                        
                                        Log.d(TAG, "Processed booking: ${bookingWithId.id} for bike: ${bookingWithId.bikeId}")
                                        Log.d(TAG, "Booking status: ${bookingWithId.status} (${bookingWithId.status.name})")
                                        Log.d(TAG, "Booking details: startDate=${bookingWithId.startDate}, endDate=${bookingWithId.endDate}, totalPrice=${bookingWithId.totalPrice}")
                                        
                                        // Return async job for fetching bike details
                                        async(Dispatchers.IO) {
                                            try {
                                                Log.d(TAG, "Fetching bike details for bike: ${bookingWithId.bikeId}")
                                                val bikeDoc = db.collection("bikes").document(bookingWithId.bikeId).get().await()
                                                val bike = bikeDoc.toObject(Bike::class.java)
                                                if (bike != null) {
                                                    // Use the first image URL or default to empty string
                                                    val imageUrl = if (bike.imageUrls.isNotEmpty()) {
                                                        bike.imageUrls.first()
                                                    } else {
                                                        bike.imageUrl.ifEmpty { "" }
                                                    }
                                                    
                                                    val bookingWithDetails = BookingWithBikeDetails(
                                                        booking = bookingWithId,
                                                        bikeName = bike.name,
                                                        bikeImage = imageUrl,
                                                        bikeType = bike.type,
                                                        bikePricePerHour = bike.priceValue
                                                    )
                                                    
                                                    Log.d(TAG, "Added booking with bike details: ${bookingWithId.id} - ${bike.name} (Status: ${bookingWithDetails.status})")
                                                    bookingWithDetails
                                                } else {
                                                    // Create a placeholder if bike not found
                                                    val placeholderBooking = BookingWithBikeDetails(
                                                        booking = bookingWithId,
                                                        bikeName = "Unknown Bike",
                                                        bikeImage = "",
                                                        bikeType = "Unknown",
                                                        bikePricePerHour = 0.0
                                                    )
                                                    
                                                    Log.w(TAG, "Bike not found for booking: ${bookingWithId.id}")
                                                    placeholderBooking
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error fetching bike for booking: ${bookingWithId.id}", e)
                                                // Add placeholder with error
                                                BookingWithBikeDetails(
                                                    booking = bookingWithId,
                                                    bikeName = "Error loading bike",
                                                    bikeImage = "",
                                                    bikeType = "Unknown",
                                                    bikePricePerHour = 0.0
                                                )
                                            }
                                        }
                                    } else {
                                        Log.w(TAG, "Skipping invalid booking document: ${document.id} - booking is null or missing bikeId")
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing booking: ${document.id}", e)
                                    Log.e(TAG, "Document data: ${document.data}")
                                    null
                                }
                            }
                            
                            // Wait for all bike details to be fetched
                            val bookingsWithBikes = bookingJobs.awaitAll()
                            
                            // Sort by start date (newest first)
                            val sortedBookings = bookingsWithBikes.sortedByDescending { it.startDate }
                            
                            // Update state on main thread
                            withContext(Dispatchers.Main) {
                                _bookings.value = sortedBookings
                                _isLoading.value = false
                                
                                Log.d(TAG, "Successfully loaded ${sortedBookings.size} bookings with bike details from main collection")
                                sortedBookings.forEach { booking ->
                                    Log.d(TAG, "Final booking: ${booking.id} - ${booking.bikeName} - Status: ${booking.status}")
                                }
                            }
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
                    
                    // Get booking from main collection
                    val bookingDoc = db.collection("bookings").document(bookingId).get().await()
                    
                    if (!bookingDoc.exists()) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Booking not found")
                        return@launch
                    }
                    
                    val booking = bookingDoc.data?.let { Booking.fromFirestoreDocument(it) }
                    
                    if (booking == null) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Booking data is corrupted")
                        return@launch
                    }
                    
                    // Check if user owns this booking
                    if (booking.userId != userId) {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("You don't have permission to cancel this booking")
                        return@launch
                    }
                    
                    Log.d(TAG, "Found booking: ${booking.id} for user: ${booking.userId}")
                    Log.d(TAG, "Booking status: ${booking.status} (type: ${booking.status::class.simpleName})")
                    
                    // Check if booking is already cancelled - handle both enum and string values
                    val currentStatus = booking.status.name
                    if (currentStatus == "CANCELLED") {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Booking is already cancelled")
                        return@launch
                    }
                    
                    // Check if booking is completed
                    if (currentStatus == "COMPLETED") {
                        _isLoading.value = false
                        activeBookingOperations.remove(bookingId)
                        onError("Cannot cancel a completed booking")
                        return@launch
                    }
                    
                    Log.d(TAG, "Attempting to cancel booking with current status: $currentStatus")
                    
                    // Update booking status to cancelled
                    val updateData = mapOf(
                        "status" to "CANCELLED",
                        "cancelledAt" to System.currentTimeMillis()
                    )
                    
                    bookingDoc.reference.update(updateData).await()
                    
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
                    
                    // Provide more specific error messages based on the exception type
                    val errorMessage = when {
                        e.message?.contains("PERMISSION_DENIED") == true -> {
                            Log.e(TAG, "Permission denied when cancelling booking. User: $userId, Booking: $bookingId")
                            "Permission denied. You may not have permission to cancel this booking."
                        }
                        e.message?.contains("NOT_FOUND") == true -> {
                            "Booking not found. It may have already been cancelled or deleted."
                        }
                        e.message?.contains("UNAVAILABLE") == true -> {
                            "Service temporarily unavailable. Please try again later."
                        }
                        else -> {
                            "Failed to cancel booking: ${e.message}"
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        onError(errorMessage)
                    }
                } finally {
                    // Always remove the operation from active operations
                    activeBookingOperations.remove(bookingId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in cancelBooking", e)
                _isLoading.value = false
                activeBookingOperations.remove(bookingId)
                onError("Unexpected error: ${e.message}")
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
                
                // First check in main bookings collection
                val mainBookingPath = "bookings/$bookingId"
                Log.d(TAG, "Checking main booking path: $mainBookingPath")
                val mainBookingDoc = db.collection("bookings").document(bookingId).get().await()
                
                if (mainBookingDoc.exists()) {
                    val bookingData = mainBookingDoc.data
                    val bookingUserId = bookingData?.get("userId") as? String
                    Log.d(TAG, "Booking found in main collection: $mainBookingPath")
                    Log.d(TAG, "Booking userId: $bookingUserId, Current userId: $userId")
                    Log.d(TAG, "Booking data: $bookingData")
                    
                    withContext(Dispatchers.Main) {
                        onResult(true, mainBookingPath)
                    }
                    return@launch
                }
                
                // If not found in main collection, try a query by ID
                Log.d(TAG, "Booking not found by document ID, trying query by ID field")
                val bookingsQuery = db.collection("bookings")
                    .whereEqualTo("id", bookingId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!bookingsQuery.isEmpty) {
                    val docPath = bookingsQuery.documents[0].reference.path
                    val bookingData = bookingsQuery.documents[0].data
                    Log.d(TAG, "Booking found via ID query: $docPath")
                    Log.d(TAG, "Booking data: $bookingData")
                    withContext(Dispatchers.Main) {
                        onResult(true, docPath)
                    }
                } else {
                    // Final attempt: check all bookings for this user
                    Log.d(TAG, "Booking not found via ID query, checking all user bookings")
                    val userBookingsQuery = db.collection("bookings")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    
                    Log.d(TAG, "Found ${userBookingsQuery.documents.size} bookings for user $userId")
                    userBookingsQuery.documents.forEach { doc ->
                        Log.d(TAG, "User booking: ${doc.id} - ID field: ${doc.getString("id")} - Status: ${doc.getString("status")}")
                    }
                    
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

    /**
     * Manually refresh bookings (useful for debugging)
     */
    fun refreshBookings() {
        Log.d(TAG, "Manual refresh triggered")
        fetchUserBookings()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Create a test booking for debugging (only in debug builds)
     */
    fun createTestBooking(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!BuildConfig.DEBUG) {
            onError("Test bookings only available in debug builds")
            return
        }
        
        val userId = auth.currentUser?.uid ?: run {
            onError("User not logged in")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val testBooking = Booking(
                    id = UUID.randomUUID().toString(),
                    bikeId = "test-bike-id",
                    userId = userId,
                    userName = auth.currentUser?.displayName ?: "Test User",
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (2 * 60 * 60 * 1000), // 2 hours later
                    status = BookingStatus.CONFIRMED,
                    totalPrice = 20.0,
                    isHourly = true
                )
                
                db.collection("bookings").document(testBooking.id)
                    .set(testBooking)
                    .await()
                
                Log.d(TAG, "Test booking created: ${testBooking.id}")
                
                // Refresh bookings
                fetchUserBookings()
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test booking", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to create test booking: ${e.message}")
                }
            }
        }
    }

    /**
     * Check user authentication and permissions (debug function)
     */
    fun checkUserPermissions(onResult: (String) -> Unit) {
        if (!BuildConfig.DEBUG) {
            onResult("Permission check only available in debug builds")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = auth.currentUser
                if (user == null) {
                    withContext(Dispatchers.Main) {
                        onResult("User not authenticated")
                    }
                    return@launch
                }
                
                val userInfo = buildString {
                    appendLine("User ID: ${user.uid}")
                    appendLine("Email: ${user.email}")
                    appendLine("Display Name: ${user.displayName}")
                    appendLine("Email Verified: ${user.isEmailVerified}")
                    appendLine("Provider: ${user.providerId}")
                }
                
                // Test basic Firestore access
                try {
                    val testDoc = db.collection("bookings").limit(1).get().await()
                    val accessInfo = "Firestore Access: SUCCESS (${testDoc.documents.size} docs)"
                    
                    withContext(Dispatchers.Main) {
                        onResult("$userInfo\n$accessInfo")
                    }
                } catch (e: Exception) {
                    val accessInfo = "Firestore Access: FAILED - ${e.message}"
                    withContext(Dispatchers.Main) {
                        onResult("$userInfo\n$accessInfo")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error checking permissions: ${e.message}")
                }
            }
        }
    }

    /**
     * Debug function to list all bookings in the database
     */
    fun debugListAllBookings(onResult: (String) -> Unit) {
        if (!BuildConfig.DEBUG) {
            onResult("Debug functions only available in debug builds")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        onResult("User not authenticated")
                    }
                    return@launch
                }
                
                Log.d(TAG, "Debug: Listing all bookings for user $userId")
                
                // Get all bookings in the collection
                val allBookingsQuery = db.collection("bookings")
                    .get()
                    .await()
                
                val result = buildString {
                    appendLine("=== ALL BOOKINGS IN DATABASE ===")
                    appendLine("Total bookings: ${allBookingsQuery.documents.size}")
                    appendLine()
                    
                    allBookingsQuery.documents.forEach { doc ->
                        appendLine("Document ID: ${doc.id}")
                        appendLine("Data: ${doc.data}")
                        appendLine("User ID: ${doc.getString("userId")}")
                        appendLine("Status: ${doc.getString("status")}")
                        appendLine("Bike ID: ${doc.getString("bikeId")}")
                        appendLine("Start Date: ${doc.getLong("startDate")}")
                        appendLine("End Date: ${doc.getLong("endDate")}")
                        appendLine("Total Price: ${doc.getDouble("totalPrice")}")
                        appendLine("Created At: ${doc.getLong("createdAt")}")
                        appendLine("Is Hourly: ${doc.getBoolean("isHourly")}")
                        appendLine("---")
                    }
                    
                    appendLine()
                    appendLine("=== USER BOOKINGS ONLY ===")
                    val userBookings = allBookingsQuery.documents.filter { 
                        doc -> doc.getString("userId") == userId 
                    }
                    appendLine("User bookings: ${userBookings.size}")
                    
                    userBookings.forEach { doc ->
                        appendLine("${doc.id}: Status=${doc.getString("status")}, BikeId=${doc.getString("bikeId")}")
                    }
                }
                
                Log.d(TAG, result)
                
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error listing bookings", e)
                withContext(Dispatchers.Main) {
                    onResult("Error listing bookings: ${e.message}")
                }
            }
        }
    }
} 