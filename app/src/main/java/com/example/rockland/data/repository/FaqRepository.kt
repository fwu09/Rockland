package com.example.rockland.data.repository

import com.example.rockland.data.model.FaqItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FaqRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val faqsRef = firestore.collection("faqs")

    suspend fun fetchFaqs(activeOnly: Boolean = false): List<FaqItem> {
        val query = if (activeOnly) {
            faqsRef.whereEqualTo("isActive", true).orderBy("order", Query.Direction.ASCENDING)
        } else {
            faqsRef.orderBy("order", Query.Direction.ASCENDING)
        }
        val snapshot = query.get().await()
        return snapshot.documents.map { doc ->
            FaqItem(
                id = doc.id,
                question = doc.getString("question") ?: "",
                answer = doc.getString("answer") ?: "",
                order = (doc.getLong("order") ?: 0L).toInt(),
                isActive = doc.getBoolean("isActive") ?: true,
                lastUpdatedAt = doc.getLong("lastUpdatedAt") ?: 0L,
                updatedBy = doc.getString("updatedBy") ?: ""
            )
        }
    }

    // Creates or updates a FAQ. Pass id empty for create; non-empty for edit.
    suspend fun upsertFaq(
        id: String,
        question: String,
        answer: String,
        order: Int,
        isActive: Boolean,
        updatedBy: String
    ) {
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "question" to question,
            "answer" to answer,
            "order" to order,
            "isActive" to isActive,
            "lastUpdatedAt" to now,
            "updatedBy" to updatedBy
        )
        if (id.isBlank()) {
            faqsRef.add(payload).await()
        } else {
            faqsRef.document(id).set(payload).await()
        }
    }

    suspend fun deleteFaq(id: String) {
        if (id.isBlank()) return
        faqsRef.document(id).delete().await()
    }
}
