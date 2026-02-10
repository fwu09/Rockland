package com.example.rockland.data.model

enum class ContentStatus {
    PENDING,
    APPROVED,
    REJECTED
}

// Represents a single comment attached to a rock location.
data class LocationComment(
    val commentId: String = "",
    val locationId: String = "",
    val locationName: String = "",
    val userId: String = "",
    val author: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val updatedAt: Long? = null,

    // ve/admin content-moderation
    val status: ContentStatus = ContentStatus.PENDING,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null
)

// Represents a user-submitted photo for a location.
data class LocationPhoto(
    val locationPhotoId: String = "",
    val locationId: String = "",
    val locationName: String = "",
    val commentId: String? = null,
    val userId: String = "",
    val author: String = "Unknown" , //change this value in prod state since there should be no more unknown users
    val caption: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,

    // ve/admin content-moderation
    val status: ContentStatus = ContentStatus.PENDING,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null
)

// Represents an expert annotation for a rock entry.
data class RockAnnotation(
    val id: String,
    val expertId: String = "",
    val expertName: String,
    val note: String,
    val timestamp: Long = 0L,
    val imageUrls: List<String> = emptyList()
)

// Aggregates all community contributions for the info card display.
data class RockCommunityContent(
    val comments: List<LocationComment> = emptyList(),
    val photos: List<LocationPhoto> = emptyList(),
    val annotations: List<RockAnnotation> = emptyList()
)
