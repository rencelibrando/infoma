package com.example.bikerental.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bikerental.models.AuthState
import com.example.bikerental.navigation.NavigationUtils
import com.example.bikerental.navigation.Screen
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.graphics.vector.ImageVector
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

/**
 * Admin Dashboard Screen
 * This screen is only accessible to admin users and provides management functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    
    // Verify the user is an admin
    LaunchedEffect(authState) {
        Log.d("AdminDashboardScreen", "Checking admin privileges, authState: $authState")
        
        if (authState !is AuthState.Authenticated) {
            Log.w("AdminDashboardScreen", "Not authenticated, redirecting to login")
            NavigationUtils.navigateToLogin(navController)
            return@LaunchedEffect
        }
        
        val authenticatedState = authState as AuthState.Authenticated
        Log.d("AdminDashboardScreen", "User: ${authenticatedState.user}")
        
        if (authenticatedState.user.isAdmin != true) {
            Log.w("AdminDashboardScreen", "User is not an admin, redirecting to login")
            Toast.makeText(
                context, 
                "Access denied: Admin privileges required", 
                Toast.LENGTH_LONG
            ).show()
            NavigationUtils.navigateToLogin(navController)
        } else {
            Log.i("AdminDashboardScreen", "Admin access confirmed for user: ${authenticatedState.user.email}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        NavigationUtils.navigateToLogin(navController)
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Admin header with welcome message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome, Admin ${currentUser?.fullName}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage your bike rental application from this dashboard",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Admin actions
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getAdminActions()) { action ->
                    AdminActionCard(
                        title = action.title,
                        description = action.description,
                        icon = action.icon,
                        onClick = { /* Handle action click */ }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Admin action data class
data class AdminAction(
    val title: String,
    val description: String,
    val icon: ImageVector
)

// Get list of admin actions
fun getAdminActions(): List<AdminAction> {
    return listOf(
        AdminAction(
            title = "Manage Bikes",
            description = "Add, edit, or remove bikes from the system",
            icon = Icons.Default.DirectionsBike
        ),
        AdminAction(
            title = "User Management",
            description = "View and manage user accounts",
            icon = Icons.Default.People
        ),
        AdminAction(
            title = "Booking Overview",
            description = "Monitor all current and upcoming bookings",
            icon = Icons.Default.DateRange
        ),
        AdminAction(
            title = "Analytics",
            description = "View usage statistics and revenue reports",
            icon = Icons.Default.BarChart
        ),
        AdminAction(
            title = "System Settings",
            description = "Configure app settings and preferences",
            icon = Icons.Default.Settings
        )
    )
} 