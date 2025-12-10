package com.example.rockland.data.datasource.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {
    private val auth = FirebaseAuth.getInstance()

    // Current user state
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Login state
    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isLoggedIn.value = firebaseAuth.currentUser != null
        }
    }

    // Register a new user with email and password
    suspend fun registerUser(email: String, password: String): FirebaseUser? {
        return try {
            android.util.Log.d("FirebaseAuthService", "Starting registration for: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            android.util.Log.d(
                "FirebaseAuthService",
                "Registration successful, uid: ${result.user?.uid}"
            )
            result.user
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthService", "Registration failed for: $email", e)
            throw e
        }
    }

    // Login with email and password
    suspend fun loginUser(email: String, password: String): FirebaseUser? {
        return try {
            android.util.Log.d("FirebaseAuthService", "Starting login for: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            android.util.Log.d("FirebaseAuthService", "Login successful, uid: ${result.user?.uid}")
            result.user
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthService", "Login failed for: $email", e)
            throw e
        }
    }

    // Logout the current user
    fun logout() {
        auth.signOut()
    }

    companion object {
        private var instance: FirebaseAuthService? = null

        fun getInstance(): FirebaseAuthService {
            if (instance == null) {
                instance = FirebaseAuthService()
            }
            return instance!!
        }
    }
}


