package com.example.bikerental.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple performance monitoring utility for tracking app startup and key operation times
 */
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    
    private val _startupMetrics = MutableStateFlow<Map<String, Long>>(emptyMap())
    val startupMetrics: StateFlow<Map<String, Long>> = _startupMetrics.asStateFlow()
    
    private val startTimes = mutableMapOf<String, Long>()
    private val metrics = mutableMapOf<String, Long>()
    
    /**
     * Start tracking a performance metric
     */
    fun startTimer(operation: String) {
        val currentTime = System.currentTimeMillis()
        startTimes[operation] = currentTime
        Log.d(TAG, "Started timer for: $operation at $currentTime")
    }
    
    /**
     * Stop tracking a performance metric and log the duration
     */
    fun endTimer(operation: String): Long {
        val endTime = System.currentTimeMillis()
        val startTime = startTimes[operation]
        
        if (startTime != null) {
            val duration = endTime - startTime
            metrics[operation] = duration
            _startupMetrics.value = metrics.toMap()
            
            Log.d(TAG, "Completed $operation in ${duration}ms")
            startTimes.remove(operation)
            return duration
        } else {
            Log.w(TAG, "No start time found for operation: $operation")
            return -1L
        }
    }
    
    /**
     * Log all current metrics
     */
    fun logAllMetrics() {
        Log.d(TAG, "=== Performance Metrics ===")
        metrics.forEach { (operation, duration) ->
            Log.d(TAG, "$operation: ${duration}ms")
        }
        Log.d(TAG, "========================")
    }
    
    /**
     * Get the duration of a specific operation
     */
    fun getMetric(operation: String): Long? {
        return metrics[operation]
    }
    
    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        startTimes.clear()
        metrics.clear()
        _startupMetrics.value = emptyMap()
        Log.d(TAG, "Cleared all performance metrics")
    }
    
    /**
     * Check if startup is taking too long and log warnings
     */
    fun checkStartupPerformance() {
        val authInitTime = getMetric("auth_initialization")
        val totalStartupTime = getMetric("total_startup")
        val mainActivityTime = getMetric("main_activity_creation")
        
        authInitTime?.let { time ->
            if (time > 3000) { // More than 3 seconds
                Log.w(TAG, "WARNING: Auth initialization took ${time}ms (>3s)")
            }
        }
        
        totalStartupTime?.let { time ->
            if (time > 5000) { // More than 5 seconds
                Log.w(TAG, "WARNING: Total startup took ${time}ms (>5s)")
            } else if (time > 2500) { // More than 2.5 seconds
                Log.i(TAG, "INFO: Startup time is ${time}ms (acceptable but could be optimized)")
            } else {
                Log.i(TAG, "SUCCESS: Fast startup achieved in ${time}ms")
            }
        }
        
        mainActivityTime?.let { time ->
            if (time > 200) { // More than 200ms for main activity creation
                Log.w(TAG, "WARNING: MainActivity creation took ${time}ms (>200ms) - may cause UI blocking")
            }
        }
        
        // Log recommendations for frame drops
        if (totalStartupTime != null && totalStartupTime > 2000) {
            Log.i(TAG, "RECOMMENDATION: If experiencing frame drops, consider:")
            Log.i(TAG, "- Moving heavy operations to background threads")
            Log.i(TAG, "- Simplifying splash screen animations")
            Log.i(TAG, "- Lazy loading non-critical components")
        }
    }
    
    /**
     * Log frame drop detection
     */
    fun logFrameDropDetected(framesSkipped: Int, context: String) {
        if (framesSkipped > 10) {
            Log.w(TAG, "FRAME_DROPS: Skipped $framesSkipped frames in $context")
            Log.w(TAG, "SUGGESTION: Check for heavy operations on main thread")
        }
    }
    
    /**
     * Benchmark against target performance
     */
    fun benchmarkResults() {
        val totalStartup = getMetric("total_startup")
        val authInit = getMetric("auth_initialization")
        
        Log.i(TAG, "=== PERFORMANCE BENCHMARK ===")
        totalStartup?.let {
            val improvement = if (it < 2000) "EXCELLENT" else if (it < 3000) "GOOD" else "NEEDS_IMPROVEMENT"
            Log.i(TAG, "Startup Time: ${it}ms [$improvement]")
        }
        
        authInit?.let {
            val improvement = if (it < 1000) "EXCELLENT" else if (it < 3000) "GOOD" else "NEEDS_IMPROVEMENT"
            Log.i(TAG, "Auth Init: ${it}ms [$improvement]")
        }
        Log.i(TAG, "=============================")
    }
} 