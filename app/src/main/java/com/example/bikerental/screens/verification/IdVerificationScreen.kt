package com.example.bikerental.screens.verification

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bikerental.utils.ColorUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt
import com.example.bikerental.navigation.popBackToProfileTab
import com.example.bikerental.navigation.ProfileBackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdVerificationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Handle system back button to return to profile
    ProfileBackHandler(navController)
    
    // ID type selection
    var expanded by remember { mutableStateOf(false) }
    val idTypes = listOf(
        "Driver's License",
        "Passport",
        "Senior Citizen ID",
        "SSS ID",
        "PhilHealth ID",
        "National ID"
    )
    var selectedIdType by remember { mutableStateOf("") }
    
    // Image selection
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // UI states
    var isLoading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var submissionStatus by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // User data
    var userId by remember { mutableStateOf<String?>(null) }
    var userIdVerificationStatus by remember { mutableStateOf("not_submitted") }
    var previousIdImageUrl by remember { mutableStateOf<String?>(null) }
    var previousIdType by remember { mutableStateOf<String?>(null) }
    var verificationNote by remember { mutableStateOf<String?>(null) }
    
    // Initialize Firebase components
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permission required to select image", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Fetch user data
    LaunchedEffect(Unit) {
        auth.currentUser?.let { user ->
            userId = user.uid
            withContext(Dispatchers.IO) {
                try {
                    val document = firestore.collection("users").document(user.uid).get().await()
                    if (document.exists()) {
                        document.getString("idVerificationStatus")?.let {
                            userIdVerificationStatus = it
                        }
                        document.getString("idType")?.let {
                            previousIdType = it
                            selectedIdType = it
                        }
                        document.getString("idImageUrl")?.let {
                            previousIdImageUrl = it
                        }
                        document.getString("idVerificationNote")?.let {
                            verificationNote = it
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to load user data: ${e.message}"
                }
            }
        }
    }
    
    fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                    Manifest.permission.READ_MEDIA_IMAGES
                else 
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                imagePickerLauncher.launch("image/*")
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as android.app.Activity,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                    Manifest.permission.READ_MEDIA_IMAGES
                else 
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                showRationaleDialog = true
            }
            else -> {
                permissionLauncher.launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                        Manifest.permission.READ_MEDIA_IMAGES
                    else 
                        Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }
    
    fun uploadIdImage() {
        if (selectedImageUri == null) {
            errorMessage = "Please select an ID image"
            return
        }
        
        if (selectedIdType.isEmpty()) {
            errorMessage = "Please select ID type"
            return
        }
        
        val user = auth.currentUser
        if (user == null) {
            errorMessage = "User not authenticated"
            return
        }
        
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                // Create a unique file name for the ID image
                val fileExtension = "jpg"
                val fileName = "id_verification/${user.uid}_${UUID.randomUUID()}.$fileExtension"
                val storageRef = storage.reference.child(fileName)
                
                // Start upload with progress tracking
                val uploadTask = storageRef.putFile(selectedImageUri!!)
                
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    uploadProgress = progress.toFloat() / 100f
                }
                
                // Wait for upload to complete
                uploadTask.await()
                
                // Get download URL
                val downloadUrl = storageRef.downloadUrl.await()
                
                // Update Firestore document
                val userData = hashMapOf(
                    "idType" to selectedIdType,
                    "idImageUrl" to downloadUrl.toString(),
                    "idVerificationStatus" to "pending",
                    "idSubmissionDate" to Timestamp.now(),
                    "isIdVerified" to false, // Will be set to true after admin verification
                    "idVerificationNote" to null // Clear any previous rejection notes
                )
                
                firestore.collection("users").document(user.uid)
                    .update(userData as Map<String, Any>)
                    .await()
                
                submissionStatus = "success"
                userIdVerificationStatus = "pending"
            } catch (e: Exception) {
                errorMessage = "Upload failed: ${e.message}"
            } finally {
                isLoading = false
                uploadProgress = null
            }
        }
    }
    
    // Permission rationale dialog
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Required") },
            text = { 
                Text("Storage permission is required to select an ID image from your device.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                                Manifest.permission.READ_MEDIA_IMAGES
                            else 
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("ID Verification Requirements") },
            text = { 
                Column {
                    Text("For successful verification, please ensure:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• The ID is valid and not expired")
                    Text("• The image is clear and all text is readable")
                    Text("• Your name and photo are clearly visible")
                    Text("• The entire ID is captured in the frame")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Verification typically takes 1-2 business days.", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ID Verification") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackToProfileTab() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            when (userIdVerificationStatus) {
                "pending" -> {
                    StatusCard(
                        icon = Icons.Default.HourglassTop,
                        title = "Verification in Progress",
                        message = "We're reviewing your submitted ID. This usually takes 1-2 business days.",
                        color = Color(0xFFF57C00) // Orange
                    )
                }
                "rejected" -> {
                    StatusCard(
                        icon = Icons.Default.Error,
                        title = "Verification Failed",
                        message = verificationNote ?: "Your ID verification was rejected. Please submit a clearer image that meets our requirements.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                "verified" -> {
                    StatusCard(
                        icon = Icons.Default.CheckCircle,
                        title = "ID Verified",
                        message = "Your ID has been successfully verified. You now have access to all features.",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Show current ID if already submitted
            if (previousIdImageUrl != null && userIdVerificationStatus != "not_submitted") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current ID on File",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = previousIdType ?: "ID",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(previousIdImageUrl),
                                contentDescription = "ID Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
            
            // Don't show form if already verified
            if (userIdVerificationStatus != "verified") {
                // ID Type Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLoading) expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIdType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select ID Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        idTypes.forEach { idType ->
                            DropdownMenuItem(
                                text = { Text(idType) },
                                onClick = {
                                    selectedIdType = idType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // ID Image Selection
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable {
                            if (!isLoading) checkAndRequestPermission()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Selected ID",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Show change button overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .clickable { checkAndRequestPermission() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap to Change Image",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Add ID Photo",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Tap to Upload ID Image",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Show upload progress if active
                        uploadProgress?.let { progress ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier.size(60.dp),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "${(progress * 100).roundToInt()}%",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Text(
                                        text = "Uploading...",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Guidelines
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Guidelines",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "ID Guidelines",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "• Upload a clear, readable image of your ID\n" +
                                "• Make sure your name and photo are visible\n" +
                                "• The ID should not be expired\n" +
                                "• Your ID must match the selected type",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Submit Button
                Button(
                    onClick = { uploadIdImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading && selectedImageUri != null && selectedIdType.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = when (userIdVerificationStatus) {
                                "rejected" -> "Resubmit ID"
                                "pending" -> "Submit New ID"
                                else -> "Submit ID for Verification"
                            }
                        )
                    }
                }
            }
            
            // Success message
            AnimatedVisibility(visible = submissionStatus == "success") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "ID Submitted Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Your ID has been submitted for verification. We'll review it within 1-2 business days.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { navController.popBackToProfileTab() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Return to Profile")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
} 