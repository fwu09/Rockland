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
import com.example.rockland.data.repository.CollectionRepository
import com.example.rockland.data.repository.AwardsRepository


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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val awardsRepository: AwardsRepository = AwardsRepository()
) : ViewModel() {

    private val collectionRepository = CollectionRepository()
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

                // Only count if it's a NEW rock in the collection
                val rockId = result.awardedRockId // already a String and not null

                // isRockInCollection() checks rockId
                val alreadyOwned = collectionRepository.isRockInCollection(userId, rockId, "")


                if (result.awardedRarity.trim().equals("Ultra Rare", ignoreCase = true)) {
                    awardsRepository.applyTrigger(userId, "collect_ultra_rare")
                }

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
