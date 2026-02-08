package com.example.rockland.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Simple repository that reads rock metadata from Firestore.
data class Rock(
    val rockID: Int = 0,
    val rockName: String = "",
    val rockRarity: String = "",
    val rockLocation: String = "",
    val rockDesc: String = "",
    val rockImageUrl: String = ""
)

class RockRepository {

    private val db = FirebaseFirestore.getInstance()

    // Fetches the first rock document that matches the provided name.
    suspend fun getRockByName(rockName: String): Rock? {
        val snapshot = db.collection("rock")
            .whereEqualTo("rockName", rockName)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(Rock::class.java)
    }

    // Fetches the rock by its numeric ID.
    suspend fun getRockById(rockId: Int): Rock? {
        val snapshot = db.collection("rock")
            .whereEqualTo("rockID", rockId)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(Rock::class.java)
    }

    // Lists every rock from Firestore.
    suspend fun getAllRocks(): List<Rock> {
        val snapshot = db.collection("rock")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(Rock::class.java) }
    }
}
