package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.model.ChatConversation
import com.example.rockland.data.model.ChatMessage
import com.example.rockland.data.model.FriendRelation
import com.example.rockland.data.repository.FriendsDataRepository
import com.example.rockland.data.repository.FriendsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatInboxUiState(
    val currentUserId: String = "",
    val conversations: List<ChatConversation> = emptyList(),
    val activeConversationId: String? = null,
    val activeOtherDisplayName: String = "",
    val messages: List<ChatMessage> = emptyList()
)

class ChatViewModel(
    private val repository: FriendsDataRepository = FriendsRepositoryProvider.get()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatInboxUiState())
    val uiState: StateFlow<ChatInboxUiState> = _uiState.asStateFlow()

    fun bindUser(userId: String?) {
        val uid = userId.orEmpty()
        _uiState.update { it.copy(currentUserId = uid) }
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.getConversationsFlow(uid).collect { list ->
                _uiState.update { it.copy(conversations = list) }
            }
        }
    }

    fun startChat(friend: FriendRelation) {
        val uid = _uiState.value.currentUserId
        if (uid.isBlank()) return
        viewModelScope.launch {
            val convId = repository.startConversation(uid, friend.friendUserId)
            openConversation(convId, friend.friendDisplayName)
        }
    }

    fun openFromConversation(conversation: ChatConversation) {
        openConversation(conversation.id, conversation.otherDisplayName)
    }

    private fun openConversation(conversationId: String, otherDisplayName: String) {
        val uid = _uiState.value.currentUserId
        val conv = _uiState.value.conversations.find { it.id == conversationId }
        val lastSeen = conv?.lastMessageAtMillis ?: System.currentTimeMillis()
        val cleared = _uiState.value.conversations.map { existing ->
            if (existing.id == conversationId) existing.copy(unreadCount = 0) else existing
        }
        _uiState.update {
            it.copy(
                activeConversationId = conversationId,
                activeOtherDisplayName = otherDisplayName,
                conversations = cleared
            )
        }
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                repository.markConversationRead(uid, conversationId, lastSeen)
            }
        }
        viewModelScope.launch {
            repository.getMessagesFlow(conversationId).collect { list ->
                _uiState.update { it.copy(messages = list) }
            }
        }
    }

    fun closeConversation() {
        _uiState.update { it.copy(activeConversationId = null, activeOtherDisplayName = "", messages = emptyList()) }
    }

    fun sendMessage(text: String) {
        val uid = _uiState.value.currentUserId
        val convId = _uiState.value.activeConversationId ?: return
        if (uid.isBlank() || text.isBlank()) return
        viewModelScope.launch { repository.sendMessage(convId, uid, text) }
    }

    fun deleteMessage(messageId: String) {
        val convId = _uiState.value.activeConversationId ?: return
        viewModelScope.launch { repository.deleteMessage(convId, messageId) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

