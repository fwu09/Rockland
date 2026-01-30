// Firestore-based user profile model and service used by UserViewModel.
package com.example.rockland.data.datasource.remote

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// Expert application model for Firestore
data class ExpertApplication(
    val status: String = "none",
    val submittedAt: String = "",
    val fullName: String = "",
    val expertise: String = "",
    val yearsOfExperience: String = "",
    val portfolioLink: String = "",
    val notes: String = ""
)

// User data model for Firestore
data class UserData(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val joinDate: String = "",
    val role: String = "nature_enthusiast",
    val points: Int = 0,
    val missionsCompleted: Int = 0,
    val achievementsCompleted: Int = 0,
    val monthlyPoints: Int = 0,
    val checkins: Int = 0,
    val observations: Int = 0,
    val states: Int = 0,
    val countries: Int = 0,
    val experience: Int = 0,
    val achievements: List<String> = listOf(),
    val badges: List<String> = listOf(),
    val triggerCounts: Map<String, Int> = mapOf(),
    val expertApplication: ExpertApplication = ExpertApplication()
)

// Firebase user service (Firestore).
class FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Create a new user profile
    suspend fun createUserProfile(user: FirebaseUser, firstName: String, lastName: String) {
        try {
            android.util.Log.d("FirebaseUserService", "Creating user profile for uid: ${user.uid}")
            val userData = UserData(
                userId = user.uid,
                firstName = firstName,
                lastName = lastName,
                email = user.email ?: "",
                joinDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US)
                    .format(java.util.Date())
            )
            usersCollection.document(user.uid).set(userData).await()
            android.util.Log.d(
                "FirebaseUserService",
                "User profile created successfully for uid: ${user.uid}"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "FirebaseUserService",
                "Error creating user profile for uid: ${user.uid}",
                e
            )
            throw e
        }
    }

    // Get user profile
    suspend fun getUserProfile(userId: String): UserData {
        try {
            android.util.Log.d("FirebaseUserService", "Getting user profile for uid: $userId")
            val document = usersCollection.document(userId).get().await()
            return if (document.exists()) {
                val userData = document.toObject(UserData::class.java) ?: UserData()
                android.util.Log.d("FirebaseUserService", "User profile found: $userData")
                userData
            } else {
                android.util.Log.w(
                    "FirebaseUserService",
                    "User profile not found for uid: $userId, returning empty UserData"
                )
                UserData()
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "FirebaseUserService",
                "Error getting user profile for uid: $userId",
                e
            )
            throw e
        }
    }

    // Update user profile
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        usersCollection.document(userId).set(updates, SetOptions.merge()).await()
    }

    companion object {
        private var instance: FirebaseUserService? = null

        fun getInstance(): FirebaseUserService {
            if (instance == null) {
                instance = FirebaseUserService()
            }
            return instance!!
        }
    }
}


