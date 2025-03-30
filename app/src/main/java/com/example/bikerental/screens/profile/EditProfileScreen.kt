package com.example.bikerental.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.core.net.toUri
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.firestore.SetOptions
import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var barangay by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch image picker
            imagePickerLauncher.launch("image/*")
        } else {
            showError = true
            errorMessage = "Permission required to select image"
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Storage permission is required to select a profile picture. Would you like to grant permission?") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Function to handle image selection
    fun handleImageSelection() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, launch picker
                imagePickerLauncher.launch("image/*")
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                // Show permission dialog
                showPermissionDialog = true
            }
            else -> {
                // Request permission directly
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Fetch existing user data
    LaunchedEffect(Unit) {
        user?.let { currentUser ->
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        fullName = document.getString("fullName") ?: currentUser.displayName ?: ""
                        phoneNumber = document.getString("phoneNumber") ?: ""
                        street = document.getString("street") ?: ""
                        city = document.getString("city") ?: ""
                        barangay = document.getString("barangay") ?: ""
                        profilePictureUrl = document.getString("profilePictureUrl")
                    } else {
                        // If document doesn't exist, use Auth data
                        fullName = currentUser.displayName ?: ""
                    }
                }
                .addOnFailureListener {
                    showError = true
                    errorMessage = "Failed to load profile data"
                }
        }
    }

    fun uploadImage(imageUri: Uri, userId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try {
            val storageRef = storage.reference
            // Use a simpler path structure
            val imageRef = storageRef.child("profile_pictures/$userId")

            // Create file metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("userId", userId)
                .build()

            // Start upload with metadata
            val uploadTask = imageRef.putFile(imageUri, metadata)

            uploadTask
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    println("Upload is $progress% done")
                }
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    println("Upload successful. Download URL: $downloadUri")
                    onSuccess(downloadUri.toString())
                }
                .addOnFailureListener { exception ->
                    println("Upload failed: ${exception.message}")
                    onError("Failed to upload image: ${exception.message}")
                }
        } catch (e: Exception) {
            println("Exception during upload: ${e.message}")
            onError("Error preparing upload: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { handleImageSelection() },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (profilePictureUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profilePictureUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Add Photo",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personal Information Section
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Address Section
            Text(
                text = "Address",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Street Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = barangay,
                onValueChange = { barangay = it },
                label = { Text("Barangay") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        showError = true
                        errorMessage = "Name cannot be empty"
                        return@Button
                    }
                    
                    isLoading = true
                    showError = false
                    
                    user?.let { currentUser ->
                        if (selectedImageUri != null) {
                            // Upload image with new function
                            uploadImage(
                                selectedImageUri!!,
                                currentUser.uid,
                                onSuccess = { downloadUrl ->
                                    // Now update Firestore with all info including the new image URL
                                    updateUserProfile(
                                        currentUser.uid,
                                        fullName,
                                        phoneNumber,
                                        currentUser.email ?: "",
                                        downloadUrl,
                                        street,
                                        city,
                                        barangay,
                                        db,
                                        currentUser,
                                        navController,
                                        onError = { message ->
                                            isLoading = false
                                            showError = true
                                            errorMessage = message
                                        }
                                    ) {
                                        isLoading = false
                                    }
                                },
                                onError = { error ->
                                    isLoading = false
                                    showError = true
                                    errorMessage = error
                                }
                            )
                        } else {
                            // Update without changing the image
                            updateUserProfile(
                                currentUser.uid,
                                fullName,
                                phoneNumber,
                                currentUser.email ?: "",
                                profilePictureUrl,
                                street,
                                city,
                                barangay,
                                db,
                                currentUser,
                                navController,
                                onError = { message ->
                                    isLoading = false
                                    showError = true
                                    errorMessage = message
                                }
                            ) {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

private fun updateUserProfile(
    userId: String,
    fullName: String,
    phoneNumber: String,
    email: String,
    profilePictureUrl: String?,
    street: String,
    city: String,
    barangay: String,
    db: FirebaseFirestore,
    currentUser: FirebaseUser,
    navController: NavController,
    onError: (String) -> Unit = {},
    onComplete: () -> Unit
) {
    try {
        val userUpdates = hashMapOf(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "authProvider" to "email",
            "street" to street,
            "city" to city,
            "barangay" to barangay
        )

        // Add profile picture URL if it exists
        profilePictureUrl?.let {
            userUpdates["profilePictureUrl"] = it
        }

        // Use set() with merge option instead of update() to create the document if it doesn't exist
        db.collection("users").document(userId)
            .set(userUpdates, SetOptions.merge())
            .addOnSuccessListener {
                println("Firestore update successful with new address fields")
                // Update Auth Profile
                val profileUpdates = userProfileChangeRequest {
                    displayName = fullName
                    profilePictureUrl?.let { url ->
                        photoUri = Uri.parse(url)
                    }
                }
                
                currentUser.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            println("Auth profile update successful")
                            // Force refresh the user data
                            currentUser.reload().addOnCompleteListener {
                                println("User data reloaded")
                                onComplete()
                                navController.popBackStack()
                            }
                        } else {
                            println("Auth profile update failed: ${task.exception?.message}")
                            onError("Failed to update profile: ${task.exception?.message}")
                            onComplete()
                        }
                    }
            }
            .addOnFailureListener { e ->
                println("Firestore update failed: ${e.message}")
                onError("Failed to save changes: ${e.message}")
                onComplete()
            }
    } catch (e: Exception) {
        println("Exception during profile update: ${e.message}")
        onError("Error updating profile: ${e.message}")
        onComplete()
    }
} 