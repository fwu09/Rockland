package com.example.rockland.data.repository

import com.example.rockland.data.model.ContentStatus
import com.example.rockland.data.model.LocationComment
import com.example.rockland.data.model.LocationPhoto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

data class UserNotification(
    val id: String,
    val title: String,
    val message: String,
    val createdAt: Long,
    val isRead: Boolean
)

class ContentReviewRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val rockLocationsRef = firestore.collection("rockLocations")
    private val usersRef = firestore.collection("users")

    suspend fun fetchPendingComments(): List<LocationComment> {
        val snapshot = firestore.collectionGroup("comments")
            .whereEqualTo("status", ContentStatus.PENDING.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            LocationComment(
                commentId = doc.id,
                locationId = doc.getString("locationId") ?: "",
                userId = doc.getString("userId") ?: "",
                author = doc.getString("author") ?: "Unknown",
                text = doc.getString("text") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                updatedAt = doc.getLong("updatedAt"),
                status = ContentStatus.PENDING,
                reviewedBy = doc.getString("reviewedBy"),
                reviewedAt = doc.getLong("reviewedAt")
            )
        }
    }

    suspend fun fetchPendingPhotos(): List<LocationPhoto> {
        val snapshot = firestore.collectionGroup("photos")
            .whereEqualTo("status", ContentStatus.PENDING.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            LocationPhoto(
                locationPhotoId = doc.id,
                locationId = doc.getString("locationId") ?: "",
                commentId = doc.getString("commentId"),
                userId = doc.getString("userId") ?: "",
                author = doc.getString("author") ?: "Unknown",
                caption = doc.getString("caption") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                status = ContentStatus.PENDING,
                reviewedBy = doc.getString("reviewedBy"),
                reviewedAt = doc.getLong("reviewedAt")
            )
        }
    }

    suspend fun updateCommentStatus(
        locationId: String,
        commentId: String,
        status: ContentStatus,
        reviewerId: String?
    ) {
        if (locationId.isBlank() || commentId.isBlank()) return
        val updates = mapOf(
            "status" to status.name,
            "reviewedBy" to reviewerId,
            "reviewedAt" to System.currentTimeMillis()
        )
        rockLocationsRef.document(locationId)
            .collection("community")
            .document("default")
            .collection("comments")
            .document(commentId)
            .update(updates)
            .await()
    }

    suspend fun updatePhotoStatus(
        locationId: String,
        photoId: String,
        status: ContentStatus,
        reviewerId: String?
    ) {
        if (locationId.isBlank() || photoId.isBlank()) return
        val updates = mapOf(
            "status" to status.name,
            "reviewedBy" to reviewerId,
            "reviewedAt" to System.currentTimeMillis()
        )
        rockLocationsRef.document(locationId)
            .collection("community")
            .document("default")
            .collection("photos")
            .document(photoId)
            .update(updates)
            .await()
    }

    suspend fun fetchUserNotifications(userId: String): List<UserNotification> {
        if (userId.isBlank()) return emptyList()
        val snapshot = usersRef.document(userId)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.map { doc ->
            val createdAt = when (val raw = doc.get("createdAt")) {
                is Number -> raw.toLong()
                is Timestamp -> raw.toDate().time
                else -> 0L
            }
            UserNotification(
                id = doc.id,
                title = doc.getString("title") ?: "",
                message = doc.getString("message") ?: "",
                createdAt = createdAt,
                isRead = doc.getBoolean("isRead") ?: false
            )
        }
    }

    suspend fun addUserNotification(
        userId: String,
        title: String,
        message: String
    ) {
        if (userId.isBlank()) return
        val payload = mapOf(
            "title" to title,
            "message" to message,
            "createdAt" to System.currentTimeMillis(),
            "isRead" to false
        )
        usersRef.document(userId)
            .collection("notifications")
            .add(payload)
            .await()
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        if (userId.isBlank() || notificationId.isBlank()) return
        usersRef.document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }

    suspend fun clearNotifications(userId: String) {
        if (userId.isBlank()) return
        val snapshot = usersRef.document(userId)
            .collection("notifications")
            .get()
            .await()
        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}
