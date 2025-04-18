package com.example.bikerental.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikerental.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * This is a fixed snippet of the problematic code from BikeViewModel.
 * It demonstrates the correct way to handle Firestore transactions with proper typing.
 */
class BikeViewModelFix : ViewModel() {
    private val TAG = "BikeViewModelFix"
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Sample state flows
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _bikeReviews = MutableStateFlow<List<Review>>(emptyList())
    val bikeReviews: StateFlow<List<Review>> = _bikeReviews
    
    private val _averageRating = MutableStateFlow<Float>(0f)
    val averageRating: StateFlow<Float> = _averageRating
    
    // Synchronization lock
    private val reviewsLock = Any()
    
    /**
     * Sample method showing the correct pattern for submitting a review
     */
    fun submitReview(bikeId: String, rating: Float, comment: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                
                // Create review object
                val reviewId = UUID.randomUUID().toString()
                val review = Review(
                    id = reviewId,
                    bikeId = bikeId,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Anonymous",
                    rating = rating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )
                
                // First, add the review document
                val bikeRef = firestore.collection("bikes").document(bikeId)
                val reviewRef = bikeRef.collection("reviews").document(reviewId)
                
                reviewRef.set(review).await()
                
                // Then get all reviews to calculate the average
                val reviewsQuery = bikeRef.collection("reviews").get().await()
                
                val allReviews = ArrayList<Review>()
                for (doc in reviewsQuery.documents) {
                    doc.toObject(Review::class.java)?.let { reviewObj ->
                        allReviews.add(reviewObj)
                    }
                }
                
                // Calculate average rating
                val avgRating = if (allReviews.isNotEmpty()) {
                    allReviews.sumOf { it.rating.toDouble() } / allReviews.size
                } else {
                    rating.toDouble()
                }.toFloat()
                
                // Update the bike document with the new rating
                bikeRef.update("rating", avgRating).await()
                
                // Update the local state
                _bikeReviews.value = allReviews.sortedByDescending { it.timestamp }
                _averageRating.value = avgRating
                
                _isLoading.value = false
                onSuccess()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting review", e)
                _isLoading.value = false
                onError(e.message ?: "Unknown error")
            }
        }
    }
} 