package com.example.rockland.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.auth.AuthRepository
import com.example.rockland.data.auth.FirebaseAuthRepository
import com.example.rockland.data.model.CollectionItem
import com.example.rockland.data.repository.CollectionRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlinx.coroutines.CancellationException

// UI state for the collection list.
data class CollectionUiState(
    val items: List<CollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CollectionEvent {
    data class Success(val message: String, val rockId: String? = null) : CollectionEvent
    data class Error(val message: String, val rockId: String? = null) : CollectionEvent
}

class CollectionViewModel(
    private val repository: CollectionRepository = CollectionRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository.getInstance()
) : ViewModel() {

    private val _events = MutableSharedFlow<CollectionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _uiState = MutableStateFlow(CollectionUiState(isLoading = true))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val currentUser =
        authRepository.authState.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var migratedLegacyImagesForUid: String? = null

    init {
        // React to auth changes and keep collection in sync with the current user.
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    // One-time migration per user: move legacy imageUrls -> userImageUrls and delete legacy field.
                    if (migratedLegacyImagesForUid != user.uid) {
                        migratedLegacyImagesForUid = user.uid
                        launch {
                            runCatching {
                                repository.migrateLegacyImageUrls(user.uid, deleteLegacyField = true)
                            }
                        }
                    }
                    loadUserCollection(user.uid)
                } else {
                    _uiState.value = CollectionUiState(isLoading = false)
                }
            }
        }
    }

    // Return current user id or set an error if not logged in.
    private fun currentUserIdOrError(): String? {
        val uid = currentUser.value?.uid
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
                // Prevent duplicates.
                if (repository.isRockInCollection(userId, rockId, rockName)) {
                    _events.tryEmit(CollectionEvent.Error("Already in your collection.", rockId = rockId))
                    return@launch
                }
                repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "map",
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl,
                    latitude = latitude,
                    longitude = longitude
                )
                _events.tryEmit(CollectionEvent.Success("Added to collection.", rockId = rockId))
                loadUserCollection(userId)
            } catch (e: Exception) {
                _events.tryEmit(
                    CollectionEvent.Error(
                        e.message ?: "Failed to add rock to collection.",
                        rockId = rockId
                    )
                )
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
                // Prevent duplicates.
                if (repository.isRockInCollection(userId, rockId, rockName)) {
                    _events.tryEmit(CollectionEvent.Error("Already in your collection.", rockId = rockId))
                    return@launch
                }
                repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "identify",
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl
                )
                _events.tryEmit(CollectionEvent.Success("Added to collection.", rockId = rockId))
                loadUserCollection(userId)
            } catch (e: Exception) {
                _events.tryEmit(
                    CollectionEvent.Error(
                        e.message ?: "Failed to add rock to collection.",
                        rockId = rockId
                    )
                )
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
        userImageUrls: List<String>
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
                    userImageUrls
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

    // Upload user-selected photos to Firebase Storage and append their download URLs
    // to the collection item (userImageUrls).
    fun uploadUserPhotos(
        itemId: String,
        uris: List<Uri>,
        context: Context,
        onUploaded: (List<String>) -> Unit = {}
    ) {
        val userId = currentUserIdOrError() ?: return
        if (uris.isEmpty()) return

        viewModelScope.launch {
            try {
                val storageRef = Firebase.storage.reference
                    .child("users")
                    .child(userId)
                    .child("collection")
                    .child(itemId)
                    .child("userPhotos")

                val urls = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val fileRef = storageRef.child("${UUID.randomUUID()}.jpg")
                        val stream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
                        stream.use {
                            fileRef.putStream(it).await()
                        }
                        fileRef.downloadUrl.await().toString()
                    }
                }

                repository.appendUserImageUrls(userId, itemId, urls)
                _events.tryEmit(CollectionEvent.Success("Photos added.", rockId = null))
                loadUserCollection(userId)
                onUploaded(urls)
            } catch (e: CancellationException) {
                // UI left composition / app backgrounded; don't show as an error banner.
                throw e
            } catch (e: Exception) {
                _events.tryEmit(CollectionEvent.Error(e.message ?: "Failed to upload photos.", rockId = null))
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to upload photos."
                )
            }
        }
    }
}
