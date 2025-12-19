package com.example.rockland.data.model

import com.google.firebase.Timestamp

// Data that represents a rock saved inside the user's Firestore collection subcollection.
data class CollectionItem(
    val id: String = "",
    val rockId: String = "",
    val rockSource: String = "",

    val rockName: String = "",
    val thumbnailUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    val customId: String = "",
    val locationLabel: String = "",
    val notes: String = "",
    // Preferred field for user-uploaded photo URLs.
    val userImageUrls: List<String> = emptyList(),
    // Legacy field kept for backward compatibility when migration is pending.
    val imageUrls: List<String> = emptyList(),

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun effectiveUserImageUrls(): List<String> = if (userImageUrls.isNotEmpty()) userImageUrls else imageUrls
}
