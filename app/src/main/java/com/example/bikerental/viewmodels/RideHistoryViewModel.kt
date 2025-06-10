package com.example.bikerental.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.BikeRide
import com.example.bikerental.utils.toBikeRideSafe
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
     * Fetch the currently logged-in user's ride history with enhanced status validation
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
                
                // ENHANCED: Fetch from main rides collection instead of user's subcollection for better consistency
                val mainRidesQuery = firestore.collection("rides")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("startTime", Query.Direction.DESCENDING)
                    .limit(50) // Limit to 50 recent rides for performance
                
                // Also fetch from user's ride history subcollection as fallback
                val userRideHistoryQuery = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("rideHistory")
                    .orderBy("startTime", Query.Direction.DESCENDING)
                    .limit(50)
                
                // Execute both queries in parallel
                val mainRidesSnapshot = mainRidesQuery.get().await()
                val userHistorySnapshot = userRideHistoryQuery.get().await()
                
                // Parse rides from main collection (primary source)
                val mainRides = mainRidesSnapshot.documents.mapNotNull { document ->
                    try {
                        val ride = document.toBikeRideSafe()
                        // CRITICAL: Validate and fix ride status based on endTime
                        ride?.let { validatedRide ->
                            if (validatedRide.endTime > 0 && validatedRide.status == "active") {
                                Log.w(TAG, "FIXING: Found ride ${validatedRide.id} marked as 'active' but has endTime - correcting status to 'completed'")
                                // Return corrected ride data
                                validatedRide.copy(status = "completed")
                            } else {
                                validatedRide
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing main ride data", e)
                        null
                    }
                }
                
                // Parse rides from user history (fallback/supplementary)
                val historyRides = userHistorySnapshot.documents.mapNotNull { document ->
                    try {
                        val ride = document.toBikeRideSafe()
                        // Apply same status validation
                        ride?.let { validatedRide ->
                            if (validatedRide.endTime > 0 && validatedRide.status == "active") {
                                Log.w(TAG, "FIXING: Found history ride ${validatedRide.id} marked as 'active' but has endTime - correcting status")
                                validatedRide.copy(status = "completed")
                            } else {
                                validatedRide
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing history ride data", e)
                        null
                    }
                }
                
                // Merge and deduplicate rides, prioritizing main collection data
                val allRides = mutableMapOf<String, BikeRide>()
                
                // Add history rides first (lower priority)
                historyRides.forEach { ride ->
                    allRides[ride.id] = ride
                }
                
                // Add main rides second (higher priority - will overwrite duplicates)
                mainRides.forEach { ride ->
                    allRides[ride.id] = ride
                }
                
                // Sort by startTime descending
                val sortedRides = allRides.values.sortedByDescending { it.startTime }
                
                Log.d(TAG, "Fetched ${sortedRides.size} rides (${mainRides.size} from main, ${historyRides.size} from history)")
                _rideHistory.value = sortedRides
                
                // BACKGROUND TASK: Fix any rides with incorrect status in Firebase
                launch {
                    fixIncorrectRideStatuses(sortedRides, currentUser.uid)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ride history", e)
                _error.value = "Failed to load ride history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Background task to fix any rides with incorrect status in Firebase
     */
    private suspend fun fixIncorrectRideStatuses(rides: List<BikeRide>, userId: String) {
        try {
            val ridesToFix = rides.filter { ride ->
                ride.endTime > 0 && ride.status == "active"
            }
            
            if (ridesToFix.isNotEmpty()) {
                Log.w(TAG, "Found ${ridesToFix.size} rides with incorrect status, fixing in background...")
                
                ridesToFix.forEach { ride ->
                    try {
                        // Update the main rides collection
                        firestore.collection("rides").document(ride.id)
                            .update(mapOf(
                                "status" to "completed",
                                "isActive" to false,
                                "statusFixedAt" to com.google.firebase.Timestamp.now()
                            ))
                        
                        // Also update user's ride history if it exists
                        firestore.collection("users")
                            .document(userId)
                            .collection("rideHistory")
                            .document(ride.id)
                            .update(mapOf(
                                "status" to "completed",
                                "statusFixedAt" to com.google.firebase.Timestamp.now()
                            ))
                            
                        Log.d(TAG, "Fixed ride status for ride: ${ride.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fix ride ${ride.id}: ${e.message}")
                    }
                }
                
                // Refresh the ride history after fixing
                fetchUserRideHistory()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in background status fix task", e)
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
                    val ride = rideDoc.toBikeRideSafe()
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