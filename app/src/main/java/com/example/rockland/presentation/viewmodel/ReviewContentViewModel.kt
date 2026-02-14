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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

data class ExpertApplicationReviewItem(
    val userId: String,
    val userDisplayName: String,
    val userEmail: String,
    val currentRole: String,
    val fullName: String,
    val expertise: String,
    val yearsOfExperience: String,
    val portfolioLink: String,
    val notes: String,
    val submittedAt: Long
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

    private val _pendingExpertApplications = MutableStateFlow<List<ExpertApplicationReviewItem>>(emptyList())
    val pendingExpertApplications: StateFlow<List<ExpertApplicationReviewItem>> = _pendingExpertApplications.asStateFlow()

    private val _notifications = MutableStateFlow<List<InboxNotification>>(emptyList())
    val notifications: StateFlow<List<InboxNotification>> = _notifications.asStateFlow()

    private val _userRawNotifications = MutableStateFlow<List<UserNotification>>(emptyList())
    private var notificationsJob: Job? = null
    private var pendingApplicationsJob: Job? = null
    private var pendingCommentsJob: Job? = null
    private var pendingPhotosJob: Job? = null

    private val _userId = MutableStateFlow<String?>(null)
    private val _role = MutableStateFlow("nature_enthusiast")
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun bindUser(userId: String?, role: String?) {
        val normalizedRole = role?.trim()?.lowercase() ?: "nature_enthusiast"
        val changed = _userId.value != userId || _role.value != normalizedRole
        _userId.value = userId
        _role.value = normalizedRole
        if (changed) {
            notificationsJob?.cancel()
            pendingApplicationsJob?.cancel()
            pendingCommentsJob?.cancel()
            pendingPhotosJob?.cancel()
            _userRawNotifications.value = emptyList()
            if (!userId.isNullOrBlank()) {
                notificationsJob = viewModelScope.launch {
                    repository.observeUserNotifications(userId).collectLatest { raw ->
                        _userRawNotifications.value = raw
                        refreshNotifications()
                    }
                }
            }
            if (normalizedRole == "admin") {
                pendingApplicationsJob = viewModelScope.launch {
                    repository.observePendingExpertApplications().collectLatest { list ->
                        _pendingExpertApplications.value = list
                        refreshNotifications()
                    }
                }
            }
            if (normalizedRole == "verified_expert") {
                pendingCommentsJob = viewModelScope.launch {
                    repository.observePendingComments().collectLatest { comments ->
                        _pendingComments.value = comments
                        refreshNotifications()
                    }
                }
                pendingPhotosJob = viewModelScope.launch {
                    repository.observePendingPhotos().collectLatest { photos ->
                        _pendingPhotos.value = photos
                        refreshNotifications()
                    }
                }
            }
        }
        if (changed) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
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
                val expertApplications = if (_role.value == "admin") {
                    try {
                        repository.fetchPendingExpertApplications()
                    } catch (e: Exception) {
                        android.util.Log.e("ReviewContentViewModel", "Failed to fetch pending expert applications", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                _pendingExpertApplications.value = expertApplications
                refreshNotifications()
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to refresh", e)
                _pendingComments.value = emptyList()
                _pendingPhotos.value = emptyList()
                _pendingRockRequests.value = emptyList()
                _pendingHelpRequests.value = emptyList()
                _pendingExpertApplications.value = emptyList()
                _notifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshNotifications() {
        val pendingComments = _pendingComments.value
        val pendingPhotos = _pendingPhotos.value
        val pendingRockRequests = _pendingRockRequests.value
        val pendingHelpRequests = _pendingHelpRequests.value
        val pendingExpertApplications = _pendingExpertApplications.value
        val userNotifications = _userRawNotifications.value
        val role = _role.value
        val userId = _userId.value
        if (role == "verified_expert") {
            val seen = if (!userId.isNullOrBlank()) repository.fetchInboxSeenState(userId) else ContentReviewRepository.InboxSeenState()
            val notifications = buildList {
                if (pendingComments.isNotEmpty() || pendingPhotos.isNotEmpty()) {
                    val commentCount = pendingComments.size
                    val photoCount = pendingPhotos.size
                    val message = if (photoCount > 0) {
                        "You have $commentCount comment(s) and $photoCount image submission(s) waiting for review."
                    } else {
                        "You have $commentCount comment(s) waiting for review."
                    }
                    add(
                        InboxNotification(
                            id = "pending_comments",
                            title = "Content Pending Review",
                            message = message,
                            date = "Today",
                            isRead = commentCount <= seen.pendingCommentCount && photoCount <= seen.pendingPhotoCount
                        )
                    )
                }
                if (userNotifications.isNotEmpty()) {
                    addAll(userNotifications.map { it.toUi() })
                }
            }
            _notifications.value = notifications
        } else if (role == "admin") {
            val seen = if (!userId.isNullOrBlank()) repository.fetchInboxSeenState(userId) else ContentReviewRepository.InboxSeenState()
            val notifications = buildList {
                if (pendingRockRequests.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_rock_dictionary",
                            title = "Rock Dictionary Review",
                            message = "You have ${pendingRockRequests.size} dictionary updates waiting for review.",
                            date = "Today",
                            isRead = pendingRockRequests.size <= seen.pendingRockCount
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
                            isRead = pendingHelpRequests.size <= seen.pendingHelpCount
                        )
                    )
                }
                if (pendingExpertApplications.isNotEmpty()) {
                    val latestSubmittedAt = pendingExpertApplications.maxOfOrNull { it.submittedAt } ?: 0L
                    add(
                        InboxNotification(
                            id = "pending_expert_applications",
                            title = "Application Review",
                            message = "You have ${pendingExpertApplications.size} expert application(s) waiting for review.",
                            date = "Today",
                            isRead = latestSubmittedAt <= seen.pendingExpertApplicationLastSeenAt
                        )
                    )
                }
            }
            _notifications.value = notifications
        } else if (role == "user_admin") {
            val seen = if (!userId.isNullOrBlank()) repository.fetchInboxSeenState(userId) else ContentReviewRepository.InboxSeenState()
            val notifications = buildList {
                if (pendingHelpRequests.isNotEmpty()) {
                    add(
                        InboxNotification(
                            id = "pending_help_requests",
                            title = "Help Requests",
                            message = "You have ${pendingHelpRequests.size} help request(s) waiting for reply.",
                            date = "Today",
                            isRead = pendingHelpRequests.size <= seen.pendingHelpCount
                        )
                    )
                }
                if (userNotifications.isNotEmpty()) {
                    addAll(userNotifications.map { it.toUi() })
                }
            }
            _notifications.value = notifications
        } else if (!userId.isNullOrBlank()) {
            _notifications.value = userNotifications.map { it.toUi() }
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
                        message = "Your comment at ${comment.locationName.ifBlank { "the location" }} was rejected by a verified expert.",
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
                                message = "Your image at ${photo.locationName.ifBlank { "the location" }} was approved by a verified expert.",
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
                                message = "Your image at ${photo.locationName.ifBlank { "the location" }} was rejected by a verified expert.",
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
                        message = "Your rock dictionary submission was approved by an admin.",
                        targetTab = "dictionary",
                        type = "rock_dictionary_approved"
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

    fun approveExpertApplication(item: ExpertApplicationReviewItem, reviewerId: String?) {
        viewModelScope.launch {
            try {
                repository.approveExpertApplication(item, reviewerId)
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to approve expert application", e)
            } finally {
                refresh()
            }
        }
    }

    fun rejectExpertApplication(item: ExpertApplicationReviewItem, reviewerId: String?) {
        viewModelScope.launch {
            try {
                repository.rejectExpertApplication(item, reviewerId)
            } catch (e: Exception) {
                android.util.Log.e("ReviewContentViewModel", "Failed to reject expert application", e)
            } finally {
                refresh()
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        val userId = _userId.value
        if (userId.isNullOrBlank()) return
        viewModelScope.launch {
            when (notificationId) {
                "pending_comments" -> repository.updateInboxSeenState(
                    userId = userId,
                    pendingCommentCount = _pendingComments.value.size,
                    pendingPhotoCount = _pendingPhotos.value.size
                )
                "pending_rock_dictionary" -> repository.updateInboxSeenState(
                    userId = userId,
                    pendingRockCount = _pendingRockRequests.value.size
                )
                "pending_help_requests" -> repository.updateInboxSeenState(
                    userId = userId,
                    pendingHelpCount = _pendingHelpRequests.value.size
                )
                "pending_expert_applications" -> repository.updateInboxSeenState(
                    userId = userId,
                    pendingExpertApplicationCount = _pendingExpertApplications.value.size,
                    pendingExpertApplicationLastSeenAt = (_pendingExpertApplications.value.maxOfOrNull { it.submittedAt } ?: 0L)
                )
                else -> repository.markNotificationRead(userId, notificationId)
            }
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

    fun deleteNotification(notificationId: String) {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            repository.deleteNotification(uid, notificationId)
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
