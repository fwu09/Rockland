package com.example.rockland.data.repository

import com.example.rockland.data.model.AchievementDefinition
import com.example.rockland.data.model.LeaderboardEntry
import com.example.rockland.data.model.MissionDefinition
import com.example.rockland.data.model.MissionProgress
import com.example.rockland.data.model.TriggerResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import com.google.firebase.storage.FirebaseStorage

class AwardsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val missionsRef = firestore.collection("missions")
    private val achievementsRef = firestore.collection("achievements")
    private val usersRef = firestore.collection("users")
    private val leaderboardsRef = firestore.collection("leaderboards")

    // award friend count
    private val friendshipsRef = firestore.collection("friendships")

    suspend fun fetchFriendCount(userId: String): Int {
        if (userId.isBlank()) return 0
        val snap = friendshipsRef
            .whereArrayContains("users", userId)
            .get()
            .await()
        return snap.size()
    }


    private val storage = FirebaseStorage.getInstance()
    private val urlCache = mutableMapOf<String, String>()

    // achievement badges image url
    private suspend fun resolveAchievementImageUrl(imageFile: String): String {
        val s = imageFile.trim().trim('"')
        if (s.isBlank()) return ""
        if (s.startsWith("http")) return s

        urlCache[s]?.let { return it }

        val resolved = try {
            when {
                s.startsWith("gs://") -> storage.getReferenceFromUrl(s).downloadUrl.await().toString()
                else -> storage.reference.child(s).downloadUrl.await().toString()
            }
        } catch (_: Exception) {
            ""
        }

        if (resolved.isNotBlank()) urlCache[s] = resolved
        return resolved
    }

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
                trigger = doc.getString("trigger") ?: "",
                rockId = (doc.getLong("rockId") ?: 0L).takeIf { it > 0L }?.toInt()
            )
        }
    }

    suspend fun fetchAchievements(): List<AchievementDefinition> {
        val snapshot = achievementsRef.get().await()
        return snapshot.documents.map { doc ->
            val image =
                ((doc.get("imageFile") ?: doc.get("imagefile")) as? String).orEmpty()

            val achievement = AchievementDefinition(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                target = (doc.getLong("target") ?: 0L).toInt(),
                rewardPoints = (doc.getLong("rewardPoints") ?: 0L).toInt(),
                trigger = doc.getString("trigger") ?: "",
                rockId = (doc.getLong("rockId") ?: 0L).takeIf { it > 0L }?.toInt(),
                imageFile = resolveAchievementImageUrl(image),
                rewardBoxType = doc.getString("rewardBoxType")
            )

            // Debug: verify Firestore imageFile mapping
            // Log.d("AwardsRepo", "achievement=${achievement.id}, imageFile=$image")

            achievement
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
                trigger = doc.getString("trigger") ?: "",
                rockId = (doc.getLong("rockId") ?: 0L).takeIf { it > 0L }?.toInt()
            )
        }

        val achievements = achievementsSnapshot.documents.map { doc ->
            val image = ((doc.get("imageFile") ?: doc.get("imagefile")) as? String).orEmpty()
            AchievementDefinition(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                target = (doc.getLong("target") ?: 0L).toInt(),
                rewardPoints = (doc.getLong("rewardPoints") ?: 0L).toInt(),
                trigger = doc.getString("trigger") ?: "",
                rockId = (doc.getLong("rockId") ?: 0L).takeIf { it > 0L }?.toInt(),
                imageFile = resolveAchievementImageUrl(image),
                rewardBoxType = doc.getString("rewardBoxType")?.trim()
            )
        }

        val userDoc = usersRef.document(userId).get().await()
        val existingAchievements =
            (userDoc.get("achievements") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val points = (userDoc.getLong("points") ?: 0L).toInt()
        val monthlyPoints = (userDoc.getLong("monthlyPoints") ?: 0L).toInt()
        val missionsCompleted = (userDoc.getLong("missionsCompleted") ?: 0L).toInt()
        val achievementsCompleted = (userDoc.getLong("achievementsCompleted") ?: 0L).toInt()
        val firstName = userDoc.getString("firstName") ?: ""
        val lastName = userDoc.getString("lastName") ?: ""
        val displayName = "$firstName $lastName".trim().ifBlank { "User" }
        val role = userDoc.getString("role")?.trim()?.lowercase().orEmpty()
        val isAdminLike = role == "admin" || role == "user_admin"

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

        // Box increments to apply once at the end
        val boxIncrements = mutableMapOf<String, Long>() // common/rare/special -> count

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

                // Give a box based on mission type
                val boxType = when (mission.type.lowercase()) {
                    "daily" -> "common"
                    "weekly" -> "rare"
                    "monthly" -> "special"
                    else -> "common"
                }
                boxIncrements[boxType] = (boxIncrements[boxType] ?: 0L) + 1L

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

                // award box if defined
                achievement.rewardBoxType?.trim()?.lowercase()?.let { boxType ->
                    boxIncrements[boxType] = (boxIncrements[boxType] ?: 0L) + 1L
                }
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
            "badges" to updatedAchievements.toList(),
            "triggerCounts" to updatedCounts
        )

        // user base updates
        batch.set(usersRef.document(userId), userUpdates, SetOptions.merge())

        // Apply box inventory increments
        val userRef = usersRef.document(userId)

        boxIncrements.forEach { (boxType, amount) ->
            batch.update(userRef, "boxInventory.$boxType", FieldValue.increment(amount))
        }


        if (!isAdminLike) {
            val monthId = currentMonthId()
            val entryRef = leaderboardsRef.document(monthId).collection("entries").document(userId)
            batch.set(
                entryRef,
                mapOf("name" to displayName, "points" to (monthlyPoints + pointsDelta)),
                SetOptions.merge()
            )
        }


        Log.d("TRIGGER_DEBUG", "boxIncrements=$boxIncrements pointsDelta=$pointsDelta messages=$messages")
        batch.commit().await()
        Log.d("TRIGGER_DEBUG", "applyTrigger called: $trigger")
        return TriggerResult(messages = messages, pointsDelta = pointsDelta)
    }


    suspend fun upsertMission(definition: MissionDefinition) {
        val data = mutableMapOf<String, Any>(
            "title" to definition.title,
            "description" to definition.description,
            "type" to definition.type,
            "target" to definition.target,
            "rewardPoints" to definition.rewardPoints,
            "startAt" to definition.startAt,
            "endAt" to definition.endAt,
            "trigger" to definition.trigger
        )
        definition.rockId?.let { rockId ->
            data["rockId"] = rockId
        }

        if (definition.id.isBlank()) {
            missionsRef.add(data).await()
        } else {
            missionsRef.document(definition.id).set(data, SetOptions.merge()).await()
        }
    }

    suspend fun deleteMission(missionId: String) {
        if (missionId.isBlank()) return
        missionsRef.document(missionId).delete().await()
    }

    suspend fun upsertAchievement(definition: AchievementDefinition) {
        val data = mutableMapOf<String, Any>(
            "title" to definition.title,
            "description" to definition.description,
            "target" to definition.target,
            "rewardPoints" to definition.rewardPoints,
            "trigger" to definition.trigger
        )
        definition.rockId?.let { rockId ->
            data["rockId"] = rockId
        }

        if (definition.id.isBlank()) {
            achievementsRef.add(data).await()
        } else {
            achievementsRef.document(definition.id).set(data, SetOptions.merge()).await()
        }
    }

    suspend fun deleteAchievement(achievementId: String) {
        if (achievementId.isBlank()) return
        achievementsRef.document(achievementId).delete().await()
    }

    private fun currentMonthId(): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Date())
    }

    suspend fun fetchUserTriggerCounts(userId: String): Map<String, Int> {
        if (userId.isBlank()) return emptyMap()

        val snap = usersRef.document(userId).get().await()
        val raw = snap.get("triggerCounts") as? Map<*, *> ?: return emptyMap()

        return raw.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null

            val value = when (v) {
                is Long -> v.toInt()
                is Int -> v
                is Double -> v.toInt()
                is Float -> v.toInt()
                else -> 0
            }

            key to value
        }.toMap()
    }

    suspend fun fetchUserPoints(userId: String): Int {
        if (userId.isBlank()) return 0
        val snap = firestore.collection("users").document(userId).get().await()
        return (snap.getLong("points") ?: 0L).toInt()
    }

    // to show monthly score instead
    suspend fun fetchUserMonthlyPoints(userId: String): Int {
        if (userId.isBlank()) return 0
        val snap = firestore.collection("users").document(userId).get().await()
        return (snap.getLong("monthlyPoints") ?: 0L).toInt()
    }



}

