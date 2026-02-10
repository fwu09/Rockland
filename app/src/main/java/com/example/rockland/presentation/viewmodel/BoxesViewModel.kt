package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.boxes.BoxOpenResult
import com.example.rockland.data.boxes.BoxRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BoxesUiState(
    val common: Int = 0,
    val rare: Int = 0,
    val special: Int = 0,
    val loading: Boolean = false,
    val lastResult: BoxOpenResult? = null,
    val error: String? = null
)

class BoxesViewModel(
    private val userId: String,
    private val boxRepo: BoxRepository = BoxRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoxesUiState())
    val uiState: StateFlow<BoxesUiState> = _uiState

    init {
        refreshInventory()
    }

    fun refreshInventory() {
        viewModelScope.launch {
            try {
                val snap = firestore.collection("users").document(userId).get().await()
                val inv = snap.get("boxInventory") as? Map<*, *> ?: emptyMap<Any, Any>()
                val common = (inv["common"] as? Long ?: 0L).toInt()
                val rare = (inv["rare"] as? Long ?: 0L).toInt()
                val special = (inv["special"] as? Long ?: 0L).toInt()
                _uiState.value = _uiState.value.copy(
                    common = common, rare = rare, special = special, error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to load inventory")
            }
        }
    }

    fun open(type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val result = boxRepo.openBox(userId, type)
                // after open, refresh inventory to reflect decrement
                refreshInventory()
                _uiState.value = _uiState.value.copy(loading = false, lastResult = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Open failed")
            }
        }
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BoxesViewModel(userId) as T
        }
    }
}
