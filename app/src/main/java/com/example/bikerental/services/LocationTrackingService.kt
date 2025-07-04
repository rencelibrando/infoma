package com.example.bikerental.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.bikerental.R
import com.example.bikerental.models.BikeLocation
import com.example.bikerental.utils.AppConfigManager
import com.example.bikerental.utils.LogManager
import com.example.bikerental.utils.DistanceCalculationUtils
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Foreground service for continuous location tracking during active rides
 * Sends real-time location updates to Firebase for admin tracking
 * Handles all Firebase persistence for ride data to avoid duplication
 */
class LocationTrackingService : Service() {
    
    private val TAG = "LocationTrackingService"
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var currentRideId: String? = null
    private var isTracking = false
    private var lastLocation: Location? = null
    private var locationCount = 0
    
    // Ride statistics tracking
    private var totalDistance: Double = 0.0 // in meters
    private var maxSpeed: Float = 0f // in km/h
    private var currentSpeed: Float = 0f // in km/h
    private var rideStartTime: Long = 0L
    
    // Add geofence-related fields to the class below other constants
    private val NOTIFICATION_ID = 12345
    private val GEOFENCE_NOTIFICATION_ID = 12346
    
    // App config manager to check if location restrictions are enabled
    private lateinit var appConfigManager: AppConfigManager
    private var isLocationRestrictionEnabled = true
    private var configListener: Job? = null
    
