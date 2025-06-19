package com.example.bikerental.screens.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.bikerental.components.RestrictedFeature
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.bikerental.models.Payment
import com.example.bikerental.models.PaymentStatus
import com.example.bikerental.models.PaymentUrgency
import com.example.bikerental.models.UnpaidBooking
import com.example.bikerental.viewmodels.PaymentUiState
import com.example.bikerental.viewmodels.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Updated color scheme for white and dark green theme
object PaymentColors {
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFF5F5F5)
    val MediumGray = Color(0xFFE0E0E0)
    val DarkGray = Color(0xFF757575)
    val TextGray = Color(0xFF424242)
    val DarkGreen = Color(0xFF1D3C34)
    val MediumGreen = Color(0xFF2D5A4C)
    val LightGreen = Color(0xFF4CAF50)
    val AccentGreen = Color(0xFF10B981)
    val Red = Color(0xFFEF4444)
    val Orange = Color(0xFFFBBF24)
    val Blue = Color(0xFF3B82F6)
    val Success = Color(0xFF059669)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFDC2626)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentTab(
    viewModel: PaymentViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController? = null
) {
    // Use RestrictedFeature to prevent access if user is not ID verified
    RestrictedFeature(
        featureType = "payment",
        navController = navController ?: androidx.navigation.compose.rememberNavController()
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
        val userPayments by viewModel.userPayments.collectAsStateWithLifecycle()
        val paymentSettings by viewModel.paymentSettings.collectAsStateWithLifecycle()
        
        // New state for unpaid bookings
        val unpaidBookings by viewModel.unpaidBookings.collectAsStateWithLifecycle()
        val isLoadingUnpaidBookings by viewModel.isLoadingUnpaidBookings.collectAsStateWithLifecycle()
        
        // State for selected unpaid booking
        val selectedUnpaidBooking by viewModel.selectedUnpaidBooking.collectAsStateWithLifecycle()
        
        // Get current user info for personalization
        val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
        val userName = remember { 
            currentUser?.displayName?.split(" ")?.firstOrNull() ?: "there"
        }
        
        // Load user payments and unpaid bookings on first composition
        LaunchedEffect(Unit) {
            viewModel.loadUserPayments()
            viewModel.loadUnpaidBookings()
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaymentColors.White)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            // Add personalized header
            if (currentStep == 1 && selectedUnpaidBooking == null) {
                PersonalizedHeader(
                    userName = userName,
                    unpaidBookingsCount = unpaidBookings.size,
                    hasPaymentHistory = userPayments.isNotEmpty()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            val currentUiState = uiState
            when (currentUiState) {
                is PaymentUiState.Success -> {
                    PaymentSuccessScreen(
                        onMakeAnotherPayment = { viewModel.resetPaymentFlow() },
                        wasBookingPayment = selectedUnpaidBooking != null
                    )
                }
                is PaymentUiState.Loading -> {
                    PaymentProcessingScreen()
                }
                is PaymentUiState.Error -> {
                    val errorMessage = currentUiState.message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaymentColors.LightGray),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PaymentColors.Error)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PaymentColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = PaymentColors.Error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                else -> {
                    // Show unpaid bookings section first if not in payment flow
                    if (currentStep == 1) {
                        UnpaidBookingsSection(
                            unpaidBookings = unpaidBookings,
                            isLoading = isLoadingUnpaidBookings,
                            userName = userName,
                            onPayBooking = { booking ->
                                // Pre-fill payment details with booking information
                                viewModel.selectUnpaidBookingForPayment(booking)
                                viewModel.setCurrentStep(2)
                            }
                        )
                        
                        // Show "No unpaid bookings" message if list is empty and not loading
                        if (unpaidBookings.isEmpty() && !isLoadingUnpaidBookings) {
                            Spacer(modifier = Modifier.height(16.dp))
                            NoUnpaidBookingsMessage(userName = userName)
                        }
                    }
                    
                    when (currentStep) {
                        1 -> {
                            // Only show payment method selection if there are unpaid bookings or if coming from booking selection
                            if (selectedUnpaidBooking != null) {
                                PaymentMethodSelection(
                                    onGCashSelected = { viewModel.setCurrentStep(2) },
                                    onCashSelected = { viewModel.setCurrentStep(4) },
                                    selectedBooking = selectedUnpaidBooking,
                                    onBackClick = { viewModel.clearSelectedUnpaidBooking() },
                                    paymentSettings = paymentSettings
                                )
                            }
                        }
                        2 -> GCashQRCodeScreen(
                            onBackClick = { 
                                viewModel.clearSelectedUnpaidBooking()
                                viewModel.setCurrentStep(1) 
                            },
                            onPaymentMade = { viewModel.setCurrentStep(3) },
                            paymentSettings = paymentSettings
                        )
                        3 -> GCashConfirmationScreen(
                            onBackClick = { viewModel.setCurrentStep(2) },
                            onConfirmPayment = { mobileNumber, referenceNumber, screenshotUri ->
                                val booking = selectedUnpaidBooking // Create local variable to fix smart cast
                                val (amount, bikeType, duration) = if (booking != null) {
                                    Triple(
                                        booking.totalPrice,
                                        booking.bikeType,
                                        booking.getFormattedDuration()
                                    )
                                } else {
                                    Triple(150.0, "Mountain Bike", "2 hours")
                                }
                                viewModel.submitPayment(
                                    mobileNumber = mobileNumber,
                                    referenceNumber = referenceNumber,
                                    amount = amount,
                                    bikeType = bikeType,
                                    duration = duration,
                                    screenshotUri = screenshotUri
                                )
                            },
                            viewModel = viewModel,
                            paymentSettings = paymentSettings,
                            selectedBooking = selectedUnpaidBooking
                        )
                        4 -> CashPaymentScreen(
                            onBackClick = { 
                                viewModel.clearSelectedUnpaidBooking()
                                viewModel.setCurrentStep(1) 
                            },
                            onConfirmCash = { 
                                viewModel.resetPaymentFlow()
                            },
                            selectedBooking = selectedUnpaidBooking
                        )
                    }
                }
            }
            
            // Show payment history if not in payment flow and there are no selected bookings
            if (currentStep == 1 && selectedUnpaidBooking == null && currentUiState !is PaymentUiState.Success && currentUiState !is PaymentUiState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                PaymentHistory(payments = userPayments, userName = userName)
            }
        }
    }
}

