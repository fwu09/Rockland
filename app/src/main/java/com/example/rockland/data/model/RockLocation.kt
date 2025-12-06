package com.example.rockland.data.model

/**
 * Rock location domain model used across map and collection features.
 */
data class RockLocation(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = ""
)
