package com.example.bikerental.utils

import com.example.bikerental.models.Bike
import com.example.bikerental.models.User
import com.google.firebase.Timestamp

/**
 * Utility class for safe Firestore data conversion.
 * Handles type conversions and null values gracefully to prevent
 * CustomClassMapper warnings and deserialization errors.
 */
object FirestoreUtils {
    
    /**
     * Creates a Bike object from Firestore document data with proper type handling
     */
    fun createBikeFromData(documentId: String, data: Map<String, Any>): Bike {
        return Bike(
            id = documentId,
            name = data["name"] as? String ?: "",
            type = data["type"] as? String ?: "",
            price = data["price"] as? String ?: "",
            priceValue = (data["priceValue"] as? Number)?.toDouble() ?: 0.0,
            imageUrl = data["imageUrl"] as? String ?: "",
            imageRes = (data["imageRes"] as? Number)?.toInt() ?: 0,
            imageUrls = data["imageUrls"] as? List<String> ?: emptyList(),
            _latitude = data["latitude"] ?: 0.0,
            _longitude = data["longitude"] ?: 0.0,
            qrCode = data["qrCode"] as? String ?: "",
            hardwareId = data["hardwareId"] as? String ?: "",
            rating = (data["rating"] as? Number)?.toFloat() ?: 0f,
            batteryLevel = (data["batteryLevel"] as? Number)?.toInt() ?: 100,
            isAvailable = data["isAvailable"] as? Boolean ?: true,
            isInUse = data["isInUse"] as? Boolean ?: false,
            isLocked = data["isLocked"] as? Boolean ?: true,
            currentRider = data["currentRider"] as? String ?: "",
            currentRideId = data["currentRideId"] as? String ?: "",
            _lastRideEnd = data["lastRideEnd"],
            _lastUpdated = data["lastUpdated"],
            _updatedAt = data["updatedAt"],
            _lastRideStart = data["lastRideStart"],
            _createdAt = data["createdAt"],
            stationId = data["stationId"] as? String ?: "",
            distanceToUser = "", // This is calculated on client
            description = data["description"] as? String ?: ""
        )
    }
    
    /**
     * Creates a User object from Firestore document data with proper type handling
     */
    fun createUserFromData(documentId: String, data: Map<String, Any>): User {
        return User(
            id = documentId,
            email = data["email"] as? String ?: "",
            fullName = data["fullName"] as? String ?: "",
            phoneNumber = data["phoneNumber"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            isEmailVerified = data["isEmailVerified"] as? Boolean ?: false,
            profilePictureUrl = data["profilePictureUrl"] as? String,
            givenName = data["givenName"] as? String,
            familyName = data["familyName"] as? String,
            displayName = data["displayName"] as? String,
            provider = data["provider"] as? String,
            googleId = data["googleId"] as? String,
            facebookId = data["facebookId"] as? String,
            twitterId = data["twitterId"] as? String,
            lastSignInTime = (data["lastSignInTime"] as? Number)?.toLong(),
            verificationSentAt = (data["verificationSentAt"] as? Number)?.toLong(),
            verificationToken = data["verificationToken"] as? String,
            hasCompletedAppVerification = data["hasCompletedAppVerification"] as? Boolean ?: false,
            lastUpdated = data["lastUpdated"] as? Timestamp ?: Timestamp.now(),
            street = data["street"] as? String,
            barangay = data["barangay"] as? String,
            city = data["city"] as? String,
            verificationMethod = data["verificationMethod"] as? String,
            isPhoneVerified = data["isPhoneVerified"] as? Boolean 
                ?: data["phoneVerified"] as? Boolean ?: false, // Handle both field names
            authProvider = data["authProvider"] as? String
        )
    }
    
    /**
     * Safely converts latitude/longitude from various types to Double
     */
    fun safeCoordinateToDouble(value: Any?): Double {
        return when (value) {
            is String -> {
                try {
                    value.toDouble()
                } catch (e: NumberFormatException) {
                    0.0
                }
            }
            is Double -> value
            is Number -> value.toDouble()
            else -> 0.0
        }
    }
    
    /**
     * Safely converts timestamp data from various types
     */
    fun safeTimestampToLong(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            else -> 0L
        }
    }
} 