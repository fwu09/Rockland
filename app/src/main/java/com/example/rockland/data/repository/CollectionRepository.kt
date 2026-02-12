package com.example.rockland.data.repository

import com.example.rockland.data.model.CollectionItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Performs Firestore reads/writes for the user's collection documents.
class CollectionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Reference to the signed-in user's collection subcollection.
    private fun userCollectionRef(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("collection")

    // Real-time listener for the user's collection.
    fun listenToCollection(
        userId: String,
        onItems: (List<CollectionItem>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        return userCollectionRef(userId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val docs = snapshot ?: return@addSnapshotListener
                val items = docs.documents.mapNotNull { doc ->
                    val item = doc.toObject(CollectionItem::class.java)
                    item?.copy(id = doc.id)
                }
                onItems(items)
            }
    }

    // Checks for duplicates by rockId first, then rockName.
    suspend fun isRockInCollection(userId: String, rockId: String, rockName: String): Boolean =
        suspendCoroutine { cont ->
            userCollectionRef(userId)
                .whereEqualTo("rockId", rockId)
                .limit(1)
                .get()
                .addOnSuccessListener { byId ->
                    if (!byId.isEmpty) {
                        cont.resume(true)
                        return@addOnSuccessListener
                    }
                    userCollectionRef(userId)
                        .whereEqualTo("rockName", rockName)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { byName ->
                            cont.resume(!byName.isEmpty)
                        }
                        .addOnFailureListener { e ->
                            cont.resumeWithException(e)
                        }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    // Adds a new collection entry and records the dictionary unlock.
    // Adds a new collection entry and records the dictionary unlock.
    suspend fun addRockToCollection(
        userId: String,
        rockId: String,
        rockSource: String,
        rockName: String,
        thumbnailUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): String =
        suspendCoroutine { cont ->
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
                "userImageUrls" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to Timestamp.now()
            )

            userCollectionRef(userId)
                .add(data)
                .addOnSuccessListener { docRef ->
                    // Persist dictionary unlock once discovered.
                    firestore.collection("users")
                        .document(userId)
                        .set(
                            mapOf("unlockedRockIds" to FieldValue.arrayUnion(rockId)),
                            SetOptions.merge()
                        )
                    cont.resume(docRef.id)   // <-- THIS is the key change
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }


    // Updates notes, IDs, and timestamps for one entry.
    suspend fun updateCollectionItem(
        userId: String,
        itemId: String,
        customId: String,
        locationLabel: String,
        notes: String,
        userImageUrls: List<String>
    ) {
        return suspendCoroutine { cont ->
            val updates = hashMapOf<String, Any>(
                "customId" to customId,
                "locationLabel" to locationLabel,
                "notes" to notes,
                "userImageUrls" to userImageUrls,
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

    // Appends new user photo URLs to the collection entry.
    suspend fun appendUserImageUrls(
        userId: String,
        itemId: String,
        urls: List<String>
    ): Unit = suspendCoroutine { cont ->
        if (urls.isEmpty()) {
            cont.resume(Unit)
            return@suspendCoroutine
        }
        userCollectionRef(userId)
            .document(itemId)
            .update(
                mapOf(
                    "userImageUrls" to FieldValue.arrayUnion(*urls.toTypedArray()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    // Removes user photo URLs from the collection entry.
    suspend fun removeUserImageUrls(
        userId: String,
        itemId: String,
        urls: List<String>
    ): Unit = suspendCoroutine { cont ->
        if (urls.isEmpty()) {
            cont.resume(Unit)
            return@suspendCoroutine
        }
        userCollectionRef(userId)
            .document(itemId)
            .update(
                mapOf(
                    "userImageUrls" to FieldValue.arrayRemove(*urls.toTypedArray()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    // Migrates legacy imageUrls into userImageUrls and optionally deletes the old field.
    suspend fun migrateLegacyImageUrls(
        userId: String,
        deleteLegacyField: Boolean = true
    ): Unit = suspendCoroutine { cont ->
        userCollectionRef(userId)
            .get()
            .addOnSuccessListener { snap ->
                val batch = firestore.batch()
                for (doc in snap.documents) {
                    val legacy = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val current = (doc.get("userImageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val shouldCopy = current.isEmpty() && legacy.isNotEmpty()
                    val shouldDeleteLegacy = deleteLegacyField && doc.contains("imageUrls")

                    if (shouldCopy || shouldDeleteLegacy) {
                        val updates = hashMapOf<String, Any>(
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (shouldCopy) updates["userImageUrls"] = legacy
                        if (shouldDeleteLegacy) updates["imageUrls"] = FieldValue.delete()
                        batch.update(doc.reference, updates)
                    }
                }
                batch.commit()
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    // Deletes the chosen collection document for this user.
    suspend fun removeRock(userId: String, itemId: String) {
        return suspendCoroutine { cont ->
            userCollectionRef(userId)
                .document(itemId)
                .delete()
                .addOnSuccessListener {
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    // function of following processes: convenient image upload of scanned rocks into collection
    // finds existing id of rock collection entry
    suspend fun findCollectionItemId(
        userId: String,
        rockId: String,
        rockName: String
    ): String? = suspendCoroutine { cont ->
        userCollectionRef(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val match = snapshot.documents.firstOrNull { doc ->
                    doc.getString("rockId") == rockId ||
                            doc.getString("rockName")?.equals(rockName, ignoreCase = true) == true
                }
                cont.resume(match?.id)
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
    }

    // (upload helpers for personal-notes flow have been removed because the flow now uses CollectionViewModel utilities)
}
