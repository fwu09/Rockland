package com.example.rockland.firebase

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// User data model for Firestore
data class UserData(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val joinDate: String = "",
    val checkins: Int = 0,
    val observations: Int = 0,
    val states: Int = 0,
    val countries: Int = 0,
    val experience: Int = 0,
    val achievements: List<String> = listOf(),
    val badges: List<String> = listOf()
)

// firebase user service
class FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Create a new user profile
    suspend fun createUserProfile(user: FirebaseUser, firstName: String, lastName: String) {
        val userData = UserData(
            userId = user.uid,
            firstName = firstName,
            lastName = lastName,
            email = user.email ?: "",
            joinDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US)
                .format(java.util.Date())
        )

        return suspendCoroutine { continuation ->
            usersCollection.document(user.uid)
                .set(userData)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    // Get user profile
    suspend fun getUserProfile(userId: String): UserData {
        return suspendCoroutine { continuation ->
            usersCollection.document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userData = document.toObject(UserData::class.java)
                        continuation.resume(userData ?: UserData())
                    } else {
                        continuation.resume(UserData())
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    //Update user profile
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        return suspendCoroutine { continuation ->
            usersCollection.document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
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
