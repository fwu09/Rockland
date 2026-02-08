package com.example.rockland.data.repository

import com.example.rockland.data.model.RockLocation
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository responsible for loading rock distribution locations from Firestore.
 */
class RockLocationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun fetchRockLocations(): List<RockLocation> = suspendCoroutine { cont ->
        firestore.collection("rockLocations")
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
}
