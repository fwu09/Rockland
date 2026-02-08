package com.example.rockland.data.auth

// Normalized authentication errors.

sealed class AuthError(
    open val debugMessage: String,
    open val throwable: Throwable? = null
) {
    data class Network(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class InvalidCredentials(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class UserNotFound(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class EmailAlreadyInUse(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class WeakPassword(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class AppCheck(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class Timeout(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)

    data class Unknown(
        override val debugMessage: String,
        override val throwable: Throwable? = null
    ) : AuthError(debugMessage, throwable)
}

fun AuthError.toUiMessage(): String {
    return when (this) {
        is AuthError.Network -> "Network error. Please try again."
        is AuthError.InvalidCredentials -> "Invalid email or password."
        is AuthError.UserNotFound -> "Account not found."
        is AuthError.EmailAlreadyInUse -> "This email is already in use."
        is AuthError.WeakPassword -> "Password is too weak."
        is AuthError.AppCheck -> "Blocked by Firebase App Check. Check Logcat for debug token."
        is AuthError.Timeout -> "Request timed out. Please try again."
        is AuthError.Unknown -> "Authentication failed. Please try again."
    }
}