    companion object {
        const val CHANNEL_ID = "LocationTracking"
        const val CHANNEL_NAME = "Location Tracking"
        
        // Geofence notification constants
        const val GEOFENCE_CHANNEL_ID = "GeofenceBoundary"
        const val GEOFENCE_CHANNEL_NAME = "Boundary Alerts"
        
        // Tracking state
        private var serviceRunning = false
        private var currentRideId: String? = null
        
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_RIDE_ID = "ride_id"
        
        // Location update intervals
        const val UPDATE_INTERVAL = 3000L // 3 seconds for active tracking
        const val FASTEST_INTERVAL = 1000L // 1 second minimum
        const val DISPLACEMENT_THRESHOLD = 2f // 2 meters minimum displacement
        
        @JvmStatic
        fun startLocationTracking(context: Context, rideId: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_RIDE_ID, rideId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        @JvmStatic
        fun stopLocationTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
    
    // Add new field to track geofence warning state
    private var insideBoundaryWarningZone = false
    private var lastBoundaryWarningTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AppConfigManager
        appConfigManager = AppConfigManager.getInstance(applicationContext)
        
        // Start collecting the configuration flow to monitor changes
        configListener = serviceScope.launch {
            appConfigManager.isLocationRestrictionEnabled.collect { isEnabled ->
                isLocationRestrictionEnabled = isEnabled
                Log.d(TAG, "Location restriction setting updated: $isEnabled")
            }
        }
        
        setupLocationClient()
        createNotificationChannel()
        
        LogManager.logInfo(TAG, "Location tracking service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val rideId = intent.getStringExtra(EXTRA_RIDE_ID)
                if (rideId != null) {
                    startLocationTracking(rideId)
                }
            }
            ACTION_STOP_TRACKING -> {
                stopLocationTracking()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMinUpdateDistanceMeters(DISPLACEMENT_THRESHOLD)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
            
            override fun onLocationAvailability(availability: LocationAvailability) {
                LogManager.logInfo("LocationTrackingService", "Location availability: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    updateNotification("GPS signal lost - searching...")
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationTracking(rideId: String) {
        if (isTracking) {
            LogManager.logWarning("LocationTrackingService", "Location tracking already active")
            return
        }
        
        if (!hasLocationPermission()) {
            LogManager.logError("LocationTrackingService", "Location permission not granted")
            stopSelf()
            return
        }
        
        if (!hasNotificationPermission()) {
            LogManager.logWarning("LocationTrackingService", "Notification permission not granted - service may not show notifications")
            // Continue with service but warn about missing notification permission
        }
        
        currentRideId = rideId
        isTracking = true
        locationCount = 0
        
        // Initialize ride statistics
        totalDistance = 0.0
        maxSpeed = 0f
        currentSpeed = 0f
        rideStartTime = System.currentTimeMillis()
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification("Starting location tracking..."))
        
        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        LogManager.logInfo("LocationTrackingService", "Location tracking started for ride: $rideId")
        updateNotification("Tracking location - Ride active")
    }
    
    private fun stopLocationTracking() {
        if (!isTracking) return
        
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        // Send final location update with stopped status and final statistics
        currentRideId?.let { rideId ->
            serviceScope.launch {
                lastLocation?.let { location ->
                    sendLocationUpdate(location, rideId, isActive = false)
                }
                
                // Ensure complete cleanup of real-time data
                cleanupRealTimeData(rideId)
            }
        }
        
        LogManager.logInfo("LocationTrackingService", "Location tracking stopped. Total updates: $locationCount, Distance: ${String.format("%.2f", totalDistance/1000)} km")
        
        // Reset ride statistics
        totalDistance = 0.0
        maxSpeed = 0f
        currentSpeed = 0f
        rideStartTime = 0L
        locationCount = 0
        currentRideId = null
        
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * Cleanup all real-time tracking data when ride ends
     */
    private suspend fun cleanupRealTimeData(rideId: String) {
        val userId = auth.currentUser?.uid ?: return
        val realtimeDb = FirebaseDatabase.getInstance()
        
        try {
            // Remove from active rides
            realtimeDb.getReference("activeRides")
                .child(userId)
                .removeValue()
                .await()
            
            // Remove live location tracking
            realtimeDb.getReference("liveLocation")
                .child(userId)
                .removeValue()
                .await()
            
            LogManager.logInfo("LocationTrackingService", "Real-time data cleanup completed for ride: $rideId")
        } catch (e: Exception) {
            LogManager.logError("LocationTrackingService", "Error during real-time data cleanup: ${e.message}")
        }
    }
    
    private fun handleLocationUpdate(location: Location) {
        // Calculate ride statistics with GPS noise filtering using centralized utility
        lastLocation?.let { previousLocation ->
            val distanceMeters = DistanceCalculationUtils.calculateDistance(
                previousLocation.latitude, previousLocation.longitude,
                location.latitude, location.longitude
            )
            
            // Filter out unrealistic GPS jumps
            val timeDiffMs = location.time - previousLocation.time
            if (timeDiffMs > 0) {
                val timeDiffHours = timeDiffMs / 3600000.0 // Convert to hours
                val calculatedSpeed = (distanceMeters / 1000.0) / timeDiffHours // km/h
                
                // Only add distance if calculated speed is realistic (< 100 km/h for bikes)
                if (calculatedSpeed < 100.0) {
                    totalDistance += distanceMeters // Add distance in meters
                } else {
                    LogManager.logWarning("LocationTrackingService", "Filtering unrealistic GPS jump: ${String.format("%.2f", calculatedSpeed)} km/h")
                }
            } else {
                // If no time difference, add distance anyway (could be same timestamp)
                totalDistance += distanceMeters
            }
        }
        
        // Update speed statistics with validation
        val rawSpeed = if (location.hasSpeed()) location.speed else 0f
        currentSpeed = rawSpeed * 3.6f // Convert m/s to km/h
        
        // Filter unrealistic speeds and update max
        if (currentSpeed > 0 && currentSpeed < 100) {
            if (currentSpeed > maxSpeed) {
                maxSpeed = currentSpeed
            }
        } else {
            currentSpeed = 0f // Reset to 0 if unrealistic
        }
        
        lastLocation = location
        locationCount++
        
        currentRideId?.let { rideId ->
            serviceScope.launch {
                val bikeLocation = BikeLocation.fromLocation(location)
                sendLocationUpdate(location, rideId, isActive = true)
                
                // Update ride statistics and POV navigation via BikeViewModel
                val bikeViewModel = com.example.bikerental.viewmodels.BikeViewModel.getInstance()
                bikeViewModel?.let { vm ->
                    // Update traditional ride stats
                    vm.updateRideStats(rideId, bikeLocation)
                    
                    // Update POV navigation with new location
                    val latLng = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
                    vm.updateCurrentLocation(latLng)
                    
                    // Update speed for real-time display
                    vm.updateCurrentSpeed(currentSpeed)
                    vm.updateMaxSpeed(maxSpeed)
                }
            }
        }
        
        // Update notification with current stats
        val speed = currentSpeed.toInt()
        val distanceKm = totalDistance / 1000.0
        updateNotification("Speed: ${speed} km/h • Distance: ${String.format("%.2f", distanceKm)} km")
        
        // Create LatLng object for geofence checking
        val latLng = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
        
        // Check geofence boundary
        checkGeofenceBoundary(latLng)
    }
    
    private fun sendLocationUpdate(location: Location, rideId: String, isActive: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            val bikeLocation = BikeLocation.fromLocation(location)
            
            // PRIMARY: Use Realtime Database for real-time location tracking (fast, cost-effective)
            val realtimeLocationData = bikeLocation.toMap().toMutableMap().apply {
                put("userId", userId)
                put("rideId", rideId)
                put("isActive", isActive)
                put("timestamp", com.google.firebase.database.ServerValue.TIMESTAMP)
                put("deviceTimestamp", location.time)
                put("sessionId", "${userId}_${rideId}")
                put("locationCount", locationCount)
            }
            
            // Real-time location for live tracking (Realtime DB)
            val realtimeDb = FirebaseDatabase.getInstance()
            
            // Store current location for real-time tracking
            realtimeDb.getReference("liveLocation")
                .child(userId)
                .setValue(realtimeLocationData)
                .addOnSuccessListener {
                    LogManager.logDebug("LocationTrackingService", "Live location updated successfully")
                }
                .addOnFailureListener { e ->
                    LogManager.logError("LocationTrackingService", "Failed to update live location: ${e.message}")
                }
            
            // Store location history for route tracking (Realtime DB - more efficient)
            if (isActive) {
                val historyRef = realtimeDb.getReference("rideLocationHistory")
                    .child(rideId)
                    .push()
                
                val historyData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "altitude" to location.altitude,
                    "speed" to bikeLocation.speedKmh,
                    "bearing" to bikeLocation.bearing,
                    "accuracy" to bikeLocation.accuracy,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "deviceTimestamp" to location.time
                )
                
                historyRef.setValue(historyData)
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to save location history: ${e.message}")
                    }
            }
            
            // Update active rides for admin dashboard (user-based tracking)
            if (isActive) {
                val currentLocationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "speed" to bikeLocation.speedKmh,
                    "bearing" to bikeLocation.bearing,
                    "accuracy" to bikeLocation.accuracy
                )
                
                val activeRideData = mapOf(
                    "rideId" to rideId,
                    "userId" to userId,
                    "userName" to (auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"),
                    "userEmail" to (auth.currentUser?.email ?: ""),
                    "currentLocation" to currentLocationData,
                    "lastLocationUpdate" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "status" to "active",
                    "isActive" to true,
                    "locationCount" to locationCount,
                    "sessionId" to "${userId}_${rideId}",
                    // Add ride statistics
                    "totalDistance" to totalDistance,
                    "maxSpeed" to maxSpeed.toDouble(),
                    "currentSpeed" to currentSpeed.toDouble(),
                    "rideDuration" to (System.currentTimeMillis() - rideStartTime)
                )
                
                // Store by userId for admin dashboard tracking
                realtimeDb.getReference("activeRides")
                    .child(userId)
                    .updateChildren(activeRideData)
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to update active ride: ${e.message}")
                    }
                
                // Update the main ride record in Realtime Database
                val rideUpdates = mapOf(
                    "lastLocation" to bikeLocation.toMap(),
                    "distanceTraveled" to totalDistance,
                    "maxSpeed" to maxSpeed.toDouble(),
                    "currentSpeed" to currentSpeed.toDouble(),
                    "lastLocationUpdate" to com.google.firebase.database.ServerValue.TIMESTAMP
                )
                
                realtimeDb.getReference("rides")
                    .child(rideId)
                    .updateChildren(rideUpdates)
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to update ride statistics: ${e.message}")
                    }
            } else {
                // Remove from active rides when ride ends
                realtimeDb.getReference("activeRides")
                    .child(userId)
                    .removeValue()
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to remove active ride: ${e.message}")
                    }
                    
                // Mark live location as inactive
                realtimeDb.getReference("liveLocation")
                    .child(userId)
                    .child("isActive")
                    .setValue(false)
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to deactivate live location: ${e.message}")
                    }
            }
            
