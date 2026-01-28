package com.example.rockland.data.repository

import com.example.rockland.data.model.RockAnnotation
import com.example.rockland.data.model.RockCommunityContent
import com.example.rockland.data.model.RockComment
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.model.RockPhoto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// Repository that exposes rockLocations documents for the map UI.
class RockLocationRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Firestore collection storing nearby rock coordinates.
    private val collectionRef = firestore.collection("rockLocations")
    suspend fun fetchRockLocations(): List<RockLocation> {
        val snapshot = collectionRef.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val lat = doc.getDouble("latitude")
            val lng = doc.getDouble("longitude")
            val name = doc.getString("name") ?: ""
            val description = doc.getString("description") ?: ""
            val category = doc.getString("category") ?: ""
            if (lat != null && lng != null) {
                RockLocation(
                    id = doc.id,
                    name = name,
                    description = description,
                    latitude = lat,
                    longitude = lng,
                    category = category
                )
            } else {
                null
            }
        }
    }

    private fun communityDoc(locationId: String) =
        collectionRef.document(locationId).collection("community").document("default")

    private fun commentsRef(locationId: String) = communityDoc(locationId).collection("comments")
    private fun photosRef(locationId: String) = communityDoc(locationId).collection("photos")
    private fun annotationsRef(locationId: String) = communityDoc(locationId).collection("annotations")

    suspend fun fetchCommunityContent(locationId: String): RockCommunityContent {
        val commentsSnapshot = commentsRef(locationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        val photosSnapshot = photosRef(locationId)
            .get()
            .await()

        val annotationsSnapshot = annotationsRef(locationId)
            .get()
            .await()

        val comments = commentsSnapshot.documents.map { doc ->
            RockComment(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                author = doc.getString("author") ?: "Unknown",
                text = doc.getString("text") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }

        val photos = photosSnapshot.documents.map { doc ->
            RockPhoto(
                id = doc.id,
                author = doc.getString("author") ?: "Unknown",
                caption = doc.getString("caption") ?: "",
                imageUrl = doc.getString("imageUrl") ?: ""
            )
        }

        val annotations = annotationsSnapshot.documents.map { doc ->
            RockAnnotation(
                id = doc.id,
                expertName = doc.getString("expertName") ?: "Expert",
                note = doc.getString("note") ?: ""
            )
        }

        return RockCommunityContent(
            comments = comments,
            photos = photos,
            annotations = annotations
        )
    }

    suspend fun addComment(locationId: String, userId: String, author: String, text: String) {
        val payload = mapOf(
            "userId" to userId,
            "author" to author,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        commentsRef(locationId).add(payload).await()
    }

    suspend fun updateComment(locationId: String, commentId: String, newText: String) {
        val payload = mapOf(
            "text" to newText,
            "updatedAt" to System.currentTimeMillis()
        )
        commentsRef(locationId).document(commentId).update(payload).await()
    }

    suspend fun deleteComment(locationId: String, commentId: String) {
        commentsRef(locationId).document(commentId).delete().await()
    }

    suspend fun addPhoto(locationId: String, author: String, caption: String, imageUrl: String) {
        val payload = mapOf(
            "author" to author,
            "caption" to caption,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )
        photosRef(locationId).add(payload).await()
    }
}
