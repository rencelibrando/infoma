package com.example.bikerental.screens.tabs

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.bikerental.viewmodels.PhoneAuthViewModel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import android.util.Log
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
import kotlin.text.get
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val phoneAuthViewModel: PhoneAuthViewModel = viewModel()
    val phoneAuthState = phoneAuthViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var verificationCode by remember { mutableStateOf("") }
    var isVerificationInProgress by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var user by remember { mutableStateOf<FirebaseUser?>(null) }
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showVerifyPhoneDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val purple500 = colorResource(id = R.color.purple_500)
    val blackcol = colorResource(id = R.color.black)
    var phoneNumber by remember { mutableStateOf("") }
    // Function to check if profile is complete

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
                !data[field].toString().isNullOrBlank()
            }
        } ?: false
    }

    fun isPhoneVerified(): Boolean {
        return profileData?.get("isPhoneVerified") as? Boolean ?: false
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
        refreshProfile()
    }

    // Effect to handle verification success
    LaunchedEffect(phoneAuthState.value) {
        when (phoneAuthState.value) {
            is PhoneAuthState.Success -> {
                showVerifyPhoneDialog = false
                isVerificationInProgress = false
                phoneNumber = ""
                verificationCode = ""
                // Refresh profile data after successful verification
                refreshProfile()
            }
            is PhoneAuthState.CodeSent -> {
                isVerificationInProgress = true
            }
            else -> {}
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
                        .clickable { showVerifyPhoneDialog = true },
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
                            onClick = { showVerifyPhoneDialog = true },
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
                                        else -> purple500
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
                            color = purple500
                        )
                        Text(
                            text = user?.email ?: "Not available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = blackcol
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
                                    else -> blackcol
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
                                !data[field].toString().isNullOrBlank()
                            }
                            completedFields.toFloat() / requiredFields.size
                        } ?: 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = purple500,
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
                                !data[field].toString().isNullOrBlank()
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
                        color = purple500
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RideHistoryContent(blackcol)
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
                        color = purple500
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsContent(navController, viewModel, blackcol)
                }
            }
        }

        // Add PullRefreshIndicator at the top center
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = purple500
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
                    color = purple500,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    // Phone Verification Dialog
    if (showVerifyPhoneDialog) {
        PhoneVerificationDialog(
            phoneNumber = profileData?.get("phoneNumber")?.toString()?.replace("+63", "") ?: "",
            onDismiss = { 
                showVerifyPhoneDialog = false
                isVerificationInProgress = false
            },
            viewModel = phoneAuthViewModel,
            activity = context as Activity
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
                    color = purple500
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
                            color = purple500
                        )
                        InfoRow("Full Name", profileData?.get("fullName")?.toString() ?: user?.displayName ?: "Not available", blackcol)
                        InfoRow("Email", user?.email ?: "Not available", blackcol)
                        InfoRow("Phone", profileData?.get("phoneNumber")?.toString() ?: "Not available", blackcol)
                        InfoRow("Member Since", profileData?.get("memberSince")?.toString() ?: "Not available", blackcol)
                        
                        // Address Information
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.titleMedium,
                            color = purple500
                        )
                        InfoRow("Street", profileData?.get("street")?.toString() ?: "Not available", blackcol)
                        InfoRow("Barangay", profileData?.get("barangay")?.toString() ?: "Not available", blackcol)
                        InfoRow("City", profileData?.get("city")?.toString() ?: "Not available", blackcol)
                        
                        // Account Information
                        Text(
                            text = "Account Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = purple500
                        )
                        InfoRow("Account Type", profileData?.get("authProvider")?.toString()?.replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase() else it.toString() 
                        } ?: "Email", blackcol)
                        InfoRow("Email Verified", if (user?.isEmailVerified == true) "Yes" else "No", blackcol)
                        InfoRow("Last Sign In", user?.metadata?.lastSignInTimestamp?.let { 
                            java.util.Date(it).toString() 
                        } ?: "Not available", blackcol)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = purple500)
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

// Add this new composable to handle rate limiting
@Composable
private fun RateLimitedSection(
    onDismiss: () -> Unit,
    uiState: PhoneAuthState.RateLimited
) {
    // Calculate initial remaining time
    val expireTimeMillis = uiState.expireTimeMillis
    
    // Create a state that will hold the formatted time
    var formattedTime by remember { mutableStateOf("") }
    
    // Create a timer that updates every second
    LaunchedEffect(expireTimeMillis) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val remainingMillis = expireTimeMillis - currentTime
            
            if (remainingMillis <= 0) {
                formattedTime = "0 seconds"
                break
            }
            
            // Format the remaining time
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
            
            // Custom countdown display with pulsating animation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = alpha),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Try again in:",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = formattedTime,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(alpha)
                )
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("OK")
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

