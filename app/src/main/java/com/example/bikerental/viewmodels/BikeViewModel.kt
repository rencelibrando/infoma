package com.example.bikerental.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.BikeRentalApplication
import com.example.bikerental.models.Bike
import com.example.bikerental.models.BikeLocation
import com.example.bikerental.models.BikeRide
import com.example.bikerental.models.Booking
import com.example.bikerental.models.BookingStatus
import com.example.bikerental.models.Review
import com.example.bikerental.models.TrackableBike
import com.example.bikerental.utils.ErrorHandler
import com.example.bikerental.utils.QRCodeHelper
import com.example.bikerental.utils.FirestoreUtils
import com.example.bikerental.utils.DistanceCalculationUtils
import com.example.bikerental.utils.toBikeRideSafe
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.job

class BikeViewModel : ViewModel() {
    private val TAG = "BikeViewModel"
    
    companion object {
        @Volatile
        private var INSTANCE: BikeViewModel? = null
        
        fun getInstance(): BikeViewModel? {
            return INSTANCE
        }
        
        fun setInstance(instance: BikeViewModel) {
            INSTANCE = instance
        }
    }
    
    // Firebase references
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val bikesRef = database.getReference("bikes")
    private val ridesRef = database.getReference("rides")
    private val activeRidesRef = database.getReference("activeRides")
    private val liveLocationRef = database.getReference("liveLocation")
    
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
    
    // State for bikes (TrackableBike for backward compatibility)
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
    
    // State for bike bookings
    private val _bikeBookings = MutableStateFlow<List<Booking>>(emptyList())
    val bikeBookings: StateFlow<List<Booking>> = _bikeBookings
    
    // Loading state specifically for booking operations
    private val _isBookingLoading = MutableStateFlow(false)
    val isBookingLoading: StateFlow<Boolean> = _isBookingLoading
    
    // Error state for booking operations
    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError
    
    // Synchronization lock for booking operations
    private val bookingsLock = Any()
    
    // State for QR scanning and bike unlocking
    private val _isUnlockingBike = MutableStateFlow(false)
    val isUnlockingBike: StateFlow<Boolean> = _isUnlockingBike
    
    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError: StateFlow<String?> = _unlockError
    
    private val _unlockSuccess = MutableStateFlow(false)
    val unlockSuccess: StateFlow<Boolean> = _unlockSuccess
    
    // ENHANCED REAL-TIME TRACKING FUNCTIONALITY
    
    // State for real-time ride tracking
    private val _rideDistance = MutableStateFlow(0f)
    val rideDistance: StateFlow<Float> = _rideDistance
    
    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed
    
    private val _ridePath = MutableStateFlow<List<LatLng>>(emptyList())
    val ridePath: StateFlow<List<LatLng>> = _ridePath
    
    private val _userBearing = MutableStateFlow(0f)
    val userBearing: StateFlow<Float> = _userBearing
    
    // Added missing _maxSpeed property
    private val _maxSpeed = MutableStateFlow(0f)
    val maxSpeed: StateFlow<Float> = _maxSpeed
    
    private val _showRideRating = MutableStateFlow(false)
    val showRideRating: StateFlow<Boolean> = _showRideRating
    
    // State for completed ride data (for rating dialog)
    private val _completedRide = MutableStateFlow<BikeRide?>(null)
    val completedRide: StateFlow<BikeRide?> = _completedRide
    
    // POV Navigation and Emergency Features
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation
    
    // Emergency state
    private val _emergencyState = MutableStateFlow<String?>(null)
    val emergencyState: StateFlow<String?> = _emergencyState
    
    // Add this section for AppConfigManager to check location restrictions
    private var _isLocationRestrictionEnabled = MutableStateFlow(true) // Default to true/enabled
    val isLocationRestrictionEnabled: StateFlow<Boolean> = _isLocationRestrictionEnabled.asStateFlow()
    private var configListener: Job? = null
    
    init {
        setInstance(this)
        fetchAllBikes()
        fetchBikesFromFirestore()
        setupBikesRealtimeUpdates()
        checkForActiveRide()
        
        // Initialize AppConfigManager to monitor location restriction setting
        initAppConfigManager()
    }
    
