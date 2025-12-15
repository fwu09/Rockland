package com.example.rockland.data.auth

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<FirebaseUser?>

    suspend fun signUpWithEmail(email: String, password: String): AuthResult<FirebaseUser>

    suspend fun signInWithEmail(email: String, password: String): AuthResult<FirebaseUser>

    suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit>

    fun signOut()
}


