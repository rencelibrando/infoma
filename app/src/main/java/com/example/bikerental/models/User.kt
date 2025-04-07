package com.example.bikerental.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * User model representing a user in the application.
 * This model is used for Firestore deserialization and must have default values for all properties.
 */
@IgnoreExtraProperties
data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0,
    val profilePictureUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val givenName: String? = null,
    val familyName: String? = null,
    val displayName: String? = null,
    val lastSignInTime: Long = 0,
    val provider: String = "email", // "email" or "google"
    val googleId: String? = null,
    val isAdmin: Boolean = false
) {
    // No-argument constructor for Firestore deserialization
    constructor() : this(
        id = "",
        email = "",
        fullName = "",
        phoneNumber = "",
        createdAt = 0
    )
    
    /**
     * Convert user to a map for Firestore storage
     */
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "email" to email,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "createdAt" to createdAt,
            "profilePictureUrl" to profilePictureUrl,
            "isEmailVerified" to isEmailVerified,
            "givenName" to givenName,
            "familyName" to familyName,
            "displayName" to displayName,
            "lastSignInTime" to lastSignInTime,
            "provider" to provider,
            "googleId" to googleId,
            "isAdmin" to isAdmin
        )
    }
} 