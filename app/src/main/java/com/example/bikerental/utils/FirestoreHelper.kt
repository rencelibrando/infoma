package com.example.bikerental.utils

import com.example.bikerental.utils.LogManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import android.util.LruCache

/**
 * Utility class for optimizing Firestore operations with efficient background execution
 */
object FirestoreHelper {
    const val TAG = "FirestoreHelper"
    
    // Improved cache implementation using LruCache for better memory management
    private val documentCache = LruCache<String, CachedDocument<*>>(100) // Limit to 100 entries
    
    // Secondary thread pool for background operations that shouldn't block the main coroutine dispatchers
    private val backgroundExecutor = Executors.newFixedThreadPool(2)
    
    // Initialization flag
    private val isInitialized = AtomicBoolean(false)
    
    // Default cache TTL
    const val DEFAULT_CACHE_TTL_MS = 60_000L // 1 minute
    const val EXTENDED_CACHE_TTL_MS = 300_000L // 5 minutes for less frequently changing data
    
    // Lazy initialization function - can be called early during app startup
    suspend fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            withContext(Dispatchers.IO) {
                try {
                    // Perform any Firestore-related initialization here
                    // For example, priming connections or preloading critical configuration
                    LogManager.d(TAG, "Firestore helper initialized")
                } catch (e: Exception) {
                    LogManager.e(TAG, "Failed to initialize Firestore helper", e)
                }
            }
        }
    }
    
    /**
     * Gets document with improved caching to prevent redundant reads
     * Optimized for background thread execution
     */
    internal suspend inline fun <reified T> getDocumentCached(
        docRef: DocumentReference,
        source: Source = Source.SERVER,
        cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS
    ): T? {
        val cacheKey = "${docRef.path}:${T::class.java.simpleName}"
        
        // Check cache first - memory efficient operation
        val cached = documentCache.get(cacheKey) as? CachedDocument<T>
        if (cached != null && cached.timestamp + cacheTtlMs > System.currentTimeMillis()) {
            return cached.data
        }
        
        // Execute the Firestore query on a background thread
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = docRef.get(source).await()
                if (snapshot.exists()) {
                    val result = snapshot.toObject(T::class.java)
                    // Update cache
                    if (result != null) {
                        documentCache.put(cacheKey, CachedDocument(result, System.currentTimeMillis()))
                    }
                    result
                } else {
                    null
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "Error fetching document: ${docRef.path}", e)
                // On error, return cached data if available (even if expired)
                cached?.data
            }
        }
    }
    
    /**
     * Prefetches a document in the background without blocking, useful during app startup
     */
    fun prefetchDocument(
        docRef: DocumentReference,
        cacheTtlMs: Long = EXTENDED_CACHE_TTL_MS
    ) {
        backgroundExecutor.execute {
            try {
                val snapshot = docRef.get(Source.CACHE).addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val cacheKey = "${docRef.path}:prefetch"
                        documentCache.put(cacheKey, CachedDocument(snapshot, System.currentTimeMillis()))
                        LogManager.d(TAG, "Prefetched document: ${docRef.path}")
                    }
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "Error prefetching document: ${docRef.path}", e)
            }
        }
    }
    
    /**
     * Executes a query with error handling and optimized settings
     * Always runs on background thread
     */
    suspend inline fun <reified T> executeQueryOptimized(
        query: Query,
        source: Source = Source.SERVER
    ): List<T> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = query.get(source).await()
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(T::class.java)
                    } catch (e: Exception) {
                        LogManager.e(TAG, "Error converting document: ${doc.id}", e)
                        null
                    }
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "Error executing query", e)
                emptyList()
            }
        }
    }
    
    /**
     * Clears the document cache
     */
    fun clearCache() {
        documentCache.evictAll()
    }
    
    /**
     * Shutdown background resources
     */
    fun shutdown() {
        try {
            backgroundExecutor.shutdown()
            backgroundExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            LogManager.e(TAG, "Error shutting down background executor", e)
            backgroundExecutor.shutdownNow()
        }
    }
    
    /**
     * Class to hold cached document data
     */
    data class CachedDocument<T>(
        val data: T,
        val timestamp: Long
    )
} 