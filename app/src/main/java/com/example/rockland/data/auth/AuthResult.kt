package com.example.rockland.data.auth

sealed class AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>()
    data class Error(val error: AuthError) : AuthResult<Nothing>()
}


