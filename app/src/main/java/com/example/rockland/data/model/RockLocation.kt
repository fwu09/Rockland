package com.example.rockland.data.model

// Domain model for locations shown on the map and dictionary screens.
data class RockLocation(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = ""
)
