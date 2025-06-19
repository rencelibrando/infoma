package com.example.bikerental.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false
)

class ChatbotEngine {
    private val responses = mapOf(
        // Password and Authentication
        "reset password" to "To reset your password, go to the login screen and tap 'Forgot Password'. We'll send a reset link to your email address.",
        "forgot password" to "To reset your password, go to the login screen and tap 'Forgot Password'. We'll send a reset link to your email address.",
        "login issue" to "If you're having trouble logging in, try resetting your password or contact support at support@infoma.com.",
        "account locked" to "If your account is locked, please wait 15 minutes and try again, or contact support for immediate assistance.",
        
        // Support and Contact
        "contact support" to "You can reach support via email at support@infoma.com or through the Help Desk tab in this screen.",
        "help" to "I'm here to help! You can ask me about passwords, transactions, data security, bike rentals, or general app usage.",
        "customer service" to "Our customer service team is available Monday-Friday 9AM-6PM. Email us at support@infoma.com for assistance.",
        "phone number" to "You can reach us at +1-555-BAMBIKE (226-2453) during business hours.",
        
        // Transaction and Payment
        "transaction history" to "You can view your transaction history under the 'Payment' tab in the main menu, then select 'Transaction History'.",
        "payment method" to "To update your payment method, go to the 'Payment' tab and select 'Payment Methods' to add or change your card.",
        "refund" to "For refunds, please contact support with your transaction ID. Refunds typically process within 3-5 business days.",
        "billing" to "For billing questions, check your transaction history in the Payment tab or contact support@infoma.com.",
        
        // Data Security and Privacy
        "data secure" to "Yes, we use Firebase Authentication and Firestore security rules to protect your data. All transactions are encrypted.",
        "privacy policy" to "You can view our privacy policy in the app settings under 'Legal' or on our website.",
        "personal information" to "Your personal information is encrypted and stored securely. We never share your data with third parties without consent.",
        
        // Bike Rental Specific
        "bike rental" to "To rent a bike, go to the 'Bikes' tab, select an available bike, and follow the rental process. Make sure to verify your ID first.",
        "unlock bike" to "To unlock a bike, scan the QR code on the bike or enter the bike ID manually in the app.",
        "return bike" to "To return a bike, park it at any designated bike station and tap 'End Ride' in the app.",
        "bike not working" to "If you encounter a bike malfunction, immediately stop your ride and report it through the 'Report Issue' button in the ride screen.",
        "find bike" to "Use the 'Map' tab to find available bikes near your location. Green markers indicate available bikes.",
        
        // Bookings and Reservations
        "booking" to "You can make a booking through the 'Bookings' tab. Select your preferred date, time, and bike type.",
        "cancel booking" to "To cancel a booking, go to 'Bookings' tab, find your booking, and tap 'Cancel'. Note cancellation policies may apply.",
        "modify booking" to "To modify a booking, cancel your current booking and create a new one, or contact support for assistance.",
        
        // App Usage
        "notification" to "You can manage notifications in the 'Notifications' tab or in your device settings under app permissions.",
        "app not working" to "Try restarting the app or clearing the app cache. If the issue persists, contact support@infoma.com.",
        "update app" to "Check the Google Play Store for app updates. Keeping the app updated ensures the best experience.",
        "location permission" to "The app needs location permission to find nearby bikes and track your rides. Enable it in your device settings.",
        
        // Account Management
        "delete account" to "To delete your account, go to Profile > Settings > Account Settings > Delete Account. This action cannot be undone.",
        "change email" to "To change your email address, go to Profile > Edit Profile and update your email. You'll need to verify the new email.",
        "profile update" to "You can update your profile information in the 'Profile' tab by tapping 'Edit Profile'.",
        
        // Emergency and Safety
        "emergency" to "In case of emergency during a ride, use the SOS button in the ride screen or call emergency services directly.",
        "safety tips" to "Always wear a helmet, follow traffic rules, and check the bike condition before starting your ride.",
        "report accident" to "If you're involved in an accident, ensure your safety first, then report it immediately through the app or contact support.",
        
        // Pricing and Plans
        "pricing" to "View our current pricing plans in the 'Payment' tab under 'Pricing Plans'. We offer hourly, daily, and monthly options.",
        "subscription" to "Manage your subscription in the 'Payment' tab. You can upgrade, downgrade, or cancel anytime.",
        "free trial" to "New users get a 30-minute free trial on their first ride. The trial automatically applies when you start your first rental."
    )
    
