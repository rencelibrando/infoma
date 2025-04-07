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
import androidx.compose.ui.res.colorResource
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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api

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
    var phoneNumber by remember { mutableStateOf("") }
    
    // Function to check if profile is complete
    fun isProfileComplete(): Boolean {
        return profileData?.let { data ->
            val requiredFields = listOf(
                "fullName",
                "phoneNumber",
                "street",
                "barangay",
                "city"
            )
            requiredFields.all { field ->
                data[field].toString().isNotBlank()
            }
        } == true
    }

    fun isPhoneVerified(): Boolean {
        return profileData?.get("isPhoneVerified") as? Boolean == true
    }
    
    // Function to refresh profile data
    fun refreshProfile() {
        user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user!!.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        profileData = document.data
                        isRefreshing = false
                    }
                }
                .addOnFailureListener {
                    isRefreshing = false
                }
        } else {
            isRefreshing = false
        }
    }

    // Initial data load
    LaunchedEffect(Unit) {
        user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navController.navigate("signin") {
                popUpTo("home") { inclusive = true }
            }
            return@LaunchedEffect
        }
        
        // Check and cleanup any expired rate limits
        phoneAuthViewModel.checkAndCleanupExpiredRateLimits()
        
        refreshProfile()
    }

    // Effect to handle verification success and other state changes
    LaunchedEffect(phoneAuthState.value) {
        when (phoneAuthState.value) {
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
                verificationAttempted = true
            }
            is PhoneAuthState.Initial -> {
                // If returned to initial state and timer completed, reset verification attempted flag
                if (verificationAttempted) {
                    val currentState = phoneAuthState.value
                    // Only reset if we're returning from a rate limit
                    if (currentState is PhoneAuthState.Initial) {
                        verificationAttempted = false
                    }
                }
            }
            else -> {}
        }
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
                                        else -> ColorUtils.purple500()
                                    },
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.default_profile_picture)
                        )
                    }

                    // Basic Info with verification status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = profileData?.get("fullName")?.toString() 
                                ?: user?.displayName 
                                ?: "Complete your profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorUtils.purple500()
                        )
                        Text(
                            text = user?.email ?: "Not available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorUtils.blackcol()
                        )
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
                    }
                }
            }

            // Progress Indicator
            if (!isProfileComplete()) {
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
                            val completedFields = requiredFields.count { field ->
                                data[field].toString().isNotBlank()
                            }
                            completedFields.toFloat() / requiredFields.size
                        } ?: 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ColorUtils.purple500(),
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
                            val completedFields = requiredFields.count { field ->
                                data[field].toString().isNotBlank()
                            }
                            (completedFields.toFloat() / requiredFields.size * 100).toInt()
                        } ?: 0)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Ride History Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Ride History",
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorUtils.purple500()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RideHistoryContent(ColorUtils.blackcol())
                }
            }

            // Account Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Account Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorUtils.purple500()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsContent(navController, viewModel, ColorUtils.blackcol())
                }
            }
        }

        // Add PullRefreshIndicator at the top center
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = ColorUtils.purple500()
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
                    color = ColorUtils.purple500(),
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
            modifier = Modifier.widthIn(max = 480.dp),
            title = {
                Text(
                    text = "Profile Information",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorUtils.purple500()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = profileData?.get("profilePictureUrl") ?: user?.photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.default_profile_picture)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Personal Information
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorUtils.purple500()
                        )
                        InfoRow("Full Name", profileData?.get("fullName")?.toString() ?: user?.displayName ?: "Not available", ColorUtils.blackcol())
                        InfoRow("Email", user?.email ?: "Not available", ColorUtils.blackcol())
                        InfoRow("Phone", profileData?.get("phoneNumber")?.toString() ?: "Not available", ColorUtils.blackcol())
                        InfoRow("Member Since", profileData?.get("memberSince")?.toString() ?: "Not available", ColorUtils.blackcol())
                        
                        // Address Information
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorUtils.purple500()
                        )
                        InfoRow("Street", profileData?.get("street")?.toString() ?: "Not available", ColorUtils.blackcol())
                        InfoRow("Barangay", profileData?.get("barangay")?.toString() ?: "Not available", ColorUtils.blackcol())
                        InfoRow("City", profileData?.get("city")?.toString() ?: "Not available", ColorUtils.blackcol())
                        
                        // Account Information
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorUtils.purple500()
                        )
                        InfoRow("Account Type", profileData?.get("authProvider")?.toString()?.replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase() else it.toString() 
                        } ?: "Email", ColorUtils.blackcol())
                        InfoRow("Email Verified", if (user?.isEmailVerified == true) "Yes" else "No", ColorUtils.blackcol())
                        InfoRow("Last Sign In", user?.metadata?.lastSignInTimestamp?.let { 
                            java.util.Date(it).toString() 
                        } ?: "Not available", ColorUtils.blackcol())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = ColorUtils.purple500())
                }
            }
        )
    }
}

