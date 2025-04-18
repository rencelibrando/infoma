package com.example.bikerental.models

import com.google.firebase.firestore.Exclude

data class Review(
    val id: String = "",
    val bikeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = 0
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "bikeId" to bikeId,
            "userId" to userId,
            "userName" to userName,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to timestamp
        )
    }
} 