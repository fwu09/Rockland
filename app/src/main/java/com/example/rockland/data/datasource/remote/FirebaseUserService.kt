// Firestore-based user profile model and service used by UserViewModel.
package com.example.rockland.data.datasource.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

// expert application status
enum class ApplicationStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED
}

// ne -> ve: verified expert application
data class ExpertApplication(
    val status: String = ApplicationStatus.NONE.name,
    val submittedAt: Timestamp? = null,
    val fullName: String = "",
    val expertise: String = "",
    val yearsOfExperience: String = "",
    val portfolioLink: String = "",
    val notes: String = "",
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String = ""
) {
    @get:Exclude
    val statusEnum: ApplicationStatus
        get() = runCatching { ApplicationStatus.valueOf(status) }
            .getOrDefault(ApplicationStatus.NONE)
}

// User data model for Firestore
data class UserData(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val joinDate: String = "",
    val role: String = "nature_enthusiast",
    val profilePictureUrl: String = "",
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
    val expertApplication: ExpertApplication = ExpertApplication(),
    val boxInventory: Map<String, Long> = emptyMap()
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
                val userData = runCatching { document.toObject(UserData::class.java) }.getOrNull()
                if (userData != null) {
                    android.util.Log.d("FirebaseUserService", "User profile found: $userData")
                    userData
                } else {
                    android.util.Log.w(
                        "FirebaseUserService",
                        "User profile mapping failed for uid: $userId, falling back to safe fields"
                    )
                    val achievements = (document.get("achievements") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()
                    val badges = (document.get("badges") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()
                    val triggerCounts = (document.get("triggerCounts") as? Map<*, *>)
                        ?.mapNotNull { (key, value) ->
                            val k = key as? String ?: return@mapNotNull null
                            val v = (value as? Number)?.toInt() ?: return@mapNotNull null
                            k to v
                        }
                        ?.toMap()
                        ?: emptyMap()
                    UserData(
                        userId = document.getString("userId") ?: document.id,
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        email = document.getString("email") ?: "",
                        joinDate = document.getString("joinDate") ?: "",
                        role = document.getString("role") ?: "nature_enthusiast",
                        profilePictureUrl = document.getString("profilePictureUrl") ?: "",
                        points = (document.getLong("points") ?: 0L).toInt(),
                        missionsCompleted = (document.getLong("missionsCompleted") ?: 0L).toInt(),
                        achievementsCompleted = (document.getLong("achievementsCompleted") ?: 0L).toInt(),
                        monthlyPoints = (document.getLong("monthlyPoints") ?: 0L).toInt(),
                        checkins = (document.getLong("checkins") ?: 0L).toInt(),
                        observations = (document.getLong("observations") ?: 0L).toInt(),
                        states = (document.getLong("states") ?: 0L).toInt(),
                        countries = (document.getLong("countries") ?: 0L).toInt(),
                        experience = (document.getLong("experience") ?: 0L).toInt(),
                        achievements = achievements,
                        badges = badges,
                        triggerCounts = triggerCounts,
                        expertApplication = ExpertApplication()
                    )
                }
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

    // Upload profile picture to Storage (users/{uid}/profile/{fileName}), return download URL
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): String {
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        val ref = FirebaseStorage.getInstance().reference.child("users").child(userId).child("profile").child(fileName)
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
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

    // submit verified expert application
    suspend fun submitExpertApplication(
        userId: String,
        fullName: String,
        expertise: String,
        yearsOfExperience: String,
        portfolioLink: String,
        notes: String
    ) {
        val application = ExpertApplication(
            status = ApplicationStatus.PENDING.name,
            submittedAt = Timestamp.now(),
            fullName = fullName.trim(),
            expertise = expertise.trim(),
            yearsOfExperience = yearsOfExperience.trim(),
            portfolioLink = portfolioLink.trim(),
            notes = notes.trim(),
            reviewedAt = null,
            reviewedBy = ""
        )

        val updates = mapOf("expertApplication" to application)
        usersCollection.document(userId).set(updates, SetOptions.merge()).await()
    }
}


