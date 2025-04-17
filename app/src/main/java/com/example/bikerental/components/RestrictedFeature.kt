package com.example.bikerental.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.ui.theme.Orange500
import com.example.bikerental.utils.ProfileRestrictionUtils

/**
 * A wrapper component that conditionally displays content based on profile completion restrictions.
 * If the feature is restricted, it shows a message explaining why and a button to complete the profile.
 * 
 * @param featureType The type of feature being restricted (booking, payment, rental, etc.)
 * @param onCompleteProfile Action to navigate to profile completion screen
 * @param content The content to display if the feature is not restricted
 */



@Composable
fun RestrictedFeature(
    featureType: String,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier,
    customMessage: String? = null,
    customIcon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load user data
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
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
    
    if (isLoading) {
        // Show loading state
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else {
        val isRestricted = ProfileRestrictionUtils.isFeatureRestricted(featureType, userData)
        
        if (isRestricted) {
            // Show restricted UI
            RestrictedFeatureMessage(
                message = customMessage ?: ProfileRestrictionUtils.getRestrictionMessage(featureType, userData),
                onButtonClick = onCompleteProfile,
                buttonText = "Verify",
                isLoading = false
            )
        } else {
            // Show the actual content
            content()
        }
    }
}

/**
 * A composable that renders a message and button when a feature is restricted
 * based on profile completion or verification status
 */
@Composable
fun RestrictedFeatureMessage(
    message: String,
    onButtonClick: () -> Unit,
    buttonText: String = "Verify",
    isLoading: Boolean = false
) {
    val isEmailVerification = message.contains("email", ignoreCase = true)
    val isProfileCompletion = message.contains("profile", ignoreCase = true)
    val isPhoneVerification = message.contains("phone", ignoreCase = true)
    
    val cardColor = when {
        isEmailVerification -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        isProfileCompletion -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    
    val icon = when {
        isEmailVerification -> Icons.Default.Email
        isPhoneVerification -> Icons.Default.Warning
        else -> Icons.Default.Warning
    }
    
    val buttonLabel = when {
        isEmailVerification -> "Verify Email"
        isProfileCompletion -> "Complete Profile"
        isPhoneVerification -> "Verify Phone"
        else -> buttonText
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEmailVerification) Orange500 else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 8.dp)
            )
            
            Text(
                text = if (isEmailVerification) "Verification Required" else "Action Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (isEmailVerification) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Benefits of verifying your email:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    VerificationBenefit("Full access to all rental features")
                    VerificationBenefit("Secure your account and personal data")
                    VerificationBenefit("Get important notifications and receipts")
                    VerificationBenefit("Faster booking process")
                }
            }
            
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmailVerification) Orange500 else MaterialTheme.colorScheme.primary
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
fun VerificationBenefit(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = DarkGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * A simpler version that can be applied to individual UI elements like buttons
 */
@Composable
fun RestrictedButton(
    text: String,
    featureType: String,
    onClick: () -> Unit,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load user data
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
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
    
    val isRestricted = !isLoading && ProfileRestrictionUtils.isFeatureRestricted(featureType, userData)
    val tooltipMessage = if (isRestricted) {
        ProfileRestrictionUtils.getRestrictionMessage(featureType, userData)
    } else {
        null
    }
    
    Button(
        onClick = if (isRestricted) onCompleteProfile else onClick,
        enabled = !isLoading && enabled && !isRestricted,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(text)
    }
    
    // Show tooltip with restriction message if needed
    if (isRestricted && tooltipMessage != null) {
        // In a real implementation, you'd show a tooltip here
        // For simplicity, we're not implementing that now
    }
}

/**
 * Interface for user restriction state
 */
interface UserRestrictionState {
    val isLoading: Boolean
    val userData: Map<String, Any>?
    val refreshUserData: () -> Unit
    val isRestricted: (String) -> Boolean
    val restrictionMessage: (String) -> String
    val isProfileComplete: Boolean
    val isPhoneVerified: Boolean
    val isEmailVerified: Boolean
}

/**
 * Utility to check if features should be restricted based on profile completion or verification
 */
object ProfileRestrictionUtils {
    /**
     * Check if a feature should be restricted based on profile criteria
     */
    fun isFeatureRestricted(featureType: String, userData: Map<String, Any>?): Boolean {
        if (userData == null) return true
        
        return when (featureType) {
            "email_verification" -> !(userData["isEmailVerified"] as? Boolean ?: false)
            "phone_verification" -> !(userData["isPhoneVerified"] as? Boolean ?: false)
            "profile_completion" -> !isProfileComplete(userData)
            "booking" -> !(userData["isEmailVerified"] as? Boolean ?: false) || !(userData["isPhoneVerified"] as? Boolean ?: false)
            else -> false
        }
    }
    
    /**
     * Get a restriction message based on the feature type
     */
    fun getRestrictionMessage(featureType: String, userData: Map<String, Any>?): String {
        if (userData == null) return "Please complete your profile to access this feature."
        
        // For Google sign-in users, use a different message acknowledging their Google account
        val provider = userData["provider"] as? String
        val isGoogleUser = provider == "google"
        
        return when (featureType) {
            "email_verification" -> {
                if (isGoogleUser) {
                    "Please verify your Google account email to access this feature. As a Google user, this process helps us ensure account security."
                } else {
                    "Please verify your email address to access this feature."
                }
            }
            "phone_verification" -> "Please verify your phone number to access this feature."
            "profile_completion" -> "Please complete your profile information to access this feature."
            "booking" -> "Please verify your email and phone number to book bikes."
            else -> "Please complete required verification to access this feature."
        }
    }
    
    /**
     * Check if a user's profile is considered complete
     */
    fun isProfileComplete(userData: Map<String, Any>): Boolean {
        val requiredFields = listOf("fullName", "email", "phoneNumber")
        return requiredFields.all { field -> 
            val value = userData[field] as? String
            value != null && !value.isNullOrBlank()
        }
    }
    
    /**
     * Check if a user's email is verified
     */
    fun isEmailVerified(userData: Map<String, Any>): Boolean {
        return userData["isEmailVerified"] as? Boolean ?: false
    }
    
    /**
     * Check if a user's phone is verified
     */
    fun isPhoneVerified(userData: Map<String, Any>): Boolean {
        return userData["isPhoneVerified"] as? Boolean ?: false
    }
    
    // Helper for UI to use all the checks together with caching
    @Composable
    fun rememberUserRestrictionState(
        userData: Map<String, Any>?,
        isLoading: Boolean,
        loadUserData: () -> Unit
    ): UserRestrictionState {
        val checkFeatureRestriction: (String) -> Boolean = { featureType ->
            isFeatureRestricted(featureType, userData)
        }
        
        val getFeatureRestrictionMessage: (String) -> String = { featureType ->
            getRestrictionMessage(featureType, userData)
        }
        
        return remember(userData, isLoading) {
            object : UserRestrictionState {
                override val isLoading = isLoading
                override val userData = userData
                override val refreshUserData = loadUserData
                override val isRestricted = checkFeatureRestriction
                override val restrictionMessage = getFeatureRestrictionMessage
                override val isProfileComplete = userData?.let { isProfileComplete(it) } ?: false
                override val isPhoneVerified = userData?.let { isPhoneVerified(it) } ?: false
                override val isEmailVerified = userData?.let { isEmailVerified(it) } ?: false
            }
        }
    }
} 