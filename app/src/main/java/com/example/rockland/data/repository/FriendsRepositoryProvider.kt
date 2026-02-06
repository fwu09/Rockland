package com.example.rockland.data.repository

object FriendsRepositoryProvider {
    @Volatile private var instance: FriendsDataRepository? = null

    fun get(): FriendsDataRepository {
        return instance ?: synchronized(this) {
            instance ?: create().also { instance = it }
        }
    }

    private fun create(): FriendsDataRepository {
        return FirestoreFriendsRepository()
    }
}

