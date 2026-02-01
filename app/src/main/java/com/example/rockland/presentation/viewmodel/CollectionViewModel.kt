// Screen responsible for managing the user's rock collection UI.
// Acts as the ViewModel layer between Compose UI and data repositories.
package com.example.rockland.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.auth.AuthRepository
import com.example.rockland.data.auth.FirebaseAuthRepository
import com.example.rockland.data.model.CollectionItem
import com.example.rockland.data.repository.AwardsRepository
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

// Holds list data, loading, and banner-ready error for the collection screen.
data class CollectionUiState(
    val items: List<CollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CollectionEvent {
    data class Success(val message: String, val rockId: String? = null) : CollectionEvent
    data class Error(val message: String, val rockId: String? = null) : CollectionEvent
}

// Holds UI state and user-driven events for the collection screen.
class CollectionViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository.getInstance()
) : ViewModel() {

    private val repository = CollectionRepository()
    private val awardsRepository = AwardsRepository()

    private val _events = MutableSharedFlow<CollectionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _uiState = MutableStateFlow(CollectionUiState(isLoading = true))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val currentUser =
        authRepository.authState.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var migratedLegacyImagesForUid: String? = null

    init {
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

    // Ensures we have an authenticated UID or surfaces an error state.
    private fun currentUserIdOrError(): String? {
        val uid = currentUser.value?.uid
        if (uid == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "You must be logged in to manage your collection."
            )
        }
        return uid
    }

    // Loads the signed-in user's collection or shows an error.
    fun loadUserCollection(userId: String? = currentUserIdOrError()) {
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

    // Handles adds triggered by the identification flow.
    fun addRockFromIdentification(
        rockId: String,
        rockName: String,
        thumbnailUrl: String? = null,
        capturedImageUri: Uri? = null,
        context: Context? = null
    ) {
        val userId = currentUserIdOrError() ?: return
        viewModelScope.launch {
            try {
                // Prevent duplicates.
                if (repository.isRockInCollection(userId, rockId, rockName)) {
                    _events.tryEmit(CollectionEvent.Error("Already in your collection.", rockId = rockId))
                    return@launch
                }

                // 1. creates collection doc and captures new rock itemId
                val itemId = repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "identify",
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl
                )

                // 2. image gets uploaded into user's collection item
                if (capturedImageUri != null && context != null) {
                    val uploaded = uploadUserPhotosSuspend(
                        userId = userId,
                        itemId = itemId,
                        uris = listOf(capturedImageUri),
                        context = context
                    )
                    if (uploaded.isNotEmpty()) {
                        _events.tryEmit(CollectionEvent.Success("Photo saved to Rock Gallery.", rockId = rockId))
                    }
                }

                // 3) successful upload + reload
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

    // Persists edits for an existing collection item.
    fun updateCollectionItem(
        itemId: String,
        customId: String,
        locationLabel: String,
        notes: String,
        userImageUrls: List<String>
    ) {
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

    // Removes an entry and reloads the list.
    fun removeFromCollection(itemId: String) {
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

    fun recordReadRockInfo() {
        val userId = currentUserIdOrError() ?: return
        viewModelScope.launch {
            try {
                val triggerResult = awardsRepository.applyTrigger(userId, "read_rock_info")
                triggerResult.messages.firstOrNull()?.let { message ->
                    _events.tryEmit(CollectionEvent.Success(message))
                }
            } catch (_: Exception) {
            }
        }
    }

    // Uploads photos to Firebase and records their download URLs.
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
                throw e
            } catch (e: Exception) {
                _events.tryEmit(CollectionEvent.Error(e.message ?: "Failed to upload photos.", rockId = null))
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to upload photos."
                )
            }
        }
    }

    // returns urls and shows error if photo uploadad issue
    private suspend fun uploadUserPhotosSuspend(
        userId: String,
        itemId: String,
        uris: List<Uri>,
        context: Context
    ): List<String> {
        if (uris.isEmpty()) return emptyList()

        val storageRef = Firebase.storage.reference
            .child("users")
            .child(userId)
            .child("collection")
            .child(itemId)
            .child("userPhotos")

        val urls = withContext(Dispatchers.IO) {
            uris.map { uri ->
                val fileRef = storageRef.child("${UUID.randomUUID()}.jpg")

                // IMPORTANT: do NOT silently ignore a null stream.
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open image URI stream: $uri")

                stream.use {
                    fileRef.putStream(it).await()
                }

                fileRef.downloadUrl.await().toString()
            }
        }

        repository.appendUserImageUrls(userId, itemId, urls)
        return urls
    }

    // adds image from identification into personal notes
    fun addCapturedImageToPersonalNotesFromIdentification(
        rockId: String,
        rockName: String,
        rockSource: String = "identify",
        thumbnailUrl: String? = null,
        imageUri: Uri?,
        context: Context? = null
    ) {
        android.util.Log.d("AddImageVM", "start rockId=$rockId uri=$imageUri")
        val userId = currentUserIdOrError() ?: return
        if (imageUri == null || context == null) return

        viewModelScope.launch {
            try {
                // 1) Find existing entry
                val existingItemId = repository.findCollectionItemId(userId, rockId, rockName)

                // 2) Create entry if missing
                val itemId = existingItemId ?: repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = rockSource,
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl
                )

                // 3) Upload image
                val uploadedUrls = uploadUserPhotosSuspend(
                    userId = userId,
                    itemId = itemId,
                    uris = listOf(imageUri),
                    context = context
                )

                if (uploadedUrls.isNotEmpty()) {
                    _events.tryEmit(CollectionEvent.Success("Image of $rockName has been saved!.", rockId = rockId))
                } else {
                    _events.tryEmit(CollectionEvent.Error("No image uploaded.", rockId = rockId))
                }

                loadUserCollection(userId)
                android.util.Log.d("AddImageVM", "uploaded urls=${uploadedUrls.size}")
            } catch (e: Exception) {
                _events.tryEmit(
                    CollectionEvent.Error(e.message ?: "Failed to save image to Personal Notes.", rockId = rockId)
                )
            }
        }
    }
}
