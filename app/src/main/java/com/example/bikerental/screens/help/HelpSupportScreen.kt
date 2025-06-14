package com.example.bikerental.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.bikerental.models.FAQ
import com.example.bikerental.models.SupportMessage
import com.example.bikerental.models.SupportReply
import com.example.bikerental.viewmodels.SupportViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HelpSupportScreen(
    navController: NavController? = null,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val accentColor = Color(0xFF4CAF50)
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Show tabs for Contact Us and My Messages
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Contact Us", "My Messages")
    
    // Pull-to-refresh state
    val isRefreshing = uiState.isLoading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                if (selectedTab == 0) {
                    viewModel.refreshContactUsTab()
                } else {
                    viewModel.loadUserMessages()
                }
            }
        }
    )
    
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
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE0E0E0),
                    titleContentColor = accentColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error message (if any)
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
            
            // Tab selection
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = accentColor
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content based on selected tab, wrapped with pullRefresh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(state = pullRefreshState)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Contact Form Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                // Contact Form
                                ContactSection(
                                    accentColor = accentColor,
                                    onSubmit = { subject, message, phone ->
                                        viewModel.submitSupportMessage(subject, message, phone)
                                    },
                                    isLoading = uiState.isLoading,
                                    isMessageSent = uiState.isMessageSent,
                                    resetMessageSent = { viewModel.resetMessageSent() }
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Find Us Section
                                FindUsSection(
                                    accentColor = accentColor,
                                    operatingHours = uiState.operatingHours,
                                    locationDetails = uiState.locationDetails
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // FAQ Section
                                Text(
                                    text = "Frequently Asked Questions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // Show loading for FAQs
                            if (uiState.isLoading && uiState.faqs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = accentColor)
                                    }
                                }
                            } 
                            // Show FAQs
                            else if (uiState.faqs.isNotEmpty()) {
                                items(uiState.faqs) { faq ->
                                    ExpandableFaqItem(
                                        question = faq.question,
                                        answer = faq.answer,
                                        accentColor = accentColor
                                    )
                                }
                                
                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            } 
                            // No FAQs found
                            else {
                                item {
                                    Text(
                                        text = "No FAQs available at the moment.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // My Messages Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "My Support Messages",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Show loading for messages
                            if (uiState.isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = accentColor)
                                    }
                                }
                            } 
                            // Show messages
                            else if (uiState.userMessages.isNotEmpty()) {
                                items(uiState.userMessages) { message ->
                                    MessageItem(
                                        message = message,
                                        replies = uiState.replies[message.id] ?: emptyList(),
                                        accentColor = accentColor,
                                        viewModel = viewModel
                                    )
                                }
                                
                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            } 
                            // No messages found
                            else {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Message,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "You haven't sent any support messages yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { selectedTab = 0 },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = accentColor
                                                )
                                            ) {
                                                Text("Contact Support")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Pull refresh indicator
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = accentColor
                )
            }
        }
    }
    
    // Load initial data
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && uiState.faqs.isEmpty()) {
            viewModel.loadFaqs()
        } else if (selectedTab == 1 && uiState.userMessages.isEmpty()) {
            viewModel.loadUserMessages()
        }
    }
}

