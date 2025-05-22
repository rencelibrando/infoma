package com.example.bikerental.utils

import com.example.bikerental.utils.LogManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class for optimizing Firestore operations
 */
object FirestoreHelper {
    const val TAG = "FirestoreHelper"
    
    // Cache to prevent duplicate reads of the same document in rapid succession
    val documentCache = ConcurrentHashMap<String, CachedDocument<*>>()
    
    // Default cache TTL
    const val DEFAULT_CACHE_TTL_MS = 60_000L // 1 minute
    
    /**
     * Gets document with caching to prevent redundant reads
     */
    internal suspend inline fun <reified T> getDocumentCached(
        docRef: DocumentReference,
        source: Source = Source.SERVER,
        cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS
    ): T? {
        val cacheKey = "${docRef.path}:${T::class.java.simpleName}"
        
        // Check cache first
        val cached = documentCache[cacheKey]
        if (cached != null && cached.timestamp + cacheTtlMs > System.currentTimeMillis()) {
            @Suppress("UNCHECKED_CAST")
            return cached.data as? T
        }
        
        return try {
            val snapshot = docRef.get(source).await()
            if (snapshot.exists()) {
                val result = snapshot.toObject(T::class.java)
                // Update cache
                if (result != null) {
                    documentCache[cacheKey] = CachedDocument(result, System.currentTimeMillis())
                }
                result
            } else {
                null
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "Error fetching document: ${docRef.path}", e)
            // On error, return cached data if available (even if expired)
            @Suppress("UNCHECKED_CAST")
            cached?.data as? T
        }
    }
    
    /**
     * Executes a query with error handling and optimized settings
     */
    suspend inline fun <reified T> executeQueryOptimized(
        query: Query,
        source: Source = Source.SERVER
    ): List<T> {
        return try {
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
    
    /**
     * Clears the document cache
     */
    fun clearCache() {
        documentCache.clear()
    }
    
    /**
     * Class to hold cached document data
     */
    data class CachedDocument<T>(
        val data: T,
        val timestamp: Long
    )
} 