package com.example.bikerental.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController? = null) {
    val scrollState = rememberScrollState()
    val greenColor = Color(0xFF4CAF50)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Help & Support", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            navController?.popBackStack() 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = greenColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE0E0E0),
                    titleContentColor = greenColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Contact Section
            ContactSection(greenColor)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Find Us Section
            FindUsSection(greenColor)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // FAQ Section with interactive expandable items
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // FAQ Items
            ExpandableFaqItem(
                question = "How do I rent a bike?",
                answer = "To rent a bike, navigate to the Bikes tab, select an available bike, and follow the booking process. You can pay using our supported payment methods.",
                accentColor = greenColor
            )
            
            ExpandableFaqItem(
                question = "What are your operating hours?",
                answer = "Our operating hours are 9AM to 6PM, seven days a week including holidays.",
                accentColor = greenColor
            )
            
            ExpandableFaqItem(
                question = "How do I report a problem with my rental?",
                answer = "If you encounter any issues with your rental, please contact our support team immediately through the contact form or call our customer service.",
                accentColor = greenColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactSection(accentColor: Color) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    Text(
        text = "Contact Us",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Contact Form Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Send us a message",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Full Name Field
            Text(
                text = "Full Name",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text("Your full name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = accentColor
                )
            )
            
            // Email Address Field
            Text(
                text = "Email Address",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Your email address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = accentColor
                )
            )
            
            // Phone Number Field
            Text(
                text = "Phone Number",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    // Filter non-numeric characters
                    val filteredInput = it.replace(Regex("[^0-9+]"), "")
                    phoneNumber = filteredInput
                },
                placeholder = { Text("Your phone number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = accentColor
                )
            )
            
            // Message Field
            Text(
                text = "Message",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Your message to admin...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = accentColor
                )
            )
            
            // Success message animation
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "Your message has been sent! We'll get back to you soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Send Message Button with loading state
            Button(
                onClick = { 
                    // Simulate sending message
                    isSubmitting = true
                    // In a real app, this would call a viewModel function
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(1000)
                        isSubmitting = false
                        showSuccessMessage = true
                        // Clear form
                        fullName = ""
                        email = ""
                        phoneNumber = ""
                        message = ""
                        // Hide success message after some time
                        kotlinx.coroutines.delay(3000)
                        showSuccessMessage = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                ),
                shape = RoundedCornerShape(4.dp),
                enabled = !isSubmitting && fullName.isNotEmpty() && email.isNotEmpty() && message.isNotEmpty()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSubmitting) "Sending..." else "Send Message",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FindUsSection(accentColor: Color) {
    Text(
        text = "Find Us",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Operating Hours - interactive card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Operating Hours",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Operating Hours",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "7 DAYS A WEEK: 9AM – 6PM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Location - interactive card with ripple effect
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = { /* Would open maps in a real app */ }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = accentColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Our Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Bambike Ecotours Intramuros\n" +
                           "Real St. corner General Luna St.\n" +
                           "Intramuros, Manila 1002\n" +
                           "Philippines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Tap to open in maps",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandableFaqItem(
    question: String,
    answer: String,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Question row with arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = accentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationState)
                )
            }
            
            // Animated answer section
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 