@Composable
private fun ContactSection(
    accentColor: Color,
    onSubmit: (subject: String, message: String, phone: String) -> Unit,
    isLoading: Boolean,
    isMessageSent: Boolean,
    resetMessageSent: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    LaunchedEffect(isMessageSent) {
        if (isMessageSent) {
            // Clear form
            subject = ""
            fullName = ""
            email = ""
            phoneNumber = ""
            message = ""
        }
    }
    
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
            
            // Subject Field
            Text(
                text = "Subject",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Brief description of your issue") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                singleLine = true,
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
                placeholder = { Text("Describe your issue or question...") },
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
                visible = isMessageSent,
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
                    onSubmit(subject, message, phoneNumber)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                ),
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading && subject.isNotEmpty() && message.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isLoading) "Sending..." else "Send Message",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: SupportMessage,
    replies: List<SupportReply>,
    accentColor: Color,
    viewModel: SupportViewModel
) {
    val dateFormatted = message.dateCreated?.toDate()?.let {
        java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault()).format(it)
    } ?: "Unknown date"
    
    var expanded by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // File picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Trigger loading replies when the item is expanded for the first time
    LaunchedEffect(expanded) {
        if (expanded && replies.isEmpty()) {
            viewModel.loadRepliesForMessage(message.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Reduced padding for more compact UI
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // Reduced padding for more compact UI
        ) {
            // Header with subject and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    StatusBadge(status = message.status, accentColor = accentColor)
                }
            }
            
            // Date and preview in a more compact format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!expanded) {
                    Text(
                        text = "${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            // Always show the user's initial message
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )
            
            // Show conversation thread when expanded
            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Conversation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "${replies.size} ${if (replies.size == 1) "message" else "messages"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Conversation thread
                    if (replies.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            items(replies) { reply ->
                                ReplyItem(reply = reply)
                            }
                        }
                    } else {
                        Text(
                            text = "No replies yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Selected image preview
                    selectedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Selected image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uri.lastPathSegment ?: "image",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    IconButton(
                                        onClick = { selectedImageUri = null }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove image",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reply input with attachment options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attach image",
                                tint = accentColor
                            )
                        }
                        
                        // Text input
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Type your reply...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = accentColor
                            ),
                            maxLines = 3
                        )
                        
                        // Send button
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank() || selectedImageUri != null) {
                                    viewModel.sendReplyWithImage(message.id, replyText, selectedImageUri)
                                    replyText = "" // Clear input after sending
                                    selectedImageUri = null // Clear selected image
                                }
                            },
                            enabled = (replyText.isNotBlank() || selectedImageUri != null) && !uiState.isSendingReply
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send reply",
                                tint = if ((replyText.isNotBlank() || selectedImageUri != null) && !uiState.isSendingReply) accentColor else Color.Gray
                            )
                        }
                    }
                    
                    // Show loading indicator when sending
                    if (uiState.isSendingReply) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isUploadingImage) "Uploading image..." else "Sending...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Show "View conversation" text when not expanded
            if (!expanded) {
                 Text(
                    text = "View conversation",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { expanded = !expanded }
                )
            }
        }
    }
}

@Composable
fun ReplyItem(reply: SupportReply) {
    val isAdmin = reply.sender == "admin"
    val backgroundColor = if (isAdmin) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val dateFormatted = reply.createdAt?.toDate()?.let {
        java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(it)
    } ?: ""

    // Align user messages to the left, admin messages to the right
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        // More compact reply bubble
        Card(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .widthIn(max = 220.dp) // Narrower for more compact display
                .align(if (isAdmin) Alignment.CenterEnd else Alignment.CenterStart),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(modifier = Modifier.padding(8.dp)) { // Reduced padding
                // Show message text if present
                if (reply.text.isNotBlank()) {
                    Text(
                        text = reply.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp // Slightly smaller text
                    )
                }
                
                // Show image if present
                reply.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(4.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Attached image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        onError = {
                            // Log error for debugging
                            Log.e("ReplyItem", "Failed to load image: $imageUrl")
                        }
                    )
                }
                
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp, // Smaller timestamp
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp) // Reduced padding
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, accentColor: Color) {
    val (backgroundColor, textColor) = when (status) {
        "new" -> Color(0xFFFF5252).copy(alpha = 0.1f) to Color(0xFFFF5252)
        "in-progress" -> Color(0xFFFFB300).copy(alpha = 0.1f) to Color(0xFFFFB300)
        "resolved" -> accentColor.copy(alpha = 0.1f) to accentColor
        else -> Color.Gray.copy(alpha = 0.1f) to Color.Gray
    }
    
    val statusText = when (status) {
        "new" -> "New"
        "in-progress" -> "In Progress"
        "resolved" -> "Resolved"
        else -> status.replaceFirstChar { it.uppercase() }
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FindUsSection(
    accentColor: Color,
    operatingHours: List<com.example.bikerental.models.OperatingHour>,
    locationDetails: com.example.bikerental.models.LocationDetails
) {
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
                
                if (operatingHours.isNotEmpty()) {
                    operatingHours.forEach { hour ->
                        val text = if (hour.closed) {
                            "${hour.day}: Closed"
                        } else {
                            "${hour.day}: ${hour.open} – ${hour.close}"
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Loading hours...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    text = locationDetails.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = locationDetails.address.replace("\\n", "\n"),
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