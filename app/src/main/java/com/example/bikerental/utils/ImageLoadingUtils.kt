package com.example.bikerental.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import java.io.File

/**
 * Optimized image loading utilities for better performance
 */
object ImageLoadingUtils {
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * Get or create optimized ImageLoader instance
     */
    fun getOptimizedImageLoader(context: Context): ImageLoader {
        return imageLoader ?: run {
            val newLoader = ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.15) // Use 15% of available memory
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.cacheDir, "image_cache"))
                        .maxSizeBytes(50 * 1024 * 1024) // 50MB disk cache
                        .build()
                }
                .crossfade(300) // Smooth transitions
                .respectCacheHeaders(false) // For consistent caching
                .build()
            
            imageLoader = newLoader
            newLoader
        }
    }
    
    /**
     * Create optimized image request for bike images
     */
    fun createBikeImageRequest(
        context: Context,
        imageUrl: String,
        bikeId: String,
        targetSize: Size = Size.ORIGINAL
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .size(targetSize)
            .memoryCacheKey("bike_${bikeId}")
            .diskCacheKey("bike_${bikeId}")
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(300)
            .build()
    }
    
    /**
     * Create optimized image request for thumbnails
     */
    fun createThumbnailImageRequest(
        context: Context,
        imageUrl: String,
        bikeId: String
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .size(300, 300) // Fixed thumbnail size
            .memoryCacheKey("bike_thumb_${bikeId}")
            .diskCacheKey("bike_thumb_${bikeId}")
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(200)
            .build()
    }
    
    /**
     * Clear image cache when needed
     */
    fun clearCache(context: Context) {
        imageLoader?.let { loader ->
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
    }
} 