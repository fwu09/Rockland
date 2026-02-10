// Domain models for Friends Management and Chat (FEATURE-12.x, FEATURE-13.x).
package com.example.rockland.data.model

/** Summary of a user for search results and contact list. */
data class UserSummary(
    val userId: String,
    val displayName: String,
    val email: String = "",
    val profilePictureUrl: String = ""
)

/** Outgoing or incoming friend request. */
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val fromDisplayName: String = "",
    val toDisplayName: String = "",
    val createdAtMillis: Long = 0L,
    val fromProfilePictureUrl: String = "",
    val toProfilePictureUrl: String = ""
)

/** Friend relation (mutual). */
data class FriendRelation(
    val friendUserId: String,
    val friendDisplayName: String = "",
    val friendEmail: String = "",
    val friendProfilePictureUrl: String = ""
)

/** Single chat message. */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val sentAtMillis: Long,
    val isDeleted: Boolean = false
)

/** Conversation between current user and a friend. */
data class ChatConversation(
    val id: String,
    val otherUserId: String,
    val otherDisplayName: String = "",
    val otherProfilePictureUrl: String = "",
    val lastMessagePreview: String = "",
    val lastMessageAtMillis: Long = 0L,
    val unreadCount: Int = 0
)
