package com.example.bikerental.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.bikerental.R
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// Data class for menu items
data class MenuItem(
    val id: Int,
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    openDrawer: () -> Unit,
    content: @Composable () -> Unit,
    navController: NavController? = null,
    viewModel: AuthViewModel? = null
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profilePictureUrl = currentUser?.photoUrl
    val userName = currentUser?.displayName ?: "User"
    val userEmail = currentUser?.email ?: ""
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    
    // State for profile data
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    // Use dark green for interactive elements
    val greenColor = ColorUtils.DarkGreen
    val warningColor = MaterialTheme.colorScheme.error
    
    // Define gray background color
    val grayBackground = Color(0xFFE0E0E0) // Light gray background
    
    // Load profile data for verification checks
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        profileData = document.data
                    }
                }
        }
    }
    
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

    // Function to check if phone is verified
    fun isPhoneVerified(): Boolean {
        return profileData?.get("isPhoneVerified") as? Boolean == true
    }
    
    // Performance optimization - pre-create menu items lists
    val mainNavigationItems = remember {
        listOf(
            MenuItem(
                id = 0,
                icon = Icons.Default.LocationOn,
                title = "Maps",
                onClick = { onItemSelected(0) }
            ),
            MenuItem(
                id = 1,
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                title = "Bikes",
                onClick = { onItemSelected(1) }
            ),
            MenuItem(
                id = 2,
                icon = Icons.Default.Bookmark,
                title = "Bookings",
                onClick = { onItemSelected(2) }
            )
        )
    }
    
    val secondaryNavigationItems = remember {
        listOf(
            MenuItem(
                id = 3,
                icon = Icons.Default.CreditCard,
                title = "Payments",
                onClick = { onItemSelected(3) }
            ),
            MenuItem(
                id = 5,
                icon = Icons.Default.History,
                title = "Ride History",
                onClick = { onItemSelected(5) }
            ),
            MenuItem(
                id = 6,
                icon = Icons.Default.Notifications,
                title = "Notifications",
                onClick = { /* Handle notifications */ },
                badgeCount = 3 // Example badge count
            )
        )
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
                drawerContainerColor = grayBackground,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // Use LazyColumn for better performance and scrollability
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with user info
                    item {
                        UserProfileHeader(
                            profilePictureUrl = profilePictureUrl,
                            userName = userName,
                            userEmail = userEmail,
                            greenColor = greenColor,
                            onManageAccountClick = { 
                                // Navigate to profile tab (index 4)
                                onItemSelected(4)
                                // Close the drawer after navigation
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        )
                        
                        // Stats with improved visual styling
                        UserStats(greenColor = greenColor)
                        
                        // Profile warnings
                        if (profileData != null) {
                            // Show warnings only if profile data is loaded
                            // Add email verification warning first
                            if (!(profileData?.get("isEmailVerified") as? Boolean ?: false)) {
                                CompactProfileWarningCard(
                                    icon = Icons.Default.Email,
                                    title = "Unverified Email",
                                    restrictionType = "email",
                                    onClick = {
                                        navController?.navigate("emailVerification")
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    },
                                    warningColor = warningColor,
                                    accentColor = greenColor
                                )
                            }
                            
                            if (!isProfileComplete()) {
                                CompactProfileWarningCard(
                                    icon = Icons.Default.Person,
                                    title = "Incomplete Profile",
                                    restrictionType = "profile",
                                    onClick = {
                                        onItemSelected(4)
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    },
                                    warningColor = warningColor,
                                    accentColor = greenColor
                                )
                            }
                            
                            if (profileData?.get("phoneNumber") != null && !isPhoneVerified()) {
                                CompactProfileWarningCard(
                                    icon = Icons.Default.Notifications,
                                    title = "Unverified Phone",
                                    restrictionType = "phone",
                                    onClick = {
                                        onItemSelected(4)
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    },
                                    warningColor = warningColor,
                                    accentColor = greenColor
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(thickness = 1.dp, color = greenColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Section header for navigation
                        SectionHeader(title = "NAVIGATION", greenColor = greenColor)
                    }
                    
                    // Main navigation items
                    items(mainNavigationItems) { item ->
                        EnhancedDrawerMenuItem(
                            icon = item.icon,
                            title = item.title,
                            selected = selectedItem == item.id,
                            onClick = item.onClick,
                            accentColor = greenColor
                        )
                    }
                    
                    // Secondary menu section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(thickness = 1.dp, color = greenColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = "MANAGE", greenColor = greenColor)
                    }
                    
                    // Secondary menu items
                    items(secondaryNavigationItems) { item ->
                        EnhancedDrawerMenuItem(
                            icon = item.icon,
                            title = item.title,
                            selected = false,
                            onClick = item.onClick,
                            accentColor = greenColor,
                            badgeCount = item.badgeCount
                        )
                    }
                    
                    // Help & Support item
                    item {
                        EnhancedDrawerMenuItem(
                            icon = Icons.Default.Help,
                            title = "Help & Support",
                            selected = false,
                            onClick = { 
                                scope.launch {
                                    drawerState.close()
                                }
                                navController?.navigate("help")
                            },
                            accentColor = greenColor
                        )
                    }
                    
                    // Sign Out and branding at bottom
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(thickness = 1.dp, color = greenColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Sign Out button
                        EnhancedDrawerMenuItem(
                            icon = Icons.Default.ExitToApp,
                            title = "Sign Out",
                            selected = false,
                            onClick = { 
                                // First sign out from the view model
                                viewModel?.signOut()
                                // Close drawer
                                scope.launch {
                                    drawerState.close()
                                }
                                // Navigate to initial login screen and clear the back stack
                                navController?.navigate("initial") {
                                    // Clear all screens from the back stack to prevent going back
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            accentColor = greenColor,
                            isSignOut = true
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Brand footer
                        BrandFooter(greenColor = greenColor)
                    }
                }
            }
        },
        content = content
    )
}

@Composable
fun UserProfileHeader(
    profilePictureUrl: android.net.Uri?,
    userName: String,
    userEmail: String,
    greenColor: Color,
    onManageAccountClick: () -> Unit = {}
) {
    // Animated elevation for card
    val elevation by animateDpAsState(
        targetValue = 4.dp,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
        label = "HeaderElevation"
    )
    
    // Interactive header with animation
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optimized image loading with cache and size specification
            if (profilePictureUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.bambikelogo)
                        .error(R.drawable.bambikelogo)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(Size(100, 100))
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Default profile image
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Profile",
                    modifier = Modifier
                        .size(50.dp)
                        .background(greenColor, CircleShape)
                        .padding(8.dp),
                    tint = Color.White
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                // User name with improved typography
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // User email
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                
                // Interactive element with visual feedback
                Text(
                    text = "Manage Account",
                    style = MaterialTheme.typography.bodySmall,
                    color = greenColor,
                    modifier = Modifier
                        .clickable { onManageAccountClick() }
                        .padding(top = 4.dp, end = 4.dp)
                        .semantics { contentDescription = "Navigate to profile tab" }
                )
            }
        }
    }
}

