// Supplies the map screen with rock locations and UI signals.
// Keeps logic inside the ViewModel layer so Compose observes StateFlows.
package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.auth.AuthRepository
import com.example.rockland.data.auth.FirebaseAuthRepository
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.datasource.remote.FirebaseUserService
import com.example.rockland.data.repository.AwardsRepository
import com.example.rockland.data.repository.RockLocationRepository
import com.example.rockland.data.model.RockCommunityContent
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.util.Log


// Tracks rock markers and loading status for the map UI.
class MapUiState(
    val locations: List<RockLocation> = emptyList(),
    val isLoading: Boolean = true
)

enum class CommunityTab {
    COMMENTS,
    PHOTOS,
    ANNOTATIONS
}

// Manages map loading, filtering, selection, and recentering for Compose screens.
class MapViewModel(
    private val repository: RockLocationRepository = RockLocationRepository(),
    private val awardsRepository: AwardsRepository = AwardsRepository(),
    private val authRepository: AuthRepository = FirebaseAuthRepository.getInstance(),
    private val userService: FirebaseUserService = FirebaseUserService.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    // Keeps full list for map filters.
    private var allLocations: List<RockLocation> = emptyList()
    private var currentFilter: String = "all"

    private val _selectedLocation = MutableStateFlow<RockLocation?>(null)
    val selectedLocation: StateFlow<RockLocation?> = _selectedLocation

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    private val _communityContent = MutableStateFlow(RockCommunityContent())
    val communityContent: StateFlow<RockCommunityContent> = _communityContent

    private val _activeCommunityTab = MutableStateFlow(CommunityTab.COMMENTS)
    val activeCommunityTab: StateFlow<CommunityTab> = _activeCommunityTab

    private val _showAddCommentForm = MutableStateFlow(false)
    val showAddCommentForm: StateFlow<Boolean> = _showAddCommentForm

    private val _showAddPhotoForm = MutableStateFlow(false)
    val showAddPhotoForm: StateFlow<Boolean> = _showAddPhotoForm

    // Tracks recenter requests so the camera animation can run even without coordinate changes.
    private val _recenterRequests = MutableStateFlow(0)
    val recenterRequests: StateFlow<Int> = _recenterRequests

    private val currentUser =
        authRepository.authState.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val authorName = MutableStateFlow("You")
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting

    private val _awardMessages = MutableSharedFlow<String>(extraBufferCapacity = 3)
    val awardMessages = _awardMessages
    private val _submissionMessages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val submissionMessages = _submissionMessages
    private val readInfoLocations = mutableSetOf<String>()
    private var pendingFocusLocationId: String? = null
    private val _openInfoCardForLocationId = MutableStateFlow<String?>(null)
    val openInfoCardForLocationId: StateFlow<String?> = _openInfoCardForLocationId.asStateFlow()

    init {
        loadNearbyRocks()
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user == null) {
                    authorName.value = "You"
                    _currentUserId.value = null
                } else {
                    _currentUserId.value = user.uid
                    try {
                        val profile = userService.getUserProfile(user.uid)
                        val displayName = "${profile.firstName} ${profile.lastName}".trim()
                        authorName.value = displayName.ifBlank {
                            user.email ?: "You"
                        }
                    } catch (_: Exception) {
                        authorName.value = user.email ?: "You"
                    }
                }
            }
        }
    }

    // Loads rock locations when the map screen starts.
    fun loadNearbyRocks() {
        viewModelScope.launch {
            try {
                val rocks = repository.fetchRockLocations()
                allLocations = rocks
                applyCurrentFilter()
                pendingFocusLocationId?.let { id ->
                    val location = rocks.firstOrNull { it.id == id }
                    if (location != null) {
                        pendingFocusLocationId = null
                        _selectedLocation.value = location
                        loadCommunityContent(location.id)
                    }
                }
            } catch (_: Throwable) {
                _uiState.value = MapUiState(locations = emptyList(), isLoading = false)
            }
        }
    }

    fun focusLocation(locationId: String) {
        if (locationId.isBlank()) return
        val location = _uiState.value.locations.firstOrNull { it.id == locationId }
        _openInfoCardForLocationId.value = locationId
        if (location != null) {
            pendingFocusLocationId = null
            _selectedLocation.value = location
            loadCommunityContent(location.id)
        } else {
            pendingFocusLocationId = locationId
            loadNearbyRocks()
        }
    }

    fun consumeOpenInfoCardRequest() {
        _openInfoCardForLocationId.value = null
    }

    // Filters cached rock data so the UI sees only one category.
    fun filterRocks(type: String) {
        currentFilter = type
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val all = allLocations
        val filtered = when (currentFilter) {
            "verified" -> all.filter { it.category.equals("verified", ignoreCase = true) }
            "unverified" -> all.filter { !it.category.equals("verified", ignoreCase = true) }
            else -> all
        }
        _uiState.value = MapUiState(locations = filtered, isLoading = false)
    }

    // Updates the selected rock for detail panels.
    fun selectLocation(id: String) {
        val location = _uiState.value.locations.firstOrNull { it.id == id }
        _selectedLocation.value = location
        location?.let {
            loadCommunityContent(it.id)
        }
    }

    // Clears the highlighted rock.
    fun clearSelection() {
        _selectedLocation.value = null
    }

    // For demo, recenter camera to a fixed rock.
    fun moveToUserLocation() {
        viewModelScope.launch {
            focusDemoLocation("rock-2")
        }
    }

    fun focusDemoLocation(locationId: String) {
        if (locationId.isBlank()) return
        val target = allLocations.firstOrNull { it.id == locationId } ?: return
        _userLocation.value = LatLng(target.latitude, target.longitude)
        _recenterRequests.value += 1
    }

    fun setCommunityTab(tab: CommunityTab) {
        _activeCommunityTab.value = tab
    }

    fun showCommentForm() {
        _showAddCommentForm.value = true
        _showAddPhotoForm.value = false
    }

    fun hideCommentForm() {
        _showAddCommentForm.value = false
    }

    fun hidePhotoForm() {
        _showAddPhotoForm.value = false
    }

    fun submitComment(text: String, author: String = "You") {
        val location = _selectedLocation.value ?: return
        val locationId = location.id
        val locationName = location.name
        if (_isPosting.value) return

        viewModelScope.launch {
            _isPosting.value = true
            try {
                val resolvedAuthor = if (author.isBlank() || author == "You") authorName.value else author
                val uid = currentUser.value?.uid ?: ""

                repository.addComment(
                    locationId = locationId,
                    locationName = locationName,
                    userId = uid,
                    author = resolvedAuthor,
                    text = text
                )

                if (uid.isNotBlank()) {
                    runCatching {
                        val result = awardsRepository.applyTrigger(uid, "post_comment")
                        result.messages.firstOrNull()?.let { _awardMessages.tryEmit(it) }
                    }
                }

                _submissionMessages.tryEmit("Comment submitted. Waiting for review.")
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            } finally {
                _isPosting.value = false
                hideCommentForm()
                _activeCommunityTab.value = CommunityTab.COMMENTS
            }
        }
    }

    fun submitPhoto(context: Context, caption: String, imageUri: Uri?) {
        val location = _selectedLocation.value ?: return
        val locationId = location.id
        val locationName = location.name
        val uid = currentUser.value?.uid ?: return
        val uri = imageUri ?: return

        viewModelScope.launch {
            _isPosting.value = true
            try {
                val resolvedAuthor = authorName.value
                val urls = uploadLocationPhotoUris(context, locationId, uid, listOf(uri))
                val url = urls.firstOrNull() ?: return@launch

                repository.addPhoto(
                    locationId = locationId,
                    locationName = locationName,
                    commentId = null,
                    userId = uid,
                    author = resolvedAuthor,
                    caption = caption,
                    imageUrl = url
                )

                runCatching {
                    val triggerResult = awardsRepository.applyTrigger(uid, "upload_photo")
                    triggerResult.messages.firstOrNull()?.let { _awardMessages.tryEmit(it) }
                }

                _submissionMessages.tryEmit("Photo submitted. Waiting for review.")
                loadCommunityContent(locationId)
            } catch (t: Throwable) {
                Log.e("MapUpload", "Upload failed in submitPhoto()", t)
            } finally {
                _isPosting.value = false
                hidePhotoForm()
                _activeCommunityTab.value = CommunityTab.PHOTOS
            }
        }
    }

    fun recordReadRockInfo(locationId: String) {
        if (!readInfoLocations.add(locationId)) return
        viewModelScope.launch {
            val uid = currentUser.value?.uid ?: return@launch
            runCatching {
                // Location pin info = visit_location (not read_rock_info)
                val result = awardsRepository.applyTrigger(uid, "visit_location")
                result.messages.firstOrNull()?.let { _awardMessages.tryEmit(it) }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val locationId = _selectedLocation.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.deleteComment(locationId, commentId)
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            }
        }
    }

    fun deletePhoto(locationPhotoId: String) {
        val locationId = _selectedLocation.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.deletePhoto(locationId, locationPhotoId)
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            }
        }
    }

    @Suppress("unused")
    fun addAnnotation(note: String, imageUrls: List<String> = emptyList()) {
        val locationId = _selectedLocation.value?.id ?: return
        val uid = currentUser.value?.uid ?: return
        val expertName = authorName.value.ifBlank { "Expert" }
        viewModelScope.launch {
            try {
                repository.addAnnotation(
                    locationId = locationId,
                    expertId = uid,
                    expertName = expertName,
                    note = note,
                    imageUrls = imageUrls
                )
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            }
        }
    }

    @Suppress("unused")
    fun updateAnnotation(annotationId: String, note: String, imageUrls: List<String> = emptyList()) {
        val locationId = _selectedLocation.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.updateAnnotation(
                    locationId = locationId,
                    annotationId = annotationId,
                    note = note,
                    imageUrls = imageUrls
                )
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            }
        }
    }

    fun deleteAnnotation(annotationId: String) {
        val locationId = _selectedLocation.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.deleteAnnotation(locationId, annotationId)
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            }
        }
    }

    fun addAnnotationWithImage(context: Context, note: String, imageUri: Uri?) {
        val locationId = _selectedLocation.value?.id ?: return
        val uid = currentUser.value?.uid ?: return
        val expertName = authorName.value.ifBlank { "Expert" }
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val urls = if (imageUri != null) {
                    uploadAnnotationPhotoUris(context, locationId, uid, listOf(imageUri))
                } else {
                    emptyList()
                }
                repository.addAnnotation(
                    locationId = locationId,
                    expertId = uid,
                    expertName = expertName,
                    note = note,
                    imageUrls = urls
                )
                // When an expert adds an annotation, promote this location to verified.
                try {
                    repository.markLocationVerified(locationId)
                } catch (_: Throwable) {
                }
                // Reload locations so filters and pins reflect updated category.
                loadNearbyRocks()
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun updateAnnotationWithImage(
        context: Context,
        annotationId: String,
        note: String,
        imageUri: Uri?,
        existingImageUrls: List<String>
    ) {
        val locationId = _selectedLocation.value?.id ?: return
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isPosting.value = true
            try {
                val urls = if (imageUri != null) {
                    uploadAnnotationPhotoUris(context, locationId, uid, listOf(imageUri))
                } else {
                    existingImageUrls
                }
                repository.updateAnnotation(
                    locationId = locationId,
                    annotationId = annotationId,
                    note = note,
                    imageUrls = urls
                )
                loadCommunityContent(locationId)
            } catch (_: Throwable) {
            } finally {
                _isPosting.value = false
            }
        }
    }


    private fun loadCommunityContent(locationId: String) {
        viewModelScope.launch {
            try {
                val content = repository.fetchCommunityContent(locationId)
                _communityContent.value = content
            } catch (_: Throwable) {
                _communityContent.value = RockCommunityContent()
            }
        }
    }

    fun submitCommentWithPhotos(context: Context, commentText: String, photoUris: List<Uri>) {
        val location = _selectedLocation.value ?: return
        val locationId = location.id
        val locationName = location.name
        val uid = currentUser.value?.uid ?: return

        viewModelScope.launch {
            _isPosting.value = true
            try {
                val resolvedAuthor = authorName.value

                // 1) create comment
                val commentId = repository.addComment(
                    locationId = locationId,
                    locationName = locationName,
                    userId = uid,
                    author = resolvedAuthor,
                    text = commentText
                )

                // 2) upload then attach up to 3 photos
                val urls = uploadLocationPhotoUris(
                    context = context,
                    locationId = locationId,
                    userId = uid,
                    uris = photoUris.take(3)
                )

                urls.forEach { url ->
                    repository.addPhoto(
                        locationId = locationId,
                        locationName = locationName,
                        commentId = commentId,
                        userId = uid,
                        author = resolvedAuthor,
                        caption = "",
                        imageUrl = url
                    )
                }

                _submissionMessages.tryEmit("Comment and photos submitted. Waiting for review.")
                loadCommunityContent(locationId)
            } catch (t: Throwable) {
                Log.e("MapUpload", "Upload failed in submitCommentWithPhotos()", t)
            } finally {
                _isPosting.value = false
                hideCommentForm()
                _activeCommunityTab.value = CommunityTab.COMMENTS
            }
        }
    }

}
// upload location photo to firebase
private suspend fun uploadLocationPhotoUris(
    context: Context,
    locationId: String,
    userId: String,
    uris: List<Uri>
): List<String> = withContext(Dispatchers.IO) {
    val storageRef = Firebase.storage.reference
        .child("locations")
        .child(locationId)
        .child("photos")
        .child(userId)

    uris.mapNotNull { uri ->
        val fileRef = storageRef.child("${UUID.randomUUID()}.jpg")
        Log.d("MapUpload", "Uploading uri=$uri")
        val stream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
        stream.use { fileRef.putStream(it).await() }
        fileRef.downloadUrl.await().toString()
    }
}

private suspend fun uploadAnnotationPhotoUris(
    context: Context,
    locationId: String,
    userId: String,
    uris: List<Uri>
): List<String> = withContext(Dispatchers.IO) {
    val storageRef = Firebase.storage.reference
        .child("locations")
        .child(locationId)
        .child("annotations")
        .child(userId)

    uris.mapNotNull { uri ->
        val fileRef = storageRef.child("${UUID.randomUUID()}.jpg")
        Log.d("MapUpload", "Uploading annotation uri=$uri")
        val stream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
        stream.use { fileRef.putStream(it).await() }
        fileRef.downloadUrl.await().toString()
    }
}
