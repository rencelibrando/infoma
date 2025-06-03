package com.example.bikerental.screens.tabs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.bikerental.BuildConfig
import com.example.bikerental.models.Bike
import com.example.bikerental.models.Booking
import com.example.bikerental.models.BookingStatus
import com.example.bikerental.models.BookingWithBikeDetails
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.viewmodels.BikeViewModel
import com.example.bikerental.viewmodels.BookingViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Booking filter categories
enum class BookingCategory(val displayName: String) {
    ALL("All Bookings"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BookingsTab(
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    bookingViewModel: BookingViewModel = viewModel(),
    bikeId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Create dedicated scopes for background operations
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val computeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // UI State for new booking flow
    var showBookingForm by remember { mutableStateOf(bikeId != null) }
    var selectedBikeType by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var selectedDuration by remember { mutableStateOf<String?>(null) }
    
    // Search and filtering state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(BookingCategory.ALL) }
    
    // Collect bookings state
    val bookings by bookingViewModel.bookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val error by bookingViewModel.error.collectAsState()
    
    // Category counts state
    var allCount by remember { mutableStateOf(0) }
    var activeCount by remember { mutableStateOf(0) }
    var completedCount by remember { mutableStateOf(0) }
    var cancelledCount by remember { mutableStateOf(0) }
    
    // Filter bookings based on search query and selected category
    val filteredBookings by remember(bookings, searchQuery, selectedCategory) {
        derivedStateOf {
            computeScope.launch {
                // Calculate counts on a background thread
                val filtered = bookings.filter { booking ->
                    // First apply category filter
                    val categoryMatch = when (selectedCategory) {
                        BookingCategory.ALL -> true
                        BookingCategory.ACTIVE -> booking.status == "PENDING" || booking.status == "CONFIRMED"
                        BookingCategory.COMPLETED -> booking.status == "COMPLETED"
                        BookingCategory.CANCELLED -> booking.status == "CANCELLED"
                    }
                    
                    // Then apply search filter if there's a query
                    val searchMatch = if (searchQuery.isBlank()) {
                        true
                    } else {
                        val query = searchQuery.lowercase()
                        booking.bikeName.lowercase().contains(query) ||
                                booking.userName.lowercase().contains(query) ||
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(booking.startDate).lowercase().contains(query)
                    }
                    
                    // Item must match both filters
                    categoryMatch && searchMatch
                }
                
                // Update counts
                val newAllCount = bookings.size
                val newActiveCount = bookings.count { it.status == "PENDING" || it.status == "CONFIRMED" }
                val newCompletedCount = bookings.count { it.status == "COMPLETED" }
                val newCancelledCount = bookings.count { it.status == "CANCELLED" }
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    allCount = newAllCount
                    activeCount = newActiveCount
                    completedCount = newCompletedCount
                    cancelledCount = newCancelledCount
                }
            }
            
            // Return filtered bookings
            bookings.filter { booking ->
                // Apply category filter
                val categoryMatch = when (selectedCategory) {
                    BookingCategory.ALL -> true
                    BookingCategory.ACTIVE -> booking.status == "PENDING" || booking.status == "CONFIRMED"
                    BookingCategory.COMPLETED -> booking.status == "COMPLETED"
                    BookingCategory.CANCELLED -> booking.status == "CANCELLED"
                }
                
                // Apply search filter
                val searchMatch = if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.lowercase()
                    booking.bikeName.lowercase().contains(query) ||
                            booking.userName.lowercase().contains(query) ||
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(booking.startDate).lowercase().contains(query)
                }
                
                // Return combined result
                categoryMatch && searchMatch
            }
        }
    }
    
    // Selected booking for detailed view
    var selectedBooking by remember { mutableStateOf<BookingWithBikeDetails?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            ioScope.launch {
                bookingViewModel.fetchUserBookings()
            }
        }
    )
    
    // Fetch bookings when screen is shown - use background thread
    LaunchedEffect(Unit) {
        ioScope.launch {
            bookingViewModel.fetchUserBookings()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (showBookingForm) {
            BookingForm(
                bikeId = bikeId,
                onBookingComplete = {
                    showBookingForm = false
                    // Refresh bookings list
                    ioScope.launch {
                        bookingViewModel.fetchUserBookings()
                    }
                },
                onCancel = {
                    showBookingForm = false
                }
            )
        } else {
            // Existing bookings list with pull-to-refresh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                if (isLoading && bookings.isEmpty()) {
                    // Show loading indicator while initializing
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DarkGreen)
                    }
                } else if (error != null && bookings.isEmpty()) {
                    // Show error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Error loading bookings",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    ioScope.launch {
                                        bookingViewModel.fetchUserBookings()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkGreen
                                )
                            ) {
                                Text("Try Again")
                            }
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
                                onClick = { showBookingForm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkGreen
                                )
                            ) {
                                Text("Book a Bike")
                            }
                        }
                    }
                } else {
                    // List of bookings
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Add search bar at the top
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            placeholder = { Text("Search bookings...") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        // Category tabs with counts
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(BookingCategory.values()) { category ->
                                val count = when (category) {
                                    BookingCategory.ALL -> allCount
                                    BookingCategory.ACTIVE -> activeCount
                                    BookingCategory.COMPLETED -> completedCount
                                    BookingCategory.CANCELLED -> cancelledCount
                                }
                                CategoryCard(
                                    category = category,
                                    isSelected = selectedCategory == category,
                                    count = count,
                                    onSelect = { selectedCategory = category }
                                )
                            }
                        }
                        
                        // Add a "Book a Bike" button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showBookingForm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Book a Bike")
                            }
                        }

                        // Debug buttons (only in debug builds)
                        if (BuildConfig.DEBUG) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        bookingViewModel.debugListAllBookings { result ->
                                            Toast.makeText(context, "Check logs for booking details", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Debug: List All", fontSize = 10.sp)
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        bookingViewModel.refreshBookings()
                                        Toast.makeText(context, "Refreshing bookings...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Force Refresh", fontSize = 10.sp)
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        bookingViewModel.checkUserPermissions { result ->
                                            Toast.makeText(context, "Check logs for permissions", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Check Perms", fontSize = 10.sp)
                                }
                            }
                        }

                        // Show filtered bookings or empty state
                        if (filteredBookings.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "No matching bookings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = "No matching bookings found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Try adjusting your search or filters",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Show filtered bookings list
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredBookings) { booking ->
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
                }

                // Pull refresh indicator with custom styling
                PullRefreshIndicator(
                    refreshing = isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = DarkGreen,
                    scale = true
                )
            }

            // Booking details sheet
            if (showBottomSheet && selectedBooking != null) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        showBottomSheet = false
                        selectedBooking = null 
                    },
                    sheetState = bottomSheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    BookingDetailSheet(
                        booking = selectedBooking!!,
                        onViewBikeDetails = { bikeId ->
                            navController?.navigate("bike_details/$bikeId")
                            showBottomSheet = false
                            selectedBooking = null
                        },
                        onCancel = { bookingId ->
                            scope.launch {
                                bookingViewModel.cancelBooking(
                                    bookingId = bookingId,
                                    onSuccess = {
                                        showBottomSheet = false
                                        selectedBooking = null
                                        Toast.makeText(context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMessage ->
                                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingForm(
    bikeId: String? = null,
    onBookingComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BikeViewModel = viewModel(),
    bookingViewModel: BookingViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ioScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val computeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // State for bike selection
    val bikes by viewModel.bikes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedBike by remember { mutableStateOf<Bike?>(null) }
    var isCreatingBooking by remember { mutableStateOf(false) }
    
    // Date selection
    val currentMonth = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    
    // Time selection
    val timeSlots = remember { listOf(
        "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
        "14:00", "15:00", "16:00", "17:00", "18:00", "19:00"
    )}
    var selectedTime by remember { mutableStateOf<String?>(null) }
    
    // Duration options
    val durationOptions = remember { listOf(
        DurationOption("1 Hour", "$10", 1),
        DurationOption("2 Hours", "$18", 2),
        DurationOption("3 Hours", "$24", 3),
        DurationOption("4 Hours", "$32", 4),
        DurationOption("6 Hours", "$45", 6),
        DurationOption("Full Day", "$60", 8)
    )}
    var selectedDuration by remember { mutableStateOf<DurationOption?>(null) }
    
    // Pre-computed price value
    var estimatedPrice by remember { mutableStateOf("₱0.00") }
    
    // Calculate estimated price in a coroutine when inputs change
    LaunchedEffect(selectedBike, selectedDuration) {
        computeScope.launch {
            val calculatedPrice = if (selectedBike != null && selectedDuration != null) {
                val hourlyRate = selectedBike!!.priceValue
                val hours = selectedDuration!!.hours
                String.format("₱%.2f", hourlyRate * hours)
            } else {
                "₱0.00"
            }
            
            withContext(Dispatchers.Main) {
                estimatedPrice = calculatedPrice
            }
        }
    }

    // Load bikes if needed
    LaunchedEffect(Unit) {
        if (bikes.isEmpty()) {
            ioScope.launch {
                viewModel.fetchBikesFromFirestore()
            }
        }
    }

    // Load specific bike if bikeId is provided
    LaunchedEffect(bikeId, bikes) {
        if (bikeId != null) {
            // If bikes are already loaded, find the bike
            val bike = bikes.find { it.id == bikeId }
            if (bike != null) {
                selectedBike = bike
            } else {
                // Otherwise fetch the specific bike
                ioScope.launch {
                    viewModel.getBikeById(bikeId)
                }
            }
        }
    }

    // Listen for selected bike updates
    val viewModelSelectedBike by viewModel.selectedBike.collectAsState()
    LaunchedEffect(viewModelSelectedBike) {
        viewModelSelectedBike?.let {
            selectedBike = it
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = "Book Your BambiBike",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Choose Your BambiBike section
        Text(
            text = "Choose Your BambiBike",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Display bike selection or loading state
        if (isLoading && bikes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkGreen)
            }
        } else if (error != null && bikes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Failed to load bikes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { 
                            ioScope.launch {
                                viewModel.fetchBikesFromFirestore()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            // Display available bikes
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bikes.filter { it.isAvailable }) { bike ->
                    BikeSelectionCard(
                        bike = bike,
                        isSelected = selectedBike?.id == bike.id,
                        onSelect = { selectedBike = bike }
                    )
                }
            }
        }

        // Calendar
        Text(
            text = "Select Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        CustomCalendar(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )
        
        // Time Selection
        Text(
            text = "Select Time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeSlots) { time ->
                TimeSlot(
                    time = time,
                    isSelected = selectedTime == time,
                    onSelect = { selectedTime = time }
                )
            }
        }
        
        // Rental Duration
        Text(
            text = "Rental Duration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        // Duration options in a grid layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(160.dp)
        ) {
            items(durationOptions) { option ->
                DurationCard(
                    durationOption = option,
                    isSelected = selectedDuration == option,
                    onSelect = { selectedDuration = option }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Show estimated price
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Total",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = estimatedPrice,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkGreen)
            ) {
                Text("Cancel", color = DarkGreen)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = {
                    if (selectedBike != null && selectedDate != null && selectedTime != null && selectedDuration != null) {
                        isCreatingBooking = true
                        
                        // Run date processing and booking creation in IO thread
                        ioScope.launch {
                            try {
                                // Parse the selected time
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val parsedTime = timeFormat.parse(selectedTime!!)
                                
                                // Combine date and time
                                val calendar = Calendar.getInstance()
                                calendar.time = selectedDate!!
                                
                                val timeCalendar = Calendar.getInstance()
                                timeCalendar.time = parsedTime!!
                                
                                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                
                                val startDateTime = calendar.time
                                
                                // Calculate end time based on duration
                                val endCalendar = calendar.clone() as Calendar
                                endCalendar.add(Calendar.HOUR, selectedDuration!!.hours)
                                val endDateTime = endCalendar.time
                                
                                // Get user information
                                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "You must be logged in to make a booking", Toast.LENGTH_SHORT).show()
                                        isCreatingBooking = false
                                    }
                                    return@launch
                                }
                                
                                val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
                                
                                // Create the booking object
                                val booking = Booking.createHourly(
                                    bikeId = selectedBike!!.id,
                                    userId = userId,
                                    userName = userName,
                                    startDate = startDateTime,
                                    endDate = endDateTime,
                                    pricePerHour = selectedBike!!.priceValue
                                )
                                
                                // Create booking in Firestore (this is already handled in IO dispatcher by BookingViewModel)
                                bookingViewModel.createBooking(
                                    booking = booking,
                                    onSuccess = {
                                        // Run this callback on main thread directly without suspend call
                                        Toast.makeText(context, "Booking confirmed!", Toast.LENGTH_SHORT).show()
                                        isCreatingBooking = false
                                        onBookingComplete()
                                    },
                                    onError = { error ->
                                        // Run this callback on main thread directly without suspend call
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                        isCreatingBooking = false
                                    }
                                )
                            } catch (e: Exception) {
                                // Use launch(Dispatchers.Main) instead of withContext since we're already in a coroutine
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Error creating booking: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    isCreatingBooking = false
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please complete all booking details", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = selectedBike != null && selectedDate != null && selectedTime != null && selectedDuration != null && !isCreatingBooking,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isCreatingBooking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Text("Confirm Booking")
                }
            }
        }
    }
}

@Composable
fun BikeSelectionCard(
    bike: Bike,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine image model with memory and disk caching optimizations
    val imageModel = ImageRequest.Builder(LocalContext.current)
        .data(bike.imageUrl)
        .crossfade(true)
        .size(width = 112, height = 112) // Target size to save memory
        .memoryCacheKey(bike.id + "_thumb")
        .diskCacheKey(bike.id + "_thumb")
        .build()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) DarkGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bike image with optimized loading
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = bike.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DarkGreen,
                            strokeWidth = 1.5.dp
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
                            text = bike.name.firstOrNull()?.toString() ?: "B",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = bike.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                
                Text(
                    text = bike.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.6f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rating stars - using the bike's actual rating
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < bike.rating) DarkGreen else DarkGreen.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (bike.isAvailable) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Available",
                            tint = DarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkGreen
                        )
                    }
                }
            }
            
            // Price
            Text(
                text = bike.price,
                style = MaterialTheme.typography.titleMedium,
                color = DarkGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CustomCalendar(
    currentMonth: Calendar,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val computeScope = rememberCoroutineScope()
    var formattedMonth by remember { mutableStateOf("") }
    var calendarDays by remember { mutableStateOf<List<CalendarDay>>(emptyList()) }
    
    // Calculate calendar days when the month changes
    LaunchedEffect(currentMonth.time) {
        computeScope.launch(Dispatchers.Default) {
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val formattedMonthText = dateFormat.format(currentMonth.time)
            
            // Calculate calendar days for the month
            val cal = currentMonth.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val days = mutableListOf<CalendarDay>()
            
            // Add empty slots for beginning of month
            for (i in 0 until firstDayOfWeek) {
                days.add(CalendarDay(date = null, isInMonth = false))
            }
            
            // Add actual days
            for (i in 1..maxDays) {
                cal.set(Calendar.DAY_OF_MONTH, i)
                val date = cal.time
                days.add(CalendarDay(date = date, isInMonth = true))
            }
            
            withContext(Dispatchers.Main) {
                formattedMonth = formattedMonthText
                calendarDays = days
            }
        }
    }
    
    // Update calendar days when the selected date changes
    LaunchedEffect(selectedDate, calendarDays) {
        if (selectedDate != null && calendarDays.isNotEmpty()) {
            computeScope.launch(Dispatchers.Default) {
                val selectedDay = Calendar.getInstance().apply { time = selectedDate }
                
                // Update calendar days with selection state
                val updatedDays = calendarDays.map { day ->
                    if (day.date != null && isSameDay(day.date, selectedDate)) {
                        day.copy(isSelected = true)
                    } else {
                        day.copy(isSelected = false)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    calendarDays = updatedDays
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row {
                IconButton(
                    onClick = {
                        currentMonth.add(Calendar.MONTH, -1)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Month"
                    )
                }
                
                IconButton(
                    onClick = {
                        currentMonth.add(Calendar.MONTH, 1)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Month"
                    )
                }
            }
        }
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = remember { listOf("S", "M", "T", "W", "T", "F", "S") }
            for (day in daysOfWeek) {
                Text(
                    text = day,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Calendar grid
        GridCalendar(
            days = calendarDays,
            selectedDate = selectedDate,
            onDateClicked = onDateSelected
        )
    }
}

@Composable
private fun GridCalendar(
    days: List<CalendarDay>,
    selectedDate: Date?,
    onDateClicked: (Date) -> Unit
) {
    // Create a grid layout with 7 columns (for days of week)
    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0 until 6) {  // 6 possible weeks in a month view
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val index = week * 7 + dayOfWeek
                    
                    if (index < days.size) {
                        val day = days[index]
                        DayCell(
                            day = day, 
                            isSelected = selectedDate != null && day.date != null && 
                                isSameDay(day.date, selectedDate),
                            onDateSelected = { 
                                if (day.date != null && day.isInMonth) {
                                    onDateClicked(day.date)
                                }
                            }
                        )
                    } else {
                        // Empty cell for padding
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onDateSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .then(
                if (day.date != null && day.isInMonth) {
                    Modifier.clickable { onDateSelected() }
                } else {
                    Modifier
                }
            )
            .background(
                color = if (isSelected) DarkGreen else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day.date != null) {
            val cal = Calendar.getInstance().apply { time = day.date }
            Text(
                text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                color = if (isSelected) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

// Updated CalendarDay class with isSelected property
data class CalendarDay(
    val date: Date?, 
    val isInMonth: Boolean,
    val isSelected: Boolean = false
)

@Composable
fun TimeSlot(
    time: String, 
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(60.dp)
            .height(40.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) DarkGreen else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) DarkGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DurationCard(
    durationOption: DurationOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkGreen else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) DarkGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = durationOption.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = durationOption.price,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) Color.White else DarkGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper function to check if two dates represent the same day
fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
           cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

// Data classes for UI
data class BikeOption(val name: String, val hourlyRate: Float, val iconRes: Int)
data class DurationOption(val label: String, val price: String, val hours: Int)

@Composable
private fun BookingCard(
    booking: BookingWithBikeDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Process dates in a coroutine
    val computeScope = rememberCoroutineScope()
    var formattedStartDate by remember { mutableStateOf("Loading...") }
    var formattedEndDate by remember { mutableStateOf("Loading...") }
    var timeRange by remember { mutableStateOf("") }
    var formattedDuration by remember { mutableStateOf("") }
    
    LaunchedEffect(booking) {
        computeScope.launch(Dispatchers.Default) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val startDateStr = dateFormat.format(booking.startDate)
            val endDateStr = dateFormat.format(booking.endDate)
            
            // Pre-compute time range
            val computedTimeRange = booking.getTimeRange()
            
            // Pre-compute duration
            val computedDuration = booking.getFormattedDuration()
            
            withContext(Dispatchers.Main) {
                formattedStartDate = startDateStr
                formattedEndDate = endDateStr
                timeRange = computedTimeRange
                formattedDuration = computedDuration
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Status and Type badges at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip
                val statusColor = when (booking.status) {
                    "PENDING" -> Color(0xFFFFA000) // Amber
                    "CONFIRMED" -> DarkGreen // Green
                    "COMPLETED" -> Color(0xFF2196F3) // Blue
                    "CANCELLED" -> Color(0xFFE53935) // Red
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = when (booking.status) {
                                "PENDING" -> Icons.Default.AccessTime
                                "CONFIRMED" -> Icons.Default.CheckCircle
                                "COMPLETED" -> Icons.Default.CheckCircle
                                "CANCELLED" -> Icons.Default.ErrorOutline
                                else -> Icons.Default.AccessTime
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = booking.status.capitalize(),
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Booking type badge
                if (booking.isHourly) {
                    Surface(
                        color = DarkGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Hourly",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFF6200EA).copy(alpha = 0.15f), // Deep Purple
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Daily",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6200EA),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bike info with image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bike image
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(booking.bikeImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = booking.bikeName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
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
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                )
                
                // Booking details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Bike name
                    Text(
                        text = booking.bikeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Bike type
                    Text(
                        text = booking.bikeType ?: "Standard Bike",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    
                    // Display bike rate
                    Text(
                        text = booking.getFormattedHourlyRate(),
                        fontSize = 12.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Date range
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$formattedStartDate${if (formattedStartDate != formattedEndDate) " - $formattedEndDate" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show time range for hourly bookings
                    if (booking.isHourly && timeRange.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = timeRange,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Price and duration column
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Price
                    Text(
                        text = booking.totalPrice ?: "N/A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    
                    // Duration
                    Text(
                        text = formattedDuration,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    
    // Use remember states for formatted dates and time
    var formattedStartDate by remember { mutableStateOf("") }
    var formattedEndDate by remember { mutableStateOf("") }
    var startTimeStr by remember { mutableStateOf("") }
    var endTimeStr by remember { mutableStateOf("") }
    
    // Move date formatting to IO thread
    LaunchedEffect(booking) {
        withContext(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            
            val startDateStr = dateFormat.format(booking.startDate)
            val endDateStr = dateFormat.format(booking.endDate)
            
            // Get formatted time for hourly bookings
            val formattedStartTime = if (booking.isHourly) timeFormat.format(booking.startDate) else ""
            val formattedEndTime = if (booking.isHourly) timeFormat.format(booking.endDate) else ""
            
            withContext(Dispatchers.Main) {
                formattedStartDate = startDateStr
                formattedEndDate = endDateStr
                startTimeStr = formattedStartTime
                endTimeStr = formattedEndTime
            }
        }
    }
    
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Cancel Booking") },
            text = { 
                Text("Are you sure you want to cancel this booking? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        isCancelling = true
                        scope.launch {
                            bookingViewModel.cancelBooking(
                                bookingId = booking.id,
                                onSuccess = {
                                    isCancelling = false
                                    Toast.makeText(context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                                    onCancel(booking.id)
                                },
                                onError = { errorMessage ->
                                    isCancelling = false
                                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Cancel Booking")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Keep Booking")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title with booking status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Booking Details",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Status chip
            val statusColor = when (booking.status) {
                "PENDING" -> Color(0xFFFFA000) // Amber
                "CONFIRMED" -> DarkGreen // Green
                "COMPLETED" -> Color(0xFF2196F3) // Blue
                "CANCELLED" -> Color(0xFFE53935) // Red
                else -> MaterialTheme.colorScheme.primary
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when (booking.status) {
                            "PENDING" -> Icons.Default.AccessTime
                            "CONFIRMED" -> Icons.Default.CheckCircle
                            "COMPLETED" -> Icons.Default.CheckCircle
                            "CANCELLED" -> Icons.Default.ErrorOutline
                            else -> Icons.Default.AccessTime
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = booking.status.capitalize(),
                        fontSize = 16.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Bike image and name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bike image
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(booking.bikeImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = booking.bikeName,
                    modifier = Modifier
                        .size(110.dp),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
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
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                )
            }
            
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (booking.isHourly) DarkGreen.copy(alpha = 0.15f) else Color(0xFF6200EA).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (booking.isHourly) "Hourly" else "Daily",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (booking.isHourly) DarkGreen else Color(0xFF6200EA),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = booking.bikeType ?: "Electric Bike",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Display bike's hourly rate
                Text(
                    text = booking.getFormattedHourlyRate(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGreen,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        // Booking details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailItem(title = "Booking ID", value = booking.id.take(8) + "...")
                DetailItem(title = "Booked By", value = booking.userName)
                
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                
                DetailItem(
                    title = "Date", 
                    value = if (formattedStartDate == formattedEndDate) formattedStartDate else "$formattedStartDate - $formattedEndDate",
                    icon = Icons.Default.DateRange
                )
                
                if (booking.isHourly) {
                    DetailItem(
                        title = "Time", 
                        value = "$startTimeStr - $endTimeStr",
                        icon = Icons.Default.AccessTime
                    )
                }
                
                DetailItem(
                    title = "Duration", 
                    value = booking.getFormattedDuration()
                )
                
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                
                DetailItem(
                    title = "Bike Rate", 
                    value = booking.getFormattedHourlyRate(),
                    valueColor = DarkGreen
                )
                
                DetailItem(
                    title = "Total Price", 
                    value = booking.totalPrice ?: "N/A",
                    valueColor = DarkGreen,
                    valueFontWeight = FontWeight.Bold,
                    valueFontSize = 18.sp
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
                    onClick = { showCancelConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isCancelling
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
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
        
        // Debug section - only in debug builds
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    isVerifying = true
                    bookingViewModel.verifyBookingExists(booking.id) { exists, path ->
                        isVerifying = false
                        if (exists) {
                            Toast.makeText(context, "Booking found at: $path", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Booking not found in Firestore!", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVerifying,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying...")
                } else {
                    Text("Verify in Database")
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    title: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontWeight: FontWeight = FontWeight.Medium,
    valueFontSize: TextUnit = 15.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = valueFontWeight,
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

@Composable
fun CategoryCard(
    category: BookingCategory,
    isSelected: Boolean,
    count: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        DarkGreen
    } else {
        Color(0xFF333333)
    }
    
    val contentColor = Color.White
    
    Surface(
        modifier = modifier
            .height(40.dp)
            .width(140.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val displayText = when (category) {
                BookingCategory.ALL -> "All Bookings${if (count > 0) " ($count)" else ""}"
                BookingCategory.ACTIVE -> "Active${if (count > 0) " ($count)" else ""}"
                BookingCategory.COMPLETED -> "Completed${if (count > 0) " ($count)" else ""}"
                BookingCategory.CANCELLED -> "Cancelled${if (count > 0) " ($count)" else ""}"
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
} 