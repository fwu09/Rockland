package com.example.rockland.data.repository

import com.example.rockland.data.model.ContentStatus
import com.example.rockland.data.model.RockAnnotation
import com.example.rockland.data.model.RockCommunityContent
import com.example.rockland.data.model.LocationComment
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.model.LocationPhoto
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
            docToRockLocation(doc)
        }
    }

    suspend fun getLocation(locationId: String): RockLocation? {
        if (locationId.isBlank()) return null
        val doc = collectionRef.document(locationId).get().await() ?: return null
        if (!doc.exists()) return null
        return docToRockLocation(doc)
    }

    private fun docToRockLocation(doc: com.google.firebase.firestore.DocumentSnapshot): RockLocation? {
        val lat = doc.getDouble("latitude")
        val lng = doc.getDouble("longitude")
        val name = doc.getString("name") ?: ""
        val description = doc.getString("description") ?: ""
        val category = doc.getString("category") ?: ""
        if (lat != null && lng != null) {
            return RockLocation(
                id = doc.id,
                name = name,
                description = description,
                latitude = lat,
                longitude = lng,
                category = category
            )
        }
        return null
    }

    suspend fun createLocation(
        name: String,
        description: String,
        latitude: Double,
        longitude: Double
    ): String {
        val payload = mapOf(
            "name" to name.trim(),
            "description" to description.trim(),
            "latitude" to latitude,
            "longitude" to longitude,
            "category" to "unverified"
        )
        val ref = collectionRef.add(payload).await()
        return ref.id
    }

    suspend fun updateLocation(
        locationId: String,
        name: String,
        description: String,
        latitude: Double,
        longitude: Double
    ) {
        if (locationId.isBlank()) return
        collectionRef.document(locationId).update(
            mapOf(
                "name" to name.trim(),
                "description" to description.trim(),
                "latitude" to latitude,
                "longitude" to longitude
            )
        ).await()
    }

    suspend fun deleteLocation(locationId: String) {
        if (locationId.isBlank()) return
        collectionRef.document(locationId).delete().await()
    }

    // Marks a rock location as verified.
    suspend fun markLocationVerified(locationId: String) {
        if (locationId.isBlank()) return
        collectionRef.document(locationId)
            .update("category", "verified")
            .await()
    }

    private fun communityDoc(locationId: String) =
        collectionRef.document(locationId).collection("community").document("default")

    private fun commentsRef(locationId: String) = communityDoc(locationId).collection("comments")
    private fun photosRef(locationId: String) = communityDoc(locationId).collection("photos")
    private fun annotationsRef(locationId: String) =
        communityDoc(locationId).collection("annotations")

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


        val comments = commentsSnapshot.documents.mapNotNull { doc ->

            val status = when (doc.getString("status")) {
                ContentStatus.PENDING.name -> ContentStatus.PENDING
                ContentStatus.REJECTED.name -> ContentStatus.REJECTED
                ContentStatus.APPROVED.name -> ContentStatus.APPROVED
                else -> ContentStatus.APPROVED
            }

            if (status != ContentStatus.APPROVED) return@mapNotNull null

            LocationComment(
                commentId = doc.id,
                locationId = doc.getString("locationId") ?: locationId,
                locationName = doc.getString("locationName") ?: "",
                userId = doc.getString("userId") ?: "",
                author = doc.getString("author") ?: "Unknown",
                text = doc.getString("text") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                updatedAt = doc.getLong("updatedAt"),

                //user content moderation
                status = status,
                reviewedBy = doc.getString("reviewedBy"),
                reviewedAt = doc.getLong("reviewedAt")
            )
        }

        val photos = photosSnapshot.documents.mapNotNull { doc ->
            val status = when (doc.getString("status")) {
                ContentStatus.PENDING.name -> ContentStatus.PENDING
                ContentStatus.REJECTED.name -> ContentStatus.REJECTED
                else -> ContentStatus.APPROVED
            }
            if (status != ContentStatus.APPROVED) return@mapNotNull null

            LocationPhoto(
                locationPhotoId = doc.id,
                locationId = doc.getString("locationId") ?: locationId,
                locationName = doc.getString("locationName") ?: "",
                commentId = doc.getString("commentId"),
                userId = doc.getString("userId") ?: "",
                author = doc.getString("author") ?: "Unknown", //change for prod env
                caption = doc.getString("caption") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                status = status
            )
        }

        val annotations = annotationsSnapshot.documents.map { doc ->
            RockAnnotation(
                id = doc.id,
                expertId = doc.getString("expertId") ?: "",
                expertName = doc.getString("expertName") ?: "Expert",
                note = doc.getString("note") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList()
            )
        }

        return RockCommunityContent(
            comments = comments,
            photos = photos,
            annotations = annotations
        )
    }

    suspend fun addComment(
        locationId: String,
        locationName: String,
        userId: String,
        author: String,
        text: String
    ): String {
        val payload = mapOf(
            "locationId" to locationId,
            "locationName" to locationName,
            "userId" to userId,
            "author" to author,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "status" to ContentStatus.PENDING.name,

            // user-content moderation null for now
            "reviewedBy" to null,
            "reviewedAt" to null
        )

        val ref = commentsRef(locationId).add(payload).await()
        return ref.id
    }

    suspend fun deleteComment(locationId: String, commentId: String) {
        commentsRef(locationId).document(commentId).delete().await()
    }

    suspend fun deletePhoto(locationId: String, locationPhotoId: String) {
        photosRef(locationId).document(locationPhotoId).delete().await()
    }


    suspend fun addPhoto(
        locationId: String,
        locationName: String,
        commentId: String?, // allow photos not attached to a comment too
        userId: String,
        author: String,
        caption: String,
        imageUrl: String
    ) {
        val payload = mapOf(
            "locationId" to locationId,
            "locationName" to locationName,
            "commentId" to commentId,
            "userId" to userId,
            "author" to author,
            "caption" to caption,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis(),
            "status" to ContentStatus.PENDING.name
        )
        photosRef(locationId).add(payload).await()
    }

    suspend fun addAnnotation(
        locationId: String,
        expertId: String,
        expertName: String,
        note: String,
        imageUrls: List<String> = emptyList()
    ) {
        val payload = mapOf(
            "locationId" to locationId,
            "expertId" to expertId,
            "expertName" to expertName,
            "note" to note,
            "timestamp" to System.currentTimeMillis(),
            "imageUrls" to imageUrls
        )
        annotationsRef(locationId).add(payload).await()
    }

    suspend fun updateAnnotation(
        locationId: String,
        annotationId: String,
        note: String,
        imageUrls: List<String> = emptyList()
    ) {
        val payload = mapOf(
            "note" to note,
            "imageUrls" to imageUrls,
            "updatedAt" to System.currentTimeMillis()
        )
        annotationsRef(locationId).document(annotationId).update(payload).await()
    }

    suspend fun deleteAnnotation(locationId: String, annotationId: String) {
        annotationsRef(locationId).document(annotationId).delete().await()
    }

    @Suppress("unused")
    suspend fun addCommentWithPhotos(
        locationId: String,
        locationName: String,
        userId: String,
        author: String,
        text: String,
        photoUrls: List<String>,
        caption: String = ""
    ): String {

        val commentId = addComment(locationId, locationName, userId, author, text)
        for (url in photoUrls) {
            addPhoto(
                locationId = locationId,
                locationName = locationName,
                commentId = commentId,
                userId = userId,
                author = author,
                caption = caption,
                imageUrl = url
            )
        }
        return commentId
    }
}

