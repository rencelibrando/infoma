package com.example.bikerental.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bikerental.utils.LocationManager
import com.example.bikerental.viewmodels.BikeViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeUploadScreen(
    navController: NavController,
    bikeViewModel: BikeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { LocationManager.getInstance(context) }
    
    // State variables
    var bikeName by remember { mutableStateOf("") }
    var bikeType by remember { mutableStateOf("") }
    var bikePrice by remember { mutableStateOf("") }
    var bikeDescription by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var location by remember { mutableStateOf<LatLng?>(null) }
    
    // State for loading and errors
    val isLoading by bikeViewModel.isLoading.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Get current location
    LaunchedEffect(Unit) {
        locationManager.getLastLocation(
            onSuccess = { location = it },
            onFailure = { errorMessage = "Failed to get location" }
        )
    }
    
    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { imageUri = it } }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Bike") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image selection
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected bike image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to add photo",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bike details form
            OutlinedTextField(
                value = bikeName,
                onValueChange = { bikeName = it },
                label = { Text("Bike Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = bikeType,
                onValueChange = { bikeType = it },
                label = { Text("Bike Type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = bikePrice,
                onValueChange = { bikePrice = it },
                label = { Text("Price per Hour (₱)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = bikeDescription,
                onValueChange = { bikeDescription = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Location display
            if (location != null) {
                Text(
                    text = "Location: ${location!!.latitude}, ${location!!.longitude}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Retrieving location...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error message
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Success message
            successMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Upload button
            Button(
                onClick = {
                    if (validateInputs(bikeName, bikeType, bikePrice, imageUri, location)) {
                        errorMessage = null
                        scope.launch {
                            bikeViewModel.uploadBike(
                                name = bikeName,
                                type = bikeType,
                                price = bikePrice.toDoubleOrNull() ?: 0.0,
                                imageUri = imageUri!!,
                                location = location!!,
                                description = bikeDescription,
                                onSuccess = {
                                    successMessage = "Bike uploaded successfully!"
                                    // Clear form
                                    bikeName = ""
                                    bikeType = ""
                                    bikePrice = ""
                                    bikeDescription = ""
                                    imageUri = null
                                },
                                onError = { error -> errorMessage = error }
                            )
                        }
                    } else {
                        errorMessage = "Please fill out all required fields"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Upload Bike")
            }
        }
    }
}

private fun validateInputs(
    name: String,
    type: String,
    price: String,
    imageUri: Uri?,
    location: LatLng?
): Boolean {
    return name.isNotBlank() && 
           type.isNotBlank() && 
           price.isNotBlank() && 
           price.toDoubleOrNull() != null && 
           imageUri != null && 
           location != null
} 