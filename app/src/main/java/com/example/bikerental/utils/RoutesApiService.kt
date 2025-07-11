package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import com.example.bikerental.BuildConfig
import com.example.bikerental.config.ApiConfig
import com.example.bikerental.ui.theme.RouteInfo
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Service to handle Google Maps Routes API v2 requests and responses
 * This is an upgrade from the legacy Directions API
 */
object RoutesApiService {
    private const val TAG = "RoutesApiService"
    private const val BASE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes"
    
    // JSON media type for API requests
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    // OkHttp client for network requests with improved configuration
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // API key will be retrieved from context to avoid hardcoding
    private var apiKey: String? = null
    
    /**
     * Initialize the service with context to get API key 
     * @param context The application context used to retrieve the API key
     */
    @JvmStatic
    fun initialize(context: Context) {
        apiKey = ApiConfig.getMapsApiKey(context)
        Log.d(TAG, "RoutesApiService initialized with key: ${ApiConfig.maskApiKey(apiKey)}")
    }
    
    /**
     * Get routes between two points using the Routes API v2
     */
    @JvmStatic
    suspend fun getRoutes(
        origin: LatLng,
        destination: LatLng,
        mode: String = "bicycling", // Default to bicycling for bike app
        alternatives: Boolean = false,
        useMockData: Boolean = false
    ): Result<List<RouteInfo>> = withContext(Dispatchers.IO) {
        try {
            // Return mock data only if explicitly requested or if API key is not set
            if (useMockData || !ApiConfig.isValidApiKey(apiKey)) {
                Log.w(TAG, "Using mock routes data - ${if (useMockData) "mock data explicitly requested" else "no valid API key"}")
                return@withContext Result.success(createMockRoutes(origin, destination))
            }
            
            Log.d(TAG, "Requesting routes from Google Maps Routes API v2 with mode: $mode")
            
            // Build JSON request body
            val requestBody = buildRoutesRequestBody(origin, destination, mode, alternatives)
            
            // Create request with API key in header
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("X-Goog-Api-Key", apiKey ?: "")
                .addHeader("X-Goog-FieldMask", getFieldMask())
                .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            
            // Log the full request for debugging
            Log.d(TAG, "Request URL: ${BASE_URL}")
            Log.d(TAG, "Request Headers: X-Goog-Api-Key: ${apiKey?.let { ApiConfig.maskApiKey(it) } ?: "null"}, X-Goog-FieldMask: ${getFieldMask()}")
            Log.d(TAG, "Request Body: ${requestBody.toString().take(500)}")
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(TAG, "API call failed: ${response.code}, Error: $errorBody")
                    
                    // Try to parse error for more details if possible
                    try {
                        val errorJson = JSONObject(errorBody)
                        val errorMsg = if (errorJson.has("error")) {
                            val error = errorJson.getJSONObject("error")
                            "${error.getString("message")} (${error.getInt("code")})"
                        } else {
                            errorBody
                        }
                        Log.e(TAG, "Detailed error: $errorMsg")
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not parse error body: ${e.message}")
                    }
                    
                    Log.w(TAG, "Falling back to mock data after API failure with code ${response.code}")
                    return@withContext Result.success(createMockRoutes(origin, destination))
                }
                
                val jsonResponse = response.body?.string() ?: ""
                Log.d(TAG, "API Response: ${jsonResponse.take(200)}...")
                
                // Debug the route response to help diagnose issues
                debugRouteResponse(jsonResponse)
                
                return@withContext Result.success(parseRoutesResponse(jsonResponse))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting routes", e)
            // Fall back to mock data on any exceptions
            Log.w(TAG, "Falling back to mock data due to exception: ${e.message}")
            return@withContext Result.success(createMockRoutes(origin, destination))
        }
    }
    
    /**
     * Build the JSON request body for the Routes API
     */
    private fun buildRoutesRequestBody(
        origin: LatLng,
        destination: LatLng,
        mode: String,
        alternatives: Boolean
    ): JSONObject {
        val jsonRequest = JSONObject()
        
        try {
            // Origin location
            val originObj = JSONObject()
            originObj.put("location", JSONObject()
                .put("latLng", JSONObject()
                    .put("latitude", origin.latitude)
                    .put("longitude", origin.longitude)
                )
            )
            
            // Destination location
            val destinationObj = JSONObject()
            destinationObj.put("location", JSONObject()
                .put("latLng", JSONObject()
                    .put("latitude", destination.latitude)
                    .put("longitude", destination.longitude)
                )
            )
            
            // Travel mode
            val travelMode = when (mode.uppercase()) {
                "BICYCLE", "BICYCLING" -> "BICYCLE" // Use BICYCLE for human-powered bicycles per Routes API v2 docs
                "WALKING" -> "WALKING"
                "DRIVING" -> "DRIVE"
                else -> "BICYCLE" // Default to bicycle for this bike rental app
            }
            
            // Build request correctly according to Routes API v2 schema
            jsonRequest.put("origin", originObj)
            jsonRequest.put("destination", destinationObj)
            jsonRequest.put("travelMode", travelMode)
            jsonRequest.put("computeAlternativeRoutes", alternatives)
            
            // Only set routing preference for driving modes, not for bicycle or walking
            if (travelMode != "BICYCLE" && travelMode != "WALKING") {
                jsonRequest.put("routingPreference", "TRAFFIC_AWARE")
            }
            
            jsonRequest.put("languageCode", "en-US")
            jsonRequest.put("units", "METRIC")
            
            // Add routing parameters to ensure road-based paths
            val routeModifiers = JSONObject()
            routeModifiers.put("avoidTolls", false)
            routeModifiers.put("avoidHighways", false)
            routeModifiers.put("avoidFerries", false)
            jsonRequest.put("routeModifiers", routeModifiers)
            
            // Request polyline with high quality to get all road segments
            jsonRequest.put("polylineQuality", "HIGH_QUALITY")
            jsonRequest.put("polylineEncoding", "ENCODED_POLYLINE")
            
            // Request traffic info for better visualization
            jsonRequest.put("extraComputations", JSONArray().put("TRAFFIC_ON_POLYLINE"))
            
            // Request extra route metadata for better display - only for driving mode
            if (travelMode == "DRIVE") {
                jsonRequest.put("requestedReferenceRoutes", JSONArray().put("FUEL_EFFICIENT"))
            }
            
            // Validate the built JSON to ensure it meets API requirements
            validateJsonRequest(jsonRequest)
            
            // Log the request for debugging (limited length)
            Log.d(TAG, "Built JSON request successfully")
            
        } catch (e: JSONException) {
            Log.e(TAG, "Error building JSON request", e)
            // Create a minimal valid request as fallback
            try {
                jsonRequest.put("origin", JSONObject().put("location", 
                    JSONObject().put("latLng", 
                        JSONObject().put("latitude", origin.latitude).put("longitude", origin.longitude)
                    )
                ))
                jsonRequest.put("destination", JSONObject().put("location", 
                    JSONObject().put("latLng", 
                        JSONObject().put("latitude", destination.latitude).put("longitude", destination.longitude)
                    )
                ))
                jsonRequest.put("travelMode", "BICYCLE")
            } catch (e2: JSONException) {
                Log.e(TAG, "Critical error creating fallback request", e2)
            }
        }
        
        return jsonRequest
    }
    
    /**
     * Validate the JSON request to ensure it meets the API requirements
     */
    private fun validateJsonRequest(jsonRequest: JSONObject) {
        // Check for required fields
        val requiredFields = listOf("origin", "destination", "travelMode")
        for (field in requiredFields) {
            if (!jsonRequest.has(field)) {
                throw JSONException("Missing required field: $field")
            }
        }
        
        // Validate origin and destination have valid latLng objects
        val locations = listOf("origin", "destination")
        for (location in locations) {
            val locObj = jsonRequest.optJSONObject(location)
            if (locObj == null || !locObj.has("location")) {
                throw JSONException("Invalid $location object structure")
            }
            
            val innerLoc = locObj.optJSONObject("location")
            if (innerLoc == null || !innerLoc.has("latLng")) {
                throw JSONException("Invalid $location.location object structure")
            }
            
            val latLng = innerLoc.optJSONObject("latLng")
            if (latLng == null || !latLng.has("latitude") || !latLng.has("longitude")) {
                throw JSONException("Invalid $location.location.latLng object structure")
            }
        }
    }
    
    /**
     * Define field mask for the API response (what fields we want to receive)
     */
    private fun getFieldMask(): String {
        return "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline," +
               "routes.legs,routes.legs.steps.navigationInstruction,routes.legs.steps.distanceMeters," +
               "routes.legs.steps.staticDuration,routes.legs.steps.polyline.encodedPolyline," +
               "routes.legs.steps.startLocation,routes.legs.steps.endLocation," +
               "routes.legs.steps.travelAdvisory,routes.legs.distanceMeters," +
               "routes.legs.duration,routes.legs.staticDuration," +
               "routes.travelAdvisory,routes.routeLabels,routes.viewport," + 
               "routes.optimizedIntermediateWaypointIndex"
    }
    
    /**
     * Parse the JSON response from the Routes API
     */
    @Throws(JSONException::class)
    private fun parseRoutesResponse(jsonResponse: String): List<RouteInfo> {
        val routes = mutableListOf<RouteInfo>()
        val jsonObject = JSONObject(jsonResponse)
        
        // Check if we have routes
        if (!jsonObject.has("routes")) {
            Log.e(TAG, "API response doesn't contain routes")
            return routes
        }
        
        val routesArray = jsonObject.getJSONArray("routes")
        Log.d(TAG, "Found ${routesArray.length()} routes in API response")
            
        for (i in 0 until routesArray.length()) {
            try {
                val route = routesArray.getJSONObject(i)
                
                // Extract route duration and distance with better error handling
                val durationSeconds = try {
                    // Handle both numeric and string format (like "637s")
                    when (val durationValue = route.get("duration")) {
                        is Int -> durationValue.toLong()
                        is Long -> durationValue
                        is String -> {
                            // Parse string like "637s" to extract the numeric part
                            val numericPart = durationValue.replace(Regex("[^0-9]"), "")
                            numericPart.toLongOrNull() ?: 600L
                        }
                        else -> 600L
                    }
                } catch (e: Exception) { 
                    Log.w(TAG, "Error parsing duration, using default", e)
                    600L // Default 10 minutes if missing
                }
                val durationString = formatDuration(durationSeconds)
                
                val distanceMeters = try { route.getInt("distanceMeters") }
                                  catch (e: Exception) { 
                                      Log.w(TAG, "Error parsing distance, using default", e)
                                      1000 // Default 1km if missing
                                  }
                val distanceString = formatDistance(distanceMeters)
                
                // Extract steps from legs first (since we need them for polyline fallback)
                val steps = mutableListOf<RouteInfo.Step>()
                
                // Extract encoded polyline with error handling
                val polylinePoints = try {
                    val polylineObj = route.getJSONObject("polyline")
                    val encodedPolyline = polylineObj.getString("encodedPolyline")
                    Log.d(TAG, "Found route polyline with length: ${encodedPolyline.length}")
                    decodePoly(encodedPolyline)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding polyline, creating simple direct path", e)
                    // We can't reference origin and destination directly here, 
                    // use empty list as fallback and rely on steps polyline if available
                    emptyList()
                }
                
                // Extract traffic data if available
                val trafficInfo = mutableMapOf<Int, Double>() // Index to congestion level mapping
                try {
                    if (route.has("travelAdvisory")) {
                        val travelAdvisory = route.getJSONObject("travelAdvisory")
                        if (travelAdvisory.has("speedReadingIntervals")) {
                            val speedReadings = travelAdvisory.getJSONArray("speedReadingIntervals")
                            for (j in 0 until speedReadings.length()) {
                                val speedReading = speedReadings.getJSONObject(j)
                                val index = j
                                val speed = speedReading.optDouble("speed", -1.0)
                                if (speed >= 0) {
                                    trafficInfo[index] = speed
                                }
                            }
                            Log.d(TAG, "Extracted traffic data for ${trafficInfo.size} road segments")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract traffic data", e)
                }
                
                // Extract legs and steps
                val legsArray = route.getJSONArray("legs")
                
                for (j in 0 until legsArray.length()) {
                    val leg = legsArray.getJSONObject(j)
                    val stepsArray = leg.getJSONArray("steps")
                    
                    for (k in 0 until stepsArray.length()) {
                        try {
                            val step = stepsArray.getJSONObject(k)
                            
                            // Extract step information
                            val stepDistanceMeters = step.getInt("distanceMeters")
                            val stepDistanceString = formatDistance(stepDistanceMeters)
                            
                            // Handle staticDuration that might be returned as a string (e.g., "99s")
                            val durationSec = try {
                                when (val durationValue = step.get("staticDuration")) {
                                    is Int -> durationValue
                                    is Long -> durationValue.toInt()
                                    is String -> {
                                        // Parse string like "99s" to extract the numeric part
                                        val numericPart = durationValue.replace(Regex("[^0-9]"), "")
                                        numericPart.toIntOrNull() ?: 60 // Default to 1 minute if parsing fails
                                    }
                                    else -> 60 // Default to 1 minute
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing step duration, using default", e)
                                60 // Default to 1 minute
                            }
                            
                            val stepDurationString = formatDuration(durationSec.toLong())
                            
                            // Get start and end location - handle both direct lat/lng or nested location format
                            val startLat: Double
                            val startLng: Double
                            val startLocation = step.getJSONObject("startLocation")
                            if (startLocation.has("latitude") && startLocation.has("longitude")) {
                                // Direct format
                                startLat = startLocation.getDouble("latitude")
                                startLng = startLocation.getDouble("longitude")
                            } else if (startLocation.has("latLng")) {
                                // Nested format
                                val latLng = startLocation.getJSONObject("latLng")
                                startLat = latLng.getDouble("latitude")
                                startLng = latLng.getDouble("longitude")
                            } else {
                                // Fallback to 0,0 with log
                                Log.e(TAG, "Cannot find latitude/longitude in startLocation")
                                startLat = 0.0
                                startLng = 0.0
                            }
                            
                            val endLat: Double
                            val endLng: Double
                            val endLocation = step.getJSONObject("endLocation")
                            if (endLocation.has("latitude") && endLocation.has("longitude")) {
                                // Direct format
                                endLat = endLocation.getDouble("latitude")
                                endLng = endLocation.getDouble("longitude")
                            } else if (endLocation.has("latLng")) {
                                // Nested format
                                val latLng = endLocation.getJSONObject("latLng")
                                endLat = latLng.getDouble("latitude")
                                endLng = latLng.getDouble("longitude")
                            } else {
                                // Fallback to 0,0 with log
                                Log.e(TAG, "Cannot find latitude/longitude in endLocation")
                                endLat = 0.0
                                endLng = 0.0
                            }
                            
                            // Get step polyline
                            val stepPolyline = try {
                                decodePoly(step.getJSONObject("polyline").getString("encodedPolyline"))
                            } catch (e: Exception) {
                                // If we can't get step polyline, create a direct line
                                listOf(
                                    LatLng(startLat, startLng),
                                    LatLng(endLat, endLng)
                                )
                            }
                            
                            // Get navigation instruction (if available)
                            var instruction = "Continue on route"
                            var maneuver = ""
                            if (step.has("navigationInstruction")) {
                                val navInstruction = step.getJSONObject("navigationInstruction")
                                instruction = navInstruction.getString("instructions")
                                if (navInstruction.has("maneuver")) {
                                    maneuver = navInstruction.getString("maneuver")
                                }
                            }
                            
                            // Add step to list
                            steps.add(
                                RouteInfo.Step(
                                    instruction = cleanHtmlInstructions(instruction),
                                    distance = stepDistanceString,
                                    distanceValue = stepDistanceMeters,
                                    duration = stepDurationString,
                                    startLocation = LatLng(startLat, startLng),
                                    endLocation = LatLng(endLat, endLng),
                                    maneuver = mapManeuverToDirectionsFormat(maneuver),
                                    polylinePoints = stepPolyline
                                )
                            )
                        } catch (e: Exception) {
                            // Log error but continue processing other steps
                            Log.e(TAG, "Error parsing step ${k} in leg ${j}, skipping", e)
                        }
                    }
                }
                
                // Create a valid route polyline from steps if main polyline is empty
                val finalPolylinePoints = if (polylinePoints.isEmpty() && steps.isNotEmpty()) {
                    // If we have steps but no main polyline, concatenate step polylines
                    val concatenatedPoints = mutableListOf<LatLng>()
                    steps.forEach { step ->
                        // Skip first point of each step except the first to avoid duplicates
                        if (concatenatedPoints.isEmpty()) {
                            concatenatedPoints.addAll(step.polylinePoints)
                        } else {
                            concatenatedPoints.addAll(step.polylinePoints.drop(1))
                        }
                    }
                    concatenatedPoints
                } else {
                    polylinePoints
                }
                
                // Add route to list
                routes.add(
                    RouteInfo(
                        distance = distanceString,
                        duration = durationString,
                        distanceValue = distanceMeters,
                        durationValue = durationSeconds.toInt(),
                        polylinePoints = finalPolylinePoints,
                        steps = steps,
                        trafficInfo = trafficInfo
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing route ${i}", e)
            }
        }
        
        return routes
    }
    
    /**
     * Map Routes API maneuver types to the format used in the Directions API for compatibility
     */
    private fun mapManeuverToDirectionsFormat(maneuver: String): String {
        return when (maneuver) {
            "TURN_LEFT" -> "turn-left"
            "TURN_RIGHT" -> "turn-right"
            "TURN_SLIGHT_LEFT" -> "slight-left"
            "TURN_SLIGHT_RIGHT" -> "slight-right"
            "TURN_SHARP_LEFT" -> "sharp-left"
            "TURN_SHARP_RIGHT" -> "sharp-right"
            "UTURN_LEFT", "UTURN_RIGHT" -> "uturn"
            "KEEP_LEFT" -> "keep-left"
            "KEEP_RIGHT" -> "keep-right"
            "STRAIGHT" -> "straight"
            "ROUNDABOUT_RIGHT", "ROUNDABOUT_LEFT" -> "roundabout"
            "DESTINATION" -> "arrive"
            else -> ""
        }
    }
    
    /**
     * Clean HTML tags from instructions (Routes API generally doesn't return HTML but keeping for compatibility)
     */
    private fun cleanHtmlInstructions(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }
    
    /**
     * Format duration in seconds to a user-friendly string
     */
    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "$hours hr ${minutes} min"
            minutes > 0 -> "$minutes min"
            else -> "1 min" // Minimum display time
        }
    }
    
    /**
     * Format distance in meters to a user-friendly string
     */
    private fun formatDistance(meters: Int): String {
        return when {
            meters < 1000 -> "$meters m"
            else -> String.format("%.1f km", meters / 1000.0)
        }
    }
    
    /**
     * Decode a polyline to a list of LatLng points
     */
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
    
    /**
     * Get the next navigation instruction based on current location
     */
    fun getNextNavigationInstruction(
        currentLocation: LatLng?,
        route: RouteInfo,
        currentStepIndex: Int
    ): Pair<String, Int> {
        // Default instruction if we can't determine
        var instruction = "Follow the route"
        var distanceToNext = 0
        
        // Return default if current location is null
        if (currentLocation == null) {
            return Pair(instruction, distanceToNext)
        }
        
        // Get current step or return default if no steps or index out of bounds
        val currentStep = route.steps.getOrNull(currentStepIndex) ?: return Pair(instruction, distanceToNext)
        
        // Calculate distance to the end of this step
        val distanceToEndOfStep = DistanceCalculationUtils.calculateDistance(currentLocation, currentStep.endLocation)
        
        // If we're very close to the end of this step, show next step instruction
        if (distanceToEndOfStep < 50 && currentStepIndex < route.steps.size - 1) {
            val nextStep = route.steps[currentStepIndex + 1]
            instruction = nextStep.instruction
            distanceToNext = nextStep.distanceValue
        } else {
            instruction = currentStep.instruction
            distanceToNext = distanceToEndOfStep.toInt()
        }
        
        return Pair(instruction, distanceToNext)
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        return DistanceCalculationUtils.calculateDistance(point1, point2)
    }
    
    /**
     * Create mock routes data for testing or when API key is not set
     */
    private fun createMockRoutes(origin: LatLng, destination: LatLng): List<RouteInfo> {
        // Create a more realistic route that follows a road-like pattern instead of straight line
        val steps = mutableListOf<RouteInfo.Step>()
        
        // Calculate midpoints with offsets to simulate road segments
        val midLat1 = origin.latitude + (destination.latitude - origin.latitude) * 0.25 + 0.0025
        val midLng1 = origin.longitude + (destination.longitude - origin.longitude) * 0.25 - 0.0015
        
        val midLat2 = origin.latitude + (destination.latitude - origin.latitude) * 0.5 - 0.0018
        val midLng2 = origin.longitude + (destination.longitude - origin.longitude) * 0.5 + 0.0032
        
        val midLat3 = origin.latitude + (destination.latitude - origin.latitude) * 0.75 + 0.0028
        val midLng3 = origin.longitude + (destination.longitude - origin.longitude) * 0.75 + 0.0022
        
        // Create LatLng points for the mock route
        val p1 = LatLng(midLat1, midLng1)
        val p2 = LatLng(midLat2, midLng2)
        val p3 = LatLng(midLat3, midLng3)
        
        // Generate polylines for each step
        val step1Polyline = mutableListOf(origin)
        addIntermediatePoints(step1Polyline, origin, p1, 10)
        
        val step2Polyline = mutableListOf(p1)
        addIntermediatePoints(step2Polyline, p1, p2, 10)
        
        val step3Polyline = mutableListOf(p2)
        addIntermediatePoints(step3Polyline, p2, p3, 10)
        
        val step4Polyline = mutableListOf(p3)
        addIntermediatePoints(step4Polyline, p3, destination, 10)
        
        // Combine step polylines to create the main route polyline
        val fullPath = (step1Polyline + step2Polyline + step3Polyline + step4Polyline).distinct()
        
        // Add steps with their respective polylines
        steps.add(
            RouteInfo.Step(
                instruction = "Head southwest toward Main St",
                distance = "500 m",
                distanceValue = 500,
                duration = "2 mins",
                startLocation = origin,
                endLocation = p1,
                maneuver = "straight",
                polylinePoints = step1Polyline
            )
        )
        
        steps.add(
            RouteInfo.Step(
                instruction = "Turn left onto Central Ave",
                distance = "750 m",
                distanceValue = 750,
                duration = "3 mins",
                startLocation = p1,
                endLocation = p2,
                maneuver = "turn-left",
                polylinePoints = step2Polyline
            )
        )
        
        steps.add(
            RouteInfo.Step(
                instruction = "Turn right onto Bike Path",
                distance = "650 m",
                distanceValue = 650,
                duration = "2 mins",
                startLocation = p2,
                endLocation = p3,
                maneuver = "turn-right",
                polylinePoints = step3Polyline
            )
        )
        
        steps.add(
            RouteInfo.Step(
                instruction = "Arrive at your destination",
                distance = "300 m",
                distanceValue = 300,
                duration = "1 min",
                startLocation = p3,
                endLocation = destination,
                maneuver = "arrive",
                polylinePoints = step4Polyline
            )
        )
        
        // Create mock routes with the road-like path and steps
        val totalDistance = 2200 // meters
        val totalDuration = 8 * 60 // seconds
        
        // Create a slightly different alternative route with its own steps and polylines
        val altSteps = mutableListOf<RouteInfo.Step>()
        val altMidLat1 = origin.latitude + (destination.latitude - origin.latitude) * 0.3 - 0.004
        val altMidLng1 = origin.longitude + (destination.longitude - origin.longitude) * 0.3 + 0.003
        val altP1 = LatLng(altMidLat1, altMidLng1)
        
        val altMidLat2 = origin.latitude + (destination.latitude - origin.latitude) * 0.6 + 0.0035
        val altMidLng2 = origin.longitude + (destination.longitude - origin.longitude) * 0.6 - 0.0025
        val altP2 = LatLng(altMidLat2, altMidLng2)
        
        val altStep1Polyline = mutableListOf(origin)
        addIntermediatePoints(altStep1Polyline, origin, altP1, 10)
        val altStep2Polyline = mutableListOf(altP1)
        addIntermediatePoints(altStep2Polyline, altP1, altP2, 10)
        val altStep3Polyline = mutableListOf(altP2)
        addIntermediatePoints(altStep3Polyline, altP2, destination, 10)
        
        val altFullPath = (altStep1Polyline + altStep2Polyline + altStep3Polyline).distinct()
        
        altSteps.add(
            RouteInfo.Step(
                instruction = "Head north on Park Ave",
                distance = "800 m",
                distanceValue = 800,
                duration = "4 mins",
                startLocation = origin,
                endLocation = altP1,
                maneuver = "straight",
                polylinePoints = altStep1Polyline
            )
        )
        altSteps.add(
            RouteInfo.Step(
                instruction = "Slight right onto River Rd",
                distance = "1100 m",
                distanceValue = 1100,
                duration = "5 mins",
                startLocation = altP1,
                endLocation = altP2,
                maneuver = "slight-right",
                polylinePoints = altStep2Polyline
            )
        )
        altSteps.add(
            RouteInfo.Step(
                instruction = "Arrive at destination",
                distance = "400 m",
                distanceValue = 400,
                duration = "2 mins",
                startLocation = altP2,
                endLocation = destination,
                maneuver = "arrive",
                polylinePoints = altStep3Polyline
            )
        )
        
        return listOf(
            RouteInfo(
                distance = "2.2 km",
                duration = "8 min",
                distanceValue = totalDistance,
                durationValue = totalDuration,
                polylinePoints = fullPath,
                steps = steps
            ),
            RouteInfo(
                distance = "2.3 km",
                duration = "11 min",
                distanceValue = 2300,
                durationValue = 11 * 60,
                polylinePoints = altFullPath,
                steps = altSteps
            )
        )
    }
    
    /**
     * Helper function to add intermediate points between two locations
     * to create a more realistic path
     */
    private fun addIntermediatePoints(path: MutableList<LatLng>, start: LatLng, end: LatLng, pointCount: Int) {
        // This function creates more realistic mock road paths by simulating road curves
        // rather than just straight lines with jitter
        
        // Calculate direct distance and bearing between start and end
        val startToEndDistance = calculateDistance(start, end)
        val bearing = calculateBearing(start, end)
        
        // Create a curved path with more points for longer segments
        val actualPointCount = if (startToEndDistance > 500) pointCount * 2 else pointCount
        val random = java.util.Random(start.latitude.toLong() + end.longitude.toLong())
        
        // Create a bezier curve effect by adding control points
        val controlPoint1 = calculateOffsetPoint(start, bearing + 30, (startToEndDistance * 0.3).toFloat())
        val controlPoint2 = calculateOffsetPoint(end, bearing + 210, (startToEndDistance * 0.3).toFloat())
        
        for (i in 1..actualPointCount) {
            val t = i.toDouble() / (actualPointCount + 1)
            val t2 = t * t
            val t3 = t2 * t
            val mt = 1 - t
            val mt2 = mt * mt
            val mt3 = mt2 * mt
            
            // Cubic bezier formula to create a smoother curve
            val lat = start.latitude * mt3 + 
                      3 * controlPoint1.latitude * mt2 * t + 
                      3 * controlPoint2.latitude * mt * t2 + 
                      end.latitude * t3
                      
            val lng = start.longitude * mt3 + 
                      3 * controlPoint1.longitude * mt2 * t + 
                      3 * controlPoint2.longitude * mt * t2 + 
                      end.longitude * t3
            
            // Add small random jitter for natural appearance
            val jitter = 0.00005 * (random.nextDouble() - 0.5) * startToEndDistance / 100
            
            path.add(LatLng(lat + jitter, lng + jitter))
        }
    }
    
    // Helper function to calculate bearing between two points
    private fun calculateBearing(start: LatLng, end: LatLng): Double {
        val startLat = Math.toRadians(start.latitude)
        val startLng = Math.toRadians(start.longitude)
        val endLat = Math.toRadians(end.latitude)
        val endLng = Math.toRadians(end.longitude)
        
        val dLng = endLng - startLng
        
        val y = Math.sin(dLng) * Math.cos(endLat)
        val x = Math.cos(startLat) * Math.sin(endLat) -
                Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng)
        
        var bearing = Math.toDegrees(Math.atan2(y, x))
        if (bearing < 0) {
            bearing += 360
        }
        
        return bearing
    }
    
    // Helper function to calculate a point at a given bearing and distance
    private fun calculateOffsetPoint(start: LatLng, bearing: Double, distance: Float): LatLng {
        val distanceRadians = distance / 6371000.0 // Earth's radius in meters
        val bearingRadians = Math.toRadians(bearing)
        val startLatRadians = Math.toRadians(start.latitude)
        val startLngRadians = Math.toRadians(start.longitude)
        
        val endLatRadians = Math.asin(
            Math.sin(startLatRadians) * Math.cos(distanceRadians) +
            Math.cos(startLatRadians) * Math.sin(distanceRadians) * Math.cos(bearingRadians)
        )
        
        val endLngRadians = startLngRadians + Math.atan2(
            Math.sin(bearingRadians) * Math.sin(distanceRadians) * Math.cos(startLatRadians),
            Math.cos(distanceRadians) - Math.sin(startLatRadians) * Math.sin(endLatRadians)
        )
        
        return LatLng(
            Math.toDegrees(endLatRadians),
            Math.toDegrees(endLngRadians)
        )
    }
    
    /**
     * Debug function to log key parts of the route response
     */
    private fun debugRouteResponse(jsonResponse: String) {
        try {
            val json = JSONObject(jsonResponse)
            if (!json.has("routes")) {
                Log.e(TAG, "No routes found in response")
                return
            }
            
            val routes = json.getJSONArray("routes")
            Log.d(TAG, "Routes found: ${routes.length()}")
            
            if (routes.length() > 0) {
                val firstRoute = routes.getJSONObject(0)
                val hasPolyline = firstRoute.has("polyline")
                val hasLegs = firstRoute.has("legs")
                
                Log.d(TAG, "Route details: has polyline=$hasPolyline, has legs=$hasLegs")
                
                if (hasLegs) {
                    val legs = firstRoute.getJSONArray("legs")
                    val firstLeg = legs.getJSONObject(0)
                    val hasSteps = firstLeg.has("steps")
                    
                    Log.d(TAG, "Legs found: ${legs.length()}, first leg has steps=$hasSteps")
                    
                    if (hasSteps) {
                        val steps = firstLeg.getJSONArray("steps")
                        Log.d(TAG, "Steps in first leg: ${steps.length()}")
                        
                        if (steps.length() > 0) {
                            val firstStep = steps.getJSONObject(0)
                            val hasStepPolyline = firstStep.has("polyline")
                            val hasNavInstruction = firstStep.has("navigationInstruction")
                            
                            Log.d(TAG, "First step: has polyline=$hasStepPolyline, has nav instruction=$hasNavInstruction")
                            
                            // Examine the location structure in detail
                            if (firstStep.has("startLocation")) {
                                val startLoc = firstStep.getJSONObject("startLocation")
                                debugJsonObject("startLocation", startLoc)
                            }
                            
                            if (firstStep.has("endLocation")) {
                                val endLoc = firstStep.getJSONObject("endLocation")
                                debugJsonObject("endLocation", endLoc)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing route response", e)
        }
    }
    
    /**
     * Helper function to debug JSON objects by listing their keys and structure
     */
    private fun debugJsonObject(name: String, jsonObject: JSONObject) {
        val keys = jsonObject.keys().asSequence().toList()
        Log.d(TAG, "$name keys: $keys")
        
        keys.forEach { key ->
            try {
                val value = jsonObject.get(key)
                if (value is JSONObject) {
                    val nestedKeys = value.keys().asSequence().toList()
                    Log.d(TAG, "$name.$key (object) keys: $nestedKeys")
                } else {
                    Log.d(TAG, "$name.$key = $value (${value.javaClass.simpleName})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inspecting $name.$key", e)
            }
        }
    }
}