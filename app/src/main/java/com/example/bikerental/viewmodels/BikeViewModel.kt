package com.example.bikerental.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.BikeLocation
import com.example.bikerental.models.BikeRide
import com.example.bikerental.models.TrackableBike
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class BikeViewModel : ViewModel() {
    private val TAG = "BikeViewModel"
    
    // Firebase references
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val bikesRef = database.getReference("bikes")
    private val ridesRef = database.getReference("rides")
    
    // State for bikes
    private val _availableBikes = MutableStateFlow<List<TrackableBike>>(emptyList())
    val availableBikes: StateFlow<List<TrackableBike>> = _availableBikes
    
    // State for the selected bike
    private val _selectedBike = MutableStateFlow<TrackableBike?>(null)
    val selectedBike: StateFlow<TrackableBike?> = _selectedBike
    
    // State for tracking the user's active ride
    private val _activeRide = MutableStateFlow<BikeRide?>(null)
    val activeRide: StateFlow<BikeRide?> = _activeRide
    
    // State for the bike's real-time location
    private val _bikeLocation = MutableStateFlow<LatLng?>(null)
    val bikeLocation: StateFlow<LatLng?> = _bikeLocation
    
    // Track location listeners to clean up
    private var bikeLocationListener: ValueEventListener? = null
    private var availableBikesListener: ValueEventListener? = null
    
    init {
        fetchAllBikes()
        checkForActiveRide()
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
                _selectedBike.value = bike
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
                        _selectedBike.value = it
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
            _selectedBike.value?.id?.let { bikeId ->
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
            val hourlyRate = _selectedBike.value?.price?.let {
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
            firestore.collection("users")
                .document(user.uid)
                .collection("rideHistory")
                .document(ride.id)
                .set(ride)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving ride to history", e)
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
    
    override fun onCleared() {
        super.onCleared()
        
        // Clean up listeners
        stopTrackingBike()
        availableBikesListener?.let {
            bikesRef.removeEventListener(it)
        }
    }
} 