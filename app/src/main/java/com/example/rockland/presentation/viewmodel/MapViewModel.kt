// Supplies the map screen with rock locations and UI signals.
// Keeps logic inside the ViewModel layer so Compose observes StateFlows.
package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.repository.RockLocationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Tracks rock markers and loading status for the map UI.
class MapUiState(
    val locations: List<RockLocation> = emptyList(),
    val isLoading: Boolean = true
)

// Manages map loading, filtering, selection, and recentering for Compose screens.
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

    // Tracks recenter requests so the camera animation can run even without coordinate changes.
    private val _recenterRequests = MutableStateFlow(0)
    val recenterRequests: StateFlow<Int> = _recenterRequests

    init {
        loadNearbyRocks()
    }

    // Loads rock locations when the map screen starts.
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

    // Filters cached rock data so the UI sees only one category.
    fun filterRocks(type: String) {
        val all = _uiState.value.locations
        val filtered = if (type == "your-sighting") {
            all
        } else {
            all.filter { it.category == type }
        }
        _uiState.value = MapUiState(locations = filtered, isLoading = false)
    }

    // Updates the selected rock for detail panels.
    fun selectLocation(id: String) {
        _selectedLocation.value = _uiState.value.locations.firstOrNull { it.id == id }
    }

    // Clears the highlighted rock.
    fun clearSelection() {
        _selectedLocation.value = null
    }

    // Centers on the nearest rock and signals recenter requests.
    fun moveToUserLocation() {
        viewModelScope.launch {
            val firstLocation = _uiState.value.locations.firstOrNull()
            if (firstLocation != null) {
                val coordinates = LatLng(firstLocation.latitude, firstLocation.longitude)
                _userLocation.value = coordinates
                _locationError.value = null
                selectNearest(coordinates)
                _recenterRequests.value += 1
            } else {
                _locationError.value =
                    "No rock distribution data found in this area. Be the first to log a discovery!."
            }
        }
    }

    // Picks the rock closest to the provided coordinates.
    private fun selectNearest(userLocation: LatLng) {
        val nearest = _uiState.value.locations.minByOrNull {
            val latDiff = it.latitude - userLocation.latitude
            val lngDiff = it.longitude - userLocation.longitude
            latDiff * latDiff + lngDiff * lngDiff
        }
        _selectedLocation.value = nearest
    }
}
