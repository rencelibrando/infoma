package com.example.bikerental.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bikerental.models.Review
import com.example.bikerental.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.NavController
import com.example.bikerental.navigation.Screen
import com.example.bikerental.utils.ProfileRestrictionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val DarkGreen = ColorUtils.DarkGreen

@Composable
fun ReviewsList(
    reviews: List<Review>,
    averageRating: Float,
    isLoading: Boolean,
    currentUserId: String? = null,
    onDeleteReview: ((Review) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Log for debugging review display
    Log.d("ReviewsList", "Displaying ${reviews.size} reviews with average rating $averageRating")
    
    // State for confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var reviewToDelete by remember { mutableStateOf<Review?>(null) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkGreen)
            }
        } else {
            // Average rating display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                RatingBar(
                    rating = averageRating,
                    modifier = Modifier.height(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${reviews.size} ${if (reviews.size == 1) "review" else "reviews"})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reviews yet. Be the first to review!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                // Log each review for debugging
                reviews.forEachIndexed { index, review ->
                    Log.d("ReviewsList", "Review $index: ID=${review.id}, Rating=${review.rating}, By=${review.userName}")
                }
                
                // Set a fixed height for the LazyColumn to prevent infinite height constraints
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp), // Fixed height to prevent layout issues
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reviews) { review ->
                        ReviewItem(
                            review = review,
                            isCurrentUserReview = currentUserId != null && review.userId == currentUserId,
                            onDeleteClick = if (onDeleteReview != null) {
                                {
                                    reviewToDelete = review
                                    showDeleteConfirmation = true
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
    
    // Confirmation dialog for deleting reviews
    if (showDeleteConfirmation && reviewToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                reviewToDelete = null
            },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete your review? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        reviewToDelete?.let { onDeleteReview?.invoke(it) }
                        showDeleteConfirmation = false
                        reviewToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmation = false
                        reviewToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReviewItem(
    review: Review, 
    modifier: Modifier = Modifier,
    isCurrentUserReview: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    // Log for debugging individual review rendering
    Log.d("ReviewItem", "Rendering review ${review.id} by ${review.userName}, isCurrentUserReview: $isCurrentUserReview")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with user name and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar and name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Simple avatar with first letter of username
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.userName.firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = review.userName,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // "You" badge for current user's reviews
                    if (isCurrentUserReview) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "You",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Date
                Text(
                    text = formatDate(review.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rating row with delete button for current user
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating stars
                RatingBar(
                    rating = review.rating,
                    modifier = Modifier.height(16.dp)
                )
                
                // Delete button (only for current user's reviews)
                if (isCurrentUserReview && onDeleteClick != null) {
                    TextButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete review",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Review comment
            Text(
                text = review.comment,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ReviewForm(
    onSubmit: (rating: Float, comment: String) -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier
) {
    var rating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Write a Review",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Star rating selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap to rate",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InteractiveRatingBar(
                rating = rating,
                onRatingChanged = { 
                    rating = it
                    validationError = null 
                },
                modifier = Modifier.height(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Comment text field
        OutlinedTextField(
            value = comment,
            onValueChange = { 
                comment = it
                validationError = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Share your experience with this bike...") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            isError = validationError != null
        )
        
        // Show validation error if present
        validationError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Submit button
        Button(
            onClick = { 
                if (rating <= 0) {
                    validationError = "Please rate the bike before submitting"
                } else if (comment.isBlank()) {
                    validationError = "Please provide a comment"
                } else {
                    validationError = null
                    onSubmit(rating, comment) 
                }
            },
            enabled = !isSubmitting && rating > 0 && comment.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Submit Review")
            }
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (i <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun InteractiveRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (i <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRatingChanged(i.toFloat()) }
            )
        }
    }
}

@Composable
fun ReviewSection(
    bikeId: String,
    showForm: Boolean,
    reviews: List<Review>,
    averageRating: Float,
    isLoading: Boolean,
    isSubmitting: Boolean,
    currentUserId: String? = null,
    onToggleForm: () -> Unit,
    onSubmitReview: (rating: Float, comment: String) -> Unit,
    onDeleteReview: ((Review) -> Unit)? = null,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    // Log for debugging visibility of review section
    Log.d("ReviewSection", "Rendering ReviewSection for bike $bikeId with ${reviews.size} reviews")
    
    // Load user data to check restrictions
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingUserData by remember { mutableStateOf(true) }
    
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
                    }
                    isLoadingUserData = false
                }
                .addOnFailureListener {
                    isLoadingUserData = false
                }
        } else {
            isLoadingUserData = false
        }
    }
    
    // Check if the write_review feature is restricted
    val isRestricted = ProfileRestrictionUtils.isFeatureRestricted("write_review", userData)
    val restrictionMessage = ProfileRestrictionUtils.getRestrictionMessage("write_review", userData)
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reviews",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (isLoadingUserData) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DarkGreen,
                    strokeWidth = 2.dp
                )
            } else if (isRestricted) {
                Button(
                    onClick = { navController?.navigate(Screen.IdVerification.route) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify ID")
                }
            } else {
            Button(
                onClick = onToggleForm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showForm) MaterialTheme.colorScheme.surfaceVariant else DarkGreen,
                    contentColor = if (showForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (showForm) "Cancel" else "Write Review")
                }
            }
        }
        
        Divider()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show restriction message if trying to write a review but restricted
        if (showForm && isRestricted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "ID Verification Required",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "ID Verification Required",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = restrictionMessage,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { navController?.navigate(Screen.IdVerification.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Verify ID Now")
                    }
                }
            }
        }
        
        // Review form (only shown if user passes ID verification)
        AnimatedVisibility(
            visible = showForm && !isRestricted,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            ReviewForm(
                onSubmit = onSubmitReview,
                isSubmitting = isSubmitting
            )
        }
        
        // Always show reviews list if not showing form
        AnimatedVisibility(
            visible = !showForm || isRestricted,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            ReviewsList(
                reviews = reviews,
                averageRating = averageRating,
                isLoading = isLoading,
                currentUserId = currentUserId,
                onDeleteReview = onDeleteReview
            )
        }
    }
}

// Helper function to format timestamp
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(date)
} 