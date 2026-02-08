package com.example.rockland.data.repository

import com.example.rockland.data.model.RockAnnotation
import com.example.rockland.data.model.RockCommunityContent
import com.example.rockland.data.model.RockComment
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.model.RockPhoto
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Repository that exposes rockLocations documents for the map UI.
class RockLocationRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Firestore collection storing nearby rock coordinates.
    private val collectionRef = firestore.collection("rockLocations")
    private val communityLibrary = mapOf(
        "demo-rock-1" to defaultCommunityContent
    )

    // TODO(Backend, RockLocationRepository.kt): Adjust query or migrate to REST backend if needed.
    suspend fun fetchRockLocations(): List<RockLocation> = suspendCoroutine { cont ->
        collectionRef
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
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
                cont.resume(list)
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }

    fun fetchCommunityContent(locationId: String): RockCommunityContent {
        return communityLibrary[locationId] ?: defaultCommunityContent
    }

    companion object {
        private val defaultCommunityContent = RockCommunityContent(
            comments = listOf(
                RockComment(
                    id = "comment-1",
                    author = "Alex",
                    text = "Sunrise light makes the mineral grains shine here.",
                    timestamp = System.currentTimeMillis() - 3600000
                ),
                RockComment(
                    id = "comment-2",
                    author = "Maya",
                    text = "I found a small deposit of quartz veins tucked behind the ledge.",
                    timestamp = System.currentTimeMillis() - 7200000
                )
            ),
            photos = listOf(
                RockPhoto(
                    id = "photo-1",
                    author = "Riley",
                    caption = "Serpentine textures caught under overcast light.",
                    imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee"
                ),
                RockPhoto(
                    id = "photo-2",
                    author = "Jordan",
                    caption = "Hand sample that reveals the sharp fracture planes.",
                    imageUrl = "https://images.unsplash.com/photo-1516501545647-5e209f2d848f"
                )
            ),
            annotations = listOf(
                RockAnnotation(
                    id = "annotation-1",
                    expertName = "Dr. Stone",
                    note = "Layered foliation points to a medium-grade metamorphic history."
                ),
                RockAnnotation(
                    id = "annotation-2",
                    expertName = "GeoNet",
                    note = "Presence of garnet-rich bands near the west trail."
                )
            )
        )
    }
}
