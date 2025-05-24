package com.example.bikerental.repositories

import com.example.bikerental.models.FAQ
import com.example.bikerental.models.SupportMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val supportMessagesCollection = firestore.collection("supportMessages")
    private val faqsCollection = firestore.collection("faqs")
    
    /**
     * Submits a new support message from the user
     */
    suspend fun submitSupportMessage(subject: String, message: String, userPhone: String): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val supportMessage = SupportMessage(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "",
                userEmail = currentUser.email ?: "",
                userPhone = userPhone,
                subject = subject,
                message = message
            )
            
            val docRef = supportMessagesCollection.add(supportMessage).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets all support messages for the current user
     */
    suspend fun getUserSupportMessages(): Result<List<SupportMessage>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = supportMessagesCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SupportMessage::class.java)
            }
            
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets all FAQs ordered by their display order
     */
    suspend fun getFAQs(): Result<List<FAQ>> {
        return try {
            val snapshot = faqsCollection
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val faqs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FAQ::class.java)
            }
            
            Result.success(faqs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 