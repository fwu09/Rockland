package com.example.rockland.data.repository

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class OpenAiScanRepository {

    // Must match your deployed region from firebase functions:list
    private val functions = Firebase.functions("us-central1")

    suspend fun testCloudFunction(): String {
        val result = functions
            .getHttpsCallable("helloRockland")
            .call()
            .await()

        val map = (result.getData() as? Map<*, *>)
            ?: throw IllegalStateException("Invalid response from scanRockImage")

        return map["message"]?.toString() ?: "No message"
    }

    /**
     * Calls scanRockImage cloud function.
     * Expects cloud function to return: { rockName: string, confidence: number }
     */
    suspend fun scanRockImage(base64: String): Pair<String, Float> {
        val data = hashMapOf("image" to base64)

        val result = functions
            .getHttpsCallable("scanRockImage")
            .call(data)
            .await()

        val map = (result.getData() as? Map<*, *>)
            ?: throw IllegalStateException("Invalid response from scanRockImage")


        val rockName = map["rockName"]?.toString() ?: "Unknown"
        val confidence = (map["confidence"] as? Number)?.toFloat() ?: 0f

        return rockName to confidence
    }
}
