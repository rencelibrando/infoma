package com.example.bikerental.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bikerental.R
import com.example.bikerental.utils.ColorUtils
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
    
    // Get the app's primary purple color
    val purpleColor = ColorUtils.Purple40
    
    // Create gradient background for drawer
    val gradientColors = listOf(
        purpleColor,
        MaterialTheme.colorScheme.primaryContainer
    )
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // User profile section at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile picture
                    if (profilePictureUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profilePictureUrl)
                                .crossfade(true)
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
                                .background(purpleColor, CircleShape)
                                .padding(8.dp),
                            tint = Color.White
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        // User name
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = purpleColor
                        )
                        
                        // User email
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = purpleColor.copy(alpha = 0.8f)
                        )
                        
                        // View Profile text
                        Text(
                            text = "View Profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = purpleColor,
                            modifier = Modifier.clickable { 
                                // Navigate to profile tab
                                onItemSelected(3) 
                            }
                        )
                    }
                }
                
                // Stats summary row - bikes and distance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Bike count stat
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = " 0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = purpleColor
                        )
                    }
                    
                    // Distance stat
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = " 0 km",
                            style = MaterialTheme.typography.bodyMedium,
                            color = purpleColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(thickness = 1.dp, color = purpleColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                
                // Main navigation tabs
                DrawerMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Maps",
                    selected = selectedItem == 0,
                    onClick = { onItemSelected(0) },
                    purpleColor = purpleColor
                )
                
                DrawerMenuItem(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    title = "BikeTab",
                    selected = selectedItem == 1,
                    onClick = { onItemSelected(1) },
                    purpleColor = purpleColor
                )
                
                DrawerMenuItem(
                    icon = Icons.Default.Person,
                    title = "ProfileTab",
                    selected = selectedItem == 3,
                    onClick = { onItemSelected(3) },
                    purpleColor = purpleColor
                )
                
                Divider(thickness = 1.dp, color = purpleColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                
                // Additional menu items
                DrawerMenuItem(
                    icon = Icons.Default.CreditCard,
                    title = "Payments",
                    selected = false,
                    onClick = { /* Handle payments */ },
                    purpleColor = purpleColor
                )
                
                DrawerMenuItem(
                    icon = Icons.Default.History,
                    title = "Ride History",
                    selected = false,
                    onClick = { /* Handle ride history */ },
                    purpleColor = purpleColor
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Sign Out button
                Divider(thickness = 1.dp, color = purpleColor.copy(alpha = 0.2f))
                DrawerMenuItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Sign Out",
                    selected = false,
                    onClick = {
                        if (viewModel != null && navController != null) {
                            viewModel.signOut()
                            scope.launch {
                                drawerState.close()
                                navController.navigate("signin") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }
                    },
                    purpleColor = purpleColor,
                    isSignOut = true
                )
                
                // Lakbike branding at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo and name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bambikelogo),
                            contentDescription = "bambike Logo",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "bambike",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = purpleColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Version info
                    Text(
                        text = "Version 1.7.7 (2|P)",
                        style = MaterialTheme.typography.bodySmall,
                        color = purpleColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    purpleColor: Color,
    isSignOut: Boolean = false
) {
    NavigationDrawerItem(
        icon = { 
            Icon(
                imageVector = icon, 
                contentDescription = title,
                tint = if (isSignOut) purpleColor else Color.Black
            ) 
        },
        label = { 
            Text(
                text = title,
                color = if (selected) purpleColor else purpleColor.copy(alpha = 0.8f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = purpleColor.copy(alpha = 0.12f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = if (isSignOut) purpleColor else Color.Black,
            unselectedIconColor = if (isSignOut) purpleColor else Color.Black,
            selectedTextColor = purpleColor,
            unselectedTextColor = purpleColor.copy(alpha = 0.8f)
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
} 