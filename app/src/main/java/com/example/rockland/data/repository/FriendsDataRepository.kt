package com.example.rockland.data.repository

import com.example.rockland.data.model.ChatConversation
import com.example.rockland.data.model.ChatMessage
import com.example.rockland.data.model.FriendRelation
import com.example.rockland.data.model.FriendRequest
import com.example.rockland.data.model.UserSummary
import kotlinx.coroutines.flow.Flow

interface FriendsDataRepository {
    // Friends
    suspend fun searchUsers(currentUserId: String, query: String): List<UserSummary>
    fun getFriendsFlow(userId: String): Flow<List<FriendRelation>>
    fun getOutgoingRequestsFlow(userId: String): Flow<List<FriendRequest>>
    fun getIncomingRequestsFlow(userId: String): Flow<List<FriendRequest>>
    fun getAcceptFriendRequestsFlow(userId: String): Flow<Boolean>
    suspend fun sendFriendRequest(fromUserId: String, fromDisplayName: String, toUserId: String, toDisplayName: String): Result<Unit>
    suspend fun deleteFriendRequest(requestId: String, currentUserId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String, acceptorUserId: String): Result<Unit>
    suspend fun rejectFriendRequest(requestId: String, rejectorUserId: String): Result<Unit>
    suspend fun deleteFriend(userId: String, friendUserId: String): Result<Unit>
    suspend fun setAcceptFriendRequests(userId: String, accept: Boolean)

    // Chat
    fun conversationId(userId1: String, userId2: String): String
    fun getConversationsFlow(userId: String): Flow<List<ChatConversation>>
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(conversationId: String, senderId: String, text: String): ChatMessage
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit>
    suspend fun startConversation(userId: String, otherUserId: String): String
    suspend fun markConversationRead(userId: String, conversationId: String, lastSeenAtMillis: Long)
}

