package com.example.rockland.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.datasource.remote.FirebaseAuthService
import com.example.rockland.data.model.CollectionItem
import com.example.rockland.data.repository.CollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI state for the collection list.
data class CollectionUiState(
    val items: List<CollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CollectionViewModel(
    private val repository: CollectionRepository = CollectionRepository(),
    private val authService: FirebaseAuthService = FirebaseAuthService.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState(isLoading = true))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        // React to auth changes and keep collection in sync with the current user.
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                if (user != null) {
                    loadUserCollection(user.uid)
                } else {
                    _uiState.value = CollectionUiState(isLoading = false)
                }
            }
        }
    }

    // Return current user id or set an error if not logged in.
    private fun currentUserIdOrError(): String? {
        val uid = authService.currentUser.value?.uid
        if (uid == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "You must be logged in to manage your collection."
            )
        }
        return uid
    }

    // Load the current user's collection.
    fun loadUserCollection(userId: String? = currentUserIdOrError()): Unit {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val items = repository.fetchCollection(uid)
                _uiState.value = CollectionUiState(items = items, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = CollectionUiState(
                    items = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load collection."
                )
            }
        }
    }

    // Add a rock from the map info card.
    fun addRockFromMap(
        rockId: String,
        rockName: String,
        thumbnailUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Unit {
        val userId = currentUserIdOrError() ?: return
        viewModelScope.launch {
            try {
                repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "map",
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl,
                    latitude = latitude,
                    longitude = longitude
                )
                loadUserCollection(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to add rock to collection."
                )
            }
        }
    }

    // Add a rock from the identification result screen.
    fun addRockFromIdentification(
        rockId: String,
        rockName: String,
        thumbnailUrl: String? = null
    ): Unit {
        val userId = currentUserIdOrError() ?: return
        viewModelScope.launch {
            try {
                repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "identify",
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl
                )
                loadUserCollection(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to add rock to collection."
                )
            }
        }
    }

    // Save edits for an existing collection item.
    fun updateCollectionItem(
        itemId: String,
        customId: String,
        locationLabel: String,
        notes: String,
        imageUrls: List<String>
    ): Unit {
        val userId = currentUserIdOrError() ?: return
        viewModelScope.launch {
            try {
                repository.updateCollectionItem(
                    userId,
                    itemId,
                    customId,
                    locationLabel,
                    notes,
                    imageUrls
                )
                loadUserCollection(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to update collection item."
                )
            }
        }
    }

    // Remove an item from the user's collection.
    fun removeFromCollection(itemId: String): Unit {
        val userId = currentUserIdOrError() ?: return
        viewModelScope.launch {
            try {
                repository.removeRock(userId, itemId)
                loadUserCollection(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to remove rock from collection."
                )
            }
        }
    }

    fun clearError(): Unit {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
