package com.example.rockland.data.repository

import com.example.rockland.data.model.AchievementDefinition
import com.example.rockland.data.model.LeaderboardEntry
import com.example.rockland.data.model.MissionDefinition
import com.example.rockland.data.model.MissionProgress
import com.example.rockland.data.model.TriggerResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AwardsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val missionsRef = firestore.collection("missions")
    private val achievementsRef = firestore.collection("achievements")
    private val usersRef = firestore.collection("users")
    private val leaderboardsRef = firestore.collection("leaderboards")

    suspend fun fetchMissions(): List<MissionDefinition> {
        val snapshot = missionsRef.get().await()
        return snapshot.documents.map { doc ->
            MissionDefinition(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                type = doc.getString("type") ?: "",
                target = (doc.getLong("target") ?: 0L).toInt(),
                rewardPoints = (doc.getLong("rewardPoints") ?: 0L).toInt(),
                startAt = doc.getLong("startAt") ?: 0L,
                endAt = doc.getLong("endAt") ?: 0L,
                trigger = doc.getString("trigger") ?: ""
            )
        }
    }

    suspend fun fetchAchievements(): List<AchievementDefinition> {
        val snapshot = achievementsRef.get().await()
        return snapshot.documents.map { doc ->
            AchievementDefinition(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                target = (doc.getLong("target") ?: 0L).toInt(),
                rewardPoints = (doc.getLong("rewardPoints") ?: 0L).toInt(),
                trigger = doc.getString("trigger") ?: ""
            )
        }
    }

    suspend fun fetchUserMissionProgress(userId: String): Map<String, MissionProgress> {
        val snapshot = usersRef.document(userId).collection("missions").get().await()
        return snapshot.documents.associate { doc ->
            val progress = MissionProgress(
                current = (doc.getLong("current") ?: 0L).toInt(),
                completed = doc.getBoolean("completed") ?: false,
                completedAt = doc.getLong("completedAt") ?: 0L,
                lastUpdated = doc.getLong("lastUpdated") ?: 0L
            )
            doc.id to progress
        }
    }

    suspend fun fetchUserAchievementIds(userId: String): List<String> {
        val snapshot = usersRef.document(userId).get().await()
        val raw = snapshot.get("achievements")
        return if (raw is List<*>) {
            raw.filterIsInstance<String>()
        } else {
            emptyList()
        }
    }

    suspend fun fetchLeaderboardEntries(monthId: String): List<LeaderboardEntry> {
        val snapshot = leaderboardsRef
            .document(monthId)
            .collection("entries")
            .orderBy("points", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapIndexed { index, doc ->
            LeaderboardEntry(
                rank = index + 1,
                name = doc.getString("name") ?: "User",
                points = (doc.getLong("points") ?: 0L).toInt()
            )
        }
    }

    suspend fun applyTrigger(userId: String, trigger: String, incrementBy: Int = 1): TriggerResult {
        if (trigger.isBlank()) return TriggerResult()
        val missionsSnapshot = missionsRef.whereEqualTo("trigger", trigger).get().await()
        val achievementsSnapshot = achievementsRef.whereEqualTo("trigger", trigger).get().await()

        val missions = missionsSnapshot.documents.map { doc ->
            MissionDefinition(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                type = doc.getString("type") ?: "",
                target = (doc.getLong("target") ?: 0L).toInt(),
                rewardPoints = (doc.getLong("rewardPoints") ?: 0L).toInt(),
                startAt = doc.getLong("startAt") ?: 0L,
                endAt = doc.getLong("endAt") ?: 0L,
                trigger = doc.getString("trigger") ?: ""
            )
        }

        val achievements = achievementsSnapshot.documents.map { doc ->
            AchievementDefinition(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                target = (doc.getLong("target") ?: 0L).toInt(),
                rewardPoints = (doc.getLong("rewardPoints") ?: 0L).toInt(),
                trigger = doc.getString("trigger") ?: ""
            )
        }

        val userDoc = usersRef.document(userId).get().await()
        val existingAchievements = (userDoc.get("achievements") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val points = (userDoc.getLong("points") ?: 0L).toInt()
        val monthlyPoints = (userDoc.getLong("monthlyPoints") ?: 0L).toInt()
        val missionsCompleted = (userDoc.getLong("missionsCompleted") ?: 0L).toInt()
        val achievementsCompleted = (userDoc.getLong("achievementsCompleted") ?: 0L).toInt()
        val firstName = userDoc.getString("firstName") ?: ""
        val lastName = userDoc.getString("lastName") ?: ""
        val displayName = "$firstName $lastName".trim().ifBlank { "User" }

        val currentCounts = (userDoc.get("triggerCounts") as? Map<*, *>)
            ?.mapNotNull { (k, v) -> (k as? String)?.let { it to (v as? Long ?: 0L).toInt() } }
            ?.toMap()
            ?: emptyMap()
        val newCount = (currentCounts[trigger] ?: 0) + incrementBy

        var pointsDelta = 0
        var missionsCompletedDelta = 0
        var achievementsCompletedDelta = 0
        val updatedAchievements = existingAchievements.toMutableSet()
        val messages = mutableListOf<String>()

        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        missions.forEach { mission ->
            val progressRef = usersRef.document(userId).collection("missions").document(mission.id)
            val progressDoc = progressRef.get().await()
            val current = (progressDoc.getLong("current") ?: 0L).toInt()
            val completed = progressDoc.getBoolean("completed") ?: false
            val newCurrent = current + incrementBy
            val isCompletedNow = mission.target > 0 && newCurrent >= mission.target

            val updates = mutableMapOf<String, Any>(
                "current" to newCurrent,
                "lastUpdated" to now
            )
            if (isCompletedNow && !completed) {
                updates["completed"] = true
                updates["completedAt"] = now
                pointsDelta += mission.rewardPoints
                missionsCompletedDelta += 1
                messages.add("Mission completed: ${mission.title} (+${mission.rewardPoints} pts)")
            } else if (!completed) {
                updates["completed"] = false
            }

            batch.set(progressRef, updates, SetOptions.merge())
        }

        achievements.forEach { achievement ->
            if (!updatedAchievements.contains(achievement.id) && newCount >= achievement.target) {
                updatedAchievements.add(achievement.id)
                pointsDelta += achievement.rewardPoints
                achievementsCompletedDelta += 1
                messages.add("Achievement unlocked: ${achievement.title} (+${achievement.rewardPoints} pts)")
            }
        }

        val updatedCounts = currentCounts.toMutableMap()
        updatedCounts[trigger] = newCount

        val userUpdates = mapOf(
            "points" to points + pointsDelta,
            "monthlyPoints" to monthlyPoints + pointsDelta,
            "missionsCompleted" to missionsCompleted + missionsCompletedDelta,
            "achievementsCompleted" to achievementsCompleted + achievementsCompletedDelta,
            "achievements" to updatedAchievements.toList(),
            "triggerCounts" to updatedCounts
        )

        batch.set(usersRef.document(userId), userUpdates, SetOptions.merge())

        val monthId = currentMonthId()
        val entryRef = leaderboardsRef.document(monthId).collection("entries").document(userId)
        batch.set(
            entryRef,
            mapOf("name" to displayName, "points" to (monthlyPoints + pointsDelta)),
            SetOptions.merge()
        )
        batch.commit().await()
        return TriggerResult(messages = messages, pointsDelta = pointsDelta)
    }

    private fun currentMonthId(): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Date())
    }
}
