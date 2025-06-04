package com.example.bikerental.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance monitoring utility for tracking UI performance metrics
 */
object PerformanceMonitor {
    internal const val TAG = "PerformanceMonitor"
    private val startTimes = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * Start timing an operation
     */
    fun startTiming(operationName: String) {
        startTimes[operationName] = System.currentTimeMillis()
        Log.d(TAG, "Started timing: $operationName")
    }
    
    /**
     * End timing and log the result
     */
    fun endTiming(operationName: String) {
        val startTime = startTimes.remove(operationName)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            scope.launch {
                logPerformance(operationName, duration)
            }
        } else {
            Log.w(TAG, "No start time found for operation: $operationName")
        }
    }
    
    /**
     * Time a suspend function
     */
    suspend fun <T> timeOperation(
        operationName: String,
        operation: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            withContext(Dispatchers.Default) {
                logPerformance(operationName, duration)
            }
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            withContext(Dispatchers.Default) {
                Log.e(TAG, "$operationName failed after ${duration}ms", e)
            }
            throw e
        }
    }
    
    /**
     * Time a non-suspend function
     */
    fun <T> timeOperationSync(
        operationName: String,
        operation: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            scope.launch {
                logPerformance(operationName, duration)
            }
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "$operationName failed after ${duration}ms", e)
            throw e
        }
    }
    
    /**
     * Log performance metrics
     */
    internal suspend fun logPerformance(operationName: String, duration: Long) {
        withContext(Dispatchers.Default) {
            when {
                duration > 2000 -> Log.w(TAG, "⚠️ SLOW: $operationName took ${duration}ms")
                duration > 1000 -> Log.i(TAG, "⚡ MEDIUM: $operationName took ${duration}ms")
                else -> Log.d(TAG, "✅ FAST: $operationName took ${duration}ms")
            }
        }
    }
    
    /**
     * Log memory usage with a specific tag
     */
    fun logMemoryUsage(tag: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        Log.d(TAG, "[$tag] Memory Usage: " +
            "Used: ${formatBytes(usedMemory)}, " +
            "Available: ${formatBytes(availableMemory)}, " +
            "Max: ${formatBytes(maxMemory)}, " +
            "Usage: ${((usedMemory.toFloat() / maxMemory) * 100).toInt()}%")
    }
    
    /**
     * Log cache cleanup results
     */
    fun logCacheCleanup(
        distanceRemoved: Int,
        statusRemoved: Int,
        remainingDistance: Int,
        remainingStatus: Int
    ) {
        Log.d(TAG, "Cache Cleanup: " +
            "Distance removed: $distanceRemoved, remaining: $remainingDistance, " +
            "Status removed: $statusRemoved, remaining: $remainingStatus")
    }
    
    /**
     * Log cache usage statistics
     */
    fun logCacheUsage(
        distanceCacheSize: Int,
        statusCacheSize: Int
    ) {
        Log.d(TAG, "Cache Usage: " +
            "Distance cache: $distanceCacheSize entries, " +
            "Status cache: $statusCacheSize entries")
    }

    /**
     * Format bytes to human readable format
     */
    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
    
    /**
     * Clear all timing data
     */
    fun clear() {
        startTimes.clear()
    }
} 