// Firebase-backed AuthRepository that wraps FirebaseAuth calls with diagnostics helpers.
package com.example.rockland.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser).isSuccess
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser).isSuccess
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            runAuthCall("signUpWithEmail", email) {
                val r = withTimeout(TIMEOUT_MS) { auth.createUserWithEmailAndPassword(email, password).await() }
                val user = r.user ?: return@runAuthCall AuthResult.Error(AuthError.Unknown("No user returned from FirebaseAuth"))
                AuthResult.Success(user)
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            runAuthCall("signInWithEmail", email) {
                val r = withTimeout(TIMEOUT_MS) { auth.signInWithEmailAndPassword(email, password).await() }
                val user = r.user ?: return@runAuthCall AuthResult.Error(AuthError.Unknown("No user returned from FirebaseAuth"))
                AuthResult.Success(user)
            }
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            runAuthCall("sendPasswordResetEmail", email) {
                withTimeout(TIMEOUT_MS) { auth.sendPasswordResetEmail(email).await() }
                AuthResult.Success(Unit)
            }
        }
    }

    override fun signOut() {
        android.util.Log.d(TAG, "signOut")
        auth.signOut()
    }

    private suspend inline fun <T> runAuthCall(
        op: String,
        email: String,
        crossinline block: suspend () -> AuthResult<T>
    ): AuthResult<T> {
        if (AuthDiagnostics.enabled) {
            android.util.Log.d(TAG, "$op: start email=$email")
        }
        return try {
            val r = block()
            if (AuthDiagnostics.enabled) {
                when (r) {
                    is AuthResult.Success -> android.util.Log.d(TAG, "$op: success email=$email")
                    is AuthResult.Error -> android.util.Log.e(TAG, "$op: error email=$email msg=${r.error.debugMessage}", r.error.throwable)
                }
            }
            r
        } catch (e: Exception) {
            val mapped = mapException(e)
            android.util.Log.e(TAG, "$op: exception email=$email mapped=${mapped.javaClass.simpleName} msg=${mapped.debugMessage}", e)
            AuthResult.Error(mapped)
        }
    }

    private fun mapException(e: Exception): AuthError {
        val msg = e.message ?: ""
        return when (e) {
            is TimeoutCancellationException -> AuthError.Timeout("Timed out waiting for Firebase response.", e)
            is FirebaseNetworkException -> AuthError.Network("Firebase network error: $msg", e)
            is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials("Invalid credentials: $msg", e)
            is FirebaseAuthInvalidUserException -> AuthError.UserNotFound("Invalid user: $msg", e)
            is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse("User collision: $msg", e)
            is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword("Weak password: $msg", e)
            is FirebaseAuthException -> {
                if (msg.contains("App Check", ignoreCase = true) ||
                    msg.contains("appcheck", ignoreCase = true) ||
                    msg.contains("attestation", ignoreCase = true) ||
                    msg.contains("integrity", ignoreCase = true)
                ) {
                    AuthError.AppCheck("Firebase App Check blocked the request: $msg", e)
                } else {
                    AuthError.Unknown("FirebaseAuthException code=${e.errorCode} msg=$msg", e)
                }
            }
            else -> {
                if (msg.contains("App Check", ignoreCase = true) ||
                    msg.contains("No AppCheckProvider installed", ignoreCase = true)
                ) {
                    AuthError.AppCheck("No AppCheckProvider installed (or not initialized): $msg", e)
                } else {
                    AuthError.Unknown("Unknown auth error type=${e::class.java.simpleName} msg=$msg", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthRepo"
        private const val TIMEOUT_MS = 60_000L

        @Volatile private var instance: FirebaseAuthRepository? = null

        fun getInstance(): FirebaseAuthRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthRepository().also { instance = it }
            }
        }
    }
}