@Composable
private fun RideHistoryContent(purple200: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sample ride history items - replace with actual data from your backend
        RideHistoryItem(
            bikeName = "Mountain Bike X1",
            date = "2024-03-15",
            duration = "45 minutes",
            cost = "$12.50",
            status = "Completed",
            purple200 = purple200
        )
        Divider()
        RideHistoryItem(
            bikeName = "City Cruiser C2",
            date = "2024-03-10",
            duration = "30 minutes",
            cost = "$8.00",
            status = "Completed",
            purple200 = purple200
        )
    }
}

@Composable
private fun RideHistoryItem(
    bikeName: String,
    date: String,
    duration: String,
    cost: String,
    status: String,
    purple200: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = bikeName,
            style = MaterialTheme.typography.titleMedium,
            color = purple200
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = purple200
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium,
                color = purple200
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cost,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = purple200
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == "Completed") Color.Green else Color.Red
            )
        }
    }
}

@Composable
private fun SettingsContent(
    navController: NavController, 
    viewModel: AuthViewModel,
    purple200: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsButton(
            icon = Icons.Default.Edit,
            text = "Edit Profile",
            onClick = { navController.navigate("editProfile") },
            purple200 = purple200
        )
        SettingsButton(
            icon = Icons.Default.Lock,
            text = "Change Password",
            onClick = { navController.navigate("changePassword") },
            purple200 = purple200
        )
        SettingsButton(
            icon = Icons.Default.Help,
            text = "Help & Support",
            onClick = { navController.navigate("help") },
            purple200 = purple200
        )
        Divider()
        SettingsButton(
            icon = Icons.Default.ExitToApp,
            text = "Sign Out",
            onClick = {
                viewModel.signOut()
                navController.navigate("signin") {
                    popUpTo("home") { inclusive = true }
                }
            },
            purple200 = purple200
        )
    }
}

