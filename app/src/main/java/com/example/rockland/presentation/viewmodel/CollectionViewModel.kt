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
import com.example.rockland.data.repository.Rock
import com.example.rockland.data.repository.RockRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

import com.google.firebase.functions.FirebaseFunctions


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
    private val rockRepository = RockRepository()
    private val firestore = FirebaseFirestore.getInstance()
    //new AI
    private val functions = FirebaseFunctions.getInstance()


    private val _events = MutableSharedFlow<CollectionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _uiState = MutableStateFlow(CollectionUiState(isLoading = true))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val currentUser =
        authRepository.authState.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var migratedLegacyImagesForUid: String? = null
    private var cachedDictionaryRocks: List<Rock>? = null
    private var collectionListener: ListenerRegistration? = null

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
                    collectionListener?.remove()
                    collectionListener = null
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
        if (collectionListener != null) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        collectionListener = repository.listenToCollection(
            userId = uid,
            onItems = { items ->
                viewModelScope.launch {
                    try {
                        val enriched = enrichThumbnailsFromDictionary(items)
                        _uiState.value = CollectionUiState(items = enriched, isLoading = false)
                    } catch (e: Exception) {
                        _uiState.value = CollectionUiState(
                            items = emptyList(),
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load collection."
                        )
                    }
                }
            },
            onError = { e ->
                _uiState.value = CollectionUiState(
                    items = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load collection."
                )
            }
        )
    }

    // Lazily loads rock dictionary and caches it in memory.
    suspend fun getDictionaryRocks(): List<Rock> {
        val cached = cachedDictionaryRocks
        if (cached != null && cached.isNotEmpty()) return cached
        val rocks = rockRepository.getAllRocks().sortedBy { it.rockName }
        cachedDictionaryRocks = rocks
        return rocks
    }

    // Computes unlocked rock IDs for the current user and collection snapshot.
    suspend fun getUnlockedRockIds(userId: String?, collectionItems: List<CollectionItem>): Set<String> {
        val uid = userId ?: return emptySet()
        val doc = firestore.collection("users").document(uid).get().await()
        val list = (doc.get("unlockedRockIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val fromCollection = collectionItems
            .mapNotNull { it.rockId.takeIf { id -> id.isNotBlank() } }
            .toSet()
        return list.toSet() + fromCollection
    }

    // Fills missing collection thumbnails from the rock dictionary images.
    private suspend fun enrichThumbnailsFromDictionary(items: List<CollectionItem>): List<CollectionItem> {
        if (items.isEmpty()) return items
        return withContext(Dispatchers.IO) {
            items.map { item ->
                if (!item.thumbnailUrl.isNullOrBlank()) return@map item

                val dictRock = runCatching {
                    val rockIdInt = item.rockId.toIntOrNull()
                    when {
                        rockIdInt != null -> rockRepository.getRockById(rockIdInt)
                        item.rockName.isNotBlank() -> rockRepository.getRockByName(item.rockName)
                        else -> null
                    }
                }.getOrNull()

                if (dictRock != null && dictRock.rockImageUrl.isNotBlank()) {
                    item.copy(thumbnailUrl = dictRock.rockImageUrl)
                } else {
                    item
                }
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
                repository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "identify",
                    rockName = rockName,
                    thumbnailUrl = thumbnailUrl
                )

                // docId is now rockId (because CollectionRepository uses .document(rockId))
                val itemId = rockId

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

                // 3) mission/achievement trigger for "collect_rock"
                runCatching {
                    val triggerResult = awardsRepository.applyTrigger(userId, "collect_rock")
                    triggerResult.messages.firstOrNull()?.let { _events.tryEmit(CollectionEvent.Success(it, rockId = rockId)) }
                }

                //trigger for ultra rare rock collection
                runCatching {
                    val rockIntId = rockId.toIntOrNull()
                    val rock = rockIntId?.let { rockRepository.getRockById(it) }
                    if (rock?.rockRarity?.trim()?.equals("Ultra Rare", ignoreCase = true) == true) {
                        val ultraResult = awardsRepository.applyTrigger(userId, "collect_ultra_rare")
                        ultraResult.messages.firstOrNull()?.let { _events.tryEmit(CollectionEvent.Success(it, rockId = rockId)) }
                    }
                }
                // 4) successful add + reload
                _events.tryEmit(CollectionEvent.Success("“$rockName” has been registered into your collection!", rockId = rockId))
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
                runCatching {
                    val triggerResult = awardsRepository.applyTrigger(userId, "edit_personal_notes")
                    triggerResult.messages.firstOrNull()?.let { message ->
                        _events.tryEmit(CollectionEvent.Success(message, rockId = null))
                    }
                }
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

    // Removes photos from personal notes by URL.
    fun removeUserPhotos(
        itemId: String,
        urls: List<String>,
        onRemoved: (List<String>) -> Unit = {}
    ) {
        val userId = currentUserIdOrError() ?: return
        if (urls.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.removeUserImageUrls(userId, itemId, urls)
                loadUserCollection(userId)
                onRemoved(urls)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.tryEmit(
                    CollectionEvent.Error(e.message ?: "Failed to remove photos.", rockId = null)
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
                val isNewAdd = existingItemId == null

                // 2) Create entry if missing
                val itemId = existingItemId ?: run {
                    repository.addRockToCollection(
                        userId = userId,
                        rockId = rockId,
                        rockSource = rockSource,
                        rockName = rockName,
                        thumbnailUrl = thumbnailUrl
                    )
                    // docId is deterministic now
                    rockId
                }

                if (isNewAdd) {
                    runCatching {
                        val triggerResult = awardsRepository.applyTrigger(userId, "collect_rock")
                        triggerResult.messages.firstOrNull()
                            ?.let { _events.tryEmit(CollectionEvent.Success(it, rockId = rockId)) }
                    }

                    runCatching {
                        val rockIntId = rockId.toIntOrNull()
                        val rock = rockIntId?.let { rockRepository.getRockById(it) }
                        if (rock?.rockRarity?.trim()?.equals("Ultra Rare", ignoreCase = true) == true) {
                            val ultraResult = awardsRepository.applyTrigger(userId, "collect_ultra_rare")
                            ultraResult.messages.firstOrNull()
                                ?.let { _events.tryEmit(CollectionEvent.Success(it, rockId = rockId)) }
                        }
                    }
                }

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
    fun testCloudFunction() {
        viewModelScope.launch {
            try {
                val result = functions
                    .getHttpsCallable("helloRockland")
                    .call()
                    .await()

                val map = (result.getData() as? Map<*, *>)
                    ?: throw IllegalStateException("Invalid response from helloRockland")

                val msg = map["message"]?.toString() ?: "No message returned"
                _events.tryEmit(CollectionEvent.Success(msg))
            } catch (e: Exception) {
                _events.tryEmit(CollectionEvent.Error("Cloud Function failed: ${e.message}"))
            }
        }
    }


}
