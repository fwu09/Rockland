package com.example.rockland.data.boxes

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class BoxOpenResult(
    val boxType: String,
    val awardedRockId: String,
    val awardedRarity: String
)

class BoxRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersRef = firestore.collection("users")
    private val rocksRef = firestore.collection("rock") // change to "rocks" if your collection name is rocks

    /**
     * Opens 1 box of type common/rare/special:
     * - decrements user's boxInventory.<type>
     * - randomly selects a rock based on weights + eligible rarities
     * - adds rock to user's collection (subcollection "collection" by rockId)
     */
    suspend fun openBox(userId: String, boxType: String): BoxOpenResult {
        val normalized = boxType.lowercase()
        require(normalized in setOf("common", "rare", "special")) { "Invalid box type: $boxType" }

        val userDocRef = usersRef.document(userId)

        firestore.runTransaction { tx ->
            val userSnap = tx.get(userDocRef)
            val inv = userSnap.get("boxInventory") as? Map<*, *> ?: emptyMap<Any, Any>()
            val currentCount = (inv[normalized] as? Long) ?: 0L
            if (currentCount <= 0L) throw IllegalStateException("No $normalized boxes to open")

            // decrement box count
            tx.update(userDocRef, "boxInventory.$normalized", FieldValue.increment(-1))

            null
        }.await()

        // Choose rarity weights
        val (rarity, allowedRarities) = when (normalized) {
            "common" -> pickRarity(
                weights = listOf("Very Common" to 55, "Common" to 40, "Rare" to 5)
            ) to setOf("Very Common", "Common", "Rare")

            "rare" -> pickRarity(
                weights = listOf("Very Common" to 50, "Common" to 35, "Rare" to 10, "Ultra Rare" to 5)
            ) to setOf("Very Common", "Common", "Rare", "Ultra Rare")

            // special: for now just pick "ROTM" if you tag it, else fall back to Ultra Rare
            else -> "Ultra Rare" to setOf("Ultra Rare")
        }

        // Query rocks by rarity (you must store a field like rarity: "Common" in each rock doc)
        val candidates = rocksRef.whereEqualTo("rockRarity", rarity).get().await().documents
        if (candidates.isEmpty()) {
            // fallback: pick any allowed rarity if the exact rarity has no docs
            val fallbackDocs = rocksRef.whereIn("rockRarity", allowedRarities.toList()).get().await().documents
            if (fallbackDocs.isEmpty()) throw IllegalStateException("No rocks found for box selection")
            val picked = fallbackDocs.random()
            val rockId = picked.id
            awardRockToUser(userId, rockId)
            return BoxOpenResult(normalized, rockId, picked.getString("rockRarity") ?: "Unknown")
        }

        val picked = candidates.random()
        val rockId = picked.id
        awardRockToUser(userId, rockId)
        return BoxOpenResult(normalized, rockId, rarity)
    }

    private suspend fun awardRockToUser(userId: String, rockId: String) {
        // Store duplicates count
        val userRockRef = usersRef.document(userId).collection("collection").document(rockId)
        firestore.runTransaction { tx ->
            val snap = tx.get(userRockRef)
            val currentQty = (snap.getLong("quantity") ?: 0L)
            val newQty = currentQty + 1L
            tx.set(userRockRef, mapOf("quantity" to newQty), SetOptions.merge())
            null
        }.await()
    }

    private fun pickRarity(weights: List<Pair<String, Int>>): String {
        val total = weights.sumOf { it.second }
        val r = Random.nextInt(1, total + 1)
        var acc = 0
        for ((label, w) in weights) {
            acc += w
            if (r <= acc) return label
        }
        return weights.last().first
    }
}