    private fun initAppConfigManager() {
        try {
            val appConfigManager = com.example.bikerental.utils.AppConfigManager.getInstance(
                com.example.bikerental.BikeRentalApplication.instance
            )
            
            // Start collecting the configuration flow
            configListener = viewModelScope.launch {
                appConfigManager.isLocationRestrictionEnabled.collect { isEnabled ->
                    _isLocationRestrictionEnabled.value = isEnabled
                    Log.d(TAG, "Location restriction setting updated in BikeViewModel: $isEnabled")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize App Config Manager in BikeViewModel", e)
        }
    }
    
    /**
     * Fetch all available bikes from Firebase Realtime Database
     */
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
    
    /**
     * Check if the user has an active ride - ENHANCED with Firestore integration and status validation
     */
    private fun checkForActiveRide() {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    // ENHANCED: Check both Firestore and Realtime Database for active rides
                    // Priority: Firestore -> Realtime Database
                    
                    // First check Firestore (primary source)
                    val firestoreActiveRide = checkForActiveRideInFirestore(user.uid)
                    
                    if (firestoreActiveRide != null) {
                        Log.d(TAG, "Found active ride in Firestore: ${firestoreActiveRide.id}")
                        _activeRide.value = firestoreActiveRide
                        startTrackingBike(firestoreActiveRide.bikeId)
                        return@launch
                    }
                    
                    // Fallback: Check Realtime Database
                    checkActiveRideInRealtimeDatabase(user.uid)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking for active rides", e)
                    // Fallback to Realtime Database only
                    checkActiveRideInRealtimeDatabase(user.uid)
                }
            }
        }
    }
    
    /**
     * Check for active ride in Firestore with enhanced validation
     */
    private suspend fun checkForActiveRideInFirestore(userId: String): BikeRide? {
        return try {
            val query = firestore.collection("rides")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val rideDoc = query.documents.firstOrNull()
            if (rideDoc != null) {
                val ride = rideDoc.toBikeRideSafe()
                
                // CRITICAL: Validate ride status consistency
                if (ride != null) {
                    val currentTime = System.currentTimeMillis()
                    val rideAge = currentTime - ride.startTime
                    val maxRideAge = 24 * 60 * 60 * 1000L // 24 hours
                    
                    // Check if ride has endTime but status is still active (inconsistent state)
                    if (ride.endTime > 0) {
                        Log.w(TAG, "FIXING: Found active ride ${ride.id} with endTime - correcting status to completed")
                        
                        // Fix the status in Firestore
                        firestore.collection("rides").document(ride.id)
                            .update(mapOf(
                                "status" to "completed",
                                "isActive" to false,
                                "statusFixedAt" to com.google.firebase.Timestamp.now()
                            ))
                        
                        return null // Don't treat this as an active ride
                    }
                    
                    // Check if ride is too old (stale)
                    if (rideAge > maxRideAge) {
                        Log.w(TAG, "Found stale active ride: ${ride.id}, auto-completing")
                        
                        // Auto-complete stale ride
                        val endTime = System.currentTimeMillis()
                        firestore.collection("rides").document(ride.id)
                            .update(mapOf(
                                "status" to "completed",
                                "isActive" to false,
                                "endTime" to endTime,
                                "autoCompleted" to true,
                                "autoCompletedAt" to com.google.firebase.Timestamp.now()
                            ))
                        
                        return null // Don't treat this as an active ride
                    }
                    
                    // Ride is valid and truly active
                    Log.d(TAG, "Valid active ride found: ${ride.id}, started ${rideAge / 60000} minutes ago")
                    return ride
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking active ride in Firestore", e)
            null
        }
    }
    
    /**
     * Fallback check in Realtime Database
     */
    private fun checkActiveRideInRealtimeDatabase(userId: String) {
        ridesRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var foundActiveRide = false
                    
                    for (rideSnapshot in snapshot.children) {
                        val ride = rideSnapshot.getValue(BikeRide::class.java)
                        
                        // Only restore rides that are truly active and recent
                        if (ride?.status == "active") {
                            val currentTime = System.currentTimeMillis()
                            val rideAge = currentTime - ride.startTime
                            val maxRideAge = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
                            
                            // Check for inconsistent state (has endTime but marked active)
                            if (ride.endTime > 0) {
                                Log.w(TAG, "FIXING: Found RT DB active ride ${ride.id} with endTime - correcting status")
                                ridesRef.child(ride.id).updateChildren(mapOf(
                                    "status" to "completed",
                                    "isActive" to false,
                                    "statusFixedAt" to System.currentTimeMillis()
                                ))
                                continue
                            }
                            
                            // Only restore rides that are less than 24 hours old
                            if (rideAge < maxRideAge) {
                                Log.d(TAG, "Restoring active ride from RT DB: ${ride.id}, started ${rideAge / 60000} minutes ago")
                                _activeRide.value = ride
                                startTrackingBike(ride.bikeId)
                                foundActiveRide = true
                                break
                            } else {
                                // Auto-complete very old rides that might be stuck
                                Log.w(TAG, "Found stale active ride in RT DB: ${ride.id}, auto-completing")
                                ridesRef.child(ride.id).updateChildren(mapOf(
                                    "status" to "completed",
                                    "isActive" to false,
                                    "endTime" to currentTime,
                                    "autoCompleted" to true
                                ))
                            }
                        }
                    }
                    
                    if (!foundActiveRide) {
                        Log.d(TAG, "No active rides found for user")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error checking for active rides in RT DB", error.toException())
                }
            })
    }
    
    // Fetch bikes from Firestore
    fun fetchBikesFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "Starting to fetch bikes from Firestore")
                
                // Use Dispatchers.IO for Firestore operations
                val bikesCollection = withContext(Dispatchers.IO) {
                    firestore.collection("bikes").get().await()
                }
                
                // Log raw document count (reduced logging)
                Log.d(TAG, "Raw document count from Firestore: ${bikesCollection.documents.size}")
                
                // Process results on CPU dispatcher with better error handling
                val fetchedBikes = withContext(Dispatchers.Default) {
                    var successCount = 0
                    var errorCount = 0
                    
                    val bikes = bikesCollection.documents.mapNotNull { doc ->
                        try {
                            // Create bike with manual field mapping to handle type conversions
                            val data = doc.data
                            if (data != null) {
                                val bike = FirestoreUtils.createBikeFromData(doc.id, data)
                                successCount++
                                bike
                            } else {
                                errorCount++
                                Log.w(TAG, "Document ${doc.id} has null data")
                                null
                            }
                        } catch (e: Exception) {
                            errorCount++
                            // Only log the first few errors to reduce log spam
                            if (errorCount <= 3) {
                                Log.e(TAG, "Error converting document ${doc.id}: ${e.message}")
                            }
                            null
                        }
                    }
                    
                    // Summary logging instead of individual bike logging
                    Log.d(TAG, "Successfully processed $successCount bikes, $errorCount errors")
                    bikes
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
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel any existing listener to avoid duplicates
            firestoreBikesListener?.remove()
            
            try {
                // Create a new listener for real-time updates
                firestoreBikesListener = firestore.collection("bikes")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error listening for bike updates", error)
                            viewModelScope.launch(Dispatchers.Main) {
                                _error.value = "Failed to listen for updates: ${error.message}"
                            }
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null) {
                            viewModelScope.launch(Dispatchers.Default) {
                                try {
                                    var successCount = 0
                                    var errorCount = 0
                                    
                                    // Process documents on background thread
                                    val bikesList = snapshot.documents.mapNotNull { document -> 
                                        try {
                                            val data = document.data
                                            if (data != null) {
                                                val bike = FirestoreUtils.createBikeFromData(document.id, data)
                                                successCount++
                                                bike
                                            } else {
                                                errorCount++
                                                null
                                            }
                                        } catch (e: Exception) {
                                            errorCount++
                                            // Only log first error to reduce spam
                                            if (errorCount == 1) {
                                                Log.e(TAG, "Error parsing bike data from Firestore: ${document.id}", e)
                                            }
                                            null
                                        }
                                    }
                                    
                                    // Update UI on main thread
                                    withContext(Dispatchers.Main) {
                                        _bikes.value = bikesList
                                        // Reduced logging frequency
                                        if (successCount > 0 || errorCount > 0) {
                                            Log.d(TAG, "Updated bikes list: $successCount bikes, $errorCount errors")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing bike updates", e)
                                    withContext(Dispatchers.Main) {
                                        _error.value = "Error processing bike updates: ${e.message}"
                                    }
                                }
                            }
                        }
                    }
                
                Log.d(TAG, "Real-time bike updates listener established")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up real-time bike updates", e)
                withContext(Dispatchers.Main) {
                    _error.value = "Failed to setup real-time updates: ${e.message}"
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
                    // If not found in cache, fetch from Firestore on IO dispatcher
                    val bikeDoc = withContext(Dispatchers.IO) {
                        firestore.collection("bikes").document(bikeId).get().await()
                    }
                    
                    if (bikeDoc.exists()) {
                        val data = bikeDoc.data
                        if (data != null) {
                            val bike = FirestoreUtils.createBikeFromData(bikeDoc.id, data)
                            _selectedBike.value = bike
                        } else {
                            _error.value = "Bike data is null"
                        }
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
    
    /**
     * Upload a new bike to Firestore with image (new)
     */
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
                
                // Execute storage operations on IO dispatcher
                withContext(Dispatchers.IO) {
                    // 1. Upload image to Firebase Storage
                    val storageRef = storage.reference.child("bikes/${UUID.randomUUID()}.jpg")
                    storageRef.putFile(imageUri).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    
                    // 2. Create bike object
                    val bike = Bike(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        price = "₱$price/hr",
                        priceValue = price,
                        imageUrl = downloadUrl,
                        _latitude = location.latitude,
                        _longitude = location.longitude,
                        description = description,
                        isAvailable = true,
                        isInUse = false
                    )
                    
                    // 3. Save to Firestore
                    firestore.collection("bikes").document(bike.id)
                        .set(bike).await()
                }
                
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
    
    /**
     * Start tracking a specific bike's location (for real-time updates)
     * ENHANCED: Improved listener management and error handling
     */
    fun startTrackingBike(bikeId: String) {
        // Remove any existing listener first to prevent duplicates
        stopTrackingBike()
        
        Log.d(TAG, "Starting bike tracking for: $bikeId")
        
        // Add a new listener with enhanced error handling
        bikeLocationListener = bikesRef.child(bikeId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                    val bike = snapshot.getValue(TrackableBike::class.java)
                    bike?.let {
                        _selectedTrackableBike.value = it
                        _bikeLocation.value = LatLng(it.latitude, it.longitude)
                            Log.d(TAG, "Bike location updated: ${it.latitude}, ${it.longitude}")
                        }
                    } else {
                        Log.w(TAG, "Bike data not found for ID: $bikeId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bike location data for $bikeId", e)
                    // Don't stop tracking due to parsing errors, just log them
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error tracking bike location for $bikeId: ${error.message}", error.toException())
                // Clean up listener reference on cancellation
                bikeLocationListener = null
            }
        })
        
        Log.d(TAG, "Bike tracking listener added for: $bikeId")
    }
    
    /**
     * Stop tracking the bike's location
     * ENHANCED: Improved cleanup and null safety
     */
    fun stopTrackingBike() {
        bikeLocationListener?.let { listener ->
            val currentBikeId = _selectedTrackableBike.value?.id
            if (currentBikeId != null) {
                try {
                    bikesRef.child(currentBikeId).removeEventListener(listener)
                    Log.d(TAG, "Bike tracking stopped for: $currentBikeId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping bike tracking for $currentBikeId", e)
                }
            }
            bikeLocationListener = null
        }
        
        // Clear selected bike data
        _selectedTrackableBike.value = null
        _bikeLocation.value = null
    }
    
    // CENTRALIZED DISTANCE CALCULATION - Use DistanceCalculationUtils for consistency
    /**
     * Calculate distance between two geographic points in meters
     * Uses centralized DistanceCalculationUtils for consistency across the app
     */
    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return DistanceCalculationUtils.calculateDistance(lat1, lon1, lat2, lon2)
    }

    // CENTRALIZED - Use DistanceCalculationUtils (returns distance in kilometers)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return DistanceCalculationUtils.calculateDistanceKm(lat1, lon1, lat2, lon2)
    }

    // CENTRALIZED DISTANCE CALCULATION for LatLng objects
    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        return DistanceCalculationUtils.calculateDistance(start, end)
    }
    
    /**
     * Fetch reviews for a specific bike
     */
    fun fetchReviewsForBike(bikeId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Cancel any existing listener first
                    synchronized(reviewsLock) {
                        reviewsListener?.remove()
                    }
                    
                    Log.d(TAG, "Fetching reviews for bike: $bikeId")
                    
                    // We need to check both review collections:
                    // 1. Global reviews collection
                    // 2. Bike-specific reviews subcollection
                    
                    val allReviews = mutableListOf<Review>()
                    
                    try {
                        // 1. Query global reviews collection
                        val globalReviewsQuery = firestore.collection("reviews")
                            .whereEqualTo("bikeId", bikeId)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                        
                        val globalSnapshot = globalReviewsQuery.get().await()
                        val globalReviews = globalSnapshot.documents.mapNotNull { document ->
                            try {
                                document.toObject(Review::class.java)?.copy(id = document.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing global review: ${document.id}", e)
                                null
                            }
                        }
                        allReviews.addAll(globalReviews)
                        Log.d(TAG, "Found ${globalReviews.size} reviews in global collection")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching global reviews", e)
                    }
                    
                    try {
                        // 2. Query bike-specific reviews subcollection
                        val bikeReviewsQuery = firestore.collection("bikes")
                            .document(bikeId)
                            .collection("reviews")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                        
                        val bikeSnapshot = bikeReviewsQuery.get().await()
                        val bikeReviews = bikeSnapshot.documents.mapNotNull { document ->
                            try {
                                document.toObject(Review::class.java)?.copy(
                                    id = document.id,
                                    bikeId = bikeId // Ensure bikeId is set
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing bike review: ${document.id}", e)
                                null
                            }
                        }
                        allReviews.addAll(bikeReviews)
                        Log.d(TAG, "Found ${bikeReviews.size} reviews in bike subcollection")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching bike subcollection reviews", e)
                    }
                    
                    // Remove duplicates based on review content and user (in case same review exists in multiple collections)
                    val uniqueReviews = allReviews.distinctBy { "${it.userId}_${it.rating}_${it.comment}_${it.timestamp}" }
                        .sortedByDescending { it.timestamp }
                    
                    // Update the state within synchronized block
                    synchronized(reviewsLock) {
                        _bikeReviews.value = uniqueReviews
                        
                        // Calculate average rating
                        val averageRating = if (uniqueReviews.isNotEmpty()) {
                            uniqueReviews.map { it.rating }.average().toFloat()
                        } else {
                            0f
                        }
                        _averageRating.value = averageRating
                        
                        Log.d(TAG, "Total unique reviews: ${uniqueReviews.size}, average rating: $averageRating")
                        
                        // Set up real-time listener for future updates (on global reviews collection)
                        reviewsListener = firestore.collection("reviews")
                            .whereEqualTo("bikeId", bikeId)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    Log.e(TAG, "Error in reviews real-time listener", error)
                                    return@addSnapshotListener
                                }
                                
                                // Re-fetch all reviews when there's a change
                                fetchReviewsForBike(bikeId)
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up reviews fetch", e)
                synchronized(reviewsLock) {
                    _bikeReviews.value = emptyList()
                    _averageRating.value = 0f
                }
            }
        }
    }
    
    // OPTIMIZED: Validate QR code and unlock bike - streamlined implementation with improved error handling
    fun validateQRCodeAndUnlockBike(qrCode: String, userLocation: LatLng) {
        viewModelScope.launch {
            try {
                _isUnlockingBike.value = true
                _unlockError.value = null
                _unlockSuccess.value = false
                
                Log.d(TAG, "Starting QR code validation for: ${qrCode.take(20)}...")
                Log.d(TAG, "User location: ${userLocation.latitude}, ${userLocation.longitude}")
                
                // Basic validation
                if (qrCode.isBlank()) {
                    _unlockError.value = "QR code is empty. Please scan a valid bike QR code."
                    return@launch
                }
                
                // Check user authentication
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _unlockError.value = "Please log in to unlock a bike."
                    return@launch
                }
                
                Log.d(TAG, "User authenticated: ${currentUser.uid}")
                
                // GEOFENCE CHECK: Verify user is within Intramuros, but only if location restriction is enabled
                if (_isLocationRestrictionEnabled.value) {
                    val locationManager = com.example.bikerental.utils.LocationManager.getInstance(
                        com.example.bikerental.BikeRentalApplication.instance
                    )
                    
                    if (!locationManager.isWithinIntramuros(userLocation)) {
                        Log.w(TAG, "Geofence restriction: User outside Intramuros area")
                        _unlockError.value = "You must be inside Intramuros, Manila to start a ride."
                        return@launch
                    }
                } else {
                    Log.d(TAG, "Location restrictions disabled - skipping geofence check")
                }
                
                // SAFETY CHECK: Ensure no active ride exists before starting a new one
                val currentActiveRide = _activeRide.value
                if (currentActiveRide != null) {
                    Log.w(TAG, "User already has an active ride: ${currentActiveRide.id}")
                    _unlockError.value = "You already have an active ride. Please end your current ride first."
                    return@launch
                }
                
                // Double-check in Firebase for any active rides
                Log.d(TAG, "Checking Firebase for existing active rides...")
                val hasActiveRideInFirebase = checkForActiveRideInFirebase()
                if (hasActiveRideInFirebase) {
                    Log.w(TAG, "Found active ride in Firebase during QR validation")
                    _unlockError.value = "You have an active ride in progress. Please end it first or contact support if this is incorrect."
                    return@launch
                }
                
                Log.d(TAG, "No active rides found, proceeding with bike unlock...")
                
                // Validate QR code format
                if (!QRCodeHelper.isValidQRCodeFormat(qrCode)) {
                    _unlockError.value = "Invalid QR code format. Please scan a valid bike QR code."
                    return@launch
                }
                
                // Extract bike identifier
                val bikeIdentifier = QRCodeHelper.extractBikeIdentifierFromQRCode(qrCode)
                if (bikeIdentifier.isNullOrBlank()) {
                    _unlockError.value = "Could not read bike information from QR code. Please try again."
                    return@launch
                }
                
                Log.d(TAG, "Extracted bike identifier: $bikeIdentifier")
                
                // Find and validate bike
                Log.d(TAG, "Searching for bike...")
                val targetBike = findAndValidateBike(bikeIdentifier)
                if (targetBike == null) {
                    _unlockError.value = "Bike not found with code '$bikeIdentifier'. Please check the QR code and try again."
                    return@launch
                }
                
                Log.d(TAG, "Found bike: ${targetBike.id} - ${targetBike.name}")
                Log.d(TAG, "Bike status: Available=${targetBike.isAvailable}, InUse: ${targetBike.isInUse}, Locked=${targetBike.isLocked}")
                
                // Validate bike availability
                if (!targetBike.isAvailable) {
                    _unlockError.value = "This bike is currently not available for use."
                    return@launch
                }
                
                if (targetBike.isInUse) {
                    _unlockError.value = "This bike is already being used by another rider."
                    return@launch
                }
                
                if (!targetBike.isLocked) {
                    _unlockError.value = "This bike is not properly secured. Please contact support."
                    return@launch
                }
                
                // Validate QR code against specific bike
                if (!QRCodeHelper.validateQRCodeForBike(qrCode, targetBike.qrCode, targetBike.hardwareId)) {
                    _unlockError.value = "QR code does not match this bike. Please scan the correct QR code."
                    return@launch
                }
                
                Log.d(TAG, "All validations passed, starting ride...")
                
                // Execute bike unlock and ride start
                val unlockResult = executeRideStart(targetBike, userLocation, currentUser.uid)
                handleUnlockResult(unlockResult)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in QR code validation", e)
                _unlockError.value = "An error occurred while unlocking the bike: ${e.message}"
            } finally {
                _isUnlockingBike.value = false
            }
        }
    }
    
    // OPTIMIZED: Check for active rides in Firebase to prevent duplicates
    private suspend fun checkForActiveRideInFirebase(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            Log.d(TAG, "Checking for active rides for user: $userId")
            
            // Check in Realtime Database first (faster)
            val activeRideSnapshot = withContext(Dispatchers.IO) {
                try {
                    activeRidesRef.child(userId).get().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to check Realtime Database, falling back to Firestore: ${e.message}")
                    null
                }
            }
            
            if (activeRideSnapshot?.exists() == true) {
                try {
                    val activeRideData = activeRideSnapshot.getValue(Map::class.java) as? Map<String, Any>
                    val status = activeRideData?.get("status") as? String
                    
                    if (status == "active") {
                        Log.w(TAG, "Found active ride in Realtime Database")
                        
                        // Try to reconstruct ride data
                        val rideId = activeRideData["rideId"] as? String ?: activeRideData["id"] as? String
                        val bikeId = activeRideData["bikeId"] as? String
                        val startTime = (activeRideData["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        
                        if (rideId != null && bikeId != null) {
                            val restoredRide = BikeRide(
                                id = rideId,
                                bikeId = bikeId,
                                userId = userId,
                                startTime = startTime,
                                status = "active"
                            )
                            
                            // Restore the active ride to local state
                            withContext(Dispatchers.Main) {
                                _activeRide.value = restoredRide
                                startTrackingBike(bikeId)
                            }
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing active ride data from Realtime Database", e)
                }
            }
            
            // Check Firestore for consistency
            val firestoreSnapshot = withContext(Dispatchers.IO) {
                try {
                    firestore.collection("rides")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("status", "active")
                        .limit(1)
                        .get()
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to check Firestore for active rides: ${e.message}")
                    null
                }
            }
            
            if (firestoreSnapshot != null && !firestoreSnapshot.isEmpty) {
                try {
                    val rideDoc = firestoreSnapshot.documents[0]
                    val rideData = rideDoc.data
                    
                    if (rideData != null) {
                        Log.w(TAG, "Found active ride in Firestore: ${rideDoc.id}")
                        
                        val bikeId = rideData["bikeId"] as? String
                        val startTime = (rideData["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        
                        if (bikeId != null) {
                            val restoredRide = BikeRide(
                                id = rideDoc.id,
                                bikeId = bikeId,
                                userId = userId,
                                startTime = startTime,
                                status = "active"
                            )
                            
                            // Restore the active ride to local state
                            withContext(Dispatchers.Main) {
                                _activeRide.value = restoredRide
                                startTrackingBike(bikeId)
                            }
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing active ride data from Firestore", e)
                }
            }
            
            Log.d(TAG, "No active rides found for user: $userId")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for active rides", e)
            false
        }
    }
    
    // OPTIMIZED: Consolidated prerequisite validation
    private suspend fun validateRidePrerequisites(qrCode: String, userLocation: LatLng): ValidationResult {
        return withContext(Dispatchers.Default) {
            when {
                !QRCodeHelper.isValidQRCodeFormat(qrCode) -> {
                    ValidationResult(false, "Invalid QR code format. Please scan a valid bike QR code.")
                }
                
                auth.currentUser == null -> {
                    ValidationResult(false, "User not authenticated")
                }
                
                _activeRide.value != null -> {
                    ValidationResult(false, "You already have an active ride. Please end your current ride first.")
                }
                
                else -> {
                    val bikeIdentifier = QRCodeHelper.extractBikeIdentifierFromQRCode(qrCode)
                    if (bikeIdentifier == null) {
                        ValidationResult(false, "Invalid QR code format. Could not extract bike identifier.")
                    } else {
                        ValidationResult(true, null, auth.currentUser, bikeIdentifier)
                    }
                }
            }
        }
    }
    
    // OPTIMIZED: Enhanced bike search with better error handling
    private suspend fun findAndValidateBike(identifier: String): Bike? {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Searching for bike with identifier: '$identifier'")
                
                // Try primary qrCode field first
                var querySnapshot = firestore.collection("bikes")
                    .whereEqualTo("qrCode", identifier)
                    .limit(1)
                    .get()
                    .await()
                
                // Fallback to hardwareId for backward compatibility
                if (querySnapshot.documents.isEmpty()) {
                    querySnapshot = firestore.collection("bikes")
                        .whereEqualTo("hardwareId", identifier)
                        .limit(1)
                        .get()
                        .await()
                }
                
                if (querySnapshot.documents.isNotEmpty()) {
                    val document = querySnapshot.documents[0]
                    val bike = document.toObject(Bike::class.java)?.copy(id = document.id)
                    Log.d(TAG, "Found bike: ${bike?.id} - Available: ${bike?.isAvailable}, InUse: ${bike?.isInUse}")
                    bike
                } else {
                    Log.w(TAG, "No bike found with identifier: '$identifier'")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for bike", e)
            null
        }
    }
    
    // OPTIMIZED: Streamlined ride start execution with atomic operations
    private suspend fun executeRideStart(bike: Bike, userLocation: LatLng, userId: String): UnlockResult {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Starting atomic ride creation for bike: ${bike.id}")
                
                val bikeRef = firestore.collection("bikes").document(bike.id)
                
                // Execute atomic transaction
                val result = firestore.runTransaction { transaction ->
                    val bikeSnapshot = transaction.get(bikeRef)
                    val currentBike = bikeSnapshot.toObject(Bike::class.java)
                        ?: return@runTransaction UnlockResult(UnlockStatus.UNKNOWN_ERROR, "Failed to parse bike data")
                    
                    // Validate bike availability
                    when {
                        !currentBike.isAvailable -> return@runTransaction UnlockResult(UnlockStatus.BIKE_NOT_AVAILABLE)
                        currentBike.isInUse -> return@runTransaction UnlockResult(UnlockStatus.BIKE_IN_USE)
                        !currentBike.isLocked -> return@runTransaction UnlockResult(UnlockStatus.BIKE_NOT_LOCKED)
                        currentBike.currentRider.isNotBlank() -> return@runTransaction UnlockResult(UnlockStatus.BIKE_IN_USE)
                    }
                    
                    // Update bike status atomically
                    val updateData = mapOf(
                        "isAvailable" to false,
                        "isInUse" to true,
                        "isLocked" to false,
                        "currentRider" to userId,
                        "lastRideStart" to com.google.firebase.Timestamp.now(),
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    )
                    
                    transaction.update(bikeRef, updateData)
                    UnlockResult(UnlockStatus.SUCCESS)
                }.await()
                
                if (result.status == UnlockStatus.SUCCESS) {
                    // Create ride after successful bike unlock
                    createRideRecord(bike.id, userLocation, userId)
                } else {
                    result
                }
            }
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Error in ride start execution", e)
            UnlockResult(UnlockStatus.TRANSACTION_FAILED, e.message)
        }
    }
    
    // OPTIMIZED: Streamlined ride creation with better error recovery
    private suspend fun createRideRecord(bikeId: String, userLocation: LatLng, userId: String): UnlockResult {
        return try {
            val newRideId = UUID.randomUUID().toString()
            
            val startLocation = BikeLocation(
                latitude = userLocation.latitude,
                longitude = userLocation.longitude,
                timestamp = System.currentTimeMillis()
            )
            
            val ride = BikeRide(
                id = newRideId,
                bikeId = bikeId,
                userId = userId,
                startTime = System.currentTimeMillis(),
                startLocation = startLocation,
                lastLocation = startLocation,
                path = listOf(startLocation),
                status = "active"
            )
            
            // Save to Realtime Database for active tracking
            ridesRef.child(newRideId).setValue(ride).await()
            
            // Save to Firestore with enhanced data structure for web dashboard
            val firestoreRideData = mapOf(
                "id" to newRideId,
                "bikeId" to bikeId,
                "userId" to userId,
                "userName" to (auth.currentUser?.displayName ?: "Unknown User"),
                "userEmail" to (auth.currentUser?.email ?: ""),
                "startTime" to System.currentTimeMillis(),
                "startLocation" to mapOf(
                    "latitude" to userLocation.latitude,
                    "longitude" to userLocation.longitude,
                    "timestamp" to System.currentTimeMillis()
                ),
                "lastLocation" to mapOf(
                    "latitude" to userLocation.latitude,
                    "longitude" to userLocation.longitude,
                    "timestamp" to System.currentTimeMillis()
                ),
                "currentLatitude" to userLocation.latitude,
                "currentLongitude" to userLocation.longitude,
                "lastLocationUpdate" to System.currentTimeMillis(),
                "status" to "active",
                "isActive" to true,
                "currentSpeed" to 0.0,
                "bearing" to 0.0,
                "totalDistance" to 0.0,
                "path" to listOf(mapOf(
                    "latitude" to userLocation.latitude,
                    "longitude" to userLocation.longitude,
                    "timestamp" to System.currentTimeMillis()
                )),
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("rides").document(newRideId).set(firestoreRideData).await()
            
            // Update Realtime Database bike status for consistency
            val bikeUpdates = mapOf(
                "isAvailable" to false,
                "isInUse" to true,
                "isLocked" to false,
                "currentRider" to userId,
                "currentRideId" to newRideId,
                "lastUpdated" to com.google.firebase.database.ServerValue.TIMESTAMP
            )
            bikesRef.child(bikeId).updateChildren(bikeUpdates).await()
            
            // Update UI state on main thread
            withContext(Dispatchers.Main) {
                _activeRide.value = ride
                startTrackingBike(bikeId)
                
                // Start location tracking service
                try {
                    val context = BikeRentalApplication.instance
                    com.example.bikerental.services.LocationTrackingService.startLocationTracking(context, newRideId)
                    Log.d(TAG, "Location tracking service started for ride: $newRideId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start location tracking service: ${e.message}")
                }
            }
            
            Log.d(TAG, "Ride started successfully with ID: $newRideId")
            UnlockResult(UnlockStatus.SUCCESS)
            
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Error creating ride record", e)
            // Attempt to revert bike state
            revertBikeState(bikeId)
            UnlockResult(UnlockStatus.TRANSACTION_FAILED, e.message)
        }
    }
    
    // OPTIMIZED: Improved error handling for unlock results
    private fun handleUnlockResult(result: UnlockResult) {
        when (result.status) {
            UnlockStatus.SUCCESS -> {
                _unlockSuccess.value = true
                Log.d(TAG, "Bike unlocked and ride started successfully")
            }
            UnlockStatus.BIKE_NOT_AVAILABLE -> {
                _unlockError.value = "This bike is currently unavailable. Please try another bike."
            }
            UnlockStatus.BIKE_IN_USE -> {
                _unlockError.value = "This bike is already in use by another rider."
            }
            UnlockStatus.BIKE_NOT_LOCKED -> {
                _unlockError.value = "This bike is not properly locked. Please contact support."
            }
            UnlockStatus.USER_HAS_ACTIVE_RIDE -> {
                _unlockError.value = "You already have an active ride. Please end your current ride first."
            }
            UnlockStatus.TRANSACTION_FAILED -> {
                _unlockError.value = "Failed to unlock bike due to a technical issue. Please try again."
            }
            UnlockStatus.GEOFENCE_ERROR -> {
                _unlockError.value = "You must be inside Intramuros, Manila to start a ride."
            }
            UnlockStatus.UNKNOWN_ERROR -> {
                _unlockError.value = result.errorMessage ?: "An unknown error occurred."
            }
        }
    }
    
    // OPTIMIZED: Bike state reversion helper
    private suspend fun revertBikeState(bikeId: String) {
        try {
            val revertData = mapOf(
                "isAvailable" to true,
                "isInUse" to false,
                "isLocked" to true,
                "currentRider" to "",
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("bikes").document(bikeId).update(revertData).await()
            Log.d(TAG, "Bike state reverted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to revert bike state", e)
        }
    }
    
    // Reset unlock states
    fun resetUnlockStates() {
        _unlockError.value = null
        _unlockSuccess.value = false
        _isUnlockingBike.value = false
    }
    
    // Update bike location during a ride
    fun updateBikeLocation(bikeId: String, newLocation: LatLng) {
        val update = mapOf(
            "latitude" to newLocation.latitude,
            "longitude" to newLocation.longitude,
            "lastUpdated" to com.google.firebase.Timestamp.now()
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
            viewModelScope.launch {
                try {
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
                        priceValue.toDoubleOrNull() ?: 50.0 // Default to 50 if parsing fails
                    } ?: 50.0
                    
                    // Calculate cost based on duration (minimum 15 minutes)
                    val effectiveDuration = maxOf(durationMinutes, 15.0) // Minimum 15 minutes
                    val cost = (effectiveDuration / 60.0) * hourlyRate
                    
                    withContext(Dispatchers.IO) {
                        try {
                            // Stop location tracking service first
                            try {
                                val context = BikeRentalApplication.instance
                                com.example.bikerental.services.LocationTrackingService.stopLocationTracking(context)
                                Log.d(TAG, "Location tracking service stopped")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to stop location tracking service: ${e.message}")
                                // Continue with ride ending even if service stop fails
                            }
                            
                            // CRITICAL FIX: Consolidate location history into ride path BEFORE saving
                            val consolidatedRideData = consolidateRideData(ride.id, endTime, durationMinutes)
                            
                            // Use the consolidated metrics or fall back to local state
                            val finalTotalDistance = if (consolidatedRideData.totalDistance > 0) {
                                consolidatedRideData.totalDistance
                            } else {
                                _rideDistance.value.toDouble()
                            }
                            
                            val finalMaxSpeed = if (consolidatedRideData.maxSpeed > 0) {
                                consolidatedRideData.maxSpeed
                            } else {
                                _maxSpeed.value.toDouble()
                            }
                            
                            val finalAverageSpeed = if (consolidatedRideData.averageSpeed > 0) {
                                consolidatedRideData.averageSpeed
                            } else if (durationMinutes > 0) {
                                (finalTotalDistance / 1000.0) / (durationMinutes / 60.0)
                            } else {
                                0.0
                            }
                            
                            Log.d(TAG, "Final ride metrics - Distance: ${String.format("%.2f", finalTotalDistance/1000)}km, Max Speed: ${String.format("%.1f", finalMaxSpeed)}km/h, Avg Speed: ${String.format("%.1f", finalAverageSpeed)}km/h, Path Points: ${consolidatedRideData.path.size}")
                            
                            // FOCUS ON FIRESTORE ONLY - Avoid Realtime DB permission issues
                            val bikeRef = firestore.collection("bikes").document(ride.bikeId)
                            
                            // Use Firestore transaction for atomic bike status update
                            firestore.runTransaction { transaction ->
                                val bikeSnapshot = transaction.get(bikeRef)
                                
                                // Update bike status atomically in Firestore
                                val bikeUpdates = mapOf(
                                    "isAvailable" to true,
                                    "isInUse" to false,
                                    "isLocked" to true,
                                    "currentRider" to "",
                                    "currentRideId" to "",
                                    "latitude" to finalLocation.latitude,
                                    "longitude" to finalLocation.longitude,
                                    "lastRideEnd" to com.google.firebase.Timestamp.now(),
                                    "lastUpdated" to com.google.firebase.Timestamp.now()
                                )
                                
                                transaction.update(bikeRef, bikeUpdates)
                                null // Return value not needed
                            }.await()
                            
                            // ENHANCED: Update ride in Firestore with consolidated path data - CRITICAL STATUS FIX
                            val firestoreRideUpdates = mapOf(
                                "endTime" to endTime,
                                "endLocation" to mapOf(
                                    "latitude" to endLocation.latitude,
                                    "longitude" to endLocation.longitude,
                                    "timestamp" to endLocation.timestamp
                                ),
                                "cost" to cost,
                                "status" to "completed", // CRITICAL: Ensure status is set to completed
                                "isActive" to false, // CRITICAL: Mark as not active
                                "duration" to (endTime - ride.startTime),
                                // CRITICAL: Save consolidated metrics and path
                                "totalDistance" to finalTotalDistance,
                                "maxSpeed" to finalMaxSpeed,
                                "averageSpeed" to finalAverageSpeed,
                                "distanceTraveled" to finalTotalDistance, // Ensure distanceTraveled is also updated
                                "path" to consolidatedRideData.path, // Save complete path for frontend calculations
                                "pathPointCount" to consolidatedRideData.path.size,
                                "metricsSource" to if (consolidatedRideData.totalDistance > 0) "locationHistory" else "localState",
                                "updatedAt" to com.google.firebase.Timestamp.now(),
                                "completedAt" to com.google.firebase.Timestamp.now() // Add completion timestamp
                            )
                            
                            // CRITICAL: Use Firestore transaction to ensure atomic updates
                            firestore.runTransaction { transaction ->
                                val rideRef = firestore.collection("rides").document(ride.id)
                                transaction.update(rideRef, firestoreRideUpdates)
                                null
                            }.await()
                            
                            Log.d(TAG, "CRITICAL: Ride status updated to 'completed' in Firestore for ride: ${ride.id}")
                            
                            // TRY Realtime Database updates (but don't fail the ride ending for Realtime DB issues)
                            try {
                                // Update ride in Realtime Database (optional - for real-time tracking)
                                val rideUpdates = mapOf(
                                    "endTime" to endTime,
                                    "endLocation" to endLocation.toMap(),
                                    "cost" to cost,
                                    "status" to "completed",
                                    "isActive" to false,
                                    "duration" to (endTime - ride.startTime),
                                    "totalDistance" to finalTotalDistance,
                                    "maxSpeed" to finalMaxSpeed,
                                    "averageSpeed" to finalAverageSpeed
                                )
                                
                                ridesRef.child(ride.id).updateChildren(rideUpdates).await()
                                
                                // Clean up active rides and live location (optional)
                                auth.currentUser?.uid?.let { userId ->
                                    // Remove from active rides (don't await to avoid blocking)
                                    activeRidesRef.child(userId).removeValue()
                                    // Deactivate live location (don't await to avoid blocking)
                                    liveLocationRef.child(userId).child("isActive").setValue(false)
                                }
                                
                                Log.d(TAG, "Realtime Database updates completed successfully")
                            } catch (realtimeError: Exception) {
                                // Log but don't fail the ride ending for Realtime DB issues
                                Log.w(TAG, "Realtime Database updates failed (non-critical): ${realtimeError.message}")
                            }
                            
                            // Add to user's ride history in Firestore with enhanced data
                            try {
                                auth.currentUser?.uid?.let { userId ->
                                    firestore.collection("users")
                                        .document(userId)
                                        .collection("rideHistory")
                                        .document(ride.id)
                                        .set(mapOf(
                                            "rideId" to ride.id,
                                            "bikeId" to ride.bikeId,
                                            "startTime" to ride.startTime,
                                            "endTime" to endTime,
                                            "cost" to cost,
                                            "duration" to (endTime - ride.startTime),
                                            "distance" to finalTotalDistance,
                                            "maxSpeed" to finalMaxSpeed,
                                            "averageSpeed" to finalAverageSpeed,
                                            "pathPointCount" to consolidatedRideData.path.size,
                                            "createdAt" to com.google.firebase.Timestamp.now()
                                        ))
                                        .await()
                                }
                            } catch (historyError: Exception) {
                                Log.w(TAG, "Failed to save ride history (non-critical): ${historyError.message}")
                            }
                            
                            Log.d(TAG, "Ride ended successfully with cost: ₱${String.format("%.2f", cost)} and ${consolidatedRideData.path.size} GPS points saved")
                            
                            // Show ride rating dialog on main thread
                            withContext(Dispatchers.Main) {
                                val completedRide = ride.copy(
                                    endTime = endTime,
                                    endLocation = endLocation,
                                    cost = cost,
                                    status = "completed",
                                    distanceTraveled = finalTotalDistance,
                                    maxSpeed = finalMaxSpeed
                                )
                                _completedRide.value = completedRide
                                _showRideRating.value = true
                                
                                // Clear active ride state
                                _activeRide.value = null
                                stopTrackingBike()
                                
                                // Clear tracking states
                                _rideDistance.value = 0f
                                _currentSpeed.value = 0f
                                _maxSpeed.value = 0f
                                _ridePath.value = emptyList()
                                _userBearing.value = 0f
                            }
                    
                        } catch (e: Exception) {
                            Log.e(TAG, "Error ending ride", e)
                            ErrorHandler.logError(TAG, "Failed to end ride properly", e)
                            
                            // Emergency cleanup - try to reset bike state at minimum using Firestore only
                            try {
                                val emergencyBikeUpdate = mapOf(
                                    "isAvailable" to true,
                                    "isInUse" to false,
                                    "currentRider" to "",
                                    "lastUpdated" to com.google.firebase.Timestamp.now()
                                )
                                firestore.collection("bikes").document(ride.bikeId).update(emergencyBikeUpdate).await()
                                Log.d(TAG, "Emergency bike status reset completed")
                                
                                // Try to save ride with local state as emergency fallback
                                try {
                                    val emergencyRideUpdate = mapOf(
                                        "endTime" to System.currentTimeMillis(),
                                        "status" to "completed",
                                        "isActive" to false,
                                        "totalDistance" to _rideDistance.value.toDouble(),
                                        "maxSpeed" to _maxSpeed.value.toDouble(),
                                        "averageSpeed" to if (durationMinutes > 0) (_rideDistance.value / 1000.0) / (durationMinutes / 60.0) else 0.0,
                                        "updatedAt" to com.google.firebase.Timestamp.now(),
                                        "emergencyCompletion" to true
                                    )
                                    firestore.collection("rides").document(ride.id).update(emergencyRideUpdate).await()
                                    Log.d(TAG, "Emergency ride completion saved with local metrics")
                                } catch (emergencyRideError: Exception) {
                                    Log.e(TAG, "Emergency ride save also failed", emergencyRideError)
                                }
                                
                                // Clear active ride state even if some operations failed
                                withContext(Dispatchers.Main) {
                                    _activeRide.value = null
                                    stopTrackingBike()
                                    _rideDistance.value = 0f
                                    _currentSpeed.value = 0f
                                    _maxSpeed.value = 0f
                                    _ridePath.value = emptyList()
                                    _userBearing.value = 0f
                                }
                            } catch (emergencyError: Exception) {
                                Log.e(TAG, "Emergency bike reset also failed", emergencyError)
                                // Still clear UI state to prevent stuck rides
                                withContext(Dispatchers.Main) {
                                    _activeRide.value = null
                                    stopTrackingBike()
                                    _rideDistance.value = 0f
                                    _currentSpeed.value = 0f
                                    _maxSpeed.value = 0f
                                    _ridePath.value = emptyList()
                                    _userBearing.value = 0f
                                }
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Critical error during ride end", e)
                    ErrorHandler.logError(TAG, "Critical ride end failure", e)
                    
                    // Force clear UI state to prevent stuck rides
                    withContext(Dispatchers.Main) {
                        _activeRide.value = null
                        stopTrackingBike()
                        _rideDistance.value = 0f
                        _currentSpeed.value = 0f
                        _maxSpeed.value = 0f
                        _ridePath.value = emptyList()
                        _userBearing.value = 0f
                    }
                }
            }
        }
    }
    
    /**
     * Data class to hold consolidated ride metrics and path
     */
    private data class ConsolidatedRideData(
        val totalDistance: Double,
        val maxSpeed: Double,
        val averageSpeed: Double,
        val path: List<Map<String, Any>>
    )
    
    /**
     * CRITICAL FIX: Consolidate location history from Firebase into final ride metrics
     * This ensures that all tracked GPS points are converted to metrics when the ride ends
     */
    private suspend fun consolidateRideData(rideId: String, endTime: Long, durationMinutes: Double): ConsolidatedRideData {
        return try {
            // Fetch location history from Realtime Database first (faster)
            val realtimeLocations = fetchLocationHistoryFromRealtime(rideId)
            
            // If Realtime DB has sufficient data, use it
            if (realtimeLocations.size >= 2) {
                Log.d(TAG, "Using Realtime DB location history: ${realtimeLocations.size} points")
                calculateMetricsFromLocationHistory(realtimeLocations, durationMinutes)
            } else {
                // Fallback to Firestore if Realtime DB is insufficient
                Log.d(TAG, "Realtime DB insufficient (${realtimeLocations.size} points), trying Firestore...")
                val firestoreLocations = fetchLocationHistoryFromFirestore(rideId)
                
                if (firestoreLocations.size >= 2) {
                    Log.d(TAG, "Using Firestore location history: ${firestoreLocations.size} points")
                    calculateMetricsFromLocationHistory(firestoreLocations, durationMinutes)
                } else {
                    Log.w(TAG, "Insufficient location history in both databases (RT: ${realtimeLocations.size}, FS: ${firestoreLocations.size})")
                    // Return empty data to fall back to local state
                    ConsolidatedRideData(0.0, 0.0, 0.0, emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consolidating ride data: ${e.message}")
            // Return empty data to fall back to local state
            ConsolidatedRideData(0.0, 0.0, 0.0, emptyList())
        }
    }
    
    /**
     * Fetch location history from Realtime Database
     */
    private suspend fun fetchLocationHistoryFromRealtime(rideId: String): List<Map<String, Any>> {
        return try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("rideLocationHistory")
                .child(rideId)
                .orderByChild("deviceTimestamp")
                .get()
                .await()
            
            val locations = mutableListOf<Map<String, Any>>()
            snapshot.children.forEach { child ->
                val locationData = child.value as? Map<String, Any>
                if (locationData != null) {
                    locations.add(locationData)
                }
            }
            
            locations
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Realtime DB location history: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Fetch location history from Firestore (fallback)
     */
    private suspend fun fetchLocationHistoryFromFirestore(rideId: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("rideLocationHistory")
                .whereEqualTo("rideId", rideId)
                .orderBy("deviceTimestamp")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Firestore location history: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Calculate ride metrics from location history with GPS noise filtering
     */
    private fun calculateMetricsFromLocationHistory(locations: List<Map<String, Any>>, durationMinutes: Double): ConsolidatedRideData {
        if (locations.size < 2) {
            return ConsolidatedRideData(0.0, 0.0, 0.0, emptyList())
        }
        
        var totalDistance = 0.0 // in meters
        var maxSpeed = 0.0 // in km/h
        val validSpeeds = mutableListOf<Double>()
        val pathPoints = mutableListOf<Map<String, Any>>()
        
        for (i in 1 until locations.size) {
            val prev = locations[i - 1]
            val curr = locations[i]
            
            try {
                val prevLat = (prev["latitude"] as? Number)?.toDouble() ?: continue
                val prevLng = (prev["longitude"] as? Number)?.toDouble() ?: continue
                val currLat = (curr["latitude"] as? Number)?.toDouble() ?: continue
                val currLng = (curr["longitude"] as? Number)?.toDouble() ?: continue
                
                val prevTime = (prev["deviceTimestamp"] as? Number)?.toLong() ?: continue
                val currTime = (curr["deviceTimestamp"] as? Number)?.toLong() ?: continue
                
                // Validate coordinates
                if (Math.abs(prevLat) <= 90 && Math.abs(prevLng) <= 180 &&
                    Math.abs(currLat) <= 90 && Math.abs(currLng) <= 180 &&
                    prevLat != 0.0 && prevLng != 0.0 && currLat != 0.0 && currLng != 0.0) {
                    
                    // Calculate distance using centralized utility
                    val distance = DistanceCalculationUtils.calculateDistance(prevLat, prevLng, currLat, currLng)
                    
                    // Filter out unrealistic GPS jumps
                    val timeDiffMs = currTime - prevTime
                    if (timeDiffMs > 0) {
                        val timeDiffHours = timeDiffMs / 3600000.0
                        val calculatedSpeed = (distance / 1000.0) / timeDiffHours // km/h
                        
                        // Only add distance if speed is realistic (< 100 km/h for bikes)
                        if (calculatedSpeed < 100.0) {
                            totalDistance += distance
                        }
                    } else {
                        // If no time difference, add distance anyway (could be same timestamp)
                        totalDistance += distance
                    }
                    
                    // Process speed data
                    val speed = (curr["speed"] as? Number)?.toDouble() ?: 0.0
                    if (speed > 0 && speed < 100.0) { // Realistic speed range
                        validSpeeds.add(speed)
                        if (speed > maxSpeed) {
                            maxSpeed = speed
                        }
                    }
                    
                    // Add to path for frontend use
                    pathPoints.add(mapOf(
                        "latitude" to currLat,
                        "longitude" to currLng,
                        "timestamp" to currTime,
                        "speed" to speed,
                        "accuracy" to ((curr["accuracy"] as? Number)?.toDouble() ?: 0.0),
                        "bearing" to ((curr["bearing"] as? Number)?.toDouble() ?: 0.0)
                    ))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error processing location point $i: ${e.message}")
                continue
            }
        }
        
        // Calculate average speed
        val averageSpeed = if (validSpeeds.isNotEmpty()) {
            validSpeeds.average()
        } else if (durationMinutes > 0) {
            // Fallback: calculate from distance and time
            (totalDistance / 1000.0) / (durationMinutes / 60.0)
        } else {
            0.0
        }
        
        Log.d(TAG, "Calculated metrics from ${locations.size} points: Distance=${String.format("%.2f", totalDistance/1000)}km, Max Speed=${String.format("%.1f", maxSpeed)}km/h, Avg Speed=${String.format("%.1f", averageSpeed)}km/h")
        
        return ConsolidatedRideData(
            totalDistance = totalDistance,
            maxSpeed = maxSpeed,
            averageSpeed = averageSpeed,
            path = pathPoints
        )
    }
    
    /**
     * Submit ride rating and feedback
     */
    fun submitRideRating(rating: Int, feedback: String) {
        _completedRide.value?.let { ride ->
            auth.currentUser?.let { user ->
                viewModelScope.launch {
                    try {
                        val timestamp = System.currentTimeMillis()
                        
                        // Create review data for the bike reviews collection
                        val reviewData = mapOf(
                            "userId" to user.uid,
                            "bikeId" to ride.bikeId,
                            "rideId" to ride.id,
                            "rating" to rating.toFloat(), // Convert to Float for consistency
                            "comment" to feedback,
                            "timestamp" to timestamp,
                            "userName" to (user.displayName ?: user.email ?: "Anonymous User")
                        )
                        
                        // Generate unique review ID
                        val reviewId = firestore.collection("reviews").document().id
                        
                        // Save as review in global reviews collection
                        firestore.collection("reviews")
                            .document(reviewId)
                            .set(reviewData)
                            .await()
                        
                        // Also save review under the specific bike's reviews subcollection
                        firestore.collection("bikes")
                            .document(ride.bikeId)
                            .collection("reviews")
                            .document(reviewId)
                            .set(reviewData)
                            .await()
                        
                        // Update ride with rating in Firestore (but don't save to rideRatings)
                        val rideUpdate = mapOf(
                            "rating" to rating,
                            "feedback" to feedback,
                            "reviewId" to reviewId
                        )
                        firestore.collection("rides").document(ride.id).update(rideUpdate).await()
                        
                        // Try updating Realtime DB but don't fail if permission denied
                        try {
                            ridesRef.child(ride.id).updateChildren(rideUpdate).await()
                        } catch (realtimeError: Exception) {
                            Log.w(TAG, "Realtime DB rating update failed (non-critical): ${realtimeError.message}")
                        }
                        
                        // Hide rating dialog
                        withContext(Dispatchers.Main) {
                            _showRideRating.value = false
                            _completedRide.value = null
                        }
                        
                        Log.d(TAG, "Ride rating and review submitted successfully to reviews collections")
                    
                    } catch (e: Exception) {
                        Log.e(TAG, "Error submitting ride rating", e)
                        
                        // Handle specific Firebase errors
                        val errorMessage = when {
                            e.message?.contains("PERMISSION_DENIED") == true -> 
                                "Permission denied. Please check your authentication."
                            e.message?.contains("INVALID_ARGUMENT") == true -> 
                                "Invalid rating data. Please try again."
                            e.message?.contains("UNAVAILABLE") == true -> 
                                "Service temporarily unavailable. Please try again later."
                            else -> "Failed to submit rating: ${e.message}"
                        }
                        
                        withContext(Dispatchers.Main) {
                            _error.value = errorMessage
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Dismiss ride rating dialog
     */
    fun dismissRideRating() {
        _showRideRating.value = false
        _completedRide.value = null
    }
    
    /**
     * Start location tracking for active ride
     */
    fun startLocationTracking() {
        val ride = _activeRide.value
        if (ride != null) {
            val context = BikeRentalApplication.instance
            com.example.bikerental.services.LocationTrackingService.startLocationTracking(context, ride.id)
            Log.d(TAG, "Location tracking started for ride: ${ride.id}")
        } else {
            Log.w(TAG, "Cannot start location tracking - no active ride")
        }
    }

    /**
     * Clear error messages
     */
    fun clearError() {
        _error.value = null
        _unlockError.value = null
        _bookingError.value = null
    }
    
    /**
     * Clear specific error types
     */
    fun clearUnlockError() {
        _unlockError.value = null
    }
    
    fun clearBookingError() {
        _bookingError.value = null
    }
    
    /**
     * Update ride statistics during an active ride (called from LocationTrackingService)
     * 
     * ARCHITECTURE NOTE: 
     * - This method handles UI state updates only (distance, speed, path for display)
     * - All Firebase persistence is handled by LocationTrackingService to avoid duplication
     * - LocationTrackingService is the single source of truth for ride data persistence
     * - This separation ensures efficient Firebase usage and prevents duplicate writes
     * 
     * This method focuses on UI state updates only - Firebase persistence is handled by LocationTrackingService
     */
    fun updateRideStats(rideId: String, location: BikeLocation) {
        viewModelScope.launch {
            try {
                val ride = _activeRide.value ?: return@launch
                if (ride.id != rideId) return@launch
                
                // Calculate distance if we have a previous location with proper validation
                val lastLocation = ride.lastLocation
                if (lastLocation.latitude != 0.0 && lastLocation.longitude != 0.0) {
                    // Use consolidated distance calculation in meters
                    val distanceInMeters = calculateDistanceInMeters(
                        lastLocation.latitude, lastLocation.longitude,
                        location.latitude, location.longitude
                    )
                    
                    // Filter out unrealistic jumps (GPS noise/errors)
                    val timeDiffMs = location.timestamp - lastLocation.timestamp
                    if (timeDiffMs > 0) {
                        val timeDiffHours = timeDiffMs / 3600000.0 // Convert to hours
                        val speedKmh = (distanceInMeters / 1000.0) / timeDiffHours
                        
                        // Only add distance if speed is realistic (< 100 km/h for bikes)
                        if (speedKmh < 100.0) {
                            _rideDistance.value += distanceInMeters
                        } else {
                            Log.w(TAG, "Filtering unrealistic GPS jump: ${String.format("%.2f", speedKmh)} km/h")
                        }
                    } else {
                        // If no time difference, add distance anyway (could be same timestamp)
                        _rideDistance.value += distanceInMeters
                    }
                }
                
                // Update speed statistics with improved validation
                val currentSpeedKmh = location.speedKmh
                if (currentSpeedKmh > 0 && currentSpeedKmh < 100) { // Filter unrealistic speeds
                    _currentSpeed.value = currentSpeedKmh
                    if (currentSpeedKmh > _maxSpeed.value) {
                        _maxSpeed.value = currentSpeedKmh
                    }
                }
                
                // Update ride path for UI display
                val currentPath = _ridePath.value.toMutableList()
                currentPath.add(LatLng(location.latitude, location.longitude))
                _ridePath.value = currentPath
                
                // Update local ride state for UI (no Firebase updates - handled by LocationTrackingService)
                val updatedRide = ride.copy(
                    lastLocation = location,
                    distanceTraveled = _rideDistance.value.toDouble(),
                    maxSpeed = _maxSpeed.value.toDouble()
                )
                _activeRide.value = updatedRide
                
                Log.d(TAG, "Ride stats updated - Distance: ${String.format("%.2f", _rideDistance.value/1000)}km, Speed: ${String.format("%.1f", currentSpeedKmh)}km/h, Max: ${String.format("%.1f", _maxSpeed.value)}km/h")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ride statistics", e)
            }
        }
    }
    
    /**
     * Get current ride statistics for real-time display
     */
    fun getCurrentRideStats(): Triple<Long, Float, Float> {
        val ride = _activeRide.value
        return if (ride != null) {
            val duration = System.currentTimeMillis() - ride.startTime
            Triple(
                duration,
                _rideDistance.value / 1000f, // distance in kilometers
                _currentSpeed.value
            )
        } else {
            Triple(0L, 0f, 0f)
        }
    }


    
    /**
     * End ride with tracking service cleanup
     */
    fun endRideWithTracking(finalLocation: LatLng) {
            viewModelScope.launch {
                try {
                // Stop location tracking service first
                val context = BikeRentalApplication.instance
                com.example.bikerental.services.LocationTrackingService.stopLocationTracking(context)
                Log.d(TAG, "Location tracking service stopped")
                
                // Clear tracking states
                _rideDistance.value = 0f
                _currentSpeed.value = 0f
                _maxSpeed.value = 0f
                _ridePath.value = emptyList()
                _userBearing.value = 0f
                
                // End the ride normally
                endRide(finalLocation)
                
                } catch (e: Exception) {
                Log.e(TAG, "Error ending ride with tracking", e)
                // Fallback to normal end ride
                endRide(finalLocation)
            }
        }
    }
    
    /**
     * Fetch bookings for a specific bike
     */
    fun fetchBookingsForBike(bikeId: String) {
        viewModelScope.launch {
            try {
                _isBookingLoading.value = true
                _bookingError.value = null
                
                val bookingsSnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("bookings")
                        .whereEqualTo("bikeId", bikeId)
                        .orderBy("startDate", Query.Direction.ASCENDING)
                        .get()
                        .await()
                }
                
                // Process bookings outside synchronized block
                val bookingsList = bookingsSnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(Booking::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing booking data", e)
                        null
                    }
                }
                
                // Only protect state updates with synchronized
                synchronized(bookingsLock) {
                    _bikeBookings.value = bookingsList
                }
                
                Log.d(TAG, "Fetched ${bookingsList.size} bookings for bike: $bikeId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching bookings for bike", e)
                _bookingError.value = "Failed to load bookings: ${e.message}"
            } finally {
                _isBookingLoading.value = false
            }
        }
    }
    
    /**
     * Get booked dates for a specific bike (for calendar display)
     */
    fun getBookedDatesForBike(bikeId: String): List<Date> {
        return _bikeBookings.value
            .filter { it.bikeId == bikeId && it.status == BookingStatus.CONFIRMED }
            .flatMap { booking ->
                // Generate list of dates between start and end date
                val dates = mutableListOf<Date>()
                val calendar = java.util.Calendar.getInstance()
                calendar.time = Date(booking.startDate)
                
                while (!calendar.time.after(Date(booking.endDate))) {
                    dates.add(Date(calendar.timeInMillis))
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                dates
        }
    }
    
    /**
     * Create a new booking for a bike
     */
    fun createBooking(
        bikeId: String,
        startDate: Date,
        endDate: Date,
        totalCost: Double = 0.0,
        onSuccess: (Booking) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _isBookingLoading.value = true
                    _bookingError.value = null
                    
                    val bookingId = UUID.randomUUID().toString()
                    val booking = Booking(
                        id = bookingId,
                        bikeId = bikeId,
                        userId = user.uid,
                        userName = user.displayName ?: user.email ?: "Unknown User",
                        startDate = startDate.time,
                        endDate = endDate.time,
                        status = BookingStatus.CONFIRMED,
                        totalPrice = totalCost,
                        createdAt = System.currentTimeMillis(),
                        isHourly = false
                    )
                    
                    withContext(Dispatchers.IO) {
                        firestore.collection("bookings")
                            .document(bookingId)
                            .set(booking)
                            .await()
                    }
                    
                    // Refresh bookings list
                    fetchBookingsForBike(bikeId)
                    
                    onSuccess(booking)
                    Log.d(TAG, "Booking created successfully: $bookingId")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating booking", e)
                    _bookingError.value = "Failed to create booking: ${e.message}"
                    onError(e.message ?: "Unknown error")
                } finally {
                    _isBookingLoading.value = false
                }
            }
        } ?: run {
            onError("User not authenticated")
        }
    }
    
    /**
     * Overloaded version without totalCost parameter for compatibility
     */
    fun createBooking(
        bikeId: String,
        startDate: Date,
        endDate: Date,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        createBooking(
            bikeId = bikeId,
            startDate = startDate,
            endDate = endDate,
            totalCost = 0.0,
            onSuccess = { _ -> onSuccess() },
            onError = onError
        )
    }
    
    /**
     * Submit a review for a bike
     */
    fun submitReview(
        bikeId: String,
        rating: Float,
        comment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val reviewId = UUID.randomUUID().toString()
                    val review = Review(
                        id = reviewId,
                        bikeId = bikeId,
                        userId = user.uid,
                        userName = user.displayName ?: user.email ?: "Anonymous",
                        rating = rating,
                        comment = comment,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    withContext(Dispatchers.IO) {
                        // Save to global reviews collection
                        firestore.collection("reviews")
                            .document(reviewId)
                            .set(review)
                            .await()
                        
                        // Also save to bike's reviews subcollection
                        firestore.collection("bikes")
                            .document(bikeId)
                            .collection("reviews")
                            .document(reviewId)
                            .set(review)
                            .await()
                    }
                    
                    // Refresh reviews for this bike
                    fetchReviewsForBike(bikeId)
                    
                    onSuccess()
                    Log.d(TAG, "Review submitted successfully to both collections: $reviewId")
                        
                    } catch (e: Exception) {
                    Log.e(TAG, "Error submitting review", e)
                    onError("Failed to submit review: ${e.message}")
                    }
                }
            } ?: run {
            onError("User not authenticated")
        }
    }
    
    /**
     * Delete a review
     */
    fun deleteReview(
        bikeId: String,
        reviewId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
                viewModelScope.launch {
                    try {
                withContext(Dispatchers.IO) {
                    // Delete from global reviews collection
                    firestore.collection("reviews")
                        .document(reviewId)
                        .delete()
                            .await()
                    
                    // Also delete from bike's reviews subcollection
                    firestore.collection("bikes")
                        .document(bikeId)
                        .collection("reviews")
                        .document(reviewId)
                        .delete()
                        .await()
                }
                
                // Refresh reviews for the bike after deletion
                fetchReviewsForBike(bikeId)
                
                onSuccess()
                Log.d(TAG, "Review deleted successfully from both collections: $reviewId")
                        
                    } catch (e: Exception) {
                Log.e(TAG, "Error deleting review", e)
                onError("Failed to delete review: ${e.message}")
            }
        }
    }
    
    /**
     * Update current location for POV navigation
     */
    fun updateCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        
        // Update bearing calculation if we have path data
        val path = _ridePath.value
        if (path.size >= 2) {
            val lastTwo = path.takeLast(2)
            val bearing = calculateBearing(lastTwo[0], lastTwo[1])
            _userBearing.value = bearing
        }
        
        // Add to ride path if ride is active
        if (_activeRide.value != null) {
            val updatedPath = _ridePath.value.toMutableList()
            updatedPath.add(location)
            _ridePath.value = updatedPath
            
            // Update ride distance
            if (updatedPath.size >= 2) {
                val lastDistance = calculateDistance(
                    updatedPath[updatedPath.size - 2], 
                    updatedPath[updatedPath.size - 1]
                )
                _rideDistance.value += lastDistance
            }
        }
    }
    
    /**
     * Send emergency alert with current location
     */
    fun sendEmergencyAlert(emergencyType: String, location: LatLng) {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _emergencyState.value = "Sending emergency alert..."
                    
                    val emergencyId = UUID.randomUUID().toString()
                    val currentRide = _activeRide.value
                    
                    val emergencyData = mapOf(
                        "id" to emergencyId,
                        "userId" to user.uid,
                        "userName" to (user.displayName ?: user.email ?: "Unknown User"),
                        "emergencyType" to emergencyType,
                        "location" to mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude
                        ),
                        "timestamp" to System.currentTimeMillis(),
                        "rideId" to (currentRide?.id ?: ""),
                        "bikeId" to (currentRide?.bikeId ?: ""),
                        "status" to "active",
                        "priority" to when (emergencyType) {
                            "Medical Emergency", "Accident" -> "HIGH"
                            "Security Issue" -> "HIGH"
                            else -> "MEDIUM"
                        }
                    )
                    
                    withContext(Dispatchers.IO) {
                        // Save to Firebase Realtime Database for immediate admin notification
                        database.getReference("emergencies")
                            .child(emergencyId)
                            .setValue(emergencyData)
                            .await()
                        
                        // Also save to Firestore for persistent records
                        firestore.collection("emergencies")
                            .document(emergencyId)
                            .set(emergencyData)
                            .await()
                        
                        // Update ride status if active
                        currentRide?.let { ride ->
                            database.getReference("rides")
                                .child(ride.id)
                                .child("emergencyStatus")
                                .setValue(emergencyType)
                                .await()
                        }
                    }
                    _emergencyState.value = "Emergency alert sent successfully"
                    Log.d(TAG, "Emergency alert sent: $emergencyType at ${location.latitude}, ${location.longitude}")
                    kotlinx.coroutines.delay(3000)
                    _emergencyState.value = null
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending emergency alert", e)
                    _emergencyState.value = "Failed to send emergency alert"
                    kotlinx.coroutines.delay(5000)
                    _emergencyState.value = null
                }
            }
        }
    }
    private fun calculateBearing(start: LatLng, end: LatLng): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLong = Math.toRadians(end.longitude - start.longitude)
        val y = sin(deltaLong) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLong)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    fun updateCurrentSpeed(speedKmh: Float) {
        if (speedKmh >= 0 && speedKmh < 100 && speedKmh.isFinite()) {
            _currentSpeed.value = speedKmh / 3.6f
        }
    }
    fun updateMaxSpeed(speedKmh: Float) {
        if (speedKmh >= 0 && speedKmh < 100 && speedKmh.isFinite()) {
            val speedMps = speedKmh / 3.6f // Convert km/h to m/s
            if (speedMps > _maxSpeed.value) {
                _maxSpeed.value = speedMps
            }
        }
    }

    fun resetNavigationState() {
        _ridePath.value = emptyList()
        _userBearing.value = 0f
        _rideDistance.value = 0f
        _currentSpeed.value = 0f
        _maxSpeed.value = 0f
        _currentLocation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        
        // Remove all Firebase listeners
        availableBikesListener?.let { listener ->
            _selectedTrackableBike.value?.id?.let { bikeId ->
                bikesRef.child(bikeId).removeEventListener(listener)
            }
        }
        
        firestoreBikesListener?.remove()
        reviewsListener?.remove()
        
        // Stop tracking any bikes
        stopTrackingBike()
        
        // Clear singleton instance
        INSTANCE = null
        
        Log.d(TAG, "BikeViewModel cleaned up")
        
        configListener?.cancel()
    }
}

// OPTIMIZED: Data classes for better validation flow
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val user: com.google.firebase.auth.FirebaseUser? = null,
    val bikeIdentifier: String? = null
)

// Existing UnlockResult and UnlockStatus enums
data class UnlockResult(
    val status: UnlockStatus,
    val errorMessage: String? = null
)

enum class UnlockStatus {
    SUCCESS,
    BIKE_NOT_AVAILABLE,
    BIKE_IN_USE,
    BIKE_NOT_LOCKED,
    USER_HAS_ACTIVE_RIDE,
    TRANSACTION_FAILED,
    GEOFENCE_ERROR, // New status for geofence restriction
    UNKNOWN_ERROR
} 