@Composable
private fun SettingsButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    purple200: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
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
                if (uiState.isDeviceBlock) {
                    // For device blocks, use 24 hours as the display duration
                    24 * 60 * 60 * 1000L
                } else {
                    // For regular rate limits, use a shorter duration
                    (expireTimeMillis - System.currentTimeMillis()).coerceAtMost(3 * 60 * 1000L)
                }
            } else {
                if (uiState.isDeviceBlock) {
                    24 * 60 * 60 * 1000L
                } else {
                    3 * 60 * 1000L
                }
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
                
                // Only attempt to reset on regular rate limits
                // For device blocks, the user will need to wait the full duration
                if (!uiState.isDeviceBlock) {
                    // Reset the verification state when timer completes
                    viewModel.resetState()
                    
                    // Cleanup any expired rate limits from server to ensure state consistency
                    scope.launch {
                        delay(500) // Short delay to ensure state update completes
                        viewModel.checkAndCleanupExpiredRateLimits()
                    }
                } else {
                    // For device blocks, check if it's truly expired on the server
                    scope.launch {
                        viewModel.checkIfDeviceBlockExpired()
                    }
                }
                
                break
            }
            
            // Calculate progress ratio for visual indicator
            timeRemainingRatio = (remainingMillis.toFloat() / totalDurationMillis).coerceIn(0f, 1f)
            
            // Format the remaining time based on duration
            formattedTime = if (uiState.isDeviceBlock) {
                // For device blocks, show hours and minutes
                val hours = remainingMillis / (60 * 60 * 1000)
                val minutes = (remainingMillis % (60 * 60 * 1000)) / (60 * 1000)
                
                if (hours > 0) {
                    "$hours hr $minutes min"
                } else {
                    val seconds = (remainingMillis % (60 * 1000)) / 1000
                    "$minutes min $seconds sec"
                }
            } else {
                // For regular rate limits, show minutes and seconds
                val minutes = remainingMillis / (60 * 1000)
                val seconds = (remainingMillis % (60 * 1000)) / 1000
                
                if (minutes > 0) {
                    "$minutes min $seconds sec"
                } else {
                    "$seconds seconds"
                }
            }
            
            // Delay for 1 second before updating again
            delay(1000)
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (uiState.isDeviceBlock) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                else 
                    MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (uiState.isDeviceBlock) Icons.Default.Block else Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isDeviceBlock) "Device Temporarily Blocked" else "Verification Limit Reached",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Text(
                text = if (uiState.isDeviceBlock) 
                         "Your device has been temporarily blocked due to unusual verification activity. This is a security measure to prevent abuse."
                       else 
                         "You've reached the maximum number of verification attempts for this phone number.",
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
                        text = if (uiState.isDeviceBlock) "Block will expire in:" else "Try again in:",
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
                
                // Add help text for device blocks
                if (uiState.isDeviceBlock) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Why am I blocked?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Firebase has detected unusual verification activity from this device. This is usually caused by too many failed verification attempts in a short period.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "You can try again in 24 hours, or use a different device for verification.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
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

// Fix the VerificationContent composable to properly handle OTP verification
@Composable
private fun VerificationContent(
    uiState: PhoneAuthState,
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    onRetry: () -> Unit,
    onRecaptchaBypass: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: PhoneAuthViewModel,
    verificationAttemptedRef: MutableState<Boolean>
) {
    // Use state to track if we're checking rate limits and store error message
    var checkingRateLimit by remember { mutableStateOf(false) }
    var errorToCheck by remember { mutableStateOf<String?>(null) }
    
    // LaunchedEffect to handle rate limit checking
    LaunchedEffect(errorToCheck) {
        if (errorToCheck != null && !checkingRateLimit) {
            val errorMessage = errorToCheck ?: ""
            if (errorMessage.contains("Too many") || 
                errorMessage.contains("quota") || 
                errorMessage.contains("limit")) {
                
                checkingRateLimit = true
                
                val rateLimitResult = try {
                    viewModel.checkRateLimitStatus()
                } catch (e: Exception) {
                    // Fallback if checking fails: create a 3-minute mock
                    Pair(true, System.currentTimeMillis() + (3 * 60 * 1000))
                }
                
                val isRateLimited = rateLimitResult.first
                val expiryTime = rateLimitResult.second
                
                // Use the actual server-stored rate limit expiry time
                if (isRateLimited) {
                    val displayDuration = "3 minutes" // Default display
                    viewModel.resetState()
                    viewModel.setRateLimited(expiryTime, displayDuration)
                }
                
                checkingRateLimit = false
                errorToCheck = null
            }
        }
    }
    
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
                OtpInputSection(
                    otpValue = otpValue,
                    onOtpValueChange = onOtpChange,
                    phoneNumber = phoneNumber,
                    onResend = onRetry,
                    onVerify = { 
                        if (otpValue.length == 6) {
                            // Call verification with the entered code
                            viewModel.verifyPhoneNumberWithCode(otpValue)
                        }
                    }
                )
            }
            
            is PhoneAuthState.Error -> {
                val errorMessage = uiState.message
                // Set the error to check in state instead of launching directly
                if ((errorMessage.contains("Too many") || 
                    errorMessage.contains("quota") || 
                    errorMessage.contains("limit")) && 
                    errorToCheck == null && !checkingRateLimit) {
                    errorToCheck = errorMessage
                    LoadingSection()
                } else if (errorMessage.contains("missing a valid app identifier") ||
                    errorMessage.contains("Play Integrity") ||
                    errorMessage.contains("reCAPTCHA")) {
                    AppIdentifierErrorSection(
                        onRetry = onRetry,
                        onDismiss = onDismiss
                    )
                } else if (errorToCheck == null || !checkingRateLimit) {
                    ErrorSection(
                        errorMessage = errorMessage,
                        onRetry = onRetry
                    )
                } else {
                    // Show loading while checking
                    LoadingSection()
                }
            }
            
            is PhoneAuthState.RateLimited -> {
                RateLimitedSection(
                    onDismiss = onDismiss,
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            is PhoneAuthState.RecaptchaError -> {
                RecaptchaErrorSection(
                    onBypass = onRecaptchaBypass
                )
            }
            
            is PhoneAuthState.Success -> {
                SuccessSection()
                // Reset verification attempted flag
                verificationAttemptedRef.value = false
            }
            
            is PhoneAuthState.Initial, is PhoneAuthState.AppCheckError -> {
                // Just show a loading indicator for these states
                LoadingSection()
            }
        }
    }
}

