package com.example.rockland.viewmodel
// connecting the UI to firebase services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.firebase.FirebaseAuthService
import com.example.rockland.firebase.FirebaseUserService
import com.example.rockland.firebase.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// User view model

class UserViewModel(
    private val authService: FirebaseAuthService = FirebaseAuthService.getInstance(),
    private val userService: FirebaseUserService = FirebaseUserService.getInstance()
) : ViewModel() {

    // User data state
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Success notification state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Auth state
    val isLoggedIn = authService.isLoggedIn
    val currentUser = authService.currentUser

    init {
        // Load user data when logged in
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                if (user != null) {
                    loadUserData(user.uid)
                } else {
                    _userData.value = null
                }
            }
        }
    }

    // Register a new user
    fun registerUser(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val user = authService.registerUser(email, password)
                if (user != null) {
                    userService.createUserProfile(user, firstName, lastName)
                    loadUserData(user.uid)
                    _successMessage.value = "Registration successful! Welcome to Rockland."
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Login user
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                authService.loginUser(email, password)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Logout user
    fun logout() {
        authService.logout()
    }

    // Load user data
    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val data = userService.getUserProfile(userId)
                _userData.value = data
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update user profile
    fun updateUserProfile(firstName: String, lastName: String) {
        val userId = currentUser.value?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val updates = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName
                )

                userService.updateUserProfile(userId, updates)
                loadUserData(userId)
                _successMessage.value = "Profile updated successfully."
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }

    // Clear success message
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                return UserViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

