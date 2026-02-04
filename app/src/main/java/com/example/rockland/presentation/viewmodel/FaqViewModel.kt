package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.model.FaqItem
import com.example.rockland.data.repository.FaqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaqViewModel(
    private val repository: FaqRepository = FaqRepository()
) : ViewModel() {

    private val _faqs = MutableStateFlow<List<FaqItem>>(emptyList())
    val faqs: StateFlow<List<FaqItem>> = _faqs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var _currentUserId: String? = null

    fun bindUser(userId: String?) {
        _currentUserId = userId
    }

    /**
     * Load FAQs. For public (See FAQ) use activeOnly = true; for admin list use false.
     */
    fun loadFaqs(activeOnly: Boolean = false) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _faqs.value = repository.fetchFaqs(activeOnly = activeOnly)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load FAQs"
                _faqs.value = emptyList()
            }
            _loading.value = false
        }
    }

    fun addFaq(question: String, answer: String, order: Int, isActive: Boolean) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.upsertFaq(
                    id = "",
                    question = question,
                    answer = answer,
                    order = order,
                    isActive = isActive,
                    updatedBy = _currentUserId.orEmpty()
                )
                loadFaqs(activeOnly = false)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add FAQ"
            }
        }
    }

    fun updateFaq(item: FaqItem, question: String, answer: String, order: Int, isActive: Boolean) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.upsertFaq(
                    id = item.id,
                    question = question,
                    answer = answer,
                    order = order,
                    isActive = isActive,
                    updatedBy = _currentUserId.orEmpty()
                )
                loadFaqs(activeOnly = false)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update FAQ"
            }
        }
    }

    fun deleteFaq(item: FaqItem) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.deleteFaq(item.id)
                loadFaqs(activeOnly = false)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete FAQ"
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FaqViewModel::class.java)) {
                return FaqViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
