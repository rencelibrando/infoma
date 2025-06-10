import com.example.bikerental.di.DispatcherProvider
import com.example.bikerental.utils.LogManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dispatcherProvider: DispatcherProvider
) {
    companion object {
        private const val TAG = "FirestoreRepository"
        private const val QUERY_LIMIT = 50 // Default query size to prevent large result sets
    }
    
    init {
        // Enable Firestore offline persistence with size limit to prevent excessive disk usage
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
        
        // Set up offline persistence with indexing
        try {
            firestore.enableNetwork().addOnCompleteListener { 
                LogManager.d(TAG, "Firebase network enabled: ${it.isSuccessful}")
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "Error enabling Firestore network", e)
        }
    }

    /**
     * Gets document reference with proper dispatching
     */
    fun <T> getDocument(
        collectionPath: String,
        documentId: String,
        type: Class<T>,
        includeMetadata: Boolean = false
    ): Flow<Result<T>> = callbackFlow {
        val docRef = firestore.collection(collectionPath).document(documentId)
        
        // Use Source.CACHE for first quick load if available, then SERVER for fresh data
        // This prevents UI blocking while waiting for network
        val initialResult = withContext(dispatcherProvider.io()) {
            try {
                val cachedDoc = docRef.get(Source.CACHE).await()
                if (cachedDoc.exists()) {
                    cachedDoc.toObject(type)
                } else {
                    null
                }
            } catch (e: Exception) {
                LogManager.w(TAG, "Cache miss for $collectionPath/$documentId", e)
                null
            }
        }
        
        if (initialResult != null) {
            send(Result.success(initialResult))
        }
        
        // Setup real-time listener with Source.SERVER
        val registration = docRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                LogManager.e(TAG, "Error fetching document: $collectionPath/$documentId", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                // Only process if data came from server or we want local changes
                if (includeMetadata || !snapshot.metadata.isFromCache) {
                    try {
                        val item = snapshot.toObject(type)
                        if (item != null) {
                            trySend(Result.success(item))
                        } else {
                            trySend(Result.failure(IllegalStateException("Document exists but could not be converted to ${type.simpleName}")))
                        }
                    } catch (e: Exception) {
                        LogManager.e(TAG, "Error converting document: $collectionPath/$documentId", e)
                        trySend(Result.failure(e))
                    }
                }
            } else {
                trySend(Result.failure(NoSuchElementException("Document $documentId does not exist")))
            }
        }
        
        awaitClose {
            // Clean up the listener when the flow collection ends
            registration.remove()
        }
    }

    /**
     * Queries collection with pagination and proper dispatching
     */
    fun <T> queryCollection(
        collectionPath: String,
        type: Class<T>,
        queryBuilder: (CollectionReference) -> Query = { it },
        pageSize: Int = QUERY_LIMIT
    ): Flow<Result<List<T>>> = flow {
        withContext(dispatcherProvider.io()) {
            try {
                // First try to get from cache for instant UI response
                val cacheQuery = queryBuilder(firestore.collection(collectionPath))
                    .limit(pageSize.toLong())
                
                try {
                    val cacheSnapshot = cacheQuery.get(Source.CACHE).await()
                    val cacheItems = cacheSnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(type)
                        } catch (e: Exception) {
                            LogManager.e(TAG, "Error converting cached document in $collectionPath", e)
                            null
                        }
                    }
                    
                    if (cacheItems.isNotEmpty()) {
                        emit(Result.success(cacheItems))
                    }
                } catch (e: Exception) {
                    LogManager.d(TAG, "Cache miss for query on $collectionPath")
                }
                
                // Then get from server
                val serverQuery = queryBuilder(firestore.collection(collectionPath))
                    .limit(pageSize.toLong())
                val snapshot = serverQuery.get(Source.SERVER).await()
                
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(type)
                    } catch (e: Exception) {
                        LogManager.e(TAG, "Error converting document in $collectionPath", e)
                        null
                    }
                }
                
                emit(Result.success(items))
            } catch (e: Exception) {
                LogManager.e(TAG, "Error querying collection: $collectionPath", e)
                emit(Result.failure(e))
            }
        }
    }

    // ... other methods with similar optimizations ...
} 