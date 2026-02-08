package com.example.rockland.data.model

// User help request stored in Firestore collection "help_requests".

data class HelpRequest(
    val id: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val subject: String = "",
    val details: String = "",
    val status: HelpRequestStatus = HelpRequestStatus.PENDING,
    val createdAt: Long = 0L,
    val repliedAt: Long? = null,
    val replyText: String = "",
    val repliedBy: String = ""
)

enum class HelpRequestStatus {
    PENDING,
    REPLIED
}
