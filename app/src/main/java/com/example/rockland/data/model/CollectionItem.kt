package com.example.rockland.data.model

import com.google.firebase.Timestamp

// One item in the user's virtual rock collection.
data class CollectionItem(
    val id: String = "",              // Firestore document ID
    val rockId: String = "",          // ID in rockLocations / rocks
    val rockSource: String = "",      // "map" or "identify"

    val rockName: String = "",
    val thumbnailUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    val customId: String = "",
    val locationLabel: String = "",
    val notes: String = "",
    // User-uploaded images for this collection item (preferred field).
    val userImageUrls: List<String> = emptyList(),
    // Legacy field name kept for backward compatibility with older Firestore documents.
    val imageUrls: List<String> = emptyList(),

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun effectiveUserImageUrls(): List<String> = if (userImageUrls.isNotEmpty()) userImageUrls else imageUrls
}
