package com.example.bikerental.models

import com.google.firebase.Timestamp
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * User model representing a user in the application.
 * This model is used for Firestore deserialization and must have default values for all properties.
 * All fields are properly configured for Firestore compatibility.
 */
@IgnoreExtraProperties
data class User(
    @get:DocumentId
    var id: String = "",
    var email: String = "",
    var fullName: String = "",
    var phoneNumber: String = "",
    var createdAt: Long = 0,
    
    // Make this a constructor parameter so data class can generate correct copy method
    @get:PropertyName("isEmailVerified") 
    @set:PropertyName("isEmailVerified")
    var isEmailVerified: Boolean = false,
    
    var profilePictureUrl: String? = null,
    var givenName: String? = null,
    var familyName: String? = null,
    var displayName: String? = null,
    var provider: String? = null,
    var googleId: String? = null,
    var facebookId: String? = null,
    var twitterId: String? = null,
    var lastSignInTime: Long? = null,
    var verificationSentAt: Long? = null,
    var verificationToken: String? = null,
    var hasCompletedAppVerification: Boolean = false,
    var lastUpdated: Timestamp = Timestamp.now(),
    var street: String? = null,
    var barangay: String? = null,
    var city: String? = null,
    var verificationMethod: String? = null,
    
    // Handle both phoneVerified and isPhoneVerified field names for compatibility
    @get:PropertyName("isPhoneVerified")
    @set:PropertyName("isPhoneVerified")
    var isPhoneVerified: Boolean = false,
    
    var authProvider: String? = null
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
            "phoneVerified" to isPhoneVerified,  // Write to both fields for compatibility
            "authProvider" to authProvider
        )
    }
} 