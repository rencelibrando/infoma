package com.example.bikerental.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(Log::class)
class LogManagerTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock Android's Log class
        PowerMockito.mockStatic(Log::class.java)
        `when`(Log.d(anyString(), anyString())).thenReturn(0)
        `when`(Log.i(anyString(), anyString())).thenReturn(0)
        `when`(Log.w(anyString(), anyString())).thenReturn(0)
        `when`(Log.e(anyString(), anyString())).thenReturn(0)
        
        // Reset LogManager state for each test
        resetLogManager()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `test log level filtering`() = runBlocking {
        // Set log level to INFO
        setLogLevel(LogManager.LogLevel.INFO)
        
        // DEBUG logs should be filtered out
        LogManager.d("TestTag", "Debug message")
        verify(Log::class.java, times(0)).d(anyString(), anyString())
        
        // INFO logs should pass through
        LogManager.i("TestTag", "Info message")
        verify(Log::class.java, times(1)).i(anyString(), anyString())
        
        // WARNING logs should pass through
        LogManager.w("TestTag", "Warning message")
        verify(Log::class.java, times(1)).w(anyString(), anyString())
        
        // ERROR logs should pass through
        LogManager.e("TestTag", "Error message")
        verify(Log::class.java, times(1)).e(anyString(), anyString())
    }
    
    @Test
    fun `test rate limiting`() = runBlocking {
        // Set a low rate limit for testing
        val testLimit = 5
        setRateLimit(testLimit)
        
        // Log more messages than the limit
        for (i in 1..testLimit + 5) {
            LogManager.d("TestTag", "Message $i")
        }
        
        // Only 'testLimit' number of messages should have been logged
        verify(Log::class.java, times(testLimit)).d(anyString(), anyString())
    }
    
    @Test
    fun `test thread safety with multiple coroutines`() = runBlocking {
        val numCoroutines = 10
        val messagesPerCoroutine = 20
        val testLimit = 50 // Allow half the messages
        setRateLimit(testLimit)
        
        val jobs = List(numCoroutines) { coroutineId ->
            launch {
                for (i in 1..messagesPerCoroutine) {
                    LogManager.d("TestTag", "Message from coroutine $coroutineId: $i")
                }
            }
        }
        
        jobs.forEach { it.join() }
        
        // Verify rate limiting works across coroutines
        // Total messages attempted: numCoroutines * messagesPerCoroutine = 200
        // But rate limit is 50, so only 50 should get through
        verify(Log::class.java, times(testLimit)).d(anyString(), anyString())
    }
    
    @Test
    fun `test concurrent debug and info logs with different tags`() = runBlocking {
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
        
        // Each tag has its own counter, so we should see all logs
        verify(Log::class.java, times(messageCount)).d(anyString(), anyString())
        verify(Log::class.java, times(messageCount)).i(anyString(), anyString())
    }
    
    // Helper methods to access and modify LogManager internals for testing
    
    private fun resetLogManager() {
        // Clear the counters
        val countersField = LogManager::class.java.getDeclaredField("logCounters")
        makeAccessible(countersField)
        val counters = countersField.get(LogManager) as java.util.concurrent.ConcurrentHashMap<*, *>
        counters.clear()
        
        // Clear the last reset times
        val resetTimesField = LogManager::class.java.getDeclaredField("lastResetTime")
        makeAccessible(resetTimesField)
        val resetTimes = resetTimesField.get(LogManager) as java.util.concurrent.ConcurrentHashMap<*, *>
        resetTimes.clear()
        
        // Reset log level to DEBUG
        setLogLevel(LogManager.LogLevel.DEBUG)
    }
    
    private fun setLogLevel(level: LogManager.LogLevel) {
        val logLevelField = LogManager::class.java.getDeclaredField("globalLogLevel")
        makeAccessible(logLevelField)
        logLevelField.set(LogManager, level)
    }
    
    private fun setRateLimit(limit: Int) {
        val rateLimitField = LogManager::class.java.getDeclaredField("DEFAULT_MAX_LOGS_PER_MINUTE")
        makeAccessible(rateLimitField)
        
        // Remove final modifier
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(rateLimitField, rateLimitField.modifiers and Modifier.FINAL.inv())
        
        // Set new value
        rateLimitField.set(null, limit)
    }
    
    private fun makeAccessible(field: Field) {
        field.isAccessible = true
    }
} 