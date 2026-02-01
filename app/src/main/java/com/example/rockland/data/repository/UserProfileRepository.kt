package com.example.rockland.data.repository

import com.example.rockland.data.datasource.remote.FirebaseUserService
import com.example.rockland.data.datasource.remote.UserData
import com.google.firebase.auth.FirebaseUser

class UserProfileRepository(
    private val userService: FirebaseUserService = FirebaseUserService.getInstance()
) {

    suspend fun createUserProfile(user: FirebaseUser, firstName: String, lastName: String) {
        userService.createUserProfile(user, firstName, lastName)
    }

    suspend fun getUserProfile(userId: String): UserData {
        return userService.getUserProfile(userId)
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        userService.updateUserProfile(userId, updates)
    }

    suspend fun submitExpertApplication(
        userId: String,
        fullName: String,
        expertise: String,
        yearsOfExperience: String,
        portfolioLink: String,
        notes: String
    ) {
        userService.submitExpertApplication(
            userId = userId,
            fullName = fullName,
            expertise = expertise,
            yearsOfExperience = yearsOfExperience,
            portfolioLink = portfolioLink,
            notes = notes
        )
    }
}
