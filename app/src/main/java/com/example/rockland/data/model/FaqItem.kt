package com.example.rockland.data.model

// Single entry for Firestore collection "faqs".
 
data class FaqItem(
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    val lastUpdatedAt: Long = 0L,
    val updatedBy: String = ""
)
