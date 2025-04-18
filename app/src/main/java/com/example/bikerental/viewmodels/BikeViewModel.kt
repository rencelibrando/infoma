package com.example.bikerental.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.Bike
import com.example.bikerental.models.BikeLocation
import com.example.bikerental.models.BikeRide
import com.example.bikerental.models.Review
import com.example.bikerental.models.TrackableBike
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class BikeViewModel : ViewModel() {
    private val TAG = "BikeViewModel"
    
    // Firebase references
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val bikesRef = database.getReference("bikes")
    private val ridesRef = database.getReference("rides")
    
    // State for bikes from Firestore (new)
    private val _bikes = MutableStateFlow<List<Bike>>(emptyList())
    val bikes: StateFlow<List<Bike>> = _bikes
    
    // Loading state (new)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Error state (new)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Selected bike for detail view
    private val _selectedBike = MutableStateFlow<Bike?>(null)
    val selectedBike: StateFlow<Bike?> = _selectedBike
    
    // State for bikes
    private val _availableBikes = MutableStateFlow<List<TrackableBike>>(emptyList())
    val availableBikes: StateFlow<List<TrackableBike>> = _availableBikes
    
    // State for the selected bike
    private val _selectedTrackableBike = MutableStateFlow<TrackableBike?>(null)
    val selectedTrackableBike: StateFlow<TrackableBike?> = _selectedTrackableBike
    
    // State for tracking the user's active ride
    private val _activeRide = MutableStateFlow<BikeRide?>(null)
    val activeRide: StateFlow<BikeRide?> = _activeRide
    
    // State for the bike's real-time location
    private val _bikeLocation = MutableStateFlow<LatLng?>(null)
    val bikeLocation: StateFlow<LatLng?> = _bikeLocation
    
    // New state for bike reviews
    private val _bikeReviews = MutableStateFlow<List<Review>>(emptyList())
    val bikeReviews: StateFlow<List<Review>> = _bikeReviews
    
    // New state for average rating
    private val _averageRating = MutableStateFlow<Float>(0f)
    val averageRating: StateFlow<Float> = _averageRating
    
    // Track location listeners to clean up
    private var bikeLocationListener: ValueEventListener? = null
    private var availableBikesListener: ValueEventListener? = null
    
    // Setup a real-time listener for bike collection changes
    private var firestoreBikesListener: ListenerRegistration? = null
    private var reviewsListener: ListenerRegistration? = null
    
    // Synchronization lock for review operations
    private val reviewsLock = Any()
    
    init {
        fetchAllBikes()
        fetchBikesFromFirestore()
        setupBikesRealtimeUpdates()
        checkForActiveRide()
    }
    
    // Fetch bikes from Firestore
    fun fetchBikesFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "Starting to fetch bikes from Firestore")
                val bikesCollection = firestore.collection("bikes").get().await()
                
                // Log raw document count
                Log.d(TAG, "Raw document count from Firestore: ${bikesCollection.documents.size}")
                
                // Log each document ID before conversion
                bikesCollection.documents.forEachIndexed { index, doc ->
                    Log.d(TAG, "Document $index ID: ${doc.id}")
                }
                
                val fetchedBikes = bikesCollection.documents.mapNotNull { doc ->
                    try {
                        val bike = doc.toObject(Bike::class.java)?.copy(id = doc.id)
                        
                        // Log each conversion result
                        if (bike == null) {
                            Log.w(TAG, "Document ${doc.id} converted to null bike object")
                            // Log the document data for debugging
                            Log.w(TAG, "Document data: ${doc.data}")
                        }
                        
                        bike
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document ${doc.id}", e)
                        // Log the document data that caused the error
                        Log.e(TAG, "Document data: ${doc.data}")
                        null
                    }
                }
                
                Log.d(TAG, "Successfully fetched ${fetchedBikes.size} bikes from Firestore")
                fetchedBikes.forEach { bike ->
                    Log.d(TAG, "Bike: ${bike.id}, Name: ${bike.name}, Available: ${bike.isAvailable}, InUse: ${bike.isInUse}")
                }
                
                _bikes.value = fetchedBikes
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching bikes", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Setup a real-time listener for bike collection changes
    fun setupBikesRealtimeUpdates() {
        // Cancel any existing listener to avoid duplicates
        firestoreBikesListener?.remove()
        
        // Create a new listener for real-time updates
        firestoreBikesListener = firestore.collection("bikes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for bike updates", error)
                    _error.value = "Failed to listen for updates: ${error.message}"
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        val bikesList = snapshot.documents.mapNotNull { document -> 
                            try {
                                document.toObject(Bike::class.java)?.copy(id = document.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing bike data from Firestore", e)
                                null
                            }
                        }
                        
                        _bikes.value = bikesList
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing bike updates", e)
                    }
                }
            }
    }
    
    // Get a specific bike by ID for the detail screen
    fun getBikeById(bikeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedBike.value = null
            
            try {
                // First check in the existing bikes list to avoid a network call if possible
                val bikeFromCache = _bikes.value.find { it.id == bikeId }
                
                if (bikeFromCache != null) {
                    _selectedBike.value = bikeFromCache
                } else {
                    // If not found in cache, fetch from Firestore
                    val bikeDoc = firestore.collection("bikes").document(bikeId).get().await()
                    if (bikeDoc.exists()) {
                        val bike = bikeDoc.toObject(Bike::class.java)?.copy(id = bikeDoc.id)
                        _selectedBike.value = bike
                    } else {
                        _error.value = "Bike not found"
                    }
                }
                
                // Fetch reviews for this bike
                fetchReviewsForBike(bikeId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching bike details", e)
                _error.value = "Failed to load bike details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Upload a new bike to Firestore with image (new)
    fun uploadBike(
        name: String,
        type: String,
        price: Double,
        imageUri: Uri,
        location: LatLng,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 1. Upload image to Firebase Storage
                val storageRef = storage.reference.child("bikes/${UUID.randomUUID()}.jpg")
                val uploadTask = storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                // 2. Create bike object
                val bike = Bike(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    price = "₱${price}/hr",
                    priceValue = price,
                    imageUrl = downloadUrl,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    description = description,
                    isAvailable = true,
                    isInUse = false
                )
                
                // 3. Save to Firestore
                firestore.collection("bikes").document(bike.id)
                    .set(bike).await()
                
                // 4. Refresh the bike list
                fetchBikesFromFirestore()
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading bike", e)
                onError("Failed to upload bike: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Fetch all available bikes from Firebase Realtime Database
    private fun fetchAllBikes() {
        availableBikesListener = bikesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bikesList = mutableListOf<TrackableBike>()
                
                for (bikeSnapshot in snapshot.children) {
                    try {
                        val bike = bikeSnapshot.getValue(TrackableBike::class.java)
                        bike?.let {
                            bikesList.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing bike data", e)
                    }
                }
                
                _availableBikes.value = bikesList
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching bikes", error.toException())
            }
        })
    }
    
    // Check if the user has an active ride
    private fun checkForActiveRide() {
        auth.currentUser?.let { user ->
            ridesRef.orderByChild("userId").equalTo(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (rideSnapshot in snapshot.children) {
                            val ride = rideSnapshot.getValue(BikeRide::class.java)
                            if (ride?.status == "active") {
                                _activeRide.value = ride
                                startTrackingBike(ride.bikeId)
                                break
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error checking for active rides", error.toException())
                    }
                })
        }
    }
    
    // Select a bike for potential rental
    fun selectBike(bikeId: String) {
        viewModelScope.launch {
            try {
                val bikeSnapshot = bikesRef.child(bikeId).get().await()
                val bike = bikeSnapshot.getValue(TrackableBike::class.java)
                _selectedTrackableBike.value = bike
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting bike", e)
            }
        }
    }
    
    // Start tracking a specific bike's location (for real-time updates)
    fun startTrackingBike(bikeId: String) {
        // Remove any existing listener
        stopTrackingBike()
        
        // Add a new listener
        bikeLocationListener = bikesRef.child(bikeId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val bike = snapshot.getValue(TrackableBike::class.java)
                    bike?.let {
                        _selectedTrackableBike.value = it
                        _bikeLocation.value = LatLng(it.latitude, it.longitude)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bike location data", e)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error tracking bike location", error.toException())
            }
        })
    }
    
    // Stop tracking the bike's location
    fun stopTrackingBike() {
        bikeLocationListener?.let {
            _selectedTrackableBike.value?.id?.let { bikeId ->
                bikesRef.child(bikeId).removeEventListener(it)
            }
            bikeLocationListener = null
        }
    }
    
    // Start a new bike ride
    fun startRide(bikeId: String, userLocation: LatLng) {
        auth.currentUser?.let { user ->
            val newRideId = ridesRef.push().key ?: UUID.randomUUID().toString()
            
            val startLocation = BikeLocation(
                latitude = userLocation.latitude,
                longitude = userLocation.longitude,
                timestamp = System.currentTimeMillis()
            )
            
            val ride = BikeRide(
                id = newRideId,
                bikeId = bikeId,
                userId = user.uid,
                startTime = System.currentTimeMillis(),
                startLocation = startLocation,
                path = listOf(startLocation),
                status = "active"
            )
            
            // Update the ride in Firebase
            ridesRef.child(newRideId).setValue(ride)
                .addOnSuccessListener {
                    _activeRide.value = ride
                    
                    // Mark the bike as in use
                    bikesRef.child(bikeId).child("isAvailable").setValue(false)
                    bikesRef.child(bikeId).child("isInUse").setValue(true)
                    bikesRef.child(bikeId).child("currentRider").setValue(user.uid)
                    
                    // Start tracking the bike
                    startTrackingBike(bikeId)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error starting ride", e)
                }
        }
    }
    
    // Update bike location during a ride
    fun updateBikeLocation(bikeId: String, newLocation: LatLng) {
        val update = mapOf(
            "latitude" to newLocation.latitude,
            "longitude" to newLocation.longitude,
            "lastUpdated" to System.currentTimeMillis()
        )
        
        bikesRef.child(bikeId).updateChildren(update)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating bike location", e)
            }
        
        // Also update the ride path
        _activeRide.value?.let { ride ->
            val locationUpdate = BikeLocation(
                latitude = newLocation.latitude,
                longitude = newLocation.longitude,
                timestamp = System.currentTimeMillis()
            )
            
            ridesRef.child(ride.id).child("path")
                .push()
                .setValue(locationUpdate)
        }
    }
    
    // End the active ride
    fun endRide(finalLocation: LatLng) {
        _activeRide.value?.let { ride ->
            val endLocation = BikeLocation(
                latitude = finalLocation.latitude,
                longitude = finalLocation.longitude,
                timestamp = System.currentTimeMillis()
            )
            
            // Calculate final ride statistics
            val endTime = System.currentTimeMillis()
            val durationMinutes = (endTime - ride.startTime) / 60000.0
            
            // Extract hourly rate from price string (e.g., "₱12/hr" -> 12.0)
            val hourlyRate = _selectedTrackableBike.value?.price?.let {
                val priceValue = it.replace(Regex("[^0-9]"), "")
                priceValue.toDoubleOrNull() ?: 10.0 // Default to 10 if parsing fails
            } ?: 10.0
            
            // Calculate cost based on duration (minimum 15 minutes)
            val cost = maxOf(durationMinutes / 60.0, 0.25) * hourlyRate
            
            // Update ride in Firebase
            val updates = mapOf(
                "endTime" to endTime,
                "endLocation" to endLocation,
                "cost" to cost,
                "status" to "completed"
            )
            
            ridesRef.child(ride.id).updateChildren(updates)
                .addOnSuccessListener {
                    // Reset active ride
                    _activeRide.value = null
                    
                    // Make bike available again
                    ride.bikeId.let { bikeId ->
                        bikesRef.child(bikeId).child("isAvailable").setValue(true)
                        bikesRef.child(bikeId).child("isInUse").setValue(false)
                        bikesRef.child(bikeId).child("currentRider").setValue("")
                        bikesRef.child(bikeId).child("latitude").setValue(finalLocation.latitude)
                        bikesRef.child(bikeId).child("longitude").setValue(finalLocation.longitude)
                    }
                    
                    // Stop tracking the bike
                    stopTrackingBike()
                    
                    // Also save ride record to Firestore for history
                    saveRideToFirestore(ride.copy(
                        endTime = endTime,
                        endLocation = endLocation,
                        cost = cost,
                        status = "completed"
                    ))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error ending ride", e)
                }
        }
    }
    
    // Save completed ride to Firestore for user history
    private fun saveRideToFirestore(ride: BikeRide) {
        auth.currentUser?.let { user ->
            try {
                val rideData = ride.toMap()
                firestore.collection("users")
                    .document(user.uid)
                    .collection("rideHistory")
                    .document(ride.id)
                    .set(rideData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Ride history saved successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving ride to history", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing ride data for Firestore", e)
            }
        }
    }
    
    // Cancel a ride
    fun cancelRide() {
        _activeRide.value?.let { ride ->
            ridesRef.child(ride.id).child("status").setValue("cancelled")
                .addOnSuccessListener {
                    // Make bike available again
                    ride.bikeId.let { bikeId ->
                        bikesRef.child(bikeId).child("isAvailable").setValue(true)
                        bikesRef.child(bikeId).child("isInUse").setValue(false)
                        bikesRef.child(bikeId).child("currentRider").setValue("")
                    }
                    
                    // Reset active ride
                    _activeRide.value = null
                    
                    // Stop tracking
                    stopTrackingBike()
                }
        }
    }
    
    // Calculate distance between two points
    fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0] // Distance in meters
    }
    
    // NEW METHODS FOR REVIEWS
    
    /**
     * Submit a new review for a bike
     * Thread-safe implementation using Firestore
     */
    fun submitReview(bikeId: String, rating: Float, comment: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                Log.d(TAG, "Starting review submission for bike: $bikeId with rating: $rating")
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _isLoading.value = false
                    Log.e(TAG, "Review submission failed: User not logged in")
                    onError("You must be logged in to submit a review")
                    return@launch
                }
                
                // Get user name if available
                val userName = try {
                    val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                    userDoc.getString("displayName") ?: currentUser.displayName ?: "Anonymous User"
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user name", e)
                    currentUser.displayName ?: "Anonymous User"
                }
                
                Log.d(TAG, "Creating review as user: $userName (${currentUser.uid})")
                
                // Create review object with a unique ID
                val reviewId = UUID.randomUUID().toString()
                val review = Review(
                    id = reviewId,
                    bikeId = bikeId,
                    userId = currentUser.uid,
                    userName = userName,
                    rating = rating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )
                
                // 1. Get references
                val bikeRef = firestore.collection("bikes").document(bikeId)
                val reviewRef = bikeRef.collection("reviews").document(reviewId)
                
                // 2. First save the review
                reviewRef.set(review).await()
                
                // 3. Then fetch all reviews to calculate average
                val reviewsQuery = bikeRef.collection("reviews").get().await()
                val allReviews = ArrayList<Review>()
                
                // 4. Process all reviews
                for (doc in reviewsQuery.documents) {
                    doc.toObject(Review::class.java)?.let { reviewObject ->
                        allReviews.add(reviewObject)
                    }
                }
                
                // 5. Calculate average rating
                val averageRating = if (allReviews.isNotEmpty()) {
                    allReviews.sumOf { it.rating.toDouble() } / allReviews.size
                } else {
                    rating.toDouble()
                }.toFloat()
                
                Log.d(TAG, "Calculated average rating: $averageRating from ${allReviews.size} reviews")
                
                // 6. Update the bike's average rating
                bikeRef.update("rating", averageRating).await()
                
                // 7. Now synchronize only the UI state updates
                synchronized(reviewsLock) {
                    // Update UI state
                    _bikeReviews.value = allReviews.sortedByDescending { it.timestamp }
                    _averageRating.value = averageRating
                    
                    // Update the bike in the bikes list
                    _selectedBike.value = _selectedBike.value?.copy(rating = averageRating)
                    
                    // Update bikes list
                    val updatedBikes = _bikes.value.map { 
                        if (it.id == bikeId) it.copy(rating = averageRating) else it 
                    }
                    _bikes.value = updatedBikes
                }
                
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Top level error submitting review", e)
                _isLoading.value = false
                onError("Failed to submit review: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    /**
     * Fetch all reviews for a specific bike
     */
    fun fetchReviewsForBike(bikeId: String) {
        // First remove any existing listener in a synchronized block
        synchronized(reviewsLock) {
            reviewsListener?.remove()
            reviewsListener = null
        }
        
        Log.d(TAG, "Starting to fetch reviews for bike: $bikeId")
        
        viewModelScope.launch {
            try {
                // Set up real-time listener for reviews
                val listener = firestore.collection("bikes").document(bikeId)
                    .collection("reviews")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error listening for review updates", error)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot == null) {
                            Log.d(TAG, "Null reviews snapshot received")
                            return@addSnapshotListener
                        }
                        
                        Log.d(TAG, "Reviews snapshot received: ${snapshot.documents.size} reviews")
                        
                        val reviewsList = ArrayList<Review>()
                        
                        // Process all documents in the snapshot
                        for (document in snapshot.documents) {
                            try {
                                val review = document.toObject(Review::class.java)
                                if (review != null) {
                                    reviewsList.add(review)
                                    Log.d(TAG, "Parsed review: ${review.id} by ${review.userName}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing review data", e)
                            }
                        }
                        
                        // Sort the reviews by timestamp (newest first)
                        reviewsList.sortByDescending { it.timestamp }
                        
                        // Update state in a synchronized manner
                        synchronized(reviewsLock) {
                            _bikeReviews.value = reviewsList
                            Log.d(TAG, "Updated reviews state with ${reviewsList.size} reviews")
                            
                            // Calculate and update average rating
                            if (reviewsList.isNotEmpty()) {
                                val avgRating = reviewsList.sumOf { it.rating.toDouble() } / reviewsList.size
                                _averageRating.value = avgRating.toFloat()
                                Log.d(TAG, "Updated average rating to ${_averageRating.value}")
                            } else {
                                _averageRating.value = 0f
                                Log.d(TAG, "No reviews found, setting average rating to 0")
                            }
                        }
                    }
                
                // Store the listener in a synchronized block
                synchronized(reviewsLock) {
                    reviewsListener = listener
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up reviews listener", e)
            }
        }
    }
    
    /**
     * Update the average rating for a bike
     * Thread-safe implementation using Firestore
     */
    private suspend fun updateBikeAverageRating(bikeId: String) {
        try {
            // Get references
            val bikeRef = firestore.collection("bikes").document(bikeId)
            val reviewsRef = bikeRef.collection("reviews")
            
            // Fetch reviews outside of synchronized block
            val reviewsSnapshot = reviewsRef.get().await()
            
            // Process reviews
            val reviewsList = ArrayList<Review>()
            for (doc in reviewsSnapshot.documents) {
                try {
                    val review = doc.toObject(Review::class.java)
                    if (review != null) {
                        reviewsList.add(review)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing review during rating calculation", e)
                }
            }
            
            // Calculate average rating
            val averageRating = if (reviewsList.isNotEmpty()) {
                reviewsList.sumOf { it.rating.toDouble() } / reviewsList.size
            } else {
                0.0
            }.toFloat()
            
            Log.d(TAG, "Calculated average rating: $averageRating from ${reviewsList.size} reviews")
            
            // Update bike document with average rating
            bikeRef.update("rating", averageRating).await()
            
            // Synchronize only UI state updates
            synchronized(reviewsLock) {
                // Update local state
                _averageRating.value = averageRating
                
                // Update the bike in the bikes list
                _selectedBike.value = _selectedBike.value?.copy(rating = averageRating)
                
                // Update bikes list
                val updatedBikes = _bikes.value.map { 
                    if (it.id == bikeId) it.copy(rating = averageRating) else it 
                }
                _bikes.value = updatedBikes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bike average rating", e)
        }
    }
    
    /**
     * Delete a review by ID
     * Only the review owner can delete their own reviews
     * This implementation is thread-safe
     */
    fun deleteReview(bikeId: String, reviewId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _isLoading.value = false
                    onError("You must be logged in to delete a review")
                    return@launch
                }
                
                // Get references
                val bikeRef = firestore.collection("bikes").document(bikeId)
                val reviewDocRef = bikeRef.collection("reviews").document(reviewId)
                
                // 1. Get the review to check ownership
                val reviewSnapshot = reviewDocRef.get().await()
                
                if (!reviewSnapshot.exists()) {
                    throw Exception("Review not found")
                }
                
                val review = reviewSnapshot.toObject(Review::class.java)
                
                if (review == null) {
                    throw Exception("Error reading review data")
                }
                
                // 2. Check if current user is the owner of the review
                if (review.userId != currentUser.uid) {
                    throw Exception("You can only delete your own reviews")
                }
                
                // 3. Delete the review document
                reviewDocRef.delete().await()
                
                // 4. Get all remaining reviews for this bike to recalculate rating
                val remainingReviewsRef = bikeRef.collection("reviews")
                val remainingReviewsSnapshot = remainingReviewsRef.get().await()
                
                // 5. Process remaining reviews
                val remainingReviews = ArrayList<Review>()
                for (doc in remainingReviewsSnapshot.documents) {
                    try {
                        val reviewObj = doc.toObject(Review::class.java)
                        if (reviewObj != null) {
                            remainingReviews.add(reviewObj)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing review during rating recalculation", e)
                    }
                }
                
                // 6. Calculate new average rating
                val newAverageRating = if (remainingReviews.isNotEmpty()) {
                    remainingReviews.sumOf { it.rating.toDouble() } / remainingReviews.size
                } else {
                    0.0 // No reviews left
                }.toFloat()
                
                // 7. Update the bike's average rating
                bikeRef.update("rating", newAverageRating).await()
                
                // 8. Update local state in synchronized block
                synchronized(reviewsLock) {
                    _bikeReviews.value = remainingReviews.sortedByDescending { it.timestamp }
                    _averageRating.value = newAverageRating
                    
                    // Update the bike in the bikes list
                    _selectedBike.value = _selectedBike.value?.copy(rating = newAverageRating)
                    
                    // Update bikes list
                    val updatedBikes = _bikes.value.map { 
                        if (it.id == bikeId) it.copy(rating = newAverageRating) else it 
                    }
                    _bikes.value = updatedBikes
                }
                
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting review", e)
                _isLoading.value = false
                onError("Failed to delete review: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Clean up listeners with thread safety
        stopTrackingBike()
        
        // Safely remove database listener
        availableBikesListener?.let { listener ->
            bikesRef.removeEventListener(listener)
        }
        
        // Safely remove Firestore listeners
        synchronized(Any()) {
            firestoreBikesListener?.remove()
        }
        
        // Safely remove reviews listener
        synchronized(reviewsLock) {
            reviewsListener?.remove()
        }
    }
} 