package com.example.rockland.presentation.viewmodel
// connecting the UI to firebase services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rockland.data.auth.AuthRepository
import com.example.rockland.data.auth.AuthResult
import com.example.rockland.data.auth.FirebaseAuthRepository
import com.example.rockland.data.auth.toUiMessage
import com.example.rockland.data.datasource.remote.FirebaseUserService
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.presentation.model.UiBanner
import com.example.rockland.presentation.model.UiBannerType
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

// User view model

class UserViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository.getInstance(),
    private val userService: FirebaseUserService = FirebaseUserService.getInstance()
) : ViewModel() {

    // Banner messages (success/error/info).
    private val _banners = MutableSharedFlow<UiBanner>(extraBufferCapacity = 1)
    val banners = _banners.asSharedFlow()

    // User data state
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Auth state
    val currentUser: StateFlow<FirebaseUser?> =
        authRepository.authState.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoggedIn: StateFlow<Boolean> =
        currentUser.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Load user data when logged in
        viewModelScope.launch {
            currentUser.collect { user ->
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

    // Register a new user - Firebase will automatically log in the user
    fun registerUser(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                android.util.Log.d("UserViewModel", "Starting registration for: $email")

                // Create Firebase Auth user
                when (val auth = authRepository.signUpWithEmail(email, password)) {
                    is AuthResult.Success -> {
                        val user = auth.value
                        android.util.Log.d("UserViewModel", "Auth user created, uid: ${user.uid}")

                        // Create Firestore user profile
                        userService.createUserProfile(user, firstName, lastName)
                        android.util.Log.d("UserViewModel", "Firestore profile created")

                        // The auth state listener in init{} will automatically load user data
                        showSuccess("Registration successful!")
                        android.util.Log.d("UserViewModel", "Registration completed successfully")
                    }
                    is AuthResult.Error -> {
                        showError(auth.error.toUiMessage())
                        android.util.Log.e("UserViewModel", "Registration failed: ${auth.error.debugMessage}", auth.error.throwable)
                    }
                }
            } catch (e: Exception) {
                showError("Registration failed.")
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

                android.util.Log.d("UserViewModel", "Attempting login for: $email")
                when (val auth = authRepository.signInWithEmail(email, password)) {
                    is AuthResult.Success -> {
                        android.util.Log.d("UserViewModel", "Login successful, uid: ${auth.value.uid}")
                        showSuccess("Signed in.")
                        // The auth state listener in init{} will automatically load user data
                    }
                    is AuthResult.Error -> {
                        showError(auth.error.toUiMessage())
                        android.util.Log.e("UserViewModel", "Login failed: ${auth.error.debugMessage}", auth.error.throwable)
                    }
                }
            } catch (e: Exception) {
                showError("Login failed.")
                android.util.Log.e("UserViewModel", "Login error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Logout user
    fun logout() {
        authRepository.signOut()
        showInfo("Signed out.")
    }

    // Load user data
    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val data = userService.getUserProfile(userId)
                _userData.value = data
            } catch (e: Exception) {
                showError(e.message ?: "Failed to load user data.")
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

                val updates = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName
                )

                userService.updateUserProfile(userId, updates)
                loadUserData(userId)
                showSuccess("Profile updated.")
            } catch (e: Exception) {
                showError(e.message ?: "Failed to update profile.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showError(message: String) {
        _banners.tryEmit(UiBanner(text = message, type = UiBannerType.Error))
    }

    fun showSuccess(message: String) {
        _banners.tryEmit(UiBanner(text = message, type = UiBannerType.Success))
    }

    fun showInfo(message: String) {
        _banners.tryEmit(UiBanner(text = message, type = UiBannerType.Info))
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