// Add or update the OtpInputSection to include a verify function
@Composable
private fun OtpInputSection(
    otpValue: String,
    onOtpValueChange: (String) -> Unit,
    phoneNumber: String,
    onResend: () -> Unit,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enter the 6-digit verification code sent to:",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = "+63$phoneNumber",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // OTP input field
        OutlinedTextField(
            value = otpValue,
            onValueChange = onOtpValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Verification Code") },
            placeholder = { Text("6-digit code") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            leadingIcon = { Icon(Icons.Default.Pin, "Verification Code") },
            singleLine = true,
            trailingIcon = {
                if (otpValue.length == 6) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Valid Code",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        
        // Character counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${otpValue.length}/6",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Verify button
        Button(
            onClick = onVerify,
            modifier = Modifier.fillMaxWidth(),
            enabled = otpValue.length == 6
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verify")
        }
        
        // Resend link
        TextButton(
            onClick = onResend,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Resend Code")
        }
    }
}

// Fix the PhoneVerificationDialog to properly handle the reCAPTCHA retry
@Composable
fun PhoneVerificationDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    viewModel: PhoneAuthViewModel,
    activity: Activity,
    verificationAttemptedRef: MutableState<Boolean> = remember { mutableStateOf(false) },
    showVerifyPhoneDialogRef: MutableState<Boolean> = remember { mutableStateOf(false) },
    actuallyShowVerifyDialogRef: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var otpValue by remember { mutableStateOf("") }
    var localPhoneNumber by remember { 
        mutableStateOf(
            if (phoneNumber.startsWith("+63")) {
                phoneNumber.substring(3)
            } else if (phoneNumber.startsWith("63")) {
                phoneNumber.substring(2)
            } else {
                phoneNumber
            }
        ) 
    }
    var isPhoneNumberEntered by remember { mutableStateOf(phoneNumber.isNotBlank()) }
    var confirmingCancellation by remember { mutableStateOf(false) }
    var showingRecaptchaMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Optimization: Memoize the formatted phone number to avoid repeated string operations
    val formattedPhoneNumber by remember(localPhoneNumber) {
        derivedStateOf {
            if (localPhoneNumber.startsWith("+63")) {
                localPhoneNumber
            } else {
                "+63$localPhoneNumber"
            }
        }
    }
    
    // Memory optimization: Dispose WebView when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            // Clean up any resources when dialog is closed
            if (uiState is PhoneAuthState.RateLimited || uiState is PhoneAuthState.Error) {
                viewModel.resetState()
            }
        }
    }
    
    // Handle dialog dismissal
    BackHandler(enabled = uiState is PhoneAuthState.CodeSent || uiState is PhoneAuthState.Loading) {
        confirmingCancellation = true
    }
    
    // Monitor state transitions with stable key extraction
    val stateKey = when (uiState) {
        is PhoneAuthState.RecaptchaError -> "recaptcha_error"
        is PhoneAuthState.Error -> "error:${(uiState as PhoneAuthState.Error).message}"
        is PhoneAuthState.RateLimited -> "rate_limited:${(uiState as PhoneAuthState.RateLimited).expireTimeMillis}"
        is PhoneAuthState.CodeSent -> "code_sent"
        is PhoneAuthState.Success -> "success"
        is PhoneAuthState.Loading -> "loading"
        else -> "initial"
    }
    
    // Monitor state transitions with improved performance
    LaunchedEffect(stateKey) {
        when (uiState) {
            is PhoneAuthState.RecaptchaError -> {
                showingRecaptchaMessage = true
            }
            is PhoneAuthState.Error -> {
                val errorMessage = (uiState as PhoneAuthState.Error).message
                // Common pattern matching for reCAPTCHA-related errors 
                if (errorMessage.contains("reCAPTCHA", ignoreCase = true) ||
                    errorMessage.contains("INVALID_APP_CREDENTIAL", ignoreCase = true) ||
                    errorMessage.contains("Error: 39", ignoreCase = true) ||
                    errorMessage.contains("missing app identifier", ignoreCase = true)) {
                    showingRecaptchaMessage = true
                }
            }
            is PhoneAuthState.Success -> {
                // Reset verification flags on success
                verificationAttemptedRef.value = false
                showVerifyPhoneDialogRef.value = false
                actuallyShowVerifyDialogRef.value = false
                // Delay dismiss to allow success animation/feedback
                delay(800)
                onDismiss()
            }
            else -> {
                // Reset recaptcha message flag for non-error states
                if (uiState !is PhoneAuthState.Loading) {
                    showingRecaptchaMessage = false
                }
            }
        }
    }
    
    // Cancellation confirmation dialog
    if (confirmingCancellation) {
        AlertDialog(
            onDismissRequest = { confirmingCancellation = false },
            title = { Text("Cancel Verification?") },
            text = { Text("Are you sure you want to cancel phone verification?") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingCancellation = false
                        viewModel.resetState()
                        showVerifyPhoneDialogRef.value = false
                        actuallyShowVerifyDialogRef.value = false
                        onDismiss()
                    }
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingCancellation = false }) {
                    Text("Continue")
                }
            }
        )
    }
    
    // Special dialog for reCAPTCHA issues with improved error information
    if (showingRecaptchaMessage) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss on outside click */ },
            title = { 
                Text(
                    text = "reCAPTCHA Verification Issue", 
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            text = { 
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
                        text = "We're having trouble with the security verification. This is often due to browser security settings or network issues.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    // Additional troubleshooting info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Troubleshooting tips:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text("• Try using Chrome (most reliable)", style = MaterialTheme.typography.bodySmall)
                            Text("• Disable VPN, proxy or ad blockers", style = MaterialTheme.typography.bodySmall)
                            Text("• Try on cellular data instead of WiFi", style = MaterialTheme.typography.bodySmall)
                            Text("• Clear browser cookies and cache", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showingRecaptchaMessage = false
                        scope.launch {
                            try {
                                // Use the memoized phone number
                                delay(500) // Short delay to avoid race conditions
                                viewModel.retryWithoutRecaptcha(formattedPhoneNumber, activity)
                            } catch (e: Exception) {
                                Log.e("ProfileTab", "Error during retry: ${e.message}")
                                // If retry fails, reset to initial state after a delay
                                delay(500)
                                viewModel.resetState()
                            }
                        }
                    }
                ) {
                    Text("Try Alternative Method")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showingRecaptchaMessage = false
                        viewModel.resetState()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
        return
    }
    
    // Main verification dialog - optimized
    AlertDialog(
        onDismissRequest = {
            if (uiState is PhoneAuthState.CodeSent || uiState is PhoneAuthState.Loading) {
                confirmingCancellation = true
            } else {
                viewModel.resetState()
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Verify Phone Number",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isPhoneNumberEntered || uiState is PhoneAuthState.Initial) {
                    PhoneNumberInputSection(
                        phoneNumber = localPhoneNumber,
                        onPhoneNumberChange = { 
                            // Improved validation - only accept digits and limit to 10 digits
                            val filtered = it.replace(Regex("[^0-9]"), "")
                            if (filtered.length <= 10) {
                                localPhoneNumber = filtered
                            }
                        }
                    )
                } else {
                    VerificationContent(
                        uiState = uiState,
                        otpValue = otpValue,
                        onOtpChange = { 
                            // Only accept 6 digits
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                otpValue = it
                            }
                        },
                        phoneNumber = localPhoneNumber,
                        onRetry = { 
                            scope.launch {
                                try {
                                    if (isPhoneNumberEntered) {
                                        // Use the memoized phone number
                                        viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                    }
                                } catch (e: Exception) {
                                    Log.e("ProfileTab", "Error during retry: ${e.message}")
                                    showingRecaptchaMessage = true
                                }
                            }
                        },
                        onRecaptchaBypass = {
                            scope.launch {
                                try {
                                    viewModel.retryWithoutRecaptcha(formattedPhoneNumber, activity)
                                } catch (e: Exception) {
                                    Log.e("ProfileTab", "Error during recaptcha bypass: ${e.message}")
                                    viewModel.resetState()
                                    showingRecaptchaMessage = true
                                }
                            }
                        },
                        onDismiss = {
                            viewModel.resetState()
                            onDismiss()
                        },
                        viewModel = viewModel,
                        verificationAttemptedRef = verificationAttemptedRef
                    )
                }
            }
        },
        confirmButton = {
            when {
                !isPhoneNumberEntered || uiState is PhoneAuthState.Initial -> {
                    Button(
                        onClick = {
                            if (localPhoneNumber.length >= 9) {
                                isPhoneNumberEntered = true
                                
                                // Start verification when user clicks Continue
                                scope.launch {
                                    try {
                                        viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                    } catch (e: Exception) {
                                        Log.e("ProfileTab", "Error during verification start: ${e.message}")
                                        showingRecaptchaMessage = true
                                    }
                                }
                            }
                        },
                        enabled = localPhoneNumber.length >= 9
                    ) {
                        Text("Continue")
                    }
                }
                uiState is PhoneAuthState.CodeSent -> {
                    Button(
                        onClick = { 
                            if (otpValue.length == 6) {
                                Log.d("PhoneVerificationDialog", "Verifying code: $otpValue")
                                viewModel.verifyPhoneNumberWithCode(otpValue) 
                            }
                        },
                        enabled = otpValue.length == 6
                    ) {
                        Text("Verify")
                    }
                }
                uiState is PhoneAuthState.Success -> {
                    Button(onClick = { onDismiss() }) {
                        Text("Done")
                    }
                }
                uiState is PhoneAuthState.Error -> {
                    Button(
                        onClick = { 
                            val errorMessage = (uiState as PhoneAuthState.Error).message
                            if (errorMessage.contains("reCAPTCHA", ignoreCase = true) ||
                                errorMessage.contains("INVALID_APP_CREDENTIAL", ignoreCase = true) ||
                                errorMessage.contains("Error: 39", ignoreCase = true)) {
                                showingRecaptchaMessage = true
                            } else {
                                // For other errors, simply retry
                                scope.launch {
                                    try {
                                        viewModel.startPhoneNumberVerification(formattedPhoneNumber, activity)
                                    } catch (e: Exception) {
                                        Log.e("ProfileTab", "Error during retry: ${e.message}")
                                        showingRecaptchaMessage = true
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Try Again")
                    }
                }
                else -> { /* No button for other states */ }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (uiState is PhoneAuthState.CodeSent || uiState is PhoneAuthState.Loading) {
                        confirmingCancellation = true
                    } else {
                        viewModel.resetState()
                        onDismiss()
                    }
                }
            ) {
                Text("Cancel")
            }
        }
    )
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
                onValueChange = onPhoneNumberChange,
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



