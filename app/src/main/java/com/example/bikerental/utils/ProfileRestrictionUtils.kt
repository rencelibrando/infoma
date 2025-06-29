package com.example.bikerental.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utility class for managing feature restrictions based on user profile completion
 */
object ProfileRestrictionUtils {
    
    /**
     * Checks if a specific feature is restricted for the current user
     * 
     * @param featureType The type of feature to check: "booking", "payment", "rental", etc.
     * @param userData The user's profile data from Firestore, or null to fetch it
     * @return true if the feature should be restricted, false otherwise
     */
    fun isFeatureRestricted(featureType: String, userData: Map<String, Any>?): Boolean {
        // If no user data is available, restrict by default
        if (userData == null) return true
        
        // Different features may have different requirements
        return when (featureType) {
            // Booking requires email verification, profile completion, and ID verification
            "booking" -> !isEmailVerified(userData) || !isProfileComplete(userData) || !isIdVerified(userData)
            
            // Payment features require email verification, profile completion, and ID verification
            "payment" -> !isEmailVerified(userData) || !isProfileComplete(userData) || !isIdVerified(userData)
            
            // Rental requires email verification and ID verification
            "rental" -> !isEmailVerified(userData) || !isIdVerified(userData)
            
            // Start ride requires ID verification
            "start_ride" -> !isIdVerified(userData)
            
            // Writing reviews requires ID verification
            "write_review" -> !isIdVerified(userData)
            
            // Chat requires phone verification
            "chat" -> !isPhoneVerified(userData)
            
            // Location sharing requires phone verification
            "location_sharing" -> !isPhoneVerified(userData)
            
            // Account settings require email verification
            "account_settings" -> !isEmailVerified(userData)
            
            // Security features require email verification
            "security_features" -> !isEmailVerified(userData)
            
            // Allow basic features even with incomplete profile
            "maps_view" -> false
            "bike_browsing" -> false
            else -> false
        }
    }
    
    /**
     * Gets a descriptive message explaining why a feature is restricted
     */
    fun getRestrictionMessage(featureType: String, userData: Map<String, Any>?): String {
        if (userData == null) return "Please sign in to access this feature"
        
        return when (featureType) {
            "booking" -> {
                if (!isEmailVerified(userData)) 
                    "Your email address needs to be verified before you can book a bike. This helps us ensure the security of our rental services."
                else if (!isProfileComplete(userData)) 
                    "Please complete your profile information to book a bike. We need your details to process your booking."
                else if (!isIdVerified(userData))
                    "Your ID verification is required before you can book a bike. This is for your security and to prevent fraud."
                else "Feature unavailable"
            }
            "payment" -> {
                if (!isEmailVerified(userData)) 
                    "Email verification is required to access payment features. This security measure protects your payment information."
                else if (!isProfileComplete(userData)) 
                    "Your profile information is incomplete. Please provide all required details to use payment features."
                else if (!isIdVerified(userData))
                    "ID verification is required to access payment features. This helps us comply with financial regulations."
                else "Feature unavailable"
            }
            "rental" -> {
                if (!isEmailVerified(userData)) 
                    "For security reasons, you need to verify your email address before renting a bike. This helps us confirm your identity."
                else if (!isIdVerified(userData))
                    "ID verification is required to rent a bike. This is for security and insurance purposes."
                else 
                    "Complete your profile information to rent a bike. We need these details to process your rental agreement."
            }
            "start_ride" -> {
                if (!isIdVerified(userData))
                    "ID verification is required before you can start a ride. This is a security measure to protect our bikes and customers."
                else "Feature unavailable"
            }
            "write_review" -> {
                if (!isIdVerified(userData))
                    "ID verification is required before you can write reviews. This helps ensure authentic and reliable feedback."
                else "Feature unavailable"
            }
            "account_settings" -> 
                "Please verify your email address to access account settings. This ensures only you can make changes to your account."
            "security_features" -> 
                "Email verification is required to access security features. This additional step helps protect your account."
            "chat" -> 
                "Verify your phone number to use our messaging service. This helps us provide a secure communication channel."
            "location_sharing" -> 
                "Phone verification is required for location sharing. This helps ensure your privacy and security when sharing your location."
            else -> "This feature is currently unavailable"
        }
    }
    
    /**
     * Check if the user's profile is complete
     */
    fun isProfileComplete(userData: Map<String, Any>?): Boolean {
        return userData?.let {
            val requiredFields = listOf(
                "fullName",
                "phoneNumber",
                "street",
                "barangay",
                "city"
            )
            requiredFields.all { field ->
                userData[field].toString().isNotBlank()
            }
        } ?: false
    }
    
    /**
     * Check if the user's phone is verified
     */
    fun isPhoneVerified(userData: Map<String, Any>?): Boolean {
        return userData?.get("isPhoneVerified") as? Boolean ?: false
    }
    
    /**
     * Check if the user's email is verified
     */
    fun isEmailVerified(userData: Map<String, Any>?): Boolean {
        return userData?.get("isEmailVerified") as? Boolean ?: false
    }

    /**
     * Check if the user's ID is verified
     */
    fun isIdVerified(userData: Map<String, Any>?): Boolean {
        return userData?.get("isIdVerified") as? Boolean ?: false
    }

    /**
     * Check if the user has submitted ID for verification
     */
    fun hasSubmittedId(userData: Map<String, Any>?): Boolean {
        val status = userData?.get("idVerificationStatus")?.toString() ?: "not_submitted"
        return status != "not_submitted"
    }

    /**
     * Get the current ID verification status
     */
    fun getIdVerificationStatus(userData: Map<String, Any>?): String {
        return userData?.get("idVerificationStatus")?.toString() ?: "not_submitted"
    }
    
    /**
     * Composable that provides the user's profile data
     * and checks if specific features are restricted
     */
    @Composable
    fun rememberProfileRestrictions() {
        val context = LocalContext.current
        var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        
        // Function to load user data
        val loadUserData: () -> Unit = {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            userData = document.data
                        }
                        isLoading = false
                    }
                    .addOnFailureListener {
                        isLoading = false
                    }
            } else {
                isLoading = false
            }
        }
        
        // Check if a feature is restricted
        val checkFeatureRestriction: (String) -> Boolean = { featureType ->
            isFeatureRestricted(featureType, userData)
        }
        
        // Get the restriction message
        val getFeatureRestrictionMessage: (String) -> String = { featureType ->
            getRestrictionMessage(featureType, userData)
        }
        
        // Return the utility functions and state
        return remember(userData, isLoading) {
            object {
                val isLoading = isLoading
                val userData = userData
                val refreshUserData = loadUserData
                val isRestricted: (String) -> Boolean = checkFeatureRestriction
                val restrictionMessage: (String) -> String = getFeatureRestrictionMessage
                val isProfileComplete = userData?.let { isProfileComplete(it) } ?: false
                val isPhoneVerified = userData?.let { isPhoneVerified(it) } ?: false
                val isEmailVerified = userData?.let { isEmailVerified(it) } ?: false
                val isIdVerified = userData?.let { isIdVerified(it) } ?: false
                val idVerificationStatus = userData?.let { getIdVerificationStatus(it) } ?: "not_submitted"
                val hasSubmittedId = userData?.let { hasSubmittedId(it) } ?: false
            }
        }
    }
} 