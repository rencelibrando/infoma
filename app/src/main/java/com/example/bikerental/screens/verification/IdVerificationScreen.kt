package com.example.bikerental.screens.verification

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bikerental.ui.theme.DarkGreen
import com.example.bikerental.utils.ProfileRestrictionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdVerificationScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var age by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // ID verification status tracking
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var idVerificationStatus by remember { mutableStateOf("unverified") }
    
    // Create a temporary file for capturing photo
    val photoFile = remember {
        File.createTempFile(
            "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }
    }
    
    val photoURI = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = photoURI
        }
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoURI)
        }
    }
    
    // Load user data when screen is first shown
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (document.exists()) {
                    userData = document.data
                    
                    // Pre-fill age if already set
                    userData?.get("age")?.toString()?.let {
                        if (it.isNotBlank() && it != "null") {
                            age = it
                        }
                    }
                    
                    // Check verification status
                    userData?.get("idVerificationStatus")?.toString()?.let {
                        idVerificationStatus = it
                    }
                    
                    // Pre-check terms if already accepted
                    userData?.get("termsAccepted")?.let {
                        if (it is Boolean) {
                            termsAccepted = it
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("IdVerificationScreen", "Error loading user data", e)
                errorMessage = "Failed to load user data: ${e.message}"
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ID Verification") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status indicator
                when (idVerificationStatus) {
                    "pending" -> {
                        StatusCard(
                            title = "Verification Pending",
                            message = "Your ID verification is under review. We'll notify you once it's approved.",
                            color = Color(0xFFFFA000)
                        )
                    }
                    "approved" -> {
                        StatusCard(
                            title = "Verification Approved",
                            message = "Your ID has been verified successfully. You can now book bikes.",
                            color = Color(0xFF4CAF50)
                        )
                    }
                    "declined" -> {
                        StatusCard(
                            title = "Verification Declined",
                            message = "Your ID verification was declined. Please upload a clearer image of your ID.",
                            color = Color(0xFFF44336)
                        )
                    }
                }
                
                Text(
                    text = "Verify Your Identity",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
                
                Text(
                    text = "To ensure safety and comply with regulations, we need to verify your age and identity before you can book a bike.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                
                // Age input
                OutlinedTextField(
                    value = age,
                    onValueChange = { 
                        if (it.isBlank() || it.matches(Regex("\\d*"))) {
                            age = it
                        }
                    },
                    label = { Text("Your Age") },
                    singleLine = true,
                    isError = age.isNotBlank() && (age.toIntOrNull() ?: 0) < 18,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (age.isNotBlank() && (age.toIntOrNull() ?: 0) < 18) {
                    Text(
                        text = "You must be at least 18 years old to rent a bike",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                // ID upload section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Upload ID Document",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Please upload a valid government-issued ID (e.g., driver's license, passport)",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Display selected image
                        if (imageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(bottom = 8.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(imageUri),
                                    contentDescription = "Selected ID",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                
                                // Clear button
                                IconButton(
                                    onClick = { imageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove image",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            // Upload placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        galleryLauncher.launch("image/*")
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Upload",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "Tap to select an image",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Upload buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Gallery")
                            }
                            
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) -> {
                                                cameraLauncher.launch(photoURI)
                                            }
                                            else -> {
                                                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    } else {
                                        cameraLauncher.launch(photoURI)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Camera")
                            }
                        }
                    }
                }
                
                // Terms and conditions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it }
                    )
                    
                    Text(
                        text = "I agree to the Terms and Conditions and give consent for my ID to be verified",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clickable { termsAccepted = !termsAccepted }
                    )
                }
                
                // Error message
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Submit button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (age.isBlank() || (age.toIntOrNull() ?: 0) < 18) {
                                errorMessage = "Please enter a valid age (18 or above)"
                                return@launch
                            }
                            
                            if (imageUri == null) {
                                errorMessage = "Please upload an ID document"
                                return@launch
                            }
                            
                            if (!termsAccepted) {
                                errorMessage = "You must accept the terms and conditions"
                                return@launch
                            }
                            
                            try {
                                isLoading = true
                                errorMessage = null
                                
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                    ?: throw Exception("User not logged in")
                                
                                // Upload image to Firebase Storage
                                val storageRef = FirebaseStorage.getInstance().reference
                                    .child("user_ids/${currentUser.uid}_${System.currentTimeMillis()}")
                                
                                val uploadTask = storageRef.putFile(imageUri!!)
                                uploadTask.await()
                                
                                // Get download URL
                                val downloadUrl = storageRef.downloadUrl.await().toString()
                                
                                // Update user record with age, ID URL and verification status
                                val userRef = FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(currentUser.uid)
                                
                                // Convert age to Int before adding to the map
                                val ageValue = age.toIntOrNull() ?: 0
                                
                                // Create updates with proper types
                                val updates = hashMapOf<String, Any>(
                                    "age" to ageValue,
                                    "idUrl" to downloadUrl,
                                    "idVerificationStatus" to "pending",
                                    "termsAccepted" to termsAccepted,
                                    "lastUpdated" to System.currentTimeMillis()
                                )
                                
                                userRef.update(updates).await()
                                
                                // Show success dialog
                                showSuccessDialog = true
                                idVerificationStatus = "pending"
                            } catch (e: Exception) {
                                Log.e("IdVerificationScreen", "Error submitting verification", e)
                                errorMessage = "Failed to submit verification: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading && idVerificationStatus != "approved"
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            when (idVerificationStatus) {
                                "pending" -> "Update Information"
                                "approved" -> "Already Verified"
                                "declined" -> "Resubmit Verification"
                                else -> "Submit for Verification"
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = {
                Text("Verification Submitted")
            },
            text = {
                Text("Your ID has been submitted for verification. You'll be notified once it's approved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("OK")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DarkGreen
                )
            }
        )
    }
}

@Composable
fun StatusCard(
    title: String,
    message: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(40.dp),
                colors = CardDefaults.cardColors(containerColor = color)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 