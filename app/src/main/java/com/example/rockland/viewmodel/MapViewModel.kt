package com.example.rockland.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapUiState(
    val locations: List<RockLocation> = emptyList(),
    val isLoading: Boolean = true
)

class MapViewModel(
    private val repository: RockLocationRepository = RockLocationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val _selectedLocation = MutableStateFlow<RockLocation?>(null)
    val selectedLocation: StateFlow<RockLocation?> = _selectedLocation

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    // Counts how many times user requested recenter; used to trigger camera animation even
    // when coordinates don't change.
    private val _recenterRequests = MutableStateFlow(0)
    val recenterRequests: StateFlow<Int> = _recenterRequests

    init {
        loadNearbyRocks()
    }

    fun loadNearbyRocks() {
        viewModelScope.launch {
            try {
                val rocks = repository.fetchRockLocations()
                _uiState.value = MapUiState(locations = rocks, isLoading = false)
            } catch (_: Throwable) {
                _uiState.value = MapUiState(locations = emptyList(), isLoading = false)
            }
        }
    }

    fun filterRocks(type: String) {
        val all = _uiState.value.locations
        val filtered = if (type == "your-sighting") {
            all
        } else {
            all.filter { it.category == type }
        }
        _uiState.value = MapUiState(locations = filtered, isLoading = false)
    }

    fun selectLocation(id: String) {
        _selectedLocation.value = _uiState.value.locations.firstOrNull { it.id == id }
    }

    fun clearSelection() {
        _selectedLocation.value = null
    }

    fun moveToUserLocation() {
        viewModelScope.launch {
            val firstLocation = _uiState.value.locations.firstOrNull()
            if (firstLocation != null) {
                val coordinates = LatLng(firstLocation.latitude, firstLocation.longitude)
                _userLocation.value = coordinates
                _locationError.value = null
                selectNearest(coordinates)
                _recenterRequests.value = _recenterRequests.value + 1
            } else {
                _locationError.value =
                    "No rock distribution data found in this area. Be the first to log a discovery!."
            }
        }
    }

    private fun selectNearest(userLocation: LatLng) {
        val nearest = _uiState.value.locations.minByOrNull {
            val latDiff = it.latitude - userLocation.latitude
            val lngDiff = it.longitude - userLocation.longitude
            latDiff * latDiff + lngDiff * lngDiff
        }
        _selectedLocation.value = nearest
    }
}
