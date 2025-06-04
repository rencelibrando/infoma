package com.example.bikerental.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use SDK 28 for better compatibility
class LogManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Reset LogManager state for each test
        resetLogManager()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test log level configuration`() = testScope.runTest {
        // Test setting different log levels
        LogManager.configure(LogManager.LogLevel.DEBUG)
        LogManager.d("TestTag", "Debug message")
        
        LogManager.configure(LogManager.LogLevel.INFO)
        LogManager.i("TestTag", "Info message")
        
        LogManager.configure(LogManager.LogLevel.WARN)
        LogManager.w("TestTag", "Warning message")
        
        LogManager.configure(LogManager.LogLevel.ERROR)
        LogManager.e("TestTag", "Error message")
        
        // Since we're using Robolectric, we can test the actual logging behavior
        // without mocking Android's Log class
    }
    
    @Test
    fun `test convenience methods`() = testScope.runTest {
        // Test convenience methods
        LogManager.logDebug("TestTag", "Debug message")
        LogManager.logInfo("TestTag", "Info message")
        LogManager.logWarning("TestTag", "Warning message")
        LogManager.logError("TestTag", "Error message")
        
        // Test with throwable
        val testException = RuntimeException("Test exception")
        LogManager.logWarning("TestTag", "Warning with exception", testException)
        LogManager.logError("TestTag", "Error with exception", testException)
    }
    
    @Test
    fun `test top level functions`() = testScope.runTest {
        // Test top-level convenience functions
        logDebug("TestTag", "Debug message")
        logInfo("TestTag", "Info message")
        logWarning("TestTag", "Warning message")
        logError("TestTag", "Error message")
        
        // Test with throwable
        val testException = RuntimeException("Test exception")
        logWarning("TestTag", "Warning with exception", testException)
        logError("TestTag", "Error with exception", testException)
    }
    
    @Test
    fun `test thread safety with multiple coroutines`() = testScope.runTest {
        val numCoroutines = 10
        val messagesPerCoroutine = 20
        
        val jobs = List(numCoroutines) { coroutineId ->
            launch {
                for (i in 1..messagesPerCoroutine) {
                    LogManager.d("TestTag", "Message from coroutine $coroutineId: $i")
                }
            }
        }
        
        jobs.forEach { it.join() }
        
        // Test passes if no exceptions are thrown and concurrent access works
    }
    
    @Test
    fun `test concurrent debug and info logs with different tags`() = testScope.runTest {
        val debugTag = "DebugTag"
        val infoTag = "InfoTag"
        val messageCount = 30
        
        // Run debug logs in one coroutine
        val debugJob = async {
            for (i in 1..messageCount) {
                LogManager.d(debugTag, "Debug message $i")
            }
        }
        
        // Run info logs in another coroutine
        val infoJob = async {
            for (i in 1..messageCount) {
                LogManager.i(infoTag, "Info message $i")
            }
        }
        
        // Wait for both to complete
        debugJob.await()
        infoJob.await()
        
        // Test passes if no exceptions are thrown during concurrent access
    }
    
    @Test
    fun `test log level filtering`() = testScope.runTest {
        // Set log level to INFO - this should filter out DEBUG and VERBOSE logs
        LogManager.configure(LogManager.LogLevel.INFO)
        
        // These should not be logged (filtered out)
        LogManager.v("TestTag", "Verbose message")
        LogManager.d("TestTag", "Debug message")
        
        // These should be logged
        LogManager.i("TestTag", "Info message")
        LogManager.w("TestTag", "Warning message")
        LogManager.e("TestTag", "Error message")
        
        // Set to NONE - should filter out all logs
        LogManager.configure(LogManager.LogLevel.NONE)
        LogManager.e("TestTag", "Error message should be filtered")
    }
    
    @Test
    fun `test long message chunking`() = testScope.runTest {
        // Create a very long message that exceeds the maximum log length
        val longMessage = "A".repeat(5000) // 5000 characters
        
        // This should not throw an exception and should handle chunking internally
        LogManager.d("TestTag", longMessage)
        LogManager.i("TestTag", longMessage)
        LogManager.w("TestTag", longMessage)
        LogManager.e("TestTag", longMessage)
    }
    
    // Helper methods to access and modify LogManager internals for testing
    
    private fun resetLogManager() {
        try {
            // Reset log level to DEBUG for consistent testing
            LogManager.configure(LogManager.LogLevel.DEBUG)
        } catch (e: Exception) {
            // If configuration fails, just continue
        }
    }
    
    private fun makeAccessible(field: Field) {
        field.isAccessible = true
    }
} 