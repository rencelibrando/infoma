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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import android.location.Location
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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

    // Enhanced cache with location-based indexing
    private val _bikesCache = MutableStateFlow<Map<String, Bike>>(emptyMap())
    private val _locationCache = ConcurrentHashMap<String, List<Bike>>()
    private val _distanceCache = ConcurrentHashMap<String, Float>()

    // User location for distance-based filtering
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    // Filtered bikes based on distance and availability
    private val _nearbyBikes = MutableStateFlow<List<Bike>>(emptyList())
    val nearbyBikes: StateFlow<List<Bike>> = _nearbyBikes.asStateFlow()

    // Distance filter setting (in meters)
    private val _maxDistance = MutableStateFlow(2000f) // 2km default
    val maxDistance: StateFlow<Float> = _maxDistance.asStateFlow()

    init {
        loadAvailableBikes()
        setupNearbyBikesFlow()
    }

    // Set up reactive flow for nearby bikes based on user location and distance filter
    private fun setupNearbyBikesFlow() {
        viewModelScope.launch {
            combine(
                _availableBikes,
                _userLocation,
                _maxDistance
            ) { bikes, location, maxDist ->
                Triple(bikes, location, maxDist)
            }
            .debounce(300) // Debounce to avoid excessive calculations
            .distinctUntilChanged()
            .collect { (bikes, location, maxDist) ->
                if (location != null) {
                    filterBikesByDistance(bikes, location, maxDist)
                } else {
                    _nearbyBikes.value = bikes
                }
            }
        }
    }

    fun loadAvailableBikes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                withContext(Dispatchers.IO) {
                    bikeRepository.getAvailableBikes().collect { bikes ->
                        // Process bikes in background thread
                        val processedBikes = processBikes(bikes)
                        val bikeMap = processedBikes.associateBy { it.id }
                        
                        // Update caches and UI state
                        withContext(Dispatchers.Main) {
                            _availableBikes.value = processedBikes
                            _bikesCache.value = bikeMap
                            _isLoading.value = false
                            
                            // Update location-based cache
                            updateLocationCache(processedBikes)
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
    
    // Enhanced bike processing with performance optimizations
    private suspend fun processBikes(bikes: List<Bike>): List<Bike> {
        return withContext(Dispatchers.Default) {
            // Parallel processing for large bike lists
            if (bikes.size > 100) {
                bikes.chunked(50).map { chunk ->
                    async {
                        chunk.filter { bike ->
                            bike.isAvailable
                        }.sortedBy { it.name }
                    }
                }.awaitAll().flatten()
            } else {
                bikes.filter { bike ->
                    bike.isAvailable
                }.sortedBy { it.name }
            }
        }
    }

    // Update location-based cache for spatial queries
    private fun updateLocationCache(bikes: List<Bike>) {
        viewModelScope.launch(Dispatchers.Default) {
            _locationCache.clear()
            
            // Group bikes by location grid for faster spatial lookups
            bikes.groupBy { bike ->
                val gridLat = (bike.latitude * 1000).toInt() / 1000
                val gridLng = (bike.longitude * 1000).toInt() / 1000
                "$gridLat,$gridLng"
            }.forEach { (key, bikesInGrid) ->
                _locationCache[key] = bikesInGrid
            }
        }
    }

    // Filter bikes by distance with caching
    private suspend fun filterBikesByDistance(bikes: List<Bike>, userLocation: LatLng, maxDistance: Float) {
        withContext(Dispatchers.Default) {
            val nearbyBikes = bikes.mapNotNull { bike ->
                val distance = calculateDistance(userLocation, LatLng(bike.latitude, bike.longitude))
                if (distance <= maxDistance) {
                    bike
                } else null
            }.sortedBy { bike ->
                // Sort by distance
                calculateDistance(userLocation, LatLng(bike.latitude, bike.longitude))
            }
            
            withContext(Dispatchers.Main) {
                _nearbyBikes.value = nearbyBikes
            }
        }
    }

    // Optimized distance calculation with caching
    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val key = "${start.latitude},${start.longitude}-${end.latitude},${end.longitude}"
        return _distanceCache.getOrPut(key) {
            val results = FloatArray(1)
            Location.distanceBetween(
                start.latitude, start.longitude,
                end.latitude, end.longitude,
                results
            )
            results[0]
        }
    }

    fun refreshBikes() {
        // Clear caches before refresh
        _locationCache.clear()
        _distanceCache.clear()
        loadAvailableBikes()
    }

    // Set user location for distance-based filtering
    fun setUserLocation(location: LatLng) {
        _userLocation.value = location
    }

    // Update maximum distance filter
    fun setMaxDistance(distance: Float) {
        _maxDistance.value = distance
    }

    // Optimized location-based bike lookup
    suspend fun getBikeAtLocation(location: LatLng, tolerance: Double = 0.0001): Bike? {
        return withContext(Dispatchers.Default) {
            val gridLat = (location.latitude * 1000).toInt() / 1000
            val gridLng = (location.longitude * 1000).toInt() / 1000
            val gridKey = "$gridLat,$gridLng"
            
            // Check adjacent grid cells for better coverage
            val adjacentKeys = listOf(
                gridKey,
                "${gridLat + 1},$gridLng",
                "${gridLat - 1},$gridLng",
                "$gridLat,${gridLng + 1}",
                "$gridLat,${gridLng - 1}"
            )
            
            adjacentKeys.forEach { key ->
                _locationCache[key]?.find { bike ->
                    val latEquals = kotlin.math.abs(bike.latitude - location.latitude) < tolerance
                    val lngEquals = kotlin.math.abs(bike.longitude - location.longitude) < tolerance
                    latEquals && lngEquals
                }?.let { return@withContext it }
            }
            
            null
        }
    }
    
    // Enhanced bike lookup by ID with cache
    fun getBikeById(id: String): Bike? {
        return _bikesCache.value[id]
    }

    // Get bikes within a specific radius
    suspend fun getBikesWithinRadius(center: LatLng, radiusMeters: Float): List<Bike> {
        return withContext(Dispatchers.Default) {
            _availableBikes.value.filter { bike ->
                val distance = calculateDistance(center, LatLng(bike.latitude, bike.longitude))
                distance <= radiusMeters
            }.sortedBy { bike ->
                calculateDistance(center, LatLng(bike.latitude, bike.longitude))
            }
        }
    }

    // Get the closest bike to a location
    suspend fun getClosestBike(location: LatLng): Bike? {
        return withContext(Dispatchers.Default) {
            _availableBikes.value.minByOrNull { bike ->
                calculateDistance(location, LatLng(bike.latitude, bike.longitude))
            }
        }
    }

    // Batch bike updates for better performance
    suspend fun updateBikes(bikes: List<Bike>) {
        withContext(Dispatchers.Default) {
            val processedBikes = processBikes(bikes)
            val bikeMap = processedBikes.associateBy { it.id }
            
            withContext(Dispatchers.Main) {
                _availableBikes.value = processedBikes
                _bikesCache.value = bikeMap
                updateLocationCache(processedBikes)
            }
        }
    }

    // Clear all caches
    fun clearCaches() {
        _locationCache.clear()
        _distanceCache.clear()
        _bikesCache.value = emptyMap()
    }

    override fun onCleared() {
        super.onCleared()
        clearCaches()
    }
} 