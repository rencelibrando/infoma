package com.example.bikerental.screens.tabs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bikerental.R
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Error
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.bikerental.viewmodels.PhoneAuthViewModel
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.ImeAction
import com.example.bikerental.models.PhoneAuthState
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.example.bikerental.utils.ColorUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.TextButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.Dp
import com.example.bikerental.navigation.Screen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow

// Profile color scheme
object ProfileColors {
    val White = Color(0xFFFFFFFF)
    val DarkGreen = Color(0xFF0A5F38)
    val LightGray = Color(0xFFF5F5F5)
    val MediumGray = Color(0xFFE0E0E0)
    val DarkGray = Color(0xFF757575)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFE53E3E)
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
}

// Add this function near the top of the file, before the ProfileTab function
fun formatPhilippinePhoneNumber(phoneNumber: String): String {
    // Remove any non-digit characters
    val digitsOnly = phoneNumber.replace(Regex("\\D"), "")
    
    return when {
        // If already in international format with +63
        digitsOnly.startsWith("63") && digitsOnly.length >= 12 -> {
            "+$digitsOnly"
        }
        // If starts with 0 (local format)
        digitsOnly.startsWith("0") && digitsOnly.length >= 11 -> {
            "+63${digitsOnly.substring(1)}"
        }
        // If starts with 9 (mobile number without prefix)
        digitsOnly.startsWith("9") && digitsOnly.length >= 10 -> {
            "+63$digitsOnly"
        }
        // Otherwise keep as is
        else -> {
            phoneNumber
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val phoneAuthViewModel: PhoneAuthViewModel = viewModel()
    val phoneAuthState = phoneAuthViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var verificationCode by remember { mutableStateOf("") }
    var isVerificationInProgress by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var user by remember { mutableStateOf<FirebaseUser?>(null) }
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showVerifyPhoneDialog by remember { mutableStateOf(false) }
    var actuallyShowVerifyDialog by remember { mutableStateOf(false) }
    var isCheckingRateLimit by remember { mutableStateOf(false) }
    var verificationAttempted by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var phoneNumber by remember {
        mutableStateOf(
            profileData?.get("phoneNumber")?.toString() ?: ""
        )
    }
    var formattedPhoneNumber by remember {
        mutableStateOf(
            formatPhilippinePhoneNumber(
                profileData?.get("phoneNumber")?.toString() ?: ""
            )
        )
    }
    
    // State for tracking previous phone auth state
    var previousPhoneAuthState by remember { mutableStateOf<PhoneAuthState?>(null) }

    // Add ID verification state
    var idVerificationStatus by remember { mutableStateOf("not_submitted") }
    var idType by remember { mutableStateOf<String?>(null) }
    
    // Function to check if profile is complete
    fun isProfileComplete(): Boolean {
        return profileData?.let { data ->
            val requiredFields = listOf(
                "fullName",
                "phoneNumber",
                "email",
                "street",
                "barangay",
                "city"
            )
            requiredFields.all { field ->
                val value = data[field]?.toString()
                value != null && value.isNotBlank()
            }
        } ?: false
    }

    fun isPhoneVerified(): Boolean {
        return profileData?.get("isPhoneVerified") as? Boolean == true
    }

    fun isIdVerified(): Boolean {
        return profileData?.get("isIdVerified") as? Boolean == true
    }
    
    // Function to get ID verification status
    fun getIdVerificationStatus(): String {
        return profileData?.get("idVerificationStatus")?.toString() ?: "not_submitted"
    }

    // Function to get ID type
    fun getIdType(): String? {
        return profileData?.get("idType")?.toString()
    }
    
    // Function to update phone number in Firestore
    fun updatePhoneNumber(formattedNumber: String) {
        // Only update if user is authenticated and phone number has changed
        if (user != null && formattedNumber != profileData?.get("phoneNumber")?.toString()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user!!.uid)
                .update("phoneNumber", formattedNumber)
                .addOnSuccessListener {
                    // Update successful
                    Toast.makeText(
                        context,
                        "Phone number updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Update local profileData
                    val updatedProfileData = profileData?.toMutableMap() ?: mutableMapOf()
                    updatedProfileData["phoneNumber"] = formattedNumber
                    profileData = updatedProfileData
                }
                .addOnFailureListener { e ->
                    // Update failed
                    Toast.makeText(
                        context,
                        "Failed to update phone number: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
    
    // Function to refresh profile data
    fun refreshProfile() {
        isRefreshing = true
        user = FirebaseAuth.getInstance().currentUser
        
        if (user != null) {
            // First reload Firebase Auth user to get fresh data
            user?.reload()?.addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    Log.d("ProfileTab", "Firebase Auth user reloaded successfully")
                    // Get the refreshed user instance
                    user = FirebaseAuth.getInstance().currentUser
                    
                    // After reload, update UI with the latest data
                    formattedPhoneNumber = formatPhilippinePhoneNumber(
                        profileData?.get("phoneNumber")?.toString() ?: ""
                    )

                    // Get ID verification data
                    idVerificationStatus = getIdVerificationStatus()
                    idType = getIdType()
                } else {
                    Log.e("ProfileTab", "Failed to reload Firebase Auth user: ${reloadTask.exception?.message}")
                }
                
                // After reload (successful or not), fetch from Firestore
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user!!.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            profileData = document.data
                            
                            // Update phone number state with the latest data
                            phoneNumber = profileData?.get("phoneNumber")?.toString() ?: ""
                            formattedPhoneNumber = formatPhilippinePhoneNumber(phoneNumber)
                            
                            // Get ID verification data
                            idVerificationStatus = getIdVerificationStatus()
                            idType = getIdType()
                            
                            // Sync Firebase Auth email with Firestore if needed
                            val firestoreEmail = profileData?.get("email")?.toString()
                            val firebaseEmail = user?.email
                            
                            if ((firestoreEmail.isNullOrEmpty() || firestoreEmail == "null") && !firebaseEmail.isNullOrEmpty()) {
                                Log.d("ProfileTab", "Updating missing email in Firestore: $firebaseEmail")
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user!!.uid)
                                    .update("email", firebaseEmail)
                                    .addOnSuccessListener {
                                        // Update local profileData to reflect the change
                                        val updatedProfileData = profileData?.toMutableMap() ?: mutableMapOf()
                                        updatedProfileData["email"] = firebaseEmail
                                        profileData = updatedProfileData
                                        
                                        Log.d("ProfileTab", "Updated email in Firestore successfully")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ProfileTab", "Failed to update email in Firestore: ${e.message}")
                                    }
                            }
                            
                            Log.d("ProfileTab", "Loaded profile data: $profileData")
                        } else {
                            Log.d("ProfileTab", "No profile document exists for user ${user?.uid}")
                            // Create a minimal profile if none exists
                            val minimalProfile = mapOf(
                                "email" to (user?.email ?: ""),
                                "fullName" to (user?.displayName ?: ""),
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )
                            
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user!!.uid)
                                .set(minimalProfile)
                                .addOnSuccessListener {
                                    Log.d("ProfileTab", "Created minimal profile for user")
                                    profileData = minimalProfile
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ProfileTab", "Failed to create minimal profile: ${e.message}")
                                }
                        }
                        isRefreshing = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileTab", "Failed to load profile data: ${e.message}")
                        isRefreshing = false
                        
                        // Show error toast
                        Toast.makeText(
                            context,
                            "Failed to load profile data. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        } else {
            isRefreshing = false
            Log.w("ProfileTab", "No user logged in, can't refresh profile")
        }
    }

    // Initial data load
    LaunchedEffect(Unit) {
        user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Instead of direct navigation that might conflict with other navControllers,
            // use a proper callback approach or let the parent handle navigation
            Log.d("ProfileTab", "No user logged in, authentication required")
            // Don't navigate directly - let MainActivity handle authentication
            return@LaunchedEffect
        }
        
        // Check and cleanup any expired rate limits
        phoneAuthViewModel.checkAndCleanupExpiredRateLimits()
        
        refreshProfile()
    }

    // Handle sign out action
    val handleSignOut = {
        coroutineScope.launch {
            try {
                Log.d("ProfileTab", "User signing out")
                viewModel.signOut()
                
                // Let MainActivity's AuthState observer handle navigation after signOut
                // Instead of navigating directly here
                Log.d("ProfileTab", "Sign out completed, waiting for auth state to update")
            } catch (e: Exception) {
                Log.e("ProfileTab", "Error during sign out: ${e.message}")
            }
        }
    }

    // Effect to handle verification success and other state changes
    LaunchedEffect(phoneAuthState.value) {
        val currentState = phoneAuthState.value // Capture current state for the effect

        when (currentState) {
            is PhoneAuthState.Success -> {
                showVerifyPhoneDialog = false
                actuallyShowVerifyDialog = false
                isVerificationInProgress = false
                phoneNumber = ""
                verificationCode = ""
                verificationAttempted = false
                // Refresh profile data after successful verification
                refreshProfile()
            }
            is PhoneAuthState.CodeSent -> {
                isVerificationInProgress = true
                verificationAttempted = true
            }
            is PhoneAuthState.RateLimited -> {
                // When rate limited, close dialog and show rate limit message
                actuallyShowVerifyDialog = false
                verificationAttempted = true // Keep this true to indicate an attempt happened
            }
            is PhoneAuthState.Initial -> {
                // Only reset verificationAttempted if the state explicitly transitions
                // back from RateLimited after the timer completes.
                if (previousPhoneAuthState is PhoneAuthState.RateLimited) {
                    verificationAttempted = false
                    Log.d("ProfileTab", "Resetting verificationAttempted as state moved from RateLimited to Initial")
                }
                // Reset other potentially stale states if needed when returning to Initial
                isVerificationInProgress = false
                // Keep phoneNumber and verificationCode if user might retry immediately?
                // Consider resetting them based on UX requirements.
            }
            // Add handling for Error state if necessary
            is PhoneAuthState.Error -> {
                 // Optionally handle error state, e.g., reset progress indicators
                 isVerificationInProgress = false
                 // Maybe show a snackbar or log the error?
                 Log.e("ProfileTab", "PhoneAuthState Error: \${currentState.error?.message}")
            }
            else -> {
                // No specific action needed for other states like Loading or Idle in this effect
            }
        }
        // Update previous state *after* processing the current state
        previousPhoneAuthState = currentState
    }

    // Check rate limit when user tries to open verification dialog
    LaunchedEffect(showVerifyPhoneDialog) {
        if (showVerifyPhoneDialog && !actuallyShowVerifyDialog && !isCheckingRateLimit) {
            isCheckingRateLimit = true
            
            try {
                val (isRateLimited, expiryTime) = phoneAuthViewModel.checkRateLimitStatus()
                
                if (isRateLimited) {
                    // Use the actual server-stored expiry time
                    val displayDuration = "3 minutes" // Default display, actual countdown will be accurate
                    phoneAuthViewModel.resetState()
                    phoneAuthViewModel.setRateLimited(expiryTime, displayDuration)
                    showVerifyPhoneDialog = false  // Reset the trigger
                } else {
                    actuallyShowVerifyDialog = true
                }
            } catch (e: Exception) {
                // If rate limit check fails, proceed with the dialog
                actuallyShowVerifyDialog = true
            } finally {
                isCheckingRateLimit = false
            }
        }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            refreshProfile()
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Completion Warning
            if (!isProfileComplete()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("editProfile") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
            Text(
                                text = "Complete Your Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Please complete your profile to access all features",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Email Verification Warning
            if (!(profileData?.get("isEmailVerified") as? Boolean ?: false)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("emailVerification") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Verify Your Email",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Email verification required to access booking and payment features",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            // Add verification benefits list
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                        verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Book bikes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Access payment features",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Button(
                            onClick = { navController.navigate("emailVerification") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Verify",
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Phone Verification Warning
            if (profileData?.get("phoneNumber") != null && !isPhoneVerified()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (!verificationAttempted) {
                                showVerifyPhoneDialog = true
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Verify Your Phone Number",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Your phone number needs to be verified for security",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { 
                                if (!verificationAttempted) {
                                    showVerifyPhoneDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Verify",
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }

            // ID Verification Warning
            if (!isIdVerified()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.IdVerification.route) },
                    colors = CardDefaults.cardColors(
                        containerColor = when (getIdVerificationStatus()) {
                            "pending" -> Color(0xFFFFF3E0) // Light orange background
                            "rejected" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (getIdVerificationStatus()) {
                                "pending" -> Icons.Default.HourglassTop
                                "rejected" -> Icons.Default.Error
                                else -> Icons.Default.Badge
                            },
                            contentDescription = "Warning",
                            tint = when (getIdVerificationStatus()) {
                                "pending" -> Color(0xFFF57C00) // Orange
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (getIdVerificationStatus()) {
                                    "pending" -> "ID Verification in Progress"
                                    "rejected" -> "ID Verification Failed"
                                    else -> "Verify Your Identity"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = when (getIdVerificationStatus()) {
                                    "pending" -> Color(0xFFF57C00) // Orange
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = when (getIdVerificationStatus()) {
                                    "pending" -> "We're reviewing your submitted ID. This usually takes 1-2 business days."
                                    "rejected" -> "Your ID verification was rejected. Please submit a clearer image."
                                    else -> "ID verification is required to rent bikes and access payment features."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { navController.navigate(Screen.IdVerification.route) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (getIdVerificationStatus()) {
                                    "pending" -> Color(0xFFF57C00) // Orange
                                    else -> MaterialTheme.colorScheme.error
                                }
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = when (getIdVerificationStatus()) {
                                    "pending" -> "Check Status"
                                    "rejected" -> "Resubmit"
                                    else -> "Verify"
                                },
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Profile Card with Verification Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileDialog = true }
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Picture with completion indicator
                    Box {
                        AsyncImage(
                            model = profileData?.get("profilePictureUrl") ?: user?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        !isProfileComplete() -> MaterialTheme.colorScheme.error
                                        !isPhoneVerified() -> MaterialTheme.colorScheme.error
                                        !isIdVerified() -> MaterialTheme.colorScheme.error
                                        else -> ColorUtils.DarkGreen
                                    },
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.default_profile_picture),
                            fallback = painterResource(id = R.drawable.default_profile_picture),
                            placeholder = painterResource(id = R.drawable.default_profile_picture)
                        )
                    }

                    // Basic Info with verification status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Enhanced name display with fallbacks
                        val displayName = when {
                            !profileData?.get("fullName")?.toString().isNullOrBlank() -> 
                                profileData?.get("fullName")?.toString()
                            !user?.displayName.isNullOrBlank() -> 
                                user?.displayName
                            !user?.email.isNullOrBlank() -> 
                                user?.email?.substringBefore('@')
                            else -> "Complete your profile"
                        }
                        
                        Text(
                            text = displayName ?: "Complete your profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorUtils.DarkGreen
                        )
                        // Improved email display with multiple fallbacks
                        val emailToShow = when {
                            // First try Firestore data
                            !profileData?.get("email")?.toString().isNullOrBlank() -> 
                                profileData?.get("email")?.toString()
                            // Then try Firebase Auth user - use safe call for email
                            !user?.email.isNullOrBlank() -> 
                                user?.email
                            // Then try current user from Firebase Auth (might be more up-to-date)
                            !FirebaseAuth.getInstance().currentUser?.email.isNullOrBlank() ->
                                FirebaseAuth.getInstance().currentUser?.email
                            // Fallback
                            else -> "Add your email"
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = emailToShow ?: "Email not available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (emailToShow.isNullOrBlank() || emailToShow == "Add your email") 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    ColorUtils.blackcol(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            
                            if (user?.isEmailVerified == true || profileData?.get("isEmailVerified") == true) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Email Verified",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = profileData?.get("phoneNumber")?.toString() 
                                    ?: "Add phone number",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    profileData?.get("phoneNumber") == null -> MaterialTheme.colorScheme.error
                                    !isPhoneVerified() -> MaterialTheme.colorScheme.error
                                    else -> ColorUtils.blackcol()
                                }
                            )
                            if (profileData?.get("phoneNumber") != null) {
                                Icon(
                                    imageVector = if (isPhoneVerified()) 
                                        Icons.Default.CheckCircle 
                                    else 
                                        Icons.Default.Warning,
                                    contentDescription = if (isPhoneVerified()) 
                                        "Verified" 
                                    else 
                                        "Not Verified",
                                    tint = if (isPhoneVerified()) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val idStatus = getIdVerificationStatus()
                            Text(
                                text = when (idStatus) {
                                    "verified" -> "ID Verified (${getIdType() ?: "Valid ID"})"
                                    "pending" -> "ID Verification Pending"
                                    "rejected" -> "ID Verification Failed"
                                    else -> "ID Not Verified"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (idStatus) {
                                    "verified" -> ColorUtils.blackcol()
                                    "pending" -> Color(0xFFF57C00) // Orange
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            
                            Icon(
                                imageVector = when (idStatus) {
                                    "verified" -> Icons.Default.CheckCircle
                                    "pending" -> Icons.Default.HourglassTop
                                    "rejected" -> Icons.Default.Error
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = when (idStatus) {
                                    "verified" -> "Verified"
                                    "pending" -> "Pending"
                                    "rejected" -> "Rejected"
                                    else -> "Not Verified"
                                },
                                tint = when (idStatus) {
                                    "verified" -> MaterialTheme.colorScheme.primary
                                    "pending" -> Color(0xFFF57C00) // Orange
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Progress Indicator
            if (!isProfileComplete() || !isIdVerified()) {
                LinearProgressIndicator(
                    progress = { 
                        profileData?.let { data ->
                            val requiredFields = listOf(
                                "fullName",
                                "phoneNumber",
                                "street",
                                "barangay",
                                "city"
                            )
                            var completeFields = requiredFields.count { field ->
                                data[field].toString().isNotBlank()
                            }.toFloat()
                            
                            // Add weight for ID verification (worth 3 regular fields)
                            if (isIdVerified()) {
                                completeFields += 3
                                completeFields / (requiredFields.size + 3)
                            } else {
                                completeFields / (requiredFields.size + 3)
                            }
                        } ?: 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ColorUtils.DarkGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Text(
                    text = "Profile completion: ${
                        (profileData?.let { data ->
                            val requiredFields = listOf(
                                "fullName",
                                "phoneNumber",
                                "street",
                                "barangay",
                                "city"
                            )
                            var completeFields = requiredFields.count { field ->
                                data[field].toString().isNotBlank()
                            }.toFloat()
                            
                            // Add weight for ID verification
                            val totalFields = requiredFields.size + 3
                            if (isIdVerified()) {
                                completeFields += 3
                            }
                            
                            (completeFields / totalFields * 100).toInt()
                        } ?: 0)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ID Verification Card (when not yet verified)
            if (getIdVerificationStatus() == "not_submitted") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.IdVerification.route) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "ID Verification",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Verify Your Identity",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Upload a valid government ID to unlock all features including bike rental and payment options.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { navController.navigate(Screen.IdVerification.route) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Upload ID Now",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Account Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Account Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorUtils.DarkGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsContent(navController, viewModel, ColorUtils.blackcol(), coroutineScope)
                }
            }
        }

        // Add PullRefreshIndicator at the top center
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = ColorUtils.DarkGreen
        )

        // Loading overlay (optional, you can keep or remove this)
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ColorUtils.DarkGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    // Phone Verification Dialog
    if (actuallyShowVerifyDialog) {
        PhoneVerificationDialog(
            phoneNumber = profileData?.get("phoneNumber")?.toString()?.replace("+63", "") ?: "",
            onDismiss = { 
                actuallyShowVerifyDialog = false
                isVerificationInProgress = false
            },
            viewModel = phoneAuthViewModel,
            activity = context as Activity,
            verificationAttemptedRef = remember { mutableStateOf(verificationAttempted) },
            showVerifyPhoneDialogRef = remember { mutableStateOf(showVerifyPhoneDialog) },
            actuallyShowVerifyDialogRef = remember { mutableStateOf(actuallyShowVerifyDialog) }
        )
    }

    // Profile Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            modifier = Modifier.widthIn(max = 400.dp),
            title = {
                Text(
                    text = "Profile Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorUtils.DarkGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact profile picture with info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = profileData?.get("profilePictureUrl") ?: user?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.default_profile_picture),
                            fallback = painterResource(id = R.drawable.default_profile_picture),
                            placeholder = painterResource(id = R.drawable.default_profile_picture)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profileData?.get("fullName")?.toString() ?: user?.displayName ?: "Not available",
                                style = MaterialTheme.typography.titleSmall,
                                color = ColorUtils.DarkGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profileData?.get("email")?.toString() ?: user?.email ?: "Not available",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorUtils.blackcol(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Compact information sections
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Personal Information
                        Text(
                            text = "Personal",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorUtils.DarkGreen,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        
                        CompactInfoRow("Phone", profileData?.get("phoneNumber")?.toString() ?: "Not available", ColorUtils.blackcol())
                        CompactInfoRow("Member Since", profileData?.get("memberSince")?.toString() ?: "Not available", ColorUtils.blackcol())
                        
                        // Address Information
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorUtils.DarkGreen,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                        CompactInfoRow("Street", profileData?.get("street")?.toString() ?: "Not available", ColorUtils.blackcol())
                        CompactInfoRow("Barangay", profileData?.get("barangay")?.toString() ?: "Not available", ColorUtils.blackcol())
                        CompactInfoRow("City", profileData?.get("city")?.toString() ?: "Not available", ColorUtils.blackcol())
                        
                        // Account Information
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorUtils.DarkGreen,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                        CompactInfoRow("Type", profileData?.get("authProvider")?.toString()?.replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase() else it.toString() 
                        } ?: "Email", ColorUtils.blackcol())
                        CompactInfoRow("Email Verified", if (user?.isEmailVerified == true) "Yes" else "No", ColorUtils.blackcol())
                        
                        // Add ID Verification Information
                        Text(
                            text = "Verification",
                            style = MaterialTheme.typography.labelLarge,
                            color = ColorUtils.DarkGreen,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                        
                        CompactInfoRow(
                            "ID Type", 
                            profileData?.get("idType")?.toString() ?: "Not submitted", 
                            ColorUtils.blackcol()
                        )
                        
                        CompactInfoRow(
                            "ID Status", 
                            when (profileData?.get("idVerificationStatus")?.toString()) {
                                "verified" -> "Verified"
                                "pending" -> "Pending verification"
                                "rejected" -> "Verification failed"
                                else -> "Not submitted"
                            },
                            ColorUtils.blackcol()
                        )
                        
                        // Show verification date if verified
                        if (profileData?.get("idVerificationStatus")?.toString() == "verified") {
                            profileData?.get("idSubmissionDate")?.let {
                                CompactInfoRow(
                                    "Verified On",
                                    // Format timestamp as readable date
                                    try {
                                        val timestamp = it as? com.google.firebase.Timestamp 
                                        val date = timestamp?.toDate()
                                        val formatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                                        formatter.format(date) ?: "Unknown"
                                    } catch (e: Exception) {
                                        "Unknown"
                                    },
                                    ColorUtils.blackcol()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = ColorUtils.DarkGreen)
                }
            }
        )
    }
}

@Composable
private fun SettingsContent(
    navController: NavController, 
    viewModel: AuthViewModel,
    purple200: Color,
    coroutineScope: CoroutineScope
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsButton(
            icon = Icons.Default.Edit,
            text = "Edit Profile",
            onClick = { navController.navigate(Screen.EditProfile.route) },
            purple200 = Color.Black
        )
        SettingsButton(
            icon = Icons.Default.Lock,
            text = "Change Password",
            onClick = { navController.navigate(Screen.ChangePassword.route) },
            purple200 = Color.Black
        )
        // Help & Support section removed
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Modern Sign Out button with proper coroutineScope usage
        Button(
            onClick = {
                // Correctly use the passed coroutineScope parameter
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        Log.d("ProfileTab", "User signing out")
                        viewModel.signOut()
                        
                        // Navigate to sign in screen after sign out
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(0) { inclusive = true } // Clear the entire back stack
                        }
                        
                        Log.d("ProfileTab", "Sign out completed, navigated to sign in")
                    } catch (e: Exception) {
                        Log.e("ProfileTab", "Error during sign out: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray.copy(alpha = 0.4f),
                contentColor = Color.Black
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Sign Out",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
        
        Divider(modifier = Modifier.padding(top = 8.dp))
        
        // Delete Account Button - Using a custom button with red color
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteAccountDialog = true },
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Account",
                    tint = Color.Red
                )
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Red
                )
            }
        }
    }
    
    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete your account? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All your data, including profile information, ride history, and payment details will be permanently removed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        // Call the delete account function in the view model
                        viewModel.deleteAccount(
                            onSuccess = {
                                // Navigate to sign-in screen after successful deletion
                                navController.navigate(Screen.SignIn.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            onError = { /* Handle error if needed */ }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text("Delete My Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    purple200: Color,
    elevation: Dp = 0.dp
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = purple200
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = purple200
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, purple200: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = purple200.copy(alpha = 0.7f),
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = purple200,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactInfoRow(label: String, value: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.weight(0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Add this class outside your composables for the phone number visual transformation
class PhilippinesPhoneNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val cleanText = text.text.replace(Regex("^\\+?63"), "")
        
        val annotatedString = AnnotatedString("+63 $cleanText")
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + 4 // +63 plus space
            }
            
            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= 4) 0 else offset - 4
            }
        }
        
        return TransformedText(annotatedString, offsetMapping)
    }
}

// Update the RateLimitedSection to properly reset verification state on completion
@Composable
private fun RateLimitedSection(
    onDismiss: () -> Unit,
    uiState: PhoneAuthState.RateLimited,
    viewModel: PhoneAuthViewModel
) {
    // More accurate timer calculation
    val expireTimeMillis = remember(uiState.expireTimeMillis) { 
        // Ensure we don't show negative times
        uiState.expireTimeMillis.coerceAtLeast(System.currentTimeMillis())
    }
    
    val scope = rememberCoroutineScope()
    
    // Create a state that will hold the formatted time
    var formattedTime by remember { mutableStateOf("") }
    var timerCompleted by remember { mutableStateOf(false) }
    var timeRemainingRatio by remember { mutableStateOf(1f) }
    
    // Calculate how much total time was initially set
    val totalDurationMillis by remember { 
        mutableStateOf(
            if (expireTimeMillis > System.currentTimeMillis()) {
                // For regular rate limits, use a shorter duration based on expiry
                (expireTimeMillis - System.currentTimeMillis()).coerceAtMost(3 * 60 * 1000L)
            } else {
                // Default duration if already expired (should be handled by resetState)
                 3 * 60 * 1000L
            }
        )
    }
    
    // Create a timer that updates every second
    LaunchedEffect(expireTimeMillis) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val remainingMillis = expireTimeMillis - currentTime
            
            if (remainingMillis <= 0) {
                formattedTime = "0 seconds"
                timerCompleted = true
                timeRemainingRatio = 0f
                
                // Always reset state when timer completes
                viewModel.resetState()
                
                break
            }
            
            // Calculate progress ratio for visual indicator
            timeRemainingRatio = (remainingMillis.toFloat() / totalDurationMillis).coerceIn(0f, 1f)
            
            // Format the remaining time based on duration
            val minutes = remainingMillis / (60 * 1000)
            val seconds = (remainingMillis % (60 * 1000)) / 1000
            
            formattedTime = if (minutes > 0) {
                "$minutes min $seconds sec"
            } else {
                "$seconds seconds"
            }
            
            // Delay for 1 second before updating again
            delay(1000)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verification Limit Reached",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Text(
                text = "You've reached the maximum number of verification attempts for this phone number.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Progress indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Try again in:",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = formattedTime,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Linear progress indicator
                LinearProgressIndicator(
                    progress = { timeRemainingRatio },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            }
            
            Button(
                onClick = {
                    if (timerCompleted) {
                        // If timer completed, reset state before dismissing
                        viewModel.resetState()
                    }
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (timerCompleted) "Try Again" else "OK")
            }
        }
    }
}

// Add this composable for the specific app identifier error
@Composable
private fun AppIdentifierErrorSection(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Verification Issue Detected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "We're having an issue with phone verification. This is typically caused by a configuration problem with the app.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text(
                text = "You can try these solutions:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Make sure you have Google Play Services installed and updated")
                Text("2. Check that your device has internet connectivity")
                Text("3. Try again later")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun RecaptchaInfoDialog(
    phoneNumber: String,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBack,
        title = { 
            Text(
                "GearTick Verification",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "You'll be redirected to Google's reCAPTCHA verification."
                )
                Text(
                    "After verification, GearTick will send a code to +63 $phoneNumber."
                )

                Text(
                    "Please complete the verification when prompted.",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneNumberInputSection(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter your phone number",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country code prefix
            Surface(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small
                    ),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "+63",
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Phone number input - use OutlinedTextField with basic properties to avoid experimental API
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input: String ->
                    // Allow only digits and filter out non-digit characters
                    val filteredInput = input.replace(Regex("[^0-9]"), "")
                    if (filteredInput.length <= 10) {
                        // Pass the filtered input to the parent
                        onPhoneNumberChange(filteredInput)
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                placeholder = { Text("9xxxxxxxxx") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }
        
        // Validation message
        AnimatedVisibility(visible = phoneNumber.isNotEmpty() && phoneNumber.length < 9) {
            Text(
                text = "Please enter a valid phone number (9 or 10 digits)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We'll send a verification code to this number",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Show the formatted number for clarity
        if (phoneNumber.isNotEmpty() && phoneNumber.length >= 9) {
            val formattedNumber = formatPhilippinePhoneNumber(phoneNumber)
            Text(
                text = "Will be saved as: $formattedNumber",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun LoadingSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "Verifying...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SuccessSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Phone number verified!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorSection(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verification Failed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun RecaptchaErrorSection(
    onBypass: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "reCAPTCHA Verification Issue",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "We're having trouble with the security verification. This is often due to browser security settings or network issues.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = { onBypass() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Try Alternative Verification")
        }
    }
}

// Add the PhoneVerificationDialog composable
@Composable
private fun PhoneVerificationDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    viewModel: PhoneAuthViewModel,
    activity: Activity,
    verificationAttemptedRef: MutableState<Boolean>,
    showVerifyPhoneDialogRef: MutableState<Boolean>,
    actuallyShowVerifyDialogRef: MutableState<Boolean>
) {
    val uiState by viewModel.uiState.collectAsState()
    var otpValue by remember { mutableStateOf("") }
    var localPhoneNumber by remember { mutableStateOf(phoneNumber) }
    val isPhoneNumberEntered by remember(localPhoneNumber) { mutableStateOf(localPhoneNumber.length >= 9) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = {
            viewModel.resetState()
            onDismiss() 
        },
        title = {
            Text(
                text = "Phone Verification",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState) {
                    is PhoneAuthState.Loading -> {
                        LoadingSection()
                    }
                    
                    is PhoneAuthState.CodeSent -> {
                        // Show OTP input
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Enter the 6-digit code sent to +63$localPhoneNumber"
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Basic OTP input
                            OutlinedTextField(
                                value = otpValue,
                                onValueChange = { 
                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                        otpValue = it
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword
                                ),
                                placeholder = { Text("6-digit verification code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Auto-verify when 6 digits entered
                            LaunchedEffect(otpValue) {
                                if (otpValue.length == 6) {
                                    viewModel.verifyPhoneNumberWithCode(otpValue)
                                }
                            }
                        }
                    }
                    
                    is PhoneAuthState.Success -> {
                        SuccessSection()
                    }
                    
                    is PhoneAuthState.Error -> {
                        val errorMessage = (uiState as PhoneAuthState.Error).message
                        ErrorSection(
                            errorMessage = errorMessage,
                            onRetry = {
                                val formattedPhoneNumber = "+63$localPhoneNumber"
                                scope.launch {
                                    viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                }
                            }
                        )
                    }
                    
                    is PhoneAuthState.RateLimited -> {
                        RateLimitedSection(
                            onDismiss = onDismiss,
                            uiState = uiState as PhoneAuthState.RateLimited,
                            viewModel = viewModel
                        )
                    }
                    
                    is PhoneAuthState.RecaptchaError -> {
                        RecaptchaErrorSection {
                            val formattedPhoneNumber = "+63$localPhoneNumber"
                            scope.launch {
                                try {
                                    viewModel.resetState()
                                    // Here would be the recaptcha bypass method
                                    viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                } catch (e: Exception) {
                                    Log.e("PhoneVerification", "Error during retry: ${e.message}")
                                }
                            }
                        }
                    }
                    
                    is PhoneAuthState.AppCheckError -> {
                        AppIdentifierErrorSection(
                            onRetry = {
                                val formattedPhoneNumber = "+63$localPhoneNumber"
                                scope.launch {
                                    viewModel.resetState()
                                    viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                }
                            },
                            onDismiss = {
                                viewModel.resetState()
                                onDismiss()
                            }
                        )
                    }
                    
                    is PhoneAuthState.Initial -> {
                        // Initial state - show phone input
                        if (!isPhoneNumberEntered) {
                            PhoneNumberInputSection(
                                phoneNumber = localPhoneNumber,
                                onPhoneNumberChange = { 
                                    val filtered = it.replace(Regex("[^0-9]"), "")
                                    if (filtered.length <= 10) {
                                        localPhoneNumber = filtered
                                    }
                                }
                            )
                        } else {
                            // Phone is entered but verification not started
                            Text(
                                text = "Ready to verify +63$localPhoneNumber",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (uiState) {
                        is PhoneAuthState.Initial -> {
                            if (isPhoneNumberEntered) {
                                val formattedPhoneNumber = "+63$localPhoneNumber"
                                verificationAttemptedRef.value = true
                                scope.launch {
                                    try {
                                        viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                    } catch (e: Exception) {
                                        Log.e("PhoneVerification", "Error starting verification: ${e.message}")
                                    }
                                }
                            }
                        }
                        is PhoneAuthState.CodeSent -> {
                            if (otpValue.length == 6) {
                                viewModel.verifyPhoneNumberWithCode(otpValue)
                            }
                        }
                        is PhoneAuthState.Success -> {
                            onDismiss()
                        }
                        is PhoneAuthState.Error, is PhoneAuthState.RecaptchaError, is PhoneAuthState.AppCheckError -> {
                            // For error states, just retry
                            viewModel.resetState()
                        }
                        is PhoneAuthState.RateLimited -> {
                            // For rate limited, just dismiss
                            onDismiss()
                        }
                        else -> {
                            // For other states, do nothing special
                        }
                    }
                },
                enabled = when (uiState) {
                    is PhoneAuthState.Initial -> isPhoneNumberEntered
                    is PhoneAuthState.CodeSent -> otpValue.length == 6
                    is PhoneAuthState.Loading -> false
                    else -> true
                }
            ) {
                Text(
                    text = when (uiState) {
                        is PhoneAuthState.Initial -> if (isPhoneNumberEntered) "Send Code" else "Continue"
                        is PhoneAuthState.CodeSent -> "Verify"
                        is PhoneAuthState.Success -> "Done"
                        is PhoneAuthState.Error, is PhoneAuthState.RecaptchaError, is PhoneAuthState.AppCheckError -> "Retry"
                        is PhoneAuthState.RateLimited -> "OK"
                        else -> "Continue"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetState()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}



