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
 * Model for a reply within a support message thread
 */
data class SupportReply(
    @DocumentId val id: String = "",
    val text: String = "",
    val sender: String = "", // Can be 'user' or 'admin'
    val userId: String = "",
    val imageUrl: String? = null, // URL to attached image (if any)
    @ServerTimestamp val createdAt: Timestamp? = null
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
 * Model for Operating Hours
 */
data class OperatingHour(
    val day: String = "",
    val open: String = "09:00",
    val close: String = "18:00",
    val closed: Boolean = false
)

/**
 * Model for Location Details
 */
data class LocationDetails(
    val name: String = "Bambike Ecotours Intramuros",
    val address: String = "Real St. corner General Luna St.\\nIntramuros, Manila 1002\\nPhilippines"
)

/**
 * Converts [Timestamp] to [Date]
 */
fun Timestamp?.toDate(): Date? = this?.toDate() 