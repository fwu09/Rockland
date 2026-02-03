package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.model.ContentStatus
import com.example.rockland.data.model.LocationComment
import com.example.rockland.data.model.LocationPhoto
import com.example.rockland.data.repository.ContentReviewRepository
import com.example.rockland.data.repository.UserNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InboxNotification(
    val id: String,
    val title: String,
    val message: String,
    val date: String,
    val isRead: Boolean
)

data class RockDictionaryRequest(
    val id: String,
    val requestPath: String,
    val requestType: String,
    val rockID: Int,
    val rockName: String,
    val rockRarity: String,
    val rockLocation: String,
    val rockDesc: String,
    val imageUrl: String,
    val submittedBy: String,
    val submittedById: String,
    val createdAt: Long,
    val status: ContentStatus,
    val reviewedBy: String?,
    val reviewedAt: Long?
)

class ReviewContentViewModel(
    private val repository: ContentReviewRepository = ContentReviewRepository()
) : ViewModel() {
    private val _pendingComments = MutableStateFlow<List<LocationComment>>(emptyList())
    val pendingComments: StateFlow<List<LocationComment>> = _pendingComments.asStateFlow()

    private val _pendingPhotos = MutableStateFlow<List<LocationPhoto>>(emptyList())
    val pendingPhotos: StateFlow<List<LocationPhoto>> = _pendingPhotos.asStateFlow()

    private val _pendingRockRequests = MutableStateFlow<List<RockDictionaryRequest>>(emptyList())
    val pendingRockRequests: StateFlow<List<RockDictionaryRequest>> = _pendingRockRequests.asStateFlow()

    private val _notifications = MutableStateFlow<List<InboxNotification>>(emptyList())
    val notifications: StateFlow<List<InboxNotification>> = _notifications.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    private val _role = MutableStateFlow("nature_enthusiast")

    fun bindUser(userId: String?, role: String?) {
        val normalizedRole = role?.trim()?.lowercase() ?: "nature_enthusiast"
        val changed = _userId.value != userId || _role.value != normalizedRole
        _userId.value = userId
        _role.value = normalizedRole
        if (changed) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val comments = repository.fetchPendingComments()
                val photos = repository.fetchPendingPhotos()
                _pendingComments.value = comments
                _pendingPhotos.value = photos
                val rockRequests = if (_role.value == "admin") {
                    repository.fetchPendingRockDictionaryRequests()
                } else {
                    emptyList()
                }
                _pendingRockRequests.value = rockRequests
                refreshNotifications(comments, photos, rockRequests)
            } catch (_: Exception) {
                _pendingComments.value = emptyList()
                _pendingPhotos.value = emptyList()
                _pendingRockRequests.value = emptyList()
                _notifications.value = emptyList()
            }
        }
    }

    private suspend fun refreshNotifications(
        pendingComments: List<LocationComment>,
        pendingPhotos: List<LocationPhoto>,
        pendingRockRequests: List<RockDictionaryRequest>
    ) {
        val role = _role.value
        val userId = _userId.value
        if (role == "verified_expert") {
            val notifications = buildList {
                if (pendingComments.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_comments",
                            title = "New Comment Pending",
                            message = "You have ${pendingComments.size} comments waiting for review.",
                            date = "Today",
                            isRead = false
                        )
                    )
                }
                if (pendingPhotos.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_photos",
                            title = "New Image Submission",
                            message = "You have ${pendingPhotos.size} image submissions waiting for review.",
                            date = "Today",
                            isRead = false
                        )
                    )
                }
                if (!userId.isNullOrBlank()) {
                    val raw = repository.fetchUserNotifications(userId)
                    addAll(raw.map { it.toUi() })
                }
            }
            _notifications.value = notifications
        } else if (role == "admin") {
            val notifications = buildList {
                if (pendingRockRequests.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_rock_dictionary",
                            title = "Rock Dictionary Review",
                            message = "You have ${pendingRockRequests.size} dictionary updates waiting for review.",
                            date = "Today",
                            isRead = false
                        )
                    )
                }
            }
            _notifications.value = notifications
        } else if (!userId.isNullOrBlank()) {
            val raw = repository.fetchUserNotifications(userId)
            _notifications.value = raw.map { it.toUi() }
        } else {
            _notifications.value = emptyList()
        }
    }

    fun approveComment(comment: LocationComment, reviewerId: String?) {
        viewModelScope.launch {
            repository.updateCommentStatus(comment.locationId, comment.commentId, ContentStatus.APPROVED, reviewerId)
            if (comment.userId.isNotBlank()) {
                repository.addUserNotification(
                    userId = comment.userId,
                    title = "Comment Approved",
                    message = "Your comment was approved by a verified expert."
                )
            }
            refresh()
        }
    }

    fun rejectComment(comment: LocationComment, reviewerId: String?) {
        viewModelScope.launch {
            repository.updateCommentStatus(comment.locationId, comment.commentId, ContentStatus.REJECTED, reviewerId)
            if (comment.userId.isNotBlank()) {
                repository.addUserNotification(
                    userId = comment.userId,
                    title = "Comment Rejected",
                    message = "Your comment was rejected by a verified expert."
                )
            }
            refresh()
        }
    }

    fun approvePhotos(photos: List<LocationPhoto>, reviewerId: String?) {
        viewModelScope.launch {
            photos.forEach { photo ->
                repository.updatePhotoStatus(photo.locationId, photo.locationPhotoId, ContentStatus.APPROVED, reviewerId)
                if (photo.userId.isNotBlank()) {
                    repository.addUserNotification(
                        userId = photo.userId,
                        title = "Image Approved",
                        message = "Your image submission was approved by a verified expert."
                    )
                }
            }
            refresh()
        }
    }

    fun rejectPhotos(photos: List<LocationPhoto>, reviewerId: String?) {
        viewModelScope.launch {
            photos.forEach { photo ->
                repository.updatePhotoStatus(photo.locationId, photo.locationPhotoId, ContentStatus.REJECTED, reviewerId)
                if (photo.userId.isNotBlank()) {
                    repository.addUserNotification(
                        userId = photo.userId,
                        title = "Image Rejected",
                        message = "Your image submission was rejected by a verified expert."
                    )
                }
            }
            refresh()
        }
    }

    fun approveRockRequest(request: RockDictionaryRequest, reviewerId: String?) {
        viewModelScope.launch {
            repository.approveRockDictionaryRequest(request, reviewerId)
            if (request.submittedById.isNotBlank()) {
                repository.addUserNotification(
                    userId = request.submittedById,
                    title = "Rock Dictionary Update Approved",
                    message = "Your rock dictionary submission was approved by an admin."
                )
            }
            refresh()
        }
    }

    fun rejectRockRequest(request: RockDictionaryRequest, reviewerId: String?) {
        viewModelScope.launch {
            repository.rejectRockDictionaryRequest(request, reviewerId)
            if (request.submittedById.isNotBlank()) {
                repository.addUserNotification(
                    userId = request.submittedById,
                    title = "Rock Dictionary Update Rejected",
                    message = "Your rock dictionary submission was rejected by an admin."
                )
            }
            refresh()
        }
    }

    fun markNotificationRead(notificationId: String) {
        val role = _role.value
        val userId = _userId.value
        if (role == "verified_expert" && notificationId.startsWith("pending_")) return
        if (role == "admin") return
        if (userId.isNullOrBlank()) return
        viewModelScope.launch {
            repository.markNotificationRead(userId, notificationId)
            refresh()
        }
    }

    fun clearNotifications() {
        val role = _role.value
        val userId = _userId.value
        if (role == "verified_expert") {
            if (userId.isNullOrBlank()) {
                _notifications.value = emptyList()
                return
            }
            viewModelScope.launch {
                repository.clearNotifications(userId)
                refresh()
            }
            return
        }
        if (role == "admin") {
            _notifications.value = emptyList()
            return
        }
        if (userId.isNullOrBlank()) return
        viewModelScope.launch {
            repository.clearNotifications(userId)
            refresh()
        }
    }

    private fun UserNotification.toUi(): InboxNotification {
        val date = if (createdAt > 0) {
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(createdAt))
        } else {
            "—"
        }
        return InboxNotification(
            id = id,
            title = title,
            message = message,
            date = date,
            isRead = isRead
        )
    }


    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReviewContentViewModel::class.java)) {
                return ReviewContentViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
