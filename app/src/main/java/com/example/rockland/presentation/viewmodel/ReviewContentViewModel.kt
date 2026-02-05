package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.model.ContentStatus
import com.example.rockland.data.model.HelpRequest
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
    val isRead: Boolean,
    val targetTab: String? = null,
    val targetLocationId: String? = null,
    val type: String? = null
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

    private val _pendingHelpRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val pendingHelpRequests: StateFlow<List<HelpRequest>> = _pendingHelpRequests.asStateFlow()

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
                val helpRequests = if (_role.value == "admin" || _role.value == "user_admin") {
                    try {
                        repository.fetchPendingHelpRequests()
                    } catch (e: Exception) {
                        android.util.Log.e("ReviewContentViewModel", "Failed to fetch pending help requests", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                _pendingHelpRequests.value = helpRequests
                refreshNotifications(comments, photos, rockRequests, helpRequests)
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to refresh", e)
                _pendingComments.value = emptyList()
                _pendingPhotos.value = emptyList()
                _pendingRockRequests.value = emptyList()
                _pendingHelpRequests.value = emptyList()
                _notifications.value = emptyList()
            }
        }
    }

    private suspend fun refreshNotifications(
        pendingComments: List<LocationComment>,
        pendingPhotos: List<LocationPhoto>,
        pendingRockRequests: List<RockDictionaryRequest>,
        pendingHelpRequests: List<HelpRequest>
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
                if (pendingHelpRequests.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_help_requests",
                            title = "Help Requests",
                            message = "You have ${pendingHelpRequests.size} help request(s) waiting for reply.",
                            date = "Today",
                            isRead = false
                        )
                    )
                }
            }
            _notifications.value = notifications
        } else if (role == "user_admin") {
            val notifications = buildList {
                if (pendingHelpRequests.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_help_requests",
                            title = "Help Requests",
                            message = "You have ${pendingHelpRequests.size} help request(s) waiting for reply.",
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
        } else if (!userId.isNullOrBlank()) {
            val raw = repository.fetchUserNotifications(userId)
            _notifications.value = raw.map { it.toUi() }
        } else {
            _notifications.value = emptyList()
        }
    }

    fun approveComment(comment: LocationComment, reviewerId: String?) {
        viewModelScope.launch {
            try {
                repository.updateCommentStatus(
                    comment.locationId,
                    comment.commentId,
                    ContentStatus.APPROVED,
                    reviewerId
                )
                if (comment.userId.isNotBlank()) {
                    repository.addUserNotification(
                        userId = comment.userId,
                        title = "Comment Approved",
                        message = "Your comment was approved by a verified expert.",
                        targetTab = "map",
                        targetLocationId = comment.locationId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to approve comment", e)
            } finally {
                refresh()
            }
        }
    }

    fun rejectComment(comment: LocationComment, reviewerId: String?) {
        viewModelScope.launch {
            try {
                repository.updateCommentStatus(
                    comment.locationId,
                    comment.commentId,
                    ContentStatus.REJECTED,
                    reviewerId
                )
                if (comment.userId.isNotBlank()) {
                    repository.addUserNotification(
                        userId = comment.userId,
                        title = "Comment Rejected",
                        message = "Your comment was rejected by a verified expert.",
                        targetTab = "map",
                        targetLocationId = comment.locationId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to reject comment", e)
            } finally {
                refresh()
            }
        }
    }

    fun approvePhotos(photos: List<LocationPhoto>, reviewerId: String?) {
        viewModelScope.launch {
            try {
                photos.forEach { photo ->
                    try {
                        repository.updatePhotoStatus(
                            photo.locationId,
                            photo.locationPhotoId,
                            ContentStatus.APPROVED,
                            reviewerId
                        )
                        if (photo.userId.isNotBlank()) {
                            repository.addUserNotification(
                                userId = photo.userId,
                                title = "Image Approved",
                                message = "Your image submission was approved by a verified expert.",
                                targetTab = "map",
                                targetLocationId = photo.locationId
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "ReviewContentViewModel",
                            "Failed to approve photo ${photo.locationPhotoId}",
                            e
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to approve photos batch", e)
            } finally {
                refresh()
            }
        }
    }

    fun rejectPhotos(photos: List<LocationPhoto>, reviewerId: String?) {
        viewModelScope.launch {
            try {
                photos.forEach { photo ->
                    try {
                        repository.updatePhotoStatus(
                            photo.locationId,
                            photo.locationPhotoId,
                            ContentStatus.REJECTED,
                            reviewerId
                        )
                        if (photo.userId.isNotBlank()) {
                            repository.addUserNotification(
                                userId = photo.userId,
                                title = "Image Rejected",
                                message = "Your image submission was rejected by a verified expert.",
                                targetTab = "map",
                                targetLocationId = photo.locationId
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "ReviewContentViewModel",
                            "Failed to reject photo ${photo.locationPhotoId}",
                            e
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to reject photos batch", e)
            } finally {
                refresh()
            }
        }
    }

    fun approveRockRequest(request: RockDictionaryRequest, reviewerId: String?) {
        viewModelScope.launch {
            try {
                repository.approveRockDictionaryRequest(request, reviewerId)
                if (request.submittedById.isNotBlank()) {
                    repository.addUserNotification(
                        userId = request.submittedById,
                        title = "Rock Dictionary Update Approved",
                        message = "Your rock dictionary submission was approved by an admin."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to approve rock request", e)
            } finally {
                refresh()
            }
        }
    }

    fun rejectRockRequest(request: RockDictionaryRequest, reviewerId: String?) {
        viewModelScope.launch {
            try {
                repository.rejectRockDictionaryRequest(request, reviewerId)
                if (request.submittedById.isNotBlank()) {
                    repository.addUserNotification(
                        userId = request.submittedById,
                        title = "Rock Dictionary Update Rejected",
                        message = "Your rock dictionary submission was rejected by an admin."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to reject rock request", e)
            } finally {
                refresh()
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        val role = _role.value
        val userId = _userId.value
        if (role == "verified_expert" && notificationId.startsWith("pending_")) return
        if (role == "admin" && notificationId.startsWith("pending_")) return
        if (role == "user_admin" && notificationId.startsWith("pending_")) return
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
        if (role == "user_admin") {
            if (!userId.isNullOrBlank()) {
                viewModelScope.launch {
                    repository.clearNotifications(userId)
                    refresh()
                }
            } else {
                _notifications.value = emptyList()
            }
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
            isRead = isRead,
            targetTab = targetTab,
            targetLocationId = targetLocationId,
            type = type
        )
    }

    fun submitHelpRequest(userId: String?, userDisplayName: String?, subject: String, details: String) {
        if (userId.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                repository.submitHelpRequest(
                    userId = userId,
                    userDisplayName = userDisplayName.orEmpty(),
                    subject = subject.ifBlank { "Help Request" },
                    details = details
                )
                // Refresh to update pending help requests for admin
                refresh()
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to submit help request", e)
            }
        }
    }

    fun replyHelpRequest(request: HelpRequest, replyText: String, repliedById: String?) {
        if (repliedById.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                repository.replyHelpRequest(
                    requestId = request.id,
                    replyText = replyText,
                    repliedById = repliedById,
                    requestUserId = request.userId
                )
                refresh()
            } catch (_: Exception) { }
        }
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