            // SECONDARY: Periodic Firestore updates for persistence (every 10th update to reduce costs)
            if (locationCount % 10 == 0 || !isActive) {
                val firestoreLocationData = mapOf(
                    "rideId" to rideId,
                    "userId" to userId,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "altitude" to location.altitude,
                    "speed" to bikeLocation.speedKmh,
                    "bearing" to bikeLocation.bearing,
                    "accuracy" to bikeLocation.accuracy,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "deviceTimestamp" to location.time,
                    "isActive" to isActive,
                    "locationCount" to locationCount
                )
                
                // Store in Firestore for long-term persistence (reduced frequency)
                db.collection("rideLocationHistory")
                    .add(firestoreLocationData)
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to save Firestore location history: ${e.message}")
                    }
                
                // Update ride statistics in Firestore (every 10th update or when ending)
                val firestoreRideUpdates = mapOf(
                    "lastLocation" to mapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "timestamp" to location.time
                    ),
                    "currentLatitude" to location.latitude,
                    "currentLongitude" to location.longitude,
                    "totalDistance" to totalDistance,
                    "maxSpeed" to maxSpeed.toDouble(),
                    "currentSpeed" to currentSpeed.toDouble(),
                    "lastLocationUpdate" to System.currentTimeMillis(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                
                db.collection("rides")
                    .document(rideId)
                    .update(firestoreRideUpdates)
                    .addOnFailureListener { e ->
                        LogManager.logError("LocationTrackingService", "Failed to update ride statistics in Firestore: ${e.message}")
                    }
            }
                
        } catch (e: Exception) {
            LogManager.logError("LocationTrackingService", "Error processing location update: ${e.message}")
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for Android 12 and below
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking for bike rides"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ride in Progress")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        if (!hasNotificationPermission()) {
            LogManager.logWarning("LocationTrackingService", "Notification permission not granted, cannot update notification")
            return
        }
        
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        
        // Cancel the config listener
        configListener?.cancel()
        
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        
        LogManager.logInfo("LocationTrackingService", "Service destroyed")
    }
    
    // Add this new method to monitor geofence boundary
    private fun checkGeofenceBoundary(location: com.google.android.gms.maps.model.LatLng) {
        try {
            // Skip geofence checking if location restrictions are disabled
            if (!isLocationRestrictionEnabled) {
                // If we were previously inside warning zone, reset it
                if (insideBoundaryWarningZone) {
                    insideBoundaryWarningZone = false
                    Log.d(TAG, "Location restrictions disabled - ignoring geofence boundaries")
                }
                return
            }
            
            val locationManager = com.example.bikerental.utils.LocationManager.getInstance(this)
            
            // If not in Intramuros at all, don't process (handled by ride start checks)
            if (!locationManager.isWithinIntramuros(location)) {
                return
            }
            
            // Check if near the boundary (but still inside)
            val isNearBoundary = locationManager.isNearIntramurosExit(location)
            
            // If state changed to near boundary or it's been more than 60 seconds since last warning
            val currentTime = System.currentTimeMillis()
            if (isNearBoundary && 
                (!insideBoundaryWarningZone || currentTime - lastBoundaryWarningTime > 60000)) {
                
                // Update warning state
                insideBoundaryWarningZone = true
                lastBoundaryWarningTime = currentTime
                
                // Show boundary warning notification
                showBoundaryWarningNotification()
            } else if (!isNearBoundary) {
                // Reset warning state when safely inside
                insideBoundaryWarningZone = false
            }
        } catch (e: Exception) {
            LogManager.logError("LocationTrackingService", "Error checking geofence boundary: ${e.message}")
        }
    }
    
    // Add this to show a boundary warning notification
    private fun showBoundaryWarningNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create the notification channel for boundary warnings (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GEOFENCE_CHANNEL_ID,
                GEOFENCE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when approaching service area boundary"
                enableVibration(true)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Build the notification
        val notification = NotificationCompat.Builder(this, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setContentTitle("⚠️ Approaching Boundary")
            .setContentText("You're near the edge of Intramuros. Please turn around to stay in the service area.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()
        
        // Show the notification
        notificationManager.notify(GEOFENCE_NOTIFICATION_ID, notification)
    }
} 