@Composable
fun PaymentMethodSelection(
    onGCashSelected: () -> Unit,
    onCashSelected: () -> Unit,
    onBackClick: () -> Unit,
    paymentSettings: com.example.bikerental.data.repository.PaymentSettings,
    selectedBooking: UnpaidBooking?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "How would you like to pay?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PaymentColors.DarkGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Payment Summary - Only show if there's a selected booking
        selectedBooking?.let { booking ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, PaymentColors.MediumGray)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Payment Summary",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentColors.DarkGreen,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = booking.bikeType,
                                color = PaymentColors.TextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Duration: ${booking.getFormattedDuration()}",
                                color = PaymentColors.DarkGray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Booking: ${booking.bookingId.takeLast(8)}",
                                color = PaymentColors.DarkGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        Text(
                            text = booking.getFormattedPrice(),
                            color = PaymentColors.DarkGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Payment Options - More compact
        PaymentMethodCard(
            icon = Icons.Default.Smartphone,
            title = "GCash",
            subtitle = "Pay instantly with GCash QR code",
            onClick = onGCashSelected
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        PaymentMethodCard(
            icon = Icons.Default.CreditCard,
            title = "Cash Payment",
            subtitle = "Pay in cash when you pick up the bike",
            onClick = onCashSelected
        )
        
        // Add back to bookings button if paying for a specific booking
        if (selectedBooking != null) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, PaymentColors.DarkGray),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = PaymentColors.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back to Unpaid Bookings",
                    color = PaymentColors.DarkGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
        border = BorderStroke(2.dp, PaymentColors.DarkGreen),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PaymentColors.DarkGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PaymentColors.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaymentColors.DarkGreen
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = PaymentColors.DarkGray,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = PaymentColors.DarkGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun GCashQRCodeScreen(
    onBackClick: () -> Unit,
    onPaymentMade: () -> Unit,
    paymentSettings: com.example.bikerental.data.repository.PaymentSettings
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header - Improved design
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(PaymentColors.LightGray, CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PaymentColors.DarkGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "GCash Payment",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen
            )
        }
        
        // QR Code Section - More compact and responsive
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.DarkGreen),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = PaymentColors.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan QR Code to Pay",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaymentColors.White
                )
                Text(
                    text = "Use your GCash app to scan this QR code",
                    fontSize = 14.sp,
                    color = PaymentColors.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // QR Code Display - Improved responsiveness
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (paymentSettings.qrCodeUrl.isNotEmpty()) {
                    AsyncImage(
                        model = paymentSettings.qrCodeUrl,
                        contentDescription = "GCash QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    QRCodePattern()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Payment Details - Improved design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.LightGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PaymentColors.MediumGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Payment Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaymentColors.DarkGreen,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PaymentDetailRow("Business Name:", paymentSettings.businessName)
                PaymentDetailRow("GCash Number:", paymentSettings.gcashNumber)
                Divider(
                    color = PaymentColors.MediumGray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                PaymentDetailRow("Amount:", "₱150.00", isHighlight = true)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Important Notice - Improved design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.Warning.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PaymentColors.Warning)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = PaymentColors.Warning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Important:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentColors.Warning
                    )
                    Text(
                        text = "After scanning and completing the payment, provide your mobile number, reference number, and screenshot for verification.",
                        fontSize = 14.sp,
                        color = PaymentColors.TextGray,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onPaymentMade,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaymentColors.DarkGreen),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                text = "I've Made the Payment",
                color = PaymentColors.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PaymentDetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHighlight) PaymentColors.DarkGreen else PaymentColors.DarkGray
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 18.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) PaymentColors.DarkGreen else PaymentColors.TextGray
        )
    }
}