@Composable
fun UserStats(greenColor: Color) {
    var bikeCount by remember { mutableStateOf("0") }
    var distance by remember { mutableStateOf("0") }
    
    // Simulated data loading effect
    LaunchedEffect(Unit) {
        // In real implementation, fetch actual data
        kotlinx.coroutines.delay(800)
        bikeCount = "3"
        kotlinx.coroutines.delay(200)
        distance = "17.5"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Bike count stat with animation
        AnimatedStat(
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            value = bikeCount,
            label = "Bikes",
            greenColor = greenColor,
            modifier = Modifier.weight(1f)
        )
        
        // Distance stat with animation
        AnimatedStat(
            icon = Icons.Default.LocationOn,
            value = distance,
            label = "km",
            greenColor = greenColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AnimatedStat(
    icon: ImageVector,
    value: String,
    label: String,
    greenColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$value $label",
            style = MaterialTheme.typography.bodyMedium,
            color = greenColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionHeader(title: String, greenColor: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = greenColor.copy(alpha = 0.7f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDrawerMenuItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    isSignOut: Boolean = false,
    badgeCount: Int = 0
) {
    val animateBackground = animateDpAsState(
        targetValue = if (selected) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "MenuItemBackground"
    )

    NavigationDrawerItem(
        icon = { 
            if (badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge { 
                            Text(text = badgeCount.toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = title,
                        tint = if (isSignOut) Color.Red else if (selected) accentColor else Color.Black
                    )
                }
            } else {
                Icon(
                    imageVector = icon, 
                    contentDescription = title,
                    tint = if (isSignOut) Color.Red else if (selected) accentColor else Color.Black
                )
            }
        },
        label = { 
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSignOut) Color.Red else if (selected) accentColor else Color.Black,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = accentColor.copy(alpha = 0.12f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = accentColor,
            unselectedIconColor = if (isSignOut) Color.Red else Color.Black,
            selectedTextColor = accentColor,
            unselectedTextColor = if (isSignOut) Color.Red else Color.Black
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(animateBackground.value))
            .semantics { contentDescription = "Menu item: $title" }
    )
}

@Composable
fun BrandFooter(greenColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo and name in a visually balanced row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bambikelogo),
                contentDescription = "Bambike Logo",
                modifier = Modifier.size(32.dp),
                tint = Color.Black
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Bambike",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = greenColor
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Version info
        Text(
            text = "Version 1.0",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A compact warning card that displays a less intrusive warning message in the navigation drawer
 */
@Composable
fun CompactProfileWarningCard(
    icon: ImageVector,
    title: String,
    restrictionType: String,
    onClick: () -> Unit,
    warningColor: Color,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        color = warningColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = warningColor
                )
                
                Text(
                    text = when (restrictionType) {
                        "profile" -> "Some features restricted"
                        "phone" -> "Verification required"
                        "email" -> "Email verification needed"
                        else -> "Action required"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = warningColor.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Complete",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Keep the original ProfileWarningCard for use elsewhere if needed
@Composable
fun ProfileWarningCard(
    icon: ImageVector,
    title: String,
    message: String,
    onClick: () -> Unit,
    warningColor: Color,
    accentColor: Color
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
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
                tint = warningColor,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = warningColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = warningColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Complete Now",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 