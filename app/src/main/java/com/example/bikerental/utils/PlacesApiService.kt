package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import com.example.bikerental.BuildConfig
import com.example.bikerental.config.ApiConfig
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latLng: LatLng? = null
)

object PlacesApiService {
    private const val TAG = "PlacesApiService"
    private var placesClient: PlacesClient? = null
    private var isInitialized = false
    private var lastInitAttempt = 0L
    private const val INIT_RETRY_DELAY = 60000L // 1 minute
    
    // Flag to track if client was created in this session to prevent duplicate initialization
    private var clientCreatedInSession = false
    
    // Cache for search results to avoid redundant API calls
    private val searchCache = ConcurrentHashMap<String, Pair<List<PlaceSuggestion>, Long>>()
    private val placeDetailsCache = ConcurrentHashMap<String, Pair<PlaceSuggestion, Long>>()
    private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
    
    // Mock data for testing or when API is not available
    private val mockPlaces = listOf(
        PlaceSuggestion(
            placeId = "mock_place_1",
            primaryText = "Manila City Hall",
            secondaryText = "Manila, Philippines"
        ),
        PlaceSuggestion(
            placeId = "mock_place_2",
            primaryText = "SM Mall of Asia",
            secondaryText = "Pasay City, Philippines"
        ),
        PlaceSuggestion(
            placeId = "mock_place_3",
            primaryText = "Intramuros",
            secondaryText = "Manila, Philippines"
        ),
        PlaceSuggestion(
            placeId = "mock_place_4",
            primaryText = "Rizal Park",
            secondaryText = "Manila, Philippines"
        ),
        PlaceSuggestion(
            placeId = "mock_place_5",
            primaryText = "Makati Central Business District",
            secondaryText = "Makati, Philippines"
        ),
        PlaceSuggestion(
            placeId = "mock_place_6",
            primaryText = "BGC (Bonifacio Global City)",
            secondaryText = "Taguig, Philippines"
        ),
        PlaceSuggestion(
            placeId = "mock_place_7",
            primaryText = "Quezon City Circle",
            secondaryText = "Quezon City, Philippines"
        )
    )
    
    private val mockPlaceDetails = mapOf(
        "mock_place_1" to PlaceSuggestion(
            placeId = "mock_place_1",
            primaryText = "Manila City Hall",
            secondaryText = "Manila, Philippines",
            latLng = LatLng(14.5946, 120.9827)
        ),
        "mock_place_2" to PlaceSuggestion(
            placeId = "mock_place_2",
            primaryText = "SM Mall of Asia",
            secondaryText = "Pasay City, Philippines",
            latLng = LatLng(14.5347, 120.9829)
        ),
        "mock_place_3" to PlaceSuggestion(
            placeId = "mock_place_3",
            primaryText = "Intramuros",
            secondaryText = "Manila, Philippines",
            latLng = LatLng(14.5890, 120.9760)
        ),
        "mock_place_4" to PlaceSuggestion(
            placeId = "mock_place_4",
            primaryText = "Rizal Park",
            secondaryText = "Manila, Philippines",
            latLng = LatLng(14.5832, 120.9797)
        ),
        "mock_place_5" to PlaceSuggestion(
            placeId = "mock_place_5",
            primaryText = "Makati Central Business District",
            secondaryText = "Makati, Philippines",
            latLng = LatLng(14.5548, 121.0244)
        ),
        "mock_place_6" to PlaceSuggestion(
            placeId = "mock_place_6",
            primaryText = "BGC (Bonifacio Global City)",
            secondaryText = "Taguig, Philippines",
            latLng = LatLng(14.5509, 121.0505)
        ),
        "mock_place_7" to PlaceSuggestion(
            placeId = "mock_place_7",
            primaryText = "Quezon City Circle",
            secondaryText = "Quezon City, Philippines",
            latLng = LatLng(14.6517, 121.0491)
        )
    )