@Composable
fun QRCodePattern() {
    Canvas(modifier = Modifier.size(200.dp)) {
        val gridSize = 12
        val cellSize = size.width / gridSize
        
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (Random.nextBoolean()) {
                    drawRect(
                        color = androidx.compose.ui.graphics.Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = i * cellSize,
                            y = j * cellSize
                        ),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
fun GCashConfirmationScreen(
    onBackClick: () -> Unit,
    onConfirmPayment: (String, String, Uri?) -> Unit,
    viewModel: PaymentViewModel,
    paymentSettings: com.example.bikerental.data.repository.PaymentSettings,
    selectedBooking: UnpaidBooking?
) {
    var mobileNumber by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    val isFormValid = mobileNumber.isNotBlank() && 
                     referenceNumber.isNotBlank() && 
                     selectedImageUri != null
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header - Improved design
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(PaymentColors.LightGray, CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PaymentColors.DarkGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Confirm Payment",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen
            )
        }
        
        // Form Fields - Improved design
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mobile Number
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = {
                    Row {
                        Text("Mobile Number", color = PaymentColors.DarkGray, fontSize = 14.sp)
                        Text(" *", color = PaymentColors.Error)
                    }
                },
                placeholder = { Text("09123456789", color = PaymentColors.DarkGray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaymentColors.DarkGreen,
                    unfocusedBorderColor = PaymentColors.MediumGray,
                    focusedTextColor = PaymentColors.TextGray,
                    unfocusedTextColor = PaymentColors.TextGray,
                    cursorColor = PaymentColors.DarkGreen,
                    focusedLabelColor = PaymentColors.DarkGreen,
                    unfocusedLabelColor = PaymentColors.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reference Number
            OutlinedTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = {
                    Row {
                        Text("Reference Number", color = PaymentColors.DarkGray, fontSize = 14.sp)
                        Text(" *", color = PaymentColors.Error)
                    }
                },
                placeholder = { Text("Enter transaction reference number", color = PaymentColors.DarkGray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaymentColors.DarkGreen,
                    unfocusedBorderColor = PaymentColors.MediumGray,
                    focusedTextColor = PaymentColors.TextGray,
                    unfocusedTextColor = PaymentColors.TextGray,
                    cursorColor = PaymentColors.DarkGreen,
                    focusedLabelColor = PaymentColors.DarkGreen,
                    unfocusedLabelColor = PaymentColors.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Screenshot Upload - Improved design
            Column {
                Row {
                    Text(
                        text = "Payment Screenshot",
                        color = PaymentColors.DarkGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(" *", color = PaymentColors.Error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { imagePickerLauncher.launch("image/*") },
                    colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
                    border = BorderStroke(
                        2.dp,
                        if (selectedImageUri != null) PaymentColors.DarkGreen else PaymentColors.MediumGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (selectedImageUri != null) PaymentColors.DarkGreen else PaymentColors.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedImageUri != null) "Screenshot uploaded" else "Click to upload screenshot",
                            color = if (selectedImageUri != null) PaymentColors.DarkGreen else PaymentColors.DarkGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Payment Summary - Improved design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaymentColors.LightGray),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PaymentColors.MediumGray)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Payment Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentColors.DarkGreen,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedBooking?.bikeType ?: "Mountain Bike",
                                color = PaymentColors.TextGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Duration: ${selectedBooking?.getFormattedDuration() ?: "2 hours"}",
                                color = PaymentColors.DarkGray,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = selectedBooking?.getFormattedPrice() ?: "₱150.00",
                            color = PaymentColors.DarkGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onConfirmPayment(mobileNumber, referenceNumber, selectedImageUri)
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaymentColors.DarkGreen,
                    disabledContainerColor = PaymentColors.MediumGray
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = "Confirm Payment",
                    color = PaymentColors.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CashPaymentScreen(
    onBackClick: () -> Unit,
    onConfirmCash: () -> Unit,
    selectedBooking: UnpaidBooking?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header - Improved design
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(PaymentColors.LightGray, CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PaymentColors.DarkGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Cash Payment",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen
            )
        }
        
        // Cash payment icon and info - Improved design
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PaymentColors.DarkGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = PaymentColors.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pay at Pickup",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen
            )
            Text(
                text = "You can pay in cash when you pick up your bike at our location.",
                fontSize = 16.sp,
                color = PaymentColors.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Payment Summary - Improved design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.LightGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PaymentColors.MediumGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Payment Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaymentColors.DarkGreen,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PaymentDetailRow("Bike Type:", selectedBooking?.bikeType ?: "Mountain Bike")
                PaymentDetailRow("Duration:", selectedBooking?.getFormattedDuration() ?: "2 hours")
                Divider(
                    color = PaymentColors.MediumGray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                PaymentDetailRow("Total Amount:", selectedBooking?.getFormattedPrice() ?: "₱150.00", isHighlight = true)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Instructions - Improved design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.Blue.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PaymentColors.Blue)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PaymentColors.Blue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Payment Instructions:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentColors.Blue
                    )
                    Text(
                        text = "Please bring the exact amount in cash when you arrive at our location. Our staff will process your rental and provide you with the bike.",
                        fontSize = 14.sp,
                        color = PaymentColors.TextGray,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onConfirmCash,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaymentColors.DarkGreen),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                text = "Confirm Cash Payment",
                color = PaymentColors.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PaymentProcessingScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PaymentColors.DarkGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = PaymentColors.White,
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Processing Payment...",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PaymentColors.DarkGreen
        )
        Text(
            text = "Please wait while we verify your payment.",
            fontSize = 16.sp,
            color = PaymentColors.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun PaymentSuccessScreen(
    onMakeAnotherPayment: () -> Unit,
    wasBookingPayment: Boolean
) {
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val userName = remember { 
        currentUser?.displayName?.split(" ")?.firstOrNull() ?: "there"
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PaymentColors.Success, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PaymentColors.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Perfect, $userName! 🎉",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PaymentColors.DarkGreen
        )
        Text(
            text = if (wasBookingPayment) {
                "Your payment went through successfully! Your bike is now reserved and ready for pickup. Safe riding! 🚴‍♀️"
            } else {
                "Payment completed! We've sent the confirmation details to your email. Thanks for choosing us! 🚲"
            },
            fontSize = 16.sp,
            color = PaymentColors.DarkGray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.LightGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PaymentColors.MediumGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                PaymentDetailRow("Payment Method:", "GCash")
                PaymentDetailRow("Amount:", "₱150.00")
                PaymentDetailRow("Booking ID:", "BR${kotlin.random.Random.nextInt(1000, 9999)}")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onMakeAnotherPayment,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaymentColors.DarkGreen),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                tint = PaymentColors.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continue with Payments",
                color = PaymentColors.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PaymentHistory(payments: List<Payment>, userName: String) {
    if (payments.isEmpty()) return
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Payment History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen
            )
            
            Text(
                text = "${payments.size} ${if (payments.size == 1) "transaction" else "transactions"}",
                fontSize = 12.sp,
                color = PaymentColors.DarkGray,
                modifier = Modifier
                    .background(
                        PaymentColors.LightGray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Use LazyColumn for better performance with many items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(payments) { payment ->
                PaymentHistoryCard(payment = payment)
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(payment: Payment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.5.dp, PaymentColors.MediumGray)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.bikeType,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentColors.DarkGreen
                    )
                    Text(
                        text = "Ref: ${payment.referenceNumber}",
                        fontSize = 10.sp,
                        color = PaymentColors.DarkGray,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                PaymentStatusChip(status = payment.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₱${payment.amount}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaymentColors.DarkGreen
                )
                Text(
                    text = payment.duration,
                    fontSize = 10.sp,
                    color = PaymentColors.DarkGray,
                    modifier = Modifier
                        .background(
                            PaymentColors.LightGray,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
    val (bgColor, textColor, text) = when (status) {
        PaymentStatus.PENDING -> Triple(PaymentColors.Warning, PaymentColors.White, "Pending")
        PaymentStatus.CONFIRMED -> Triple(PaymentColors.Success, PaymentColors.White, "Confirmed")
        PaymentStatus.REJECTED -> Triple(PaymentColors.Error, PaymentColors.White, "Rejected")
    }
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(1.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PersonalizedHeader(
    userName: String,
    unpaidBookingsCount: Int,
    hasPaymentHistory: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaymentColors.DarkGreen),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hi, $userName! 👋",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaymentColors.White
                )
                
                val statusMessage = when {
                    unpaidBookingsCount > 0 -> {
                        "You have $unpaidBookingsCount ${if (unpaidBookingsCount == 1) "booking" else "bookings"} waiting for payment"
                    }
                    hasPaymentHistory -> {
                        "All caught up! Your payments are up to date ✅"
                    }
                    else -> {
                        "Ready to start your cycling adventure?"
                    }
                }
                
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = PaymentColors.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (unpaidBookingsCount > 0) PaymentColors.Warning else PaymentColors.Success,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (unpaidBookingsCount > 0) {
                    Text(
                        text = unpaidBookingsCount.toString(),
                        color = PaymentColors.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PaymentColors.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoUnpaidBookingsMessage(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, PaymentColors.MediumGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PaymentColors.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = PaymentColors.DarkGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "You're all set, $userName! 🎉",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "No pending payments right now. When you make a new booking, you'll see payment options here.",
                fontSize = 13.sp,
                color = PaymentColors.DarkGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun UnpaidBookingsSection(
    unpaidBookings: List<UnpaidBooking>,
    isLoading: Boolean,
    userName: String,
    onPayBooking: (UnpaidBooking) -> Unit
) {
    if (isLoading) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, PaymentColors.MediumGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PaymentColors.DarkGreen,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Checking your bookings...",
                            color = PaymentColors.DarkGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "This won't take long! 🔍",
                            color = PaymentColors.DarkGray.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
            }
        }
        return
    }
    
    if (unpaidBookings.isEmpty()) {
        return
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Pending Payments",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PaymentColors.DarkGreen
            )
            
            Text(
                text = "${unpaidBookings.size} ${if (unpaidBookings.size == 1) "item" else "items"}",
                fontSize = 12.sp,
                color = PaymentColors.DarkGray,
                modifier = Modifier
                    .background(
                        PaymentColors.LightGray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier.height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(unpaidBookings) { booking ->
                UnpaidBookingCard(
                    booking = booking,
                    onPayNow = { onPayBooking(booking) }
                )
            }
        }
    }
}

@Composable
fun UnpaidBookingCard(
    booking: UnpaidBooking,
    onPayNow: () -> Unit
) {
    val urgency = booking.getPaymentUrgency()
    val urgencyColor = when (urgency) {
        PaymentUrgency.OVERDUE -> PaymentColors.Error
        PaymentUrgency.URGENT -> PaymentColors.Orange
        PaymentUrgency.MODERATE -> PaymentColors.Warning
        PaymentUrgency.LOW -> PaymentColors.AccentGreen
    }
    
    val urgencyText = when (urgency) {
        PaymentUrgency.OVERDUE -> "OVERDUE"
        PaymentUrgency.URGENT -> "URGENT"
        PaymentUrgency.MODERATE -> "DUE SOON"
        PaymentUrgency.LOW -> "ACTIVE"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaymentColors.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, urgencyColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Urgency Badge and Booking ID Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = urgencyColor,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = urgencyText,
                        color = PaymentColors.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                
                Text(
                    text = "ID: ${booking.bookingId.takeLast(8)}",
                    fontSize = 11.sp,
                    color = PaymentColors.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Bike Information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bike Image
                if (booking.bikeImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = booking.bikeImageUrl,
                        contentDescription = "Bike image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PaymentColors.LightGray),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PaymentColors.LightGray, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = PaymentColors.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Bike Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.bikeName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentColors.DarkGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = booking.bikeType,
                        fontSize = 12.sp,
                        color = PaymentColors.DarkGray,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                        text = booking.getFormattedDuration(),
                        fontSize = 11.sp,
                        color = PaymentColors.DarkGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = booking.getFormattedPrice(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaymentColors.DarkGreen
                    )
                    Text(
                        text = "Due",
                        fontSize = 10.sp,
                        color = PaymentColors.DarkGray,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "START DATE",
                        fontSize = 10.sp,
                        color = PaymentColors.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(booking.startDate)),
                        fontSize = 11.sp,
                        color = PaymentColors.TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "END DATE",
                        fontSize = 10.sp,
                        color = PaymentColors.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(booking.endDate)),
                        fontSize = 11.sp,
                        color = PaymentColors.TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pay Now Button
            Button(
                onClick = onPayNow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = urgencyColor
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = PaymentColors.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val buttonText = when (urgency) {
                    PaymentUrgency.OVERDUE -> "Pay Overdue Amount"
                    PaymentUrgency.URGENT -> "Pay Now - Urgent!"
                    PaymentUrgency.MODERATE -> "Complete Payment"
                    PaymentUrgency.LOW -> "Pay for This Booking"
                }
                Text(
                    text = buttonText,
                    color = PaymentColors.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
} 