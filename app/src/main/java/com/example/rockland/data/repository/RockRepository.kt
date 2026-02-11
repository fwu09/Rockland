package com.example.rockland.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

// Simple repository that reads rock metadata from Firestore.
data class Rock(
    val rockID: Int = 0,
    val rockName: String = "",
    val rockRarity: String = "",
    val rockLocation: String = "",
    val rockDesc: String = "",

    // from Firestore (e.g. "shale.jpg" or "shale.png")
    val rockImageName: String = "",

    // used by UI (filled in by repository)
    val rockImageUrl: String = ""
)

class RockRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Resolve rockImageUrl from Storage if it's blank and rockImageName is present.
     * This is SAFE: if resolving fails, we return the original rock unchanged.
     */
    private suspend fun withResolvedImageUrl(rock: Rock?): Rock? {
        if (rock == null) return null

        Log.d("ROCKIMG", "Firestore rock=${rock.rockName} name='${rock.rockImageName}' url='${rock.rockImageUrl}'")

        if (rock.rockImageUrl.isNotBlank()) return rock
        if (rock.rockImageName.isBlank()) return rock

        return try {
            // ✅ If your Storage folder is actually "rocks/" (plural), change here.
            val path = "rock/${rock.rockImageName}"
            Log.d("ROCKIMG", "Resolving storage path=$path")

            val url = storage.reference.child(path).downloadUrl.await().toString()
            Log.d("ROCKIMG", "Resolved url=$url")

            rock.copy(rockImageUrl = url)
        } catch (e: Exception) {
            Log.e("ROCKIMG", "Failed to resolve url for ${rock.rockImageName}: ${e.message}", e)
            rock
        }
    }

    suspend fun getRockByName(rockName: String): Rock? {
        val snapshot = db.collection("rock")
            // ✅ If your Firestore field is stored as "rockname" or something else, change here.
            .whereEqualTo("rockName", rockName)
            .limit(1)
            .get()
            .await()

        val rock = snapshot.documents.firstOrNull()?.toObject(Rock::class.java)
        return withResolvedImageUrl(rock)
    }

    suspend fun getRockById(rockId: Int): Rock? {
        val snapshot = db.collection("rock")
            .whereEqualTo("rockID", rockId)
            .limit(1)
            .get()
            .await()

        val rock = snapshot.documents.firstOrNull()?.toObject(Rock::class.java)
        return withResolvedImageUrl(rock)
    }

    suspend fun getAllRocks(): List<Rock> {
        val snapshot = db.collection("rock").get().await()
        val rocks = snapshot.documents.mapNotNull { it.toObject(Rock::class.java) }

        // Resolve URLs in parallel (faster)
        return coroutineScope {
            rocks.map { r -> async { withResolvedImageUrl(r) ?: r } }.map { it.await() }
        }
    }

    suspend fun hasActiveDependencies(rockId: Int): Boolean {
        // Prefer new explicit rockId field on missions/achievements; fall back to legacy rockID if present.
        val missionByRockId = db.collection("missions")
            .whereEqualTo("rockId", rockId)
            .limit(1)
            .get()
            .await()
        if (!missionByRockId.isEmpty) return true

        val missionByLegacyId = db.collection("missions")
            .whereEqualTo("rockID", rockId)
            .limit(1)
            .get()
            .await()
        if (!missionByLegacyId.isEmpty) return true

        val achievementByRockId = db.collection("achievements")
            .whereEqualTo("rockId", rockId)
            .limit(1)
            .get()
            .await()
        if (!achievementByRockId.isEmpty) return true

        val achievementByLegacyId = db.collection("achievements")
            .whereEqualTo("rockID", rockId)
            .limit(1)
            .get()
            .await()
        return !achievementByLegacyId.isEmpty
    }

    suspend fun deleteRock(rockId: Int) {
        val snapshot = db.collection("rock")
            .whereEqualTo("rockID", rockId)
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return
        doc.reference.delete().await()
    }
}
