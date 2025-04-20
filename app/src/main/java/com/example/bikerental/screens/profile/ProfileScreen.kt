import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.bikerental.models.User
import com.example.bikerental.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.CircularProgressIndicator as MaterialCircularProgressIndicator

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Get current user
    val user by viewModel.currentUser.collectAsState()
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    
    // Handle back button to maintain context
    LaunchedEffect(navController) {
        val navBackStackEntry = navController.currentBackStackEntry
        
        // Listen for back navigation events
        val callback = navBackStackEntry?.savedStateHandle?.get<Boolean>("returnToProfileTab")
        if (callback == true) {
            // Set this flag on the current entry to maintain state across navigations
            navController.currentBackStackEntry?.savedStateHandle?.set("returnToProfileTab", true)
        }
    }
    
    // Check if we have a valid user session
    LaunchedEffect(firebaseUser) {
        if (firebaseUser == null) {
            Log.d("ProfileScreen", "No Firebase user found, redirecting to login")
            navController.navigate("signin") {
                popUpTo("home") { inclusive = true }
            }
        } else {
            Log.d("ProfileScreen", "Firebase user authenticated: ${firebaseUser.uid}")
        }
    }
    
    // Only render the profile if we have a user
    if (firebaseUser != null && user != null) {
        // Render profile content
        ProfileContent(navController, user!!, viewModel)
    } else {
        // Show loading state while checking auth
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MaterialCircularProgressIndicator()
        }
    }
}

@Composable
private fun ProfileContent(
    navController: NavController,
    user: User,
    viewModel: AuthViewModel
) {
    // Ensure we have the latest Firebase Auth data
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val coroutineScope = rememberCoroutineScope()
    
    // Get email from multiple sources with fallbacks
    val userEmail = when {
        !user.email.isNullOrEmpty() -> user.email
        firebaseUser?.email != null -> firebaseUser.email
        else -> "No email available"
    }
    
    // If email is missing in User object but available in Firebase, update Firestore
    LaunchedEffect(firebaseUser?.email, user.email) {
        if (user.email.isNullOrEmpty() && !firebaseUser?.email.isNullOrEmpty()) {
            coroutineScope.launch {
                try {
                    // Update Firestore with the email from Firebase Auth
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.id)
                        .update("email", firebaseUser!!.email!!)
                        .await()
                        
                    Log.d("ProfileScreen", "Updated Firestore with email from Firebase Auth: ${firebaseUser.email}")
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to update email in Firestore: ${e.message}")
                }
            }
        }
    }
    
    // Navigate to edit profile
    val navigateToEditProfile = {
        Log.d("ProfileScreen", "Navigating to edit profile screen")
        navController.navigate("edit_profile")
    }
} 