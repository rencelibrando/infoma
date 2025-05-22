package com.example.bikerental.utils

import com.google.firebase.BuildConfig

/**
 * Utility extension functions to make logging more convenient.
 * These extensions allow any class to log with its class name as the tag.
 */

/**
 * Get a simple tag from a class name (removes package info)
 */
fun Any.getTag(): String {
    val fullClassName = this.javaClass.name
    val simpleClassName = fullClassName.substringAfterLast('.')
    return if (simpleClassName.length <= 23) {
        simpleClassName
    } else {
        // Android LogCat has a 23 character limit for tags, so truncate if needed
        simpleClassName.takeLast(23)
    }
}

/**
 * Log a debug message using the class name as tag
 */
fun Any.logD(message: String) {
    LogManager.d(this.getTag(), message)
}

/**
 * Log an info message using the class name as tag
 */
fun Any.logI(message: String) {
    LogManager.i(this.getTag(), message)
}

/**
 * Log a warning message using the class name as tag
 */
fun Any.logW(message: String) {
    LogManager.w(this.getTag(), message)
}

/**
 * Log an error message using the class name as tag
 */
fun Any.logE(message: String, throwable: Throwable? = null) {
    LogManager.e(this.getTag(), message, throwable)
}

/**
 * Log a message only in debug builds, using class name as tag
 */
inline fun Any.logDebug(message: () -> String) {
    if (BuildConfig.DEBUG) {
        LogManager.d(this.getTag(), message())
    }
}

/**
 * Log an error with exception, using class name as tag
 */
fun Any.logException(message: String, throwable: Throwable) {
    LogManager.e(this.getTag(), message, throwable)
}

/**
 * Log with custom tag (when class name isn't appropriate)
 */
object L {
    fun d(tag: String, message: String) = LogManager.d(tag, message)
    fun i(tag: String, message: String) = LogManager.i(tag, message)
    fun w(tag: String, message: String) = LogManager.w(tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = 
        LogManager.e(tag, message, throwable)
} 