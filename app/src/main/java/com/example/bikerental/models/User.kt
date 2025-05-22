package com.example.bikerental.models

import com.google.firebase.Timestamp
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * User model representing a user in the application.
 * This model is used for Firestore deserialization and must have default values for all properties.
 */
@IgnoreExtraProperties
data class User(
    @get:DocumentId
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0,
    
    // Make this a constructor parameter so data class can generate correct copy method
    @get:PropertyName("isEmailVerified") 
    @set:PropertyName("isEmailVerified")
    var isEmailVerified: Boolean = false,
    
    val profilePictureUrl: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val displayName: String? = null,
    val provider: String? = null,
    val googleId: String? = null,
    val facebookId: String? = null,
    val twitterId: String? = null,
    val lastSignInTime: Long? = null,
    val verificationSentAt: Long? = null,
    val verificationToken: String? = null,
    val hasCompletedAppVerification: Boolean = false,
    val lastUpdated: Timestamp = Timestamp.now(),
    val street: String? = null,
    val barangay: String? = null,
    val city: String? = null,
    val verificationMethod: String? = null,
    val isPhoneVerified: Boolean = false,
    val authProvider: String? = null
) {
    /**
     * Convert user to a map for Firestore updates
     */
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "createdAt" to createdAt,
            "isEmailVerified" to isEmailVerified,
            "emailVerified" to isEmailVerified,  // Write to both fields for compatibility
            "profilePictureUrl" to profilePictureUrl,
            "givenName" to givenName,
            "familyName" to familyName,
            "displayName" to displayName,
            "provider" to provider,
            "googleId" to googleId,
            "facebookId" to facebookId,
            "twitterId" to twitterId,
            "lastSignInTime" to lastSignInTime,
            "verificationSentAt" to verificationSentAt,
            "verificationToken" to verificationToken,
            "hasCompletedAppVerification" to hasCompletedAppVerification,
            "lastUpdated" to lastUpdated,
            "street" to street,
            "barangay" to barangay,
            "city" to city,
            "verificationMethod" to verificationMethod,
            "isPhoneVerified" to isPhoneVerified,
            "authProvider" to authProvider
        )
    }
} 