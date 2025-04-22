package com.example.bikerental.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bikerental.models.Booking
import com.example.bikerental.models.BookingWithBikeDetails
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.viewmodels.BookingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BookingsTab(
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    bookingViewModel: BookingViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Collect bookings state
    val bookings by bookingViewModel.bookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val error by bookingViewModel.error.collectAsState()
    
    // Selected booking for detailed view
    var selectedBooking by remember { mutableStateOf<BookingWithBikeDetails?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            scope.launch {
                bookingViewModel.refreshBookings()
            }
        }
    )
    
    // Fetch bookings when screen is shown
    LaunchedEffect(Unit) {
        bookingViewModel.refreshBookings()
        bookingViewModel.setupBookingsRealtimeUpdates()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Your Bookings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (isLoading && bookings.isEmpty()) {
                // Shimmer loading animation
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(3) {
                        BookingCardShimmer()
                    }
                }
            } else if (error != null && bookings.isEmpty()) {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Error loading bookings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (bookings.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "No Bookings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Active Bookings",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Your booking history will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { navController?.navigate("bikes") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Book a Bike")
                        }
                    }
                }
            } else {
                // Bookings list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bookings) { booking ->
                        BookingCard(
                            booking = booking,
                            onClick = {
                                selectedBooking = booking
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
        }
        
        // Pull refresh indicator
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = DarkGreen,
            scale = true
        )
    }
    
    // Booking details bottom sheet
    if (showBottomSheet && selectedBooking != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false
                selectedBooking = null
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 8.dp,
            dragHandle = { 
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            BookingDetailSheet(
                booking = selectedBooking!!,
                onViewBikeDetails = { bikeId ->
                    navController?.navigate("bikeDetail/$bikeId")
                    showBottomSheet = false
                },
                onCancel = { bookingId ->
                    // Implement booking cancellation
                    bookingViewModel.cancelBooking(
                        bookingId = bookingId,
                        onSuccess = {
                            showBottomSheet = false
                        },
                        onError = { errorMessage ->
                            // Show error message
                            // In a real app, you would show a snackbar or toast
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingWithBikeDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bike image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(booking.bikeImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = booking.bikeName,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DarkGreen,
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = booking.bikeName.firstOrNull()?.toString() ?: "B",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
            
            // Booking details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Bike name and booking type badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bike name
                    Text(
                        text = booking.bikeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Booking type badge
                    if (booking.isHourly) {
                        Surface(
                            color = DarkGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "Hourly",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Status chip
                val statusColor = when (booking.status) {
                    "PENDING", "CONFIRMED" -> DarkGreen // Green
                    "COMPLETED" -> Color(0xFF2196F3) // Blue
                    "CANCELLED" -> Color(0xFFE53935) // Red
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = booking.status.capitalize(),
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Date range
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val startDateStr = dateFormat.format(booking.startDate)
                val endDateStr = dateFormat.format(booking.endDate)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$startDateStr - $endDateStr",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Show time range for hourly bookings
                if (booking.isHourly) {
                    val timeRange = booking.getTimeRange()
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeRange,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Duration and price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration
                    Text(
                        text = booking.getFormattedDuration(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Price
                    Text(
                        text = booking.totalPrice ?: "N/A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                }
            }
        }
    }
}

@Composable
fun BookingDetailSheet(
    booking: BookingWithBikeDetails,
    onViewBikeDetails: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val startDateStr = dateFormat.format(booking.startDate)
    val endDateStr = dateFormat.format(booking.endDate)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    // Get formatted time for hourly bookings
    val startTimeStr = if (booking.isHourly) timeFormat.format(booking.startDate) else ""
    val endTimeStr = if (booking.isHourly) timeFormat.format(booking.endDate) else ""
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title
        Text(
            text = "Booking Details",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Bike image and name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bike image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(booking.bikeImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = booking.bikeName,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Bike details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = booking.bikeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    
                    // Booking type badge
                    if (booking.isHourly) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = DarkGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Hourly",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = booking.bikeType ?: "Electric Bike",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Status chip
                val statusColor = when (booking.status) {
                    "PENDING", "CONFIRMED" -> DarkGreen // Green
                    "COMPLETED" -> Color(0xFF2196F3) // Blue
                    "CANCELLED" -> Color(0xFFE53935) // Red
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = booking.status.capitalize(),
                        fontSize = 14.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        // Booking details card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailItem(title = "Booking ID", value = booking.id.take(8) + "...")
                DetailItem(title = "Start Date", value = startDateStr)
                DetailItem(title = "End Date", value = endDateStr)
                
                if (booking.isHourly) {
                    DetailItem(title = "Start Time", value = startTimeStr)
                    DetailItem(title = "End Time", value = endTimeStr)
                }
                
                DetailItem(
                    title = "Duration", 
                    value = booking.getFormattedDuration()
                )
                
                DetailItem(
                    title = "Price", 
                    value = booking.totalPrice ?: "N/A",
                    valueColor = DarkGreen
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // View Bike Details button
            OutlinedButton(
                onClick = { onViewBikeDetails(booking.bikeId) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DarkGreen
                )
            ) {
                Text("View Bike")
            }
            
            // Cancel booking button (only for active bookings)
            if (booking.status == "PENDING" || booking.status == "CONFIRMED") {
                Button(
                    onClick = { onCancel(booking.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            } else {
                // Rebook button for completed/cancelled bookings
                Button(
                    onClick = { onViewBikeDetails(booking.bikeId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Rebook")
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun BookingCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shimmer image placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Shimmer content placeholders
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(80.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(150.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
} 