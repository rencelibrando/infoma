package com.example.bikerental.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import com.example.bikerental.utils.PlaceSuggestion
import com.example.bikerental.utils.PlacesApiService
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import com.example.bikerental.BuildConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Dialog for searching and selecting destinations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSearchDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onDestinationSelected: (LatLng) -> Unit
) {
    if (!isVisible) return
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val TAG = "DestinationSearchDialog"
    
    // Use LazyListState to optimize list performance
    val lazyListState = rememberLazyListState()
    
    // Keep track of the current search job to cancel previous ones
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Keep updated reference to callbacks
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnDestinationSelected by rememberUpdatedState(onDestinationSelected)

    // Initialize Places API when dialog is shown
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initializing Places API from dialog")
        try {
            // Use IO dispatcher for API initialization
            withContext(Dispatchers.IO) {
                PlacesApiService.initialize(context)
            }
            
            Log.d(TAG, "Places API initialized from dialog")
            
            // Show a mock entry immediately to provide feedback
            if (BuildConfig.DEBUG) {
                searchResults = withContext(Dispatchers.IO) {
                    PlacesApiService.searchPlaces("Default").take(5) // Limit initial results
                }
                Log.d(TAG, "Loaded ${searchResults.size} default suggestions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Places API", e)
            isError = true
            errorMessage = "Failed to initialize location search: ${e.message}"
        }
    }

    // Clean up resources when dialog closes
    DisposableEffect(isVisible) {
        onDispose {
            searchJob?.cancel()
            searchJob = null
        }
    }

    // Search when query changes
    LaunchedEffect(searchQuery) {
        // Cancel previous search if any
        searchJob?.cancel()
        
        if (searchQuery.length < 3) {
            searchResults = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        
        delay(300) // Debounce search
        isLoading = true
        isError = false
        
        searchJob = coroutineScope.launch {
            try {
                Log.d(TAG, "Searching places for: $searchQuery")
                
                // Use IO dispatcher for network calls
                val results = withContext(Dispatchers.IO) {
                    PlacesApiService.searchPlaces(searchQuery)
                }
                
                // Limit results to prevent performance issues with large lists
                searchResults = results.take(10)
                Log.d(TAG, "Search completed, found ${searchResults.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching places", e)
                isError = true
                errorMessage = "Search failed: ${e.message}"
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set Destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for a destination") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                // Error message
                if (isError) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Could not load search results.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                // Destination list with optimized LazyColumn
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Initial prompt when no search
                    if (searchQuery.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Enter a location to search",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Search results
                    items(
                        items = searchResults,
                        key = { it.placeId } // Provide stable keys for better performance
                    ) { suggestion ->
                        DestinationItem(
                            suggestion = suggestion,
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        // Show loading while fetching details
                                        isLoading = true
                                        
                                        Log.d(TAG, "Getting place details for: ${suggestion.placeId}")
                                        
                                        // Use IO dispatcher for network call
                                        val placeDetails = withContext(Dispatchers.IO) {
                                            PlacesApiService.getPlaceDetails(suggestion.placeId)
                                        }
                                        
                                        if (placeDetails?.latLng != null) {
                                            Log.d(TAG, "Got place details, lat/lng: ${placeDetails.latLng}")
                                            currentOnDestinationSelected(placeDetails.latLng!!)
                                            currentOnDismiss()
                                        } else {
                                            Log.e(TAG, "Place details returned null latLng")
                                            isError = true
                                            errorMessage = "Could not get location coordinates"
                                            isLoading = false
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error getting place details", e)
                                        isError = true
                                        errorMessage = "Failed to get location details: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                    
                    // If no results found but search was performed
                    if (searchResults.isEmpty() && searchQuery.length >= 3 && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No destinations found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Help text at bottom
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type at least 3 characters to search",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Individual destination item in the search results
 */
@Composable
fun DestinationItem(
    suggestion: PlaceSuggestion,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp) // Reduced padding for better list density
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(32.dp) // Slightly smaller 
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp) // Smaller icon
                )
            }
            
            // Location details
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = suggestion.primaryText,
                    style = MaterialTheme.typography.bodyMedium, // Smaller text
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Prevent long text from causing layout issues
                )
                
                Text(
                    text = suggestion.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Prevent long text from causing layout issues
                )
            }
        }
        
        Divider(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}