    fun initialize(context: Context) {
        try {
            // Check if we've tried recently and failed
            val now = System.currentTimeMillis()
            if (!isInitialized && now - lastInitAttempt < INIT_RETRY_DELAY) {
                Log.w(TAG, "Skipping initialization attempt - too soon after last failure")
                return
            }
            
            lastInitAttempt = now
            
            // If client was already created in this session, don't recreate it
            if (clientCreatedInSession && placesClient != null) {
                Log.d(TAG, "Places client already created in this session, reusing")
                isInitialized = true
                return
            }
            
            if (!Places.isInitialized()) {
                // Try getting the API key from AndroidManifest.xml first
                val apiKey = getApiKeyFromManifest(context)
                
                if (apiKey != null) {
                    Log.d(TAG, "Initializing Places API with key from manifest")
                    Places.initialize(context.applicationContext, apiKey)
                    placesClient = Places.createClient(context)
                    clientCreatedInSession = true
                    isInitialized = true
                    Log.d(TAG, "Places API initialized successfully from manifest.")
                } else {
                    // Fall back to ApiConfig
                    val configApiKey = ApiConfig.getMapsApiKey(context)
                    if (configApiKey != null) {
                        Log.d(TAG, "Initializing Places API with key from ApiConfig")
                        Places.initialize(context.applicationContext, configApiKey)
                        placesClient = Places.createClient(context)
                        clientCreatedInSession = true
                        isInitialized = true
                        Log.d(TAG, "Places API initialized successfully from ApiConfig.")
                    } else {
                        Log.e(TAG, "Failed to initialize Places API: No valid API key found.")
                    }
                }
            } else {
                Log.d(TAG, "Places API already initialized, creating client")
                placesClient = Places.createClient(context)
                clientCreatedInSession = true
                isInitialized = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Places API", e)
        }
    }

    private fun getApiKeyFromManifest(context: Context): String? {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
            Log.d(TAG, "API key from manifest: ${if (apiKey != null) "Found" else "Not found"}")
            apiKey
        } catch (e: Exception) {
            Log.e(TAG, "Error getting API key from manifest", e)
            null
        }
    }

