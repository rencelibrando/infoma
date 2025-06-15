package com.example.bikerental.utils

import android.content.Context
import android.util.Log
import com.example.bikerental.BuildConfig
import com.example.bikerental.R
import com.example.bikerental.config.ApiConfig
import com.example.bikerental.ui.theme.BikeLocation
import com.example.bikerental.ui.theme.RouteInfo
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Service to handle Google Maps Directions API requests and responses
 */
object DirectionsApiService {
    private const val TAG = "DirectionsApiService"
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"
    
    // OkHttp client for network requests
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    // API key will be retrieved from context to avoid hardcoding
    private var apiKey: String? = null
    
    /**
     * Initialize the service with context to get API key
     */
    fun initialize(context: Context) {
        apiKey = ApiConfig.getDirectionsApiKey(context)
        Log.d(TAG, "DirectionsApiService initialized with key: ${maskApiKey(apiKey)}")
    }
    
    /**
     * Mask API key for logging purposes
     */
    private fun maskApiKey(key: String?): String {
        return ApiConfig.maskApiKey(key)
    }
    
    /**
     * Get directions between two points
     */
    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        mode: String = "bicycling",
        alternatives: Boolean = false
    ): Result<List<RouteInfo>> = withContext(Dispatchers.IO) {
        try {
            // Return mock data in debug mode or if API key is not set
            if (!ApiConfig.isValidApiKey(apiKey) || BuildConfig.DEBUG) {
                Log.w(TAG, "Using mock directions data - no API key set or in debug mode")
                return@withContext Result.success(createMockDirections(origin, destination))
            }
            
            val url = buildDirectionsUrl(origin, destination, mode, alternatives)
            Log.d(TAG, "Requesting directions from Google Maps API")
            
            val request = Request.Builder()
                .url(url)
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed: ${response.code}")
                    return@withContext Result.failure(IOException("API call failed with code ${response.code}"))
                }
                
                val jsonResponse = response.body?.string() ?: ""
                return@withContext Result.success(parseDirectionsResponse(jsonResponse))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting directions", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Build the Directions API URL
     */
    private fun buildDirectionsUrl(
        origin: LatLng,
        destination: LatLng,
        mode: String,
        alternatives: Boolean
    ): String {
        return "$BASE_URL?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=$mode" +
                "&alternatives=${alternatives}" +
                "&key=$apiKey"
    }
    
    /**
     * Parse the JSON response from the Directions API
     */
    @Throws(JSONException::class)
    private fun parseDirectionsResponse(jsonResponse: String): List<RouteInfo> {
        val routes = mutableListOf<RouteInfo>()
        val jsonObject = JSONObject(jsonResponse)
        
        val status = jsonObject.getString("status")
        if (status != "OK") {
            Log.e(TAG, "API returned status: $status")
            return routes
        }
        
        val routesArray = jsonObject.getJSONArray("routes")
        for (i in 0 until routesArray.length()) {
            val route = routesArray.getJSONObject(i)
            val legs = route.getJSONArray("legs").getJSONObject(0)
            
            val distance = legs.getJSONObject("distance").getString("text")
            val duration = legs.getJSONObject("duration").getString("text")
            val distanceValue = legs.getJSONObject("distance").getInt("value")
            val durationValue = legs.getJSONObject("duration").getInt("value")
            
            val encodedPolyline = route.getJSONObject("overview_polyline").getString("points")
            val polylinePoints = decodePoly(encodedPolyline)
            
            // Parse steps for turn-by-turn instructions
            val stepsArray = legs.getJSONArray("steps")
            val steps = ArrayList<RouteInfo.Step>()
            
            for (j in 0 until stepsArray.length()) {
                val step = stepsArray.getJSONObject(j)
                val instruction = step.getString("html_instructions")
                val stepDistance = step.getJSONObject("distance").getString("text")
                val stepDistanceValue = step.getJSONObject("distance").getInt("value")
                val stepDuration = step.getJSONObject("duration").getString("text")
                
                val startLocation = step.getJSONObject("start_location")
                val startLat = startLocation.getDouble("lat")
                val startLng = startLocation.getDouble("lng")
                
                val endLocation = step.getJSONObject("end_location")
                val endLat = endLocation.getDouble("lat")
                val endLng = endLocation.getDouble("lng")
                
                var maneuver = ""
                if (step.has("maneuver")) {
                    maneuver = step.getString("maneuver")
                }
                
                steps.add(
                    RouteInfo.Step(
                        instruction = cleanHtmlInstructions(instruction),
                        distance = stepDistance,
                        distanceValue = stepDistanceValue,
                        duration = stepDuration,
                        startLocation = LatLng(startLat, startLng),
                        endLocation = LatLng(endLat, endLng),
                        maneuver = maneuver
                    )
                )
            }
            
            // Add summary to routes list
            routes.add(
                RouteInfo(
                    distance = distance,
                    duration = duration,
                    distanceValue = distanceValue,
                    durationValue = durationValue,
                    polylinePoints = polylinePoints,
                    steps = steps
                )
            )
        }
        
        return routes
    }
    
    /**
     * Clean HTML tags from instructions
     */
    private fun cleanHtmlInstructions(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
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
     * Create mock directions data for testing or when API key is not set
     */
    private fun createMockDirections(origin: LatLng, destination: LatLng): List<RouteInfo> {
        // Create a simple straight line between origin and destination
        val steps = mutableListOf<RouteInfo.Step>()
        
        // Add start step
        steps.add(
            RouteInfo.Step(
                instruction = "Head toward your destination",
                distance = "500 m",
                distanceValue = 500,
                duration = "2 mins",
                startLocation = origin,
                endLocation = LatLng(
                    origin.latitude + (destination.latitude - origin.latitude) * 0.3,
                    origin.longitude + (destination.longitude - origin.longitude) * 0.3
                ),
                maneuver = "straight"
            )
        )
        
        // Add middle step
        steps.add(
            RouteInfo.Step(
                instruction = "Continue toward destination",
                distance = "1.2 km",
                distanceValue = 1200,
                duration = "5 mins",
                startLocation = LatLng(
                    origin.latitude + (destination.latitude - origin.latitude) * 0.3,
                    origin.longitude + (destination.longitude - origin.longitude) * 0.3
                ),
                endLocation = LatLng(
                    origin.latitude + (destination.latitude - origin.latitude) * 0.7,
                    origin.longitude + (destination.longitude - origin.longitude) * 0.7
                ),
                maneuver = "straight"
            )
        )
        
        // Add final step
        steps.add(
            RouteInfo.Step(
                instruction = "Arrive at your destination",
                distance = "500 m",
                distanceValue = 500,
                duration = "2 mins",
                startLocation = LatLng(
                    origin.latitude + (destination.latitude - origin.latitude) * 0.7,
                    origin.longitude + (destination.longitude - origin.longitude) * 0.7
                ),
                endLocation = destination,
                maneuver = "arrive"
            )
        )
        
        // Create a simple polyline between origin and destination
        val polylinePoints = createMockPolyline(origin, destination)
        
        // Create multiple mock routes with slight variations
        val routes = mutableListOf<RouteInfo>()
        
        // Main route
        routes.add(
            RouteInfo(
                distance = "2.2 km",
                duration = "9 mins",
                distanceValue = 2200,
                durationValue = 540,
                polylinePoints = polylinePoints,
                steps = steps
            )
        )
        
        // Alternate route 1 (slightly longer)
        routes.add(
            RouteInfo(
                distance = "2.5 km",
                duration = "11 mins",
                distanceValue = 2500,
                durationValue = 660,
                polylinePoints = createMockPolyline(origin, destination, 0.02),
                steps = steps
            )
        )
        
        return routes
    }
    
    /**
     * Create mock polyline between two points with optional deviation
     */
    private fun createMockPolyline(origin: LatLng, destination: LatLng, deviation: Double = 0.0): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(origin)
        
        // Add intermediate points
        val numPoints = 10
        for (i in 1 until numPoints) {
            val ratio = i.toDouble() / numPoints
            val lat = origin.latitude + (destination.latitude - origin.latitude) * ratio
            val lng = origin.longitude + (destination.longitude - origin.longitude) * ratio
            
            // Add some randomness for more realistic paths
            val deviationLat = (Math.random() - 0.5) * 2 * deviation
            val deviationLng = (Math.random() - 0.5) * 2 * deviation
            
            points.add(LatLng(lat + deviationLat, lng + deviationLng))
        }
        
        points.add(destination)
        return points
    }
    
    /**
     * Get the next navigation instruction based on current location
     */
    fun getNextNavigationInstruction(
        currentLocation: LatLng,
        route: RouteInfo,
        currentStepIndex: Int
    ): Pair<String, Int> {
        // Validate step index
        val validStepIndex = currentStepIndex.coerceIn(0, route.steps.size - 1)
        val currentStep = route.steps[validStepIndex]
        
        // Calculate distance to the end of current step
        val distanceToStepEnd = calculateDistance(
            currentLocation,
            currentStep.endLocation
        )
        
        return Pair(currentStep.instruction, distanceToStepEnd.toInt())
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }
} 