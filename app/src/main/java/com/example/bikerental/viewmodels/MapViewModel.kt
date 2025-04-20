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

    init {
        loadAvailableBikes()
    }

    fun loadAvailableBikes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                bikeRepository.getAvailableBikes().collect { bikes ->
                    _availableBikes.value = bikes
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load available bikes"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshBikes() {
        loadAvailableBikes()
    }

    fun getBikeAtLocation(location: LatLng): Bike? {
        return availableBikes.value.find { bike ->
            bike.latitude == location.latitude && bike.longitude == location.longitude
        }
    }
} 