    suspend fun searchPlaces(query: String): List<PlaceSuggestion> {
        // Check cache first
        val trimmedQuery = query.trim().lowercase()
        val cachedResult = searchCache[trimmedQuery]
        
        // Return cached result if it's still valid
        if (cachedResult != null && (System.currentTimeMillis() - cachedResult.second) < CACHE_EXPIRATION_MS) {
            Log.d(TAG, "Using cached search results for query: $trimmedQuery")
            return cachedResult.first
        }
    
        // Return mock data in debug mode or if Places API is not initialized
        if (BuildConfig.DEBUG && (!isInitialized || placesClient == null)) {
            Log.d(TAG, "Using mock place predictions for query: $trimmedQuery because API not initialized")
            val results = mockPlaces.filter {
                it.primaryText.contains(trimmedQuery, ignoreCase = true) ||
                it.secondaryText.contains(trimmedQuery, ignoreCase = true)
            }
            // Cache the mock results too
            searchCache[trimmedQuery] = Pair(results, System.currentTimeMillis())
            return results
        }

        if (placesClient == null || trimmedQuery.isBlank()) {
            return emptyList()
        }

        Log.d(TAG, "Searching places for query: $trimmedQuery")

        return withContext(Dispatchers.IO) {
            try {
                // Add timeout to prevent hanging
                withTimeout(5000) {
                    val token = AutocompleteSessionToken.newInstance()
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setSessionToken(token)
                        .setQuery(trimmedQuery)
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .build()

                    val response = placesClient?.findAutocompletePredictions(request)?.await()
                    val predictions = response?.autocompletePredictions?.map {
                        PlaceSuggestion(
                            placeId = it.placeId,
                            primaryText = it.getPrimaryText(null).toString(),
                            secondaryText = it.getSecondaryText(null).toString()
                        )
                    } ?: emptyList()
                    
                    Log.d(TAG, "Found ${predictions.size} place predictions")
                    
                    // Cache the results
                    searchCache[trimmedQuery] = Pair(predictions, System.currentTimeMillis())
                    predictions
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Log.e(TAG, "Place prediction request failed with status code: ${e.statusCode}", e)
                if (e.statusCode == 9011 && BuildConfig.DEBUG) {
                    Log.w(TAG, "API Key not authorized. Falling back to mock data for query: $trimmedQuery")
                    val mockResults = mockPlaces.filter {
                        it.primaryText.contains(trimmedQuery, ignoreCase = true) ||
                        it.secondaryText.contains(trimmedQuery, ignoreCase = true)
                    }
                    // Cache the mock results
                    searchCache[trimmedQuery] = Pair(mockResults, System.currentTimeMillis())
                    return@withContext mockResults
                }
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Place prediction request failed with a general exception", e)
                emptyList()
            }
        }
    }
    
    suspend fun getPlaceDetails(placeId: String): PlaceSuggestion? {
        // Check cache first
        val cachedResult = placeDetailsCache[placeId]
        
        // Return cached result if it's still valid
        if (cachedResult != null && (System.currentTimeMillis() - cachedResult.second) < CACHE_EXPIRATION_MS) {
            Log.d(TAG, "Using cached place details for ID: $placeId")
            return cachedResult.first
        }
        
        // Return mock data for testing
        if (BuildConfig.DEBUG && (!isInitialized || placesClient == null)) {
            Log.d(TAG, "Using mock place details for ID: $placeId because API not initialized")
            val mockResult = mockPlaceDetails[placeId] ?: mockPlaceDetails["mock_place_1"]
            // Cache the mock result
            if (mockResult != null) {
                placeDetailsCache[placeId] = Pair(mockResult, System.currentTimeMillis())
            }
            return mockResult
        }
        
        if (placesClient == null) {
            Log.e(TAG, "Places client is null, cannot get place details")
            return null
        }

        Log.d(TAG, "Getting details for place ID: $placeId")
        
        return withContext(Dispatchers.IO) {
            try {
                // Add timeout to prevent hanging
                withTimeout(5000) {
                    val placeFields = listOf(
                        Place.Field.ID, 
                        Place.Field.NAME, 
                        Place.Field.LAT_LNG, 
                        Place.Field.ADDRESS,
                        Place.Field.ADDRESS_COMPONENTS
                    )
                    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

                    val response = placesClient?.fetchPlace(request)?.await()
                    response?.place?.let {
                        val result = PlaceSuggestion(
                            placeId = it.id!!,
                            primaryText = it.name!!,
                            secondaryText = it.address ?: "",
                            latLng = it.latLng!!
                        )
                        Log.d(TAG, "Successfully retrieved place details: ${result.primaryText} at ${result.latLng}")
                        
                        // Cache the result
                        placeDetailsCache[placeId] = Pair(result, System.currentTimeMillis())
                        result
                    }
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Log.e(TAG, "Fetch place details failed with status code: ${e.statusCode}", e)
                if (e.statusCode == 9011 && BuildConfig.DEBUG) {
                    Log.w(TAG, "API Key not authorized. Falling back to mock data for placeId: $placeId")
                    val mockResult = mockPlaceDetails[placeId] ?: mockPlaceDetails["mock_place_1"]
                    // Cache the mock result
                    if (mockResult != null) {
                        placeDetailsCache[placeId] = Pair(mockResult, System.currentTimeMillis())
                    }
                    return@withContext mockResult
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Fetch place details failed with a general exception", e)
                null
            }
        }
    }
    
    /**
     * Clear any cached data when needed
     */
    fun clearCache() {
        searchCache.clear()
        placeDetailsCache.clear()
        Log.d(TAG, "Places API cache cleared")
    }
    
    /**
     * Properly shutdown Places API client to release resources
     * Should be called when app is going to background or being destroyed
     */
    fun shutdown() {
        try {
            if (isInitialized && placesClient != null) {
                Log.d(TAG, "Shutting down Places API client")
                placesClient = null
                clientCreatedInSession = false
                
                // Clear caches to free up memory
                clearCache()
                
                // Places API doesn't have a direct shutdown method, but we can release our reference
                // to trigger garbage collection. The warning is about internal grpc channels.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down Places API client", e)
        }
    }
}