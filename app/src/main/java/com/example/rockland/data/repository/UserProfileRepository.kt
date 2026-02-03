package com.example.rockland.data.repository

import com.example.rockland.data.datasource.remote.FirebaseUserService
import com.example.rockland.data.datasource.remote.UserData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
    private val userService: FirebaseUserService = FirebaseUserService.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val awardsRepository: AwardsRepository = AwardsRepository(),
    private val rockRepository: RockRepository = RockRepository(),
    private val collectionRepository: CollectionRepository = CollectionRepository()
) {

    suspend fun createUserProfile(user: FirebaseUser, firstName: String, lastName: String) {
        userService.createUserProfile(user, firstName, lastName)
    }

    suspend fun getUserProfile(userId: String): UserData {
        return userService.getUserProfile(userId)
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        userService.updateUserProfile(userId, updates)
    }

    suspend fun ensureAdminData(userId: String) {
        val userDoc = firestore.collection("users").document(userId).get().await()
        val role = userDoc.getString("role")?.trim()?.lowercase().orEmpty()
        if (role != "admin" && role != "user_admin") return
        if ((userDoc.getLong("adminSeededAt") ?: 0L) > 0L) return

        val missions = awardsRepository.fetchMissions()
        val achievements = awardsRepository.fetchAchievements()
        val totalPoints = missions.sumOf { it.rewardPoints } + achievements.sumOf { it.rewardPoints }
        val now = System.currentTimeMillis()

        val updates = mapOf(
            "points" to totalPoints,
            "monthlyPoints" to totalPoints,
            "missionsCompleted" to missions.size,
            "achievementsCompleted" to achievements.size,
            "achievements" to achievements.map { it.id },
            "adminSeededAt" to now
        )
        firestore.collection("users").document(userId).set(updates, SetOptions.merge()).await()

        val missionsRef = firestore.collection("users").document(userId).collection("missions")
        val batch = firestore.batch()
        missions.forEach { mission ->
            val progress = mapOf(
                "current" to mission.target,
                "completed" to true,
                "completedAt" to now,
                "lastUpdated" to now
            )
            batch.set(missionsRef.document(mission.id), progress, SetOptions.merge())
        }
        batch.commit().await()

        val rocks = rockRepository.getAllRocks()
        for (rock in rocks) {
            val rockId = rock.rockID.toString()
            val exists = collectionRepository.isRockInCollection(userId, rockId, rock.rockName)
            if (!exists) {
                collectionRepository.addRockToCollection(
                    userId = userId,
                    rockId = rockId,
                    rockSource = "admin_seed",
                    rockName = rock.rockName,
                    thumbnailUrl = rock.rockImageUrl
                )
            }
        }
    }

    suspend fun submitExpertApplication(
        userId: String,
        fullName: String,
        expertise: String,
        yearsOfExperience: String,
        portfolioLink: String,
        notes: String
    ) {
        userService.submitExpertApplication(
            userId = userId,
            fullName = fullName,
            expertise = expertise,
            yearsOfExperience = yearsOfExperience,
            portfolioLink = portfolioLink,
            notes = notes
        )
    }
}
