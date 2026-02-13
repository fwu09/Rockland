package com.example.rockland.data.model

data class MissionDefinition(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val target: Int = 0,
    val rewardPoints: Int = 0,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val trigger: String = "",
    val rockId: Int? = null
)

data class MissionProgress(
    val current: Int = 0,
    val completed: Boolean = false,
    val completedAt: Long = 0L,
    val lastUpdated: Long = 0L
)

data class MissionWithProgress(
    val mission: MissionDefinition,
    val current: Int,
    val completed: Boolean,
    val expiryLabel: String,
    val progress: Float
)

data class AchievementDefinition(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val target: Int = 0,
    val rewardPoints: Int = 0,
    val trigger: String = "",
    val rockId: Int? = null,
    val imageFile: String = "",
    val rewardBoxType: String? = null
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val points: Int
)

data class TriggerResult(
    val messages: List<String> = emptyList(),
    val pointsDelta: Int = 0
)
