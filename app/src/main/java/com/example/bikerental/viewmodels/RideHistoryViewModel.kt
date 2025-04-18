package com.example.bikerental.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.BikeRide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RideHistoryViewModel : ViewModel() {
    private val TAG = "RideHistoryViewModel"
    
    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // State for ride history
    private val _rideHistory = MutableStateFlow<List<BikeRide>>(emptyList())
    val rideHistory: StateFlow<List<BikeRide>> = _rideHistory
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        // Load ride history when the ViewModel is created
        fetchUserRideHistory()
    }
    
    /**
     * Fetch the currently logged-in user's ride history
     */
    fun fetchUserRideHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                Log.d(TAG, "Fetching ride history for user: ${currentUser.uid}")
                
                // Reference to the user's ride history collection
                val rideHistoryRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("rideHistory")
                    .orderBy("startTime", Query.Direction.DESCENDING)
                
                // Fetch the ride history
                val snapshot = rideHistoryRef.get().await()
                
                val rides = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(BikeRide::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing ride data", e)
                        null
                    }
                }
                
                Log.d(TAG, "Fetched ${rides.size} rides")
                _rideHistory.value = rides
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ride history", e)
                _error.value = "Failed to load ride history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get details for a specific ride
     */
    fun getRideDetails(rideId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }
                
                // Check if the ride is already in our state
                val existingRide = _rideHistory.value.find { it.id == rideId }
                if (existingRide != null) {
                    // Ride already in state, no need to fetch
                    return@launch
                }
                
                // Fetch the specific ride
                val rideDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("rideHistory")
                    .document(rideId)
                    .get()
                    .await()
                
                if (rideDoc.exists()) {
                    val ride = rideDoc.toObject(BikeRide::class.java)
                    if (ride != null) {
                        // Add to the ride history list
                        val updatedList = _rideHistory.value.toMutableList()
                        updatedList.add(ride)
                        _rideHistory.value = updatedList.sortedByDescending { it.startTime }
                    }
                } else {
                    _error.value = "Ride not found"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ride details", e)
                _error.value = "Failed to load ride details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 