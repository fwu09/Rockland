package com.example.rockland.data.repository

import com.example.rockland.data.model.CollectionItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CollectionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userCollectionRef(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("collection")

    // Load all collection items for a given user.
    suspend fun fetchCollection(userId: String): List<CollectionItem> =
        suspendCoroutine { cont ->
            userCollectionRef(userId)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { snapshot ->
                    val items = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(CollectionItem::class.java)
                        item?.copy(id = doc.id)
                    }
                    cont.resume(items)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    // Check whether the given rock is already in the user's collection.
    suspend fun isRockInCollection(userId: String, rockId: String): Boolean =
        suspendCoroutine { cont ->
            userCollectionRef(userId)
                .whereEqualTo("rockId", rockId)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(!snapshot.isEmpty)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    // Add a new collection entry for the current user.
    suspend fun addRockToCollection(
        userId: String,
        rockId: String,
        rockSource: String,
        rockName: String,
        thumbnailUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Unit {
        return suspendCoroutine { cont ->
            val data = hashMapOf(
                "rockId" to rockId,
                "rockSource" to rockSource,
                "rockName" to rockName,
                "thumbnailUrl" to thumbnailUrl,
                "latitude" to latitude,
                "longitude" to longitude,
                "customId" to "",
                "locationLabel" to "",
                "notes" to "",
                "imageUrls" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to Timestamp.now()
            )

            userCollectionRef(userId)
                .add(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    // Update notes and extra fields of an existing collection entry.
    suspend fun updateCollectionItem(
        userId: String,
        itemId: String,
        customId: String,
        locationLabel: String,
        notes: String,
        imageUrls: List<String>
    ): Unit {
        return suspendCoroutine { cont ->
            val updates = hashMapOf<String, Any>(
                "customId" to customId,
                "locationLabel" to locationLabel,
                "notes" to notes,
                "imageUrls" to imageUrls,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            userCollectionRef(userId)
                .document(itemId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    // Delete a collection entry for the user.
    suspend fun removeRock(userId: String, itemId: String): Unit {
        return suspendCoroutine { cont ->
            userCollectionRef(userId)
                .document(itemId)
                .delete()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }
}