    fun getResponse(userInput: String): String {
        val lowercaseInput = userInput.lowercase().trim()
        
        // Find the best matching response
        val matchingKey = responses.keys.find { key ->
            lowercaseInput.contains(key) || key.split(" ").any { keyword ->
                lowercaseInput.contains(keyword)
            }
        }
        
        return matchingKey?.let { responses[it] } 
            ?: getDefaultResponse(lowercaseInput)
    }
    
    private fun getDefaultResponse(input: String): String {
        return when {
            input.contains("thank") -> "You're welcome! Is there anything else I can help you with?"
            input.contains("hello") || input.contains("hi") -> "Hello! I'm BamBot. How can I help you today?"
            input.contains("bye") || input.contains("goodbye") -> "Goodbye! Feel free to ask me anything anytime. Have a great day!"
            input.length < 3 -> "Could you please be more specific? I'm here to help with any questions about the app."
            else -> "I'm not sure about that specific question. You can contact our support team at support@infoma.com for more detailed assistance, or try asking about:\n\n• Password reset\n• Transaction history\n• Bike rentals\n• Data security\n• Contact information"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    val chatbotEngine = remember { ChatbotEngine() }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize with welcome message
    LaunchedEffect(isVisible) {
        if (isVisible && messages.isEmpty()) {
            messages = listOf(
                ChatMessage(
                    text = "Hello! I'm BamBot, your friendly assistant. I can help you with:\n\n• Password and login issues\n• Transaction history\n• Bike rentals and bookings\n• Data security questions\n• Contact information\n\nWhat would you like to know?",
                    isUser = false
                )
            )
        }
    }
    
    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Surface(
                        color = accentColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Chatbot",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "BamBot",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Online",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = onDismiss
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { message ->
                            ChatMessageItem(
                                message = message,
                                accentColor = accentColor
                            )
                        }
                        
                        // Typing indicator
                        if (isTyping) {
                            item {
                                TypingIndicator(accentColor = accentColor)
                            }
                        }
                    }
                    
                    // Input area
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Type your message...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = accentColor
                                ),
                                maxLines = 3,
                                shape = RoundedCornerShape(20.dp)
                            )
                            
                            FloatingActionButton(
                                onClick = {
                                    if (inputText.isNotBlank() && !isTyping) {
                                        val userMessage = ChatMessage(
                                            text = inputText.trim(),
                                            isUser = true
                                        )
                                        messages = messages + userMessage
                                        val userInput = inputText.trim()
                                        inputText = ""
                                        
                                        // Show typing indicator
                                        isTyping = true
                                        
                                        coroutineScope.launch {
                                            // Simulate typing delay
                                            delay(800L + (userInput.length * 20L))
                                            
                                            val botResponse = chatbotEngine.getResponse(userInput)
                                            val botMessage = ChatMessage(
                                                text = botResponse,
                                                isUser = false
                                            )
                                            
                                            isTyping = false
                                            messages = messages + botMessage
                                        }
                                    }
                                },
                                containerColor = accentColor,
                                contentColor = Color.White,
                                modifier = Modifier.size(48.dp),
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    accentColor: Color
) {
    val backgroundColor = if (message.isUser) {
        accentColor
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val textColor = if (message.isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .align(if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 20.sp
                )
                
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator(accentColor: Color) {
    val alpha1 by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "alpha1"
    )
    val alpha2 by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, delayMillis = 200),
        label = "alpha2"
    )
    val alpha3 by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, delayMillis = 400),
        label = "alpha3"
    )
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BamBot is typing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { index ->
                        val alpha = when (index) {
                            0 -> alpha1
                            1 -> alpha2
                            else -> alpha3
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    accentColor.copy(alpha = alpha * 0.6f)
                                )
                        )
                    }
                }
            }
        }
    }
} 