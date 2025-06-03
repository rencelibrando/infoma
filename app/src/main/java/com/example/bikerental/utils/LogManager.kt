package com.example.bikerental.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.example.bikerental.BuildConfig

/**
 * Centralized logging utility that handles both Android logging and Crashlytics reporting.
 */
object LogManager {
    // Define log levels
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }
    
    // Allow log level to be toggled based on build config
    private var LOG_LEVEL = if (BuildConfig.DEBUG) Log.VERBOSE else Log.INFO
    
    // Control whether to send non-error logs to Crashlytics
    private const val SEND_INFO_TO_CRASHLYTICS = false
    
    // Maximum log length
    private const val MAX_LOG_LENGTH = 4000
    
    /**
     * Configure the LogManager with the specified LogLevel
     */
    fun configure(logLevel: LogLevel) {
        LOG_LEVEL = when (logLevel) {
            LogLevel.VERBOSE -> Log.VERBOSE
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
            LogLevel.NONE -> Int.MAX_VALUE
        }
    }
    
    /**
     * Set log level for dynamic configuration
     */
    fun setLogLevel(logLevel: LogLevel) {
        configure(logLevel)
    }
    
    /**
     * Log verbose message
     */
    fun v(tag: String, message: String) {
        if (LOG_LEVEL <= Log.VERBOSE) {
            log(Log.VERBOSE, tag, message)
        }
    }
    
    /**
     * Log debug message
     */
    fun d(tag: String, message: String) {
        if (LOG_LEVEL <= Log.DEBUG) {
            log(Log.DEBUG, tag, message)
        }
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String) {
        if (LOG_LEVEL <= Log.INFO) {
            log(Log.INFO, tag, message)
            if (SEND_INFO_TO_CRASHLYTICS) {
                FirebaseCrashlytics.getInstance().log("$tag: $message")
            }
        }
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (LOG_LEVEL <= Log.WARN) {
            if (throwable != null) {
                log(Log.WARN, tag, "$message\n${throwable.stackTraceToString()}")
            } else {
                log(Log.WARN, tag, message)
            }
            
            // Record warning in Crashlytics
            FirebaseCrashlytics.getInstance().log("WARN: $tag: $message")
            if (throwable != null) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        }
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (LOG_LEVEL <= Log.ERROR) {
            if (throwable != null) {
                log(Log.ERROR, tag, "$message\n${throwable.stackTraceToString()}")
            } else {
                log(Log.ERROR, tag, message)
            }
            
            // Record error in Crashlytics
            FirebaseCrashlytics.getInstance().log("ERROR: $tag: $message")
            if (throwable != null) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        }
    }
    
    // Convenience functions for easier usage
    /**
     * Log debug message - convenience function
     */
    fun logDebug(tag: String, message: String) {
        d(tag, message)
    }
    
    /**
     * Log info message - convenience function
     */
    fun logInfo(tag: String, message: String) {
        i(tag, message)
    }
    
    /**
     * Log warning message - convenience function
     */
    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        w(tag, message, throwable)
    }
    
    /**
     * Log error message - convenience function
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        e(tag, message, throwable)
    }
    
    /**
     * Log a message and handles chunking for long messages
     */
    private fun log(priority: Int, tag: String, message: String) {
        // Split by line, then ensure each line can fit into Log's maximum length
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = minOf(newline, i + MAX_LOG_LENGTH)
                Log.println(priority, tag, message.substring(i, end))
                i = end
            } while (i < newline)
            i++
        }
    }
}

// Top-level convenience functions for even easier access
fun logDebug(tag: String, message: String) = LogManager.logDebug(tag, message)
fun logInfo(tag: String, message: String) = LogManager.logInfo(tag, message)
fun logWarning(tag: String, message: String, throwable: Throwable? = null) = LogManager.logWarning(tag, message, throwable)
fun logError(tag: String, message: String, throwable: Throwable? = null) = LogManager.logError(tag, message, throwable) 