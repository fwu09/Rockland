package com.example.rockland.viewmodel
// connecting the UI to firebase services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.datasource.remote.FirebaseAuthService
import com.example.rockland.data.datasource.remote.FirebaseUserService
import com.example.rockland.data.datasource.remote.UserData
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
                    android.util.Log.d(
                        "UserViewModel",
                        "Auth state changed: User logged in (${user.uid})"
                    )
                    // Only load user data if we don't have it already
                    if (_userData.value == null || _userData.value?.userId != user.uid) {
                        try {
                            val userData = userService.getUserProfile(user.uid)
                            _userData.value = userData
                            android.util.Log.d(
                                "UserViewModel",
                                "User data loaded in init: $userData"
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "UserViewModel",
                                "Error loading user data in init",
                                e
                            )
                        }
                    }
                } else {
                    android.util.Log.d("UserViewModel", "Auth state changed: User logged out")
                    _userData.value = null
                }
            }
        }
    }

    // TODO: CRITICAL - Firebase authentication currently failing, needs App Check configuration
    // Register a new user - Firebase will automatically log in the user
    fun registerUser(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                android.util.Log.d("UserViewModel", "Starting registration for: $email")

                // Create Firebase Auth user
                val user = authService.registerUser(email, password)

                if (user != null) {
                    android.util.Log.d("UserViewModel", "Auth user created, uid: ${user.uid}")

                    // Create Firestore user profile
                    userService.createUserProfile(user, firstName, lastName)
                    android.util.Log.d("UserViewModel", "Firestore profile created")

                    // The auth state listener in init{} will automatically load user data
                    _successMessage.value = "Registration successful!"
                    android.util.Log.d("UserViewModel", "Registration completed successfully")
                } else {
                    _error.value = "Registration failed: User creation returned null"
                    android.util.Log.e("UserViewModel", "Registration failed: user is null")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
                android.util.Log.e("UserViewModel", "Registration error: ${e.message}", e)
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

                android.util.Log.d("UserViewModel", "Attempting login for: $email")
                val user = authService.loginUser(email, password)

                if (user != null) {
                    android.util.Log.d("UserViewModel", "Login successful, uid: ${user.uid}")
                    // The auth state listener in init{} will automatically load user data
                } else {
                    _error.value = "Login failed: User returned null"
                    android.util.Log.e("UserViewModel", "Login failed: User returned null")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
                android.util.Log.e("UserViewModel", "Login error: ${e.message}", e)
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

