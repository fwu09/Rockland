package com.example.rockland.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.model.AchievementDefinition
import com.example.rockland.data.model.LeaderboardEntry
import com.example.rockland.data.model.MissionDefinition
import com.example.rockland.data.model.MissionProgress
import com.example.rockland.data.model.MissionWithProgress
import com.example.rockland.data.repository.AwardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AwardsUiState(
    val isLoading: Boolean = true,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val missions: List<MissionWithProgress> = emptyList(),
    val completedMissions: List<MissionWithProgress> = emptyList(),
    val completedAchievements: List<AchievementDefinition> = emptyList(),
    val allAchievements: List<AchievementDefinition> = emptyList(),
    val missionsSummary: String = "0/0",
    val achievementsSummary: String = "0/0",
    val achievementProgress: Map<String, Int> = emptyMap(),
    val currentPoints: Int = 0
)

class AwardsViewModel(
    private val userId: String?,
    private val repository: AwardsRepository = AwardsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AwardsUiState())
    val uiState: StateFlow<AwardsUiState> = _uiState.asStateFlow()

    init {
        loadAwards()
    }

    fun loadAwards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val missions = repository.fetchMissions()
                val achievements = repository.fetchAchievements()
                val leaderboard = repository.fetchLeaderboardEntries(currentMonthId())

                val currentUserPoints = if (!userId.isNullOrBlank()) {
                    repository.fetchUserPoints(userId)   // or fetchUserMonthlyPoints(userId)
                } else 0

                var userMissionProgress = if (!userId.isNullOrBlank()) {
                    repository.fetchUserMissionProgress(userId)
                } else {
                    emptyMap()
                }

                val userAchievementIds = if (!userId.isNullOrBlank()) {
                    repository.fetchUserAchievementIds(userId)
                } else {
                    emptyList()
                }

                // get trigger counts (suspend)  the coroutine
                var userTriggerCounts: Map<String, Int> = if (!userId.isNullOrBlank()) {
                    repository.fetchUserTriggerCounts(userId)
                } else {
                    emptyMap()
                }

                // sync friend_count using real friendships count
                if (!userId.isNullOrBlank()) {
                    runCatching {
                        val actualFriends = repository.fetchFriendCount(userId)
                        val storedFriends = userTriggerCounts["friend_count"] ?: 0
                        val nettFriends = actualFriends - storedFriends

                        if (nettFriends > 0) {
                            // refresh triggerCounts after applying
                            repository.applyTrigger(userId, "friend_count", nettFriends)

                            // refresh triggerCounts after applying so missionUI updates
                            userTriggerCounts = repository.fetchUserTriggerCounts(userId)
                            userMissionProgress = repository.fetchUserMissionProgress(userId) // refresh for UI
                        }
                    }.onFailure {
                        android.util.Log.e("AwardsViewModel", "friend_count sync failed", it)
                    }
                }

                // build achievementId -> current
                val achievementProgressMap: Map<String, Int> = achievements.associate { a ->
                    val target = a.target.coerceAtLeast(1)
                    val currentRaw = userTriggerCounts[a.trigger] ?: 0
                    a.id to currentRaw.coerceIn(0, target)
                }

                val missionUi = missions.map { mission ->
                    val progress = userMissionProgress[mission.id] ?: MissionProgress()
                    val current = progress.current
                    val completed = progress.completed || (mission.target > 0 && current >= mission.target)
                    val progressValue = if (mission.target > 0) {
                        current.toFloat() / mission.target.toFloat()
                    } else {
                        0f
                    }
                    MissionWithProgress(
                        mission = mission,
                        current = current,
                        completed = completed,
                        expiryLabel = formatExpiry(mission),
                        progress = progressValue.coerceIn(0f, 1f)
                    )
                }

                val completedMissions = missionUi.filter { it.completed }
                val completedAchievements = achievements.filter { userAchievementIds.contains(it.id) }

                _uiState.value = AwardsUiState(
                    isLoading = false,
                    leaderboard = leaderboard,
                    currentPoints = currentUserPoints,
                    missions = missionUi,
                    completedMissions = completedMissions,
                    completedAchievements = completedAchievements,
                    allAchievements = achievements,
                    missionsSummary = "${completedMissions.size}/${missions.size}",
                    achievementsSummary = "${completedAchievements.size}/${achievements.size}",
                    achievementProgress = achievementProgressMap
                )
            } catch (e: Throwable) {
                android.util.Log.e("AwardsViewModel", "loadAwards failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }


    fun deleteMission(missionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMission(missionId)
                loadAwards()
            } catch (_: Throwable) {
            }
        }
    }

    fun upsertMission(definition: MissionDefinition) {
        viewModelScope.launch {
            try {
                repository.upsertMission(definition)
                loadAwards()
            } catch (_: Throwable) { }
        }
    }


    fun upsertAchievement(definition: AchievementDefinition) {
        viewModelScope.launch {
            try {
                repository.upsertAchievement(definition)
                loadAwards()
            } catch (_: Throwable) {
            }
        }
    }

    fun deleteAchievement(achievementId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAchievement(achievementId)
                loadAwards()
            } catch (_: Throwable) {
            }
        }
    }

    private fun currentMonthId(): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Date())
    }

    private fun formatExpiry(mission: MissionDefinition): String {
        if (mission.endAt <= 0L) return "NONE"
        val formatter = SimpleDateFormat("MMM dd", Locale.US)
        return formatter.format(Date(mission.endAt))
    }

    class Factory(private val userId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AwardsViewModel(userId = userId) as T
        }
    }

    //award trigger
    fun applyTrigger(trigger: String, incrementBy: Int = 1, onDone: (() -> Unit)? = null) {
        val uid = userId ?: return
        if (trigger.isBlank()) return

        viewModelScope.launch {
            try {
                repository.applyTrigger(uid, trigger, incrementBy)
                loadAwards()      // refresh UI (completedAchievements etc.)
                onDone?.invoke()
            } catch (e: Throwable) {
                android.util.Log.e("AwardsViewModel", "applyTrigger failed: $trigger", e)
            }
        }
    }
}
