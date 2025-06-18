package com.example.bikerental.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.ui.theme.Orange500
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.utils.ProfileRestrictionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.bikerental.navigation.Screen

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
    navController: NavController,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRestricted by remember { mutableStateOf(true) }
    var restrictionMessage by remember { mutableStateOf("") }
    var restrictionType by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.data
                        isRestricted = ProfileRestrictionUtils.isFeatureRestricted(featureType, userData)
                        restrictionMessage = ProfileRestrictionUtils.getRestrictionMessage(featureType, userData)
                        
                        // Determine the type of restriction
                        restrictionType = when {
                            !ProfileRestrictionUtils.isEmailVerified(userData) -> "email"
                            !ProfileRestrictionUtils.isProfileComplete(userData) -> "profile"
                            !ProfileRestrictionUtils.isPhoneVerified(userData) -> "phone"
                            !ProfileRestrictionUtils.isIdVerified(userData) -> "id"
                            else -> ""
                        }
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
        // Show a loading state
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material.CircularProgressIndicator(color = ColorUtils.DarkGreen)
        }
    } else if (isRestricted) {
        // Show a restriction message with guidance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (restrictionType) {
                        "email" -> Icons.Default.Email
                        "profile" -> Icons.Default.Person
                        "phone" -> Icons.Default.Phone
                        "id" -> Icons.Default.Badge
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = when (restrictionType) {
                        "email" -> "Email Verification Required"
                        "profile" -> "Complete Your Profile"
                        "phone" -> "Phone Verification Required"
                        "id" -> "ID Verification Required"
                        else -> "Feature Restricted"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = restrictionMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        when (restrictionType) {
                            "email" -> navController.navigate("emailVerification")
                            "profile" -> navController.navigate("edit_profile")
                            "id" -> navController.navigate("id_verification")
                            else -> navController.navigate("profile")
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (restrictionType) {
                            "email" -> "Verify Email"
                            "profile" -> "Complete Profile"
                            "phone" -> "Verify Phone Number"
                            "id" -> "Verify ID"
                            else -> "View Profile"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        // Show the actual feature content
        content()
    }
}

/**
 * A smaller version of the restricted feature component
 * that can be used inline or in smaller UI components
 */
@Composable
fun CompactRestrictedFeature(
    featureType: String,
    navController: NavController,
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRestricted by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.data
                        isRestricted = ProfileRestrictionUtils.isFeatureRestricted(featureType, userData)
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
        // Simple loading state
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material.CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = ColorUtils.DarkGreen
            )
        }
    } else if (isRestricted) {
        // Compact restriction notice
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    // Determine where to navigate based on restriction type
                    if (!ProfileRestrictionUtils.isEmailVerified(userData)) {
                        navController.navigate("emailVerification")
                    } else if (!ProfileRestrictionUtils.isProfileComplete(userData)) {
                        navController.navigate("edit_profile") 
                    } else if (!ProfileRestrictionUtils.isIdVerified(userData)) {
                        navController.navigate("id_verification")
                    } else {
                        navController.navigate("profile")
                    }
                },
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = when {
                            !ProfileRestrictionUtils.isEmailVerified(userData) -> "Email verification required"
                            !ProfileRestrictionUtils.isProfileComplete(userData) -> "Complete your profile first"
                            !ProfileRestrictionUtils.isIdVerified(userData) -> "ID verification required"
                            else -> "Access restricted"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Restricted",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        // Show the actual feature content
        content()
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

/**
 * Handle user tapping "verify" for different restriction types
 */
private fun handleVerification(restrictionType: String, navController: NavController) {
    when (restrictionType) {
        "email" -> navController.navigate("emailVerification")
        "profile" -> navController.navigate("editProfile")
        "phone" -> navController.navigate("profile") // Show phone verification on profile page
        "id" -> navController.navigate("id_verification")
        else -> navController.navigate("profile") // Default to profile
    }
}

/**
 * Show verification call-to-action if needed
 */
@Composable
fun VerificationCallToAction(
    showRestrictionInfo: Boolean,
    navController: NavController?,
    restrictionType: String?
) {
    if (showRestrictionInfo && navController != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            when {
                // If it's specifically ID verification that's missing
                restrictionType?.contains("id", ignoreCase = true) == true -> {
                    Button(
                        onClick = { navController.navigate("id_verification") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Verify ID Now")
                    }
                }
                // For other verification types
                restrictionType != null -> {
                    Button(
                        onClick = { handleVerification(restrictionType, navController) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Verify Now")
                    }
                }
            }
        }
    }
}

/**
 * Helper functions for checking ID verification requirements
 */

/**
 * Checks if the user can create a booking based on ID verification status
 * 
 * @param userData The user's profile data
 * @param onRestricted Action to perform when the user is restricted
 * @param onAllowed Action to perform when the user is allowed to proceed
 */
fun checkBookingAllowed(
    userData: Map<String, Any>?,
    navController: NavController?,
    onRestricted: () -> Unit = {},
    onAllowed: () -> Unit
) {
    if (ProfileRestrictionUtils.isFeatureRestricted("booking", userData)) {
        // User is restricted from booking, show restriction message
        onRestricted()
        
        // Navigate to ID verification if that's the missing requirement
        if (!ProfileRestrictionUtils.isIdVerified(userData)) {
            navController?.navigate("id_verification")
        }
    } else {
        // User is allowed to book
        onAllowed()
    }
}

/**
 * Checks if the user can start a ride based on ID verification status
 * 
 * @param userData The user's profile data
 * @param navController Navigation controller for redirecting to ID verification
 * @param onRestricted Action to perform when the user is restricted
 * @param onAllowed Action to perform when the user is allowed to proceed
 */
fun checkStartRideAllowed(
    userData: Map<String, Any>?,
    navController: NavController?,
    onRestricted: () -> Unit = {},
    onAllowed: () -> Unit
) {
    if (ProfileRestrictionUtils.isFeatureRestricted("start_ride", userData)) {
        // User is restricted from starting a ride
        onRestricted()
        
        // Navigate to ID verification if that's the missing requirement
        if (!ProfileRestrictionUtils.isIdVerified(userData)) {
            navController?.navigate("id_verification")
        }
    } else {
        // User is allowed to start the ride
        onAllowed()
    }
}

/**
 * Checks if the user can write a review based on ID verification status
 * 
 * @param userData The user's profile data
 * @param navController Navigation controller for redirecting to ID verification
 * @param onRestricted Action to perform when the user is restricted
 * @param onAllowed Action to perform when the user is allowed to proceed
 */
fun checkReviewAllowed(
    userData: Map<String, Any>?,
    navController: NavController?,
    onRestricted: () -> Unit = {},
    onAllowed: () -> Unit
) {
    if (ProfileRestrictionUtils.isFeatureRestricted("write_review", userData)) {
        // User is restricted from writing a review
        onRestricted()
        
        // Navigate to ID verification if that's the missing requirement
        if (!ProfileRestrictionUtils.isIdVerified(userData)) {
            navController?.navigate("id_verification")
        }
    } else {
        // User is allowed to write a review
        onAllowed()
    }
} 