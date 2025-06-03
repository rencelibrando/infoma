package com.example.bikerental.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling app permissions
 */
object PermissionUtils {
    
    // Permission request codes
    const val REQUEST_LOCATION_PERMISSION = 1001
    const val REQUEST_NOTIFICATION_PERMISSION = 1002
    const val REQUEST_ALL_PERMISSIONS = 1003
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if notification permission is granted (Android 13+ only)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for Android 12 and below
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasLocationPermission(context) && hasNotificationPermission(context)
    }
    
    /**
     * Request location permission
     */
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }
    
    /**
     * Request notification permission (Android 13+ only)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }
    
    /**
     * Request all required permissions
     */
    fun requestAllPermissions(activity: Activity) {
        val permissions = mutableListOf<String>()
        
        if (!hasLocationPermission(activity)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (!hasNotificationPermission(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                REQUEST_ALL_PERMISSIONS
            )
        }
    }
    
    /**
     * Check if permission was granted from request result
     */
    fun isPermissionGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
    
    /**
     * Get permission rationale message for users
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> 
                "Location permission is required to track your bike rides and provide real-time navigation."
            Manifest.permission.POST_NOTIFICATIONS -> 
                "Notification permission is required to show ride status and location tracking updates."
            else -> "This permission is required for the app to function properly."
        }
    }
} 