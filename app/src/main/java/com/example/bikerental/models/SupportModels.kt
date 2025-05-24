package com.example.bikerental.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Model for a support message sent by a user
 */
data class SupportMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val subject: String = "",
    val message: String = "",
    @ServerTimestamp val dateCreated: Timestamp? = null,
    val status: String = "new", // new, in-progress, resolved
    val response: String? = null,
    val respondedAt: Timestamp? = null
)

/**
 * Model for FAQ items
 */
data class FAQ(
    @DocumentId val id: String = "",
    val question: String = "",
    val answer: String = "",
    val order: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Converts [Timestamp] to [Date]
 */
fun Timestamp?.toDate(): Date? = this?.toDate() 