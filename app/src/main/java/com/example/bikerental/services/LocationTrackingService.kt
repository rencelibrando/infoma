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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.bikerental.R
import com.example.bikerental.models.BikeLocation
import com.example.bikerental.utils.LogManager
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.util.*

/**
 * Foreground service for continuous location tracking during active rides
 * Sends real-time location updates to Firebase for admin tracking
 * Handles all Firebase persistence for ride data to avoid duplication
 */
class LocationTrackingService : Service() {
    
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
    
    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_RIDE_ID = "ride_id"
        
        // Location update intervals
        const val UPDATE_INTERVAL = 3000L // 3 seconds for active tracking
        const val FASTEST_INTERVAL = 1000L // 1 second minimum
        const val DISPLACEMENT_THRESHOLD = 2f // 2 meters minimum displacement
        
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
        
        fun stopLocationTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        setupLocationClient()
        createNotificationChannel()
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
            }
        }
        
        LogManager.logInfo("LocationTrackingService", "Location tracking stopped. Total updates: $locationCount, Distance: ${String.format("%.2f", totalDistance/1000)} km")
        
        // Reset ride statistics
        totalDistance = 0.0
        maxSpeed = 0f
        currentSpeed = 0f
        rideStartTime = 0L
        locationCount = 0
        
        stopForeground(true)
        stopSelf()
    }
    
    private fun handleLocationUpdate(location: Location) {
        // Calculate ride statistics
        lastLocation?.let { previousLocation ->
            val distance = FloatArray(1)
            Location.distanceBetween(
                previousLocation.latitude, previousLocation.longitude,
                location.latitude, location.longitude,
                distance
            )
            totalDistance += distance[0] // Add distance in meters
        }
        
        // Update speed statistics
        currentSpeed = location.speed * 3.6f // Convert m/s to km/h
        if (currentSpeed > maxSpeed) {
            maxSpeed = currentSpeed
        }
        
        lastLocation = location
        locationCount++
        
        currentRideId?.let { rideId ->
            serviceScope.launch {
                val bikeLocation = BikeLocation.fromLocation(location)
                sendLocationUpdate(location, rideId, isActive = true)
                
                // Update ride statistics via BikeViewModel (UI updates only)
                val bikeViewModel = com.example.bikerental.viewmodels.BikeViewModel.getInstance()
                bikeViewModel?.updateRideStats(rideId, bikeLocation)
            }
        }
        
        // Update notification with current stats
        val speed = currentSpeed.toInt()
        val distanceKm = totalDistance / 1000.0
        updateNotification("Speed: ${speed} km/h • Distance: ${String.format("%.2f", distanceKm)} km")
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
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location during active bike rides"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
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
            .setContentTitle("Bike Ride Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        if (!hasNotificationPermission()) {
            LogManager.logWarning("LocationTrackingService", "Notification permission not granted, cannot update notification")
            return
        }
        
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        
        LogManager.logInfo("LocationTrackingService", "Service destroyed")
    }
} 