// Update VerificationContent to handle this specific error
@Composable
private fun VerificationContent(
    uiState: PhoneAuthState,
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    onRetry: () -> Unit,
    onRecaptchaBypass: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    onResend = onRetry
                )
            }
            
            is PhoneAuthState.Error -> {
                val errorMessage = (uiState as PhoneAuthState.Error).message
                when {
                    errorMessage.contains("Too many") || 
                    errorMessage.contains("quota") || 
                    errorMessage.contains("limit") -> {
                        // Create a mock RateLimited state with current time + 5 min
                        val expiryTime = System.currentTimeMillis() + (5 * 60 * 1000)
                        RateLimitedSection(
                            onDismiss = onDismiss,
                            uiState = PhoneAuthState.RateLimited(expiryTime, "5 minutes")
                        )
                    }
                    errorMessage.contains("missing a valid app identifier") ||
                    errorMessage.contains("Play Integrity") ||
                    errorMessage.contains("reCAPTCHA") -> {
                        AppIdentifierErrorSection(
                            onRetry = onRetry,
                            onDismiss = onDismiss
                        )
                    }
                    else -> {
                        ErrorSection(
                            errorMessage = errorMessage,
                            onRetry = onRetry
                        )
                    }
                }
            }
            
            is PhoneAuthState.RecaptchaError -> {
                RecaptchaErrorSection(
                    onBypass = onRecaptchaBypass
                )
            }
            
            is PhoneAuthState.Success -> {
                SuccessSection()
            }
            
            PhoneAuthState.Initial -> {
                Text("Preparing verification...")
            }
            
            is PhoneAuthState.RateLimited -> {
                RateLimitedSection(
                    onDismiss = onDismiss,
                    uiState = uiState
                )
            }

            PhoneAuthState.AppCheckError -> {
                AppIdentifierErrorSection(
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun RecaptchaErrorSection(
    onBypass: () -> Unit
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
                text = "reCAPTCHA Verification Issue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "We're having trouble with the reCAPTCHA verification. This sometimes happens due to network issues or security settings.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = onBypass,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Try Direct Verification")
            }
        }
    }
}

// Then update the PhoneVerificationDialog function:

@Composable
fun PhoneVerificationDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    viewModel: PhoneAuthViewModel,
    activity: Activity
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
    var isPhoneNumberEntered by remember { mutableStateOf(phoneNumber.isNotEmpty()) }
    var confirmingCancellation by remember { mutableStateOf(false) }
    var showingRecaptchaMessage by remember { mutableStateOf(false) }
    
    // Cancel dialog handler
    BackHandler(enabled = uiState is PhoneAuthState.CodeSent || uiState is PhoneAuthState.Loading) {
        confirmingCancellation = true
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
    
    // reCAPTCHA info dialog
    if (showingRecaptchaMessage) {
        RecaptchaInfoDialog(
            phoneNumber = localPhoneNumber,
            onContinue = {
                showingRecaptchaMessage = false
                // Ensure the phone number starts with +63
                val formattedNumber = if (localPhoneNumber.startsWith("+63")) {
                    localPhoneNumber
                } else {
                    "+63${localPhoneNumber}"
                }
                viewModel.startPhoneNumberVerification(formattedNumber, activity, "GearTick")
            },
            onBack = {
                showingRecaptchaMessage = false
            }
        )
    }

    // Main verification dialog
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
                        onPhoneNumberChange = { localPhoneNumber = it }
                    )
                } else {
                    VerificationContent(
                        uiState = uiState,
                        otpValue = otpValue,
                        onOtpChange = { otpValue = it },
                        phoneNumber = localPhoneNumber,
                        onRetry = { showingRecaptchaMessage = true },
                        onRecaptchaBypass = {
                            viewModel.retryWithoutRecaptcha("+63$localPhoneNumber", activity)
                        },
                        onDismiss = {
                            viewModel.resetState()
                            onDismiss()
                        }
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
                                showingRecaptchaMessage = true
                            }
                        },
                        enabled = localPhoneNumber.length >= 9
                    ) {
                        Text("Continue")
                    }
                }
                uiState is PhoneAuthState.CodeSent -> {
                    Button(
                        onClick = { viewModel.verifyPhoneNumberWithCode(otpValue) },
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
                        onClick = { showingRecaptchaMessage = true }
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

@Composable
private fun PhoneNumberInputSection(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Enter your Philippine mobile number:",
            style = MaterialTheme.typography.bodyMedium
        )
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { value ->
                // Only accept digits and limit to 10 digits
                if (value.all { it.isDigit() } && value.length <= 10) {
                    // Only store the number part without the prefix
                    val cleanNumber = value.replace(Regex("^\\+?63"), "")
                    if (cleanNumber.isEmpty() || cleanNumber.startsWith("9")) {
                        onPhoneNumberChange(cleanNumber)
                    }
                }
            },
            visualTransformation = PhilippinesPhoneNumberTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            leadingIcon = {
                Text(
                    text = "+63",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            },
            label = { Text("Phone Number") },
            placeholder = { Text("9XXXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Enter a 10-digit number starting with 9",
            style = MaterialTheme.typography.bodySmall
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
private fun OtpInputSection(
    otpValue: String,
    onOtpValueChange: (String) -> Unit,
    phoneNumber: String,
    onResend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "GearTick sent a code to +63 $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = otpValue,
            onValueChange = { 
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    onOtpValueChange(it)
                }
            },
            label = { Text("Code") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            placeholder = { Text("6-digit code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onResend) {
                Text("Resend Code")
            }
        }
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


