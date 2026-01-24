package com.example.rockland.data.model

// Represents a single comment attached to a rock location.
data class RockComment(
    val id: String,
    val author: String,
    val text: String,
    val timestamp: Long
)

// Represents a user-submitted photo for a location.
data class RockPhoto(
    val id: String,
    val author: String,
    val caption: String,
    val imageUrl: String
)

// Represents an expert annotation for a rock entry.
data class RockAnnotation(
    val id: String,
    val expertName: String,
    val note: String
)

// Aggregates all community contributions for the info card display.
data class RockCommunityContent(
    val comments: List<RockComment> = emptyList(),
    val photos: List<RockPhoto> = emptyList(),
    val annotations: List<RockAnnotation> = emptyList()
)
