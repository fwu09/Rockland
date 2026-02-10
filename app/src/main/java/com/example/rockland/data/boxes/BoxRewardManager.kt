package com.example.rockland.data.boxes

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object BoxRewardManager {

    private val db = FirebaseFirestore.getInstance()

    fun grantBox(userId: String, missionType: String) {
        val boxType = when (missionType.lowercase()) {
            "daily" -> "common"
            "weekly" -> "rare"
            "monthly" -> "special"
            else -> "common"
        }

        val userRef = db.collection("users").document(userId)

        userRef.update("boxInventory.$boxType", FieldValue.increment(1))
            .addOnSuccessListener {
                println("✅ Box added: $boxType")
            }
            .addOnFailureListener {
                println("❌ Failed to add box: ${it.message}")
            }
    }
}
