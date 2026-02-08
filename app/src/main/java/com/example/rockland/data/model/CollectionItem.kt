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
    val imageUrls: List<String> = emptyList(),

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
