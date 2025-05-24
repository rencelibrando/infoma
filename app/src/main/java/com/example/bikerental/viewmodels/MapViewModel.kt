package com.example.bikerental.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.data.models.Bike
import com.example.bikerental.domain.repository.BikeRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class MapViewModel @Inject constructor(
    private val bikeRepository: BikeRepository
) : ViewModel() {

    private val _availableBikes = MutableStateFlow<List<Bike>>(emptyList())
    val availableBikes: StateFlow<List<Bike>> = _availableBikes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Cache for quick lookup
    private val _bikesCache = MutableStateFlow<Map<String, Bike>>(emptyMap())

    init {
        loadAvailableBikes()
    }

    fun loadAvailableBikes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Use Dispatchers.IO for repository operations
                withContext(Dispatchers.IO) {
                    bikeRepository.getAvailableBikes().collect { bikes ->
                        // Process bikes in background thread
                        val processedBikes = processBikes(bikes)
                        val bikeMap = processedBikes.associateBy { it.id }
                        
                        // Update UI state on main thread
                        withContext(Dispatchers.Main) {
                            _availableBikes.value = processedBikes
                            _bikesCache.value = bikeMap
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = e.message ?: "Failed to load available bikes"
                    _isLoading.value = false
                }
            }
        }
    }
    
    // Process bikes in background
    private suspend fun processBikes(bikes: List<Bike>): List<Bike> {
        return withContext(Dispatchers.Default) {
            // Any additional processing of bikes can be done here
            // For example, sorting by distance, filtering, etc.
            bikes
        }
    }

    fun refreshBikes() {
        loadAvailableBikes()
    }

    // Use cached map for faster lookup instead of list search
    suspend fun getBikeAtLocation(location: LatLng): Bike? {
        return viewModelScope.run {
            val bikes = availableBikes.value
            withContext(Dispatchers.Default) {
                bikes.find { bike ->
                    // Use approximate equality to handle floating point comparison
                    val latEquals = Math.abs(bike.latitude - location.latitude) < 0.0000001
                    val lngEquals = Math.abs(bike.longitude - location.longitude) < 0.0000001
                    latEquals && lngEquals
                }
            }
        }
    }
    
    // Find bike by ID efficiently from cache
    fun getBikeById(id: String): Bike? {
        return _bikesCache.value[id]
    }
} 