package com.example.bikerental.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bikerental.models.Bike
import com.example.bikerental.ui.theme.DarkGreen
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun BookingCalendar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    bike: Bike,
    bookedDates: List<Date>,
    isLoading: Boolean = false,
    onDateSelected: (Date) -> Unit,
    onBookingConfirmed: (Date, Date) -> Unit
) {
    // Early return if dialog is not visible
    if (!isVisible) return
    
    // State for the current month being displayed
    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    
    // Selected date range
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    
    // Time selection state
    var bookingType by remember { mutableStateOf(BookingType.DAILY) }
    var startHour by remember { mutableStateOf(9) } // Default: 9 AM
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(18) } // Default: 6 PM
    var endMinute by remember { mutableStateOf(0) }
    
    // Scroll state for the dialog content
    val scrollState = rememberScrollState()
    
    // When dialog opens, reset the selected dates
    LaunchedEffect(isVisible) {
        if (isVisible) {
            startDate = null
            endDate = null
            
            // Reset to current month/year
            val today = Calendar.getInstance()
            currentMonth = today.get(Calendar.MONTH)
            currentYear = today.get(Calendar.YEAR)
        }
    }
    
    // Date formatter for month/year display
    val dateFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    
    // Current month's calendar for display
    val displayCalendar = remember(currentMonth, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal
    }
    
    // Get all days in the current month for display
    val daysInMonth = remember(displayCalendar) {
        val cal = displayCalendar.clone() as Calendar
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Calculate the first day of the month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        // Calculate empty slots at the beginning
        val emptySlots = firstDayOfWeek - Calendar.SUNDAY
        val totalDays = emptySlots + maxDays
        
        // Create the list of day cells
        val days = mutableListOf<CalendarDay>()
        
        // Add empty slots
        for (i in 0 until emptySlots) {
            days.add(CalendarDay.Empty)
        }
        
        // Add days of month
        for (i in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val date = cal.time
            
            val isToday = isDateToday(date)
            val isPastDate = isDateInPast(date)
            val isBooked = isDateBooked(date, bookedDates)
            
            days.add(
                CalendarDay.Day(
                    date = date,
                    isToday = isToday,
                    isPastDate = isPastDate,
                    isBooked = isBooked,
                    isSelectionStart = false,
                    isSelectionEnd = false,
                    isInSelectedRange = false
                )
            )
        }
        
        days
    }
    
    // Update day objects with selection state
    val daysWithSelection = daysInMonth.map { day ->
        if (day is CalendarDay.Day) {
            day.copy(
                isSelectionStart = startDate != null && isSameDay(day.date, startDate!!),
                isSelectionEnd = endDate != null && isSameDay(day.date, endDate!!),
                isInSelectedRange = startDate != null && endDate != null && 
                        day.date.after(startDate) && day.date.before(endDate)
            )
        } else {
            day
        }
    }
    
    // Day of week headers
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f) // Take up to 90% of screen height
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(scrollState) // Make the content scrollable
            ) {
                // Header with bike name and image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bike image
                    SubcomposeAsyncImage(
                        model = bike.imageUrl,
                        contentDescription = bike.name,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = DarkGreen
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = bike.name.firstOrNull()?.toString() ?: "B")
                            }
                        }
                    )
                    
                    // Bike details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Book ${bike.name}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bike.price,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Text(
                            text = bike.type,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                // Booking type selection
                Text(
                    text = "Booking Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookingTypeButton(
                        text = "Daily",
                        selected = bookingType == BookingType.DAILY,
                        onClick = { bookingType = BookingType.DAILY },
                        modifier = Modifier.weight(1f)
                    )
                    BookingTypeButton(
                        text = "Hourly",
                        selected = bookingType == BookingType.HOURLY,
                        onClick = { bookingType = BookingType.HOURLY },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Calendar header with month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous month button
                    IconButton(
                        onClick = {
                            val newMonth = if (currentMonth == Calendar.JANUARY) {
                                currentYear--
                                Calendar.DECEMBER
                            } else {
                                currentMonth - 1
                            }
                            currentMonth = newMonth
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Previous month",
                            tint = DarkGreen
                        )
                    }
                    
                    // Month and year display
                    Text(
                        text = dateFormat.format(displayCalendar.time).uppercase(),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Next month button
                    IconButton(
                        onClick = {
                            val newMonth = if (currentMonth == Calendar.DECEMBER) {
                                currentYear++
                                Calendar.JANUARY
                            } else {
                                currentMonth + 1
                            }
                            currentMonth = newMonth
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Next month",
                            tint = DarkGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Day of week headers
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false,
                    modifier = Modifier.height(36.dp)
                ) {
                    items(daysOfWeek) { day ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = day,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Days grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false,
                    modifier = Modifier.height(240.dp) // Fixed height for 6 weeks
                ) {
                    items(daysWithSelection) { day ->
                        when (day) {
                            is CalendarDay.Empty -> {
                                // Empty day slot
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                )
                            }
                            is CalendarDay.Day -> {
                                // Regular day
                                DayCell(
                                    day = day,
                                    onDateClick = { date ->
                                        if (!day.isPastDate && !day.isBooked) {
                                            // Handle date selection
                                            when {
                                                startDate == null -> {
                                                    // First selection - start date
                                                    startDate = date
                                                    onDateSelected(date)
                                                }
                                                endDate == null && date.after(startDate) -> {
                                                    // Second selection - end date
                                                    endDate = date
                                                    onDateSelected(date)
                                                }
                                                else -> {
                                                    // Reset selection
                                                    startDate = date
                                                    endDate = null
                                                    onDateSelected(date)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Legend for calendar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        text = "Today"
                    )
                    LegendItem(
                        color = Color.Red.copy(alpha = 0.7f),
                        text = "Booked"
                    )
                    LegendItem(
                        color = DarkGreen.copy(alpha = 0.7f),
                        text = "Selected"
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Selected date range display
                if (startDate != null) {
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val startDateStr = dateFormat.format(startDate!!)
                    val endDateStr = endDate?.let { dateFormat.format(it) } ?: "Select End Date"
                    
                    Text(
                        text = "From $startDateStr to $endDateStr",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    // Show time selection for hourly bookings
                    if (bookingType == BookingType.HOURLY && endDate != null) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        
                        // Time selection section
                        Text(
                            text = "Select Hours",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start time
                            Column {
                                Text(
                                    text = "Start Time",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TimePickerRow(
                                    hour = startHour,
                                    minute = startMinute,
                                    onHourChange = { startHour = it },
                                    onMinuteChange = { startMinute = it }
                                )
                            }
                            
                            // End time
                            Column {
                                Text(
                                    text = "End Time",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TimePickerRow(
                                    hour = endHour,
                                    minute = endMinute,
                                    onHourChange = { endHour = it },
                                    onMinuteChange = { endMinute = it }
                                )
                            }
                        }
                    }
                    
                    // Add booking summary when both dates are selected
                    if (endDate != null) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        
                        // Calculate duration and cost
                        val (totalHours, totalCost) = calculateBookingCost(
                            startDate = startDate!!,
                            endDate = endDate!!,
                            bookingType = bookingType,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            pricePerHour = bike.priceValue
                        )
                        
                        // Booking summary card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = DarkGreen.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Booking Summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DarkGreen
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                BookingSummaryRow(
                                    label = "Booking Type:",
                                    value = if (bookingType == BookingType.DAILY) "Daily" else "Hourly"
                                )
                                
                                if (bookingType == BookingType.HOURLY) {
                                    val startTimeStr = formatTime(startHour, startMinute)
                                    val endTimeStr = formatTime(endHour, endMinute)
                                    BookingSummaryRow(label = "Time:", value = "$startTimeStr - $endTimeStr")
                                }
                                
                                val daysBetween = calculateDaysBetween(startDate!!, endDate!!)
                                BookingSummaryRow(
                                    label = "Duration:",
                                    value = when (bookingType) {
                                        BookingType.DAILY -> "$daysBetween ${if (daysBetween == 1) "day" else "days"}"
                                        BookingType.HOURLY -> "$totalHours ${if (totalHours == 1.0) "hour" else "hours"}"
                                    }
                                )
                                
                                BookingSummaryRow(
                                    label = "Rate:",
                                    value = if (bookingType == BookingType.DAILY) 
                                        "₱${String.format("%.2f", bike.priceValue)}/day" 
                                    else 
                                        "₱${String.format("%.2f", bike.priceValue)}/hour"
                                )
                                
                                Divider(
                                    color = DarkGreen.copy(alpha = 0.3f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                BookingSummaryRow(
                                    label = "Total:",
                                    value = "₱${String.format("%.2f", totalCost)}",
                                    isHighlighted = true
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (startDate != null && endDate != null) {
                                // Apply time selection if hourly booking
                                if (bookingType == BookingType.HOURLY) {
                                    val startCalendar = Calendar.getInstance().apply {
                                        time = startDate!!
                                        set(Calendar.HOUR_OF_DAY, startHour)
                                        set(Calendar.MINUTE, startMinute)
                                    }
                                    
                                    val endCalendar = Calendar.getInstance().apply {
                                        time = endDate!!
                                        set(Calendar.HOUR_OF_DAY, endHour)
                                        set(Calendar.MINUTE, endMinute)
                                    }
                                    
                                    onBookingConfirmed(startCalendar.time, endCalendar.time)
                                } else {
                                    onBookingConfirmed(startDate!!, endDate!!)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = startDate != null && endDate != null && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirm Booking")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) DarkGreen else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) DarkGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hour picker
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = DarkGreen,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Column {
                    IconButton(
                        onClick = {
                            val newHour = if (hour < 23) hour + 1 else 0
                            onHourChange(newHour)
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text(
                            text = "▲",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            val newHour = if (hour > 0) hour - 1 else 23
                            onHourChange(newHour)
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text(
                            text = "▼",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Text(text = ":")
                
                Column {
                    IconButton(
                        onClick = {
                            val newMinute = if (minute < 59) minute + 1 else 0
                            onMinuteChange(newMinute)
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text(
                            text = "▲",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            val newMinute = if (minute > 0) minute - 1 else 59
                            onMinuteChange(newMinute)
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text(
                            text = "▼",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingSummaryRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isHighlighted) 16.sp else 14.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = if (isHighlighted) 16.sp else 14.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) DarkGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DayCell(
    day: CalendarDay.Day,
    onDateClick: (Date) -> Unit
) {
    val isSelectable = !day.isPastDate && !day.isBooked
    
    // Determine background color
    val backgroundColor = when {
        day.isSelectionStart || day.isSelectionEnd -> DarkGreen
        day.isInSelectedRange -> DarkGreen.copy(alpha = 0.4f)
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        day.isBooked -> Color.Red.copy(alpha = 0.3f)
        day.isPastDate -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    
    // Determine text color
    val textColor = when {
        day.isSelectionStart || day.isSelectionEnd -> Color.White
        day.isPastDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        day.isBooked -> Color.Red
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isSelectable) { onDateClick(day.date) }
    ) {
        Text(
            text = day.date.date.toString(),
            fontSize = 14.sp,
            color = textColor,
            fontWeight = if (day.isToday || day.isSelectionStart || day.isSelectionEnd) 
                FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function to check if date is today
private fun isDateToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val dateCalendar = Calendar.getInstance().apply {
        time = date
    }
    
    return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
            today.get(Calendar.MONTH) == dateCalendar.get(Calendar.MONTH) &&
            today.get(Calendar.DAY_OF_MONTH) == dateCalendar.get(Calendar.DAY_OF_MONTH)
}

// Helper function to check if date is in the past
private fun isDateInPast(date: Date): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    return date.before(today.time)
}

// Helper function to check if date is already booked
private fun isDateBooked(date: Date, bookedDates: List<Date>): Boolean {
    return bookedDates.any { isSameDay(it, date) }
}

// Helper function to check if two dates are the same day
private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}

// Helper function to calculate days between two dates
private fun calculateDaysBetween(startDate: Date, endDate: Date): Int {
    val diffInMillis = endDate.time - startDate.time
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 to include both start and end days
}

// Helper function to format time
private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when (hour) {
        0 -> 12
        in 1..12 -> hour
        else -> hour - 12
    }
    return String.format("%d:%02d %s", hour12, minute, amPm)
}

// Helper function to calculate booking cost
private fun calculateBookingCost(
    startDate: Date,
    endDate: Date,
    bookingType: BookingType,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    pricePerHour: Double
): Pair<Double, Double> {
    return when (bookingType) {
        BookingType.DAILY -> {
            val days = calculateDaysBetween(startDate, endDate)
            val totalCost = days * pricePerHour * 24 // Daily rate is 24x hourly rate
            Pair(days * 24.0, totalCost)
        }
        BookingType.HOURLY -> {
            val startCalendar = Calendar.getInstance().apply {
                time = startDate
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
            }
            
            val endCalendar = Calendar.getInstance().apply {
                time = endDate
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
            }
            
            val diffInMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
            val hours = diffInMillis / (1000.0 * 60 * 60)
            val totalCost = hours * pricePerHour
            Pair(hours, totalCost)
        }
    }
}

// Enum to represent booking types
enum class BookingType {
    DAILY,
    HOURLY
}

// Sealed class to represent different types of calendar days
sealed class CalendarDay {
    // Empty calendar cell
    object Empty : CalendarDay()
    
    // Regular day with date and selection state
    data class Day(
        val date: Date,
        val isToday: Boolean = false,
        val isPastDate: Boolean = false,
        val isBooked: Boolean = false,
        val isSelectionStart: Boolean = false,
        val isSelectionEnd: Boolean = false,
        val isInSelectedRange: Boolean = false
    ) : CalendarDay()
} 