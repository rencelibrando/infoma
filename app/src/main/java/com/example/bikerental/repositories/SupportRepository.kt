package com.example.bikerental.repositories

import android.net.Uri
import android.util.Log
import com.example.bikerental.models.FAQ
import com.example.bikerental.models.SupportMessage
import com.example.bikerental.models.SupportReply
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.example.bikerental.models.OperatingHour
import com.example.bikerental.models.LocationDetails

@Singleton
class SupportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {
    private val supportMessagesCollection = firestore.collection("supportMessages")
    private val faqsCollection = firestore.collection("faqs")
    private val operatingHoursCollection = firestore.collection("operating_hours")
    private val configCollection = firestore.collection("config")
    
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
    
    /**
     * Gets Operating Hours
     */
    suspend fun getOperatingHours(): Result<List<OperatingHour>> {
        return try {
            val snapshot = operatingHoursCollection.get().await()
            Log.d("SupportRepository", "Fetched ${snapshot.size()} operating hour documents.")
            val hours = snapshot.documents.mapNotNull { doc ->
                doc.toObject(OperatingHour::class.java)
            }
            
            // Sort the hours by day of the week
            val dayOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val sortedHours = hours.sortedBy { dayOrder.indexOf(it.day) }

            Result.success(sortedHours)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets Location Details
     */
    suspend fun getLocationDetails(): Result<LocationDetails> {
        return try {
            val snapshot = configCollection.document("location").get().await()
            val location = snapshot.toObject(LocationDetails::class.java)
            Result.success(location ?: LocationDetails())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets all replies for a specific support message
     */
    suspend fun getReplies(messageId: String): Result<List<SupportReply>> {
        return try {
            val snapshot = supportMessagesCollection.document(messageId)
                .collection("replies")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val replies = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SupportReply::class.java)
            }

            Result.success(replies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a reply from the user to a support message
     */
    suspend fun sendReply(messageId: String, replyText: String): Result<Unit> {
        return sendReplyWithImage(messageId, replyText, null)
    }
    
    /**
     * Uploads an image to Firebase Storage and returns the download URL
     */
    suspend fun uploadSupportImage(messageId: String, imageUri: Uri): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Create a reference to the file location in Firebase Storage
            val timestamp = System.currentTimeMillis()
            val imagePath = "support_images/${messageId}/${currentUser.uid}_${timestamp}.jpg"
            val imageRef = storage.reference.child(imagePath)
            
            // Log the upload attempt
            Log.d("SupportRepository", "Uploading image to $imagePath")
            
            // Upload the file
            val uploadTask = imageRef.putFile(imageUri).await()
            
            // Get the download URL
            val downloadUrl = imageRef.downloadUrl.await().toString()
            
            // Log success
            Log.d("SupportRepository", "Image uploaded successfully. URL: $downloadUrl")
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("SupportRepository", "Error uploading image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sends a reply with an optional image attachment
     */
    suspend fun sendReplyWithImage(messageId: String, replyText: String, imageUri: Uri?): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Upload image if provided
            var imageUrl: String? = null
            if (imageUri != null) {
                Log.d("SupportRepository", "Preparing to upload image for message $messageId")
                val uploadResult = uploadSupportImage(messageId, imageUri)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                    Log.d("SupportRepository", "Image upload successful, URL: $imageUrl")
                } else {
                    Log.e("SupportRepository", "Failed to upload image", uploadResult.exceptionOrNull())
                    return Result.failure(uploadResult.exceptionOrNull() 
                        ?: Exception("Failed to upload image"))
                }
            }
            
            val reply = SupportReply(
                text = replyText,
                sender = "user",
                userId = currentUser.uid,
                imageUrl = imageUrl
            )
            
            Log.d("SupportRepository", "Sending reply with${if (imageUrl != null) " image: $imageUrl" else "out image"}")

            supportMessagesCollection.document(messageId)
                .collection("replies")
                .add(reply)
                .await()

            // Also update the status of the main message to "in-progress"
            // if it was "resolved", to indicate the user has replied again.
            val messageRef = supportMessagesCollection.document(messageId)
            val messageDoc = messageRef.get().await()
            if (messageDoc.exists()) {
                val currentStatus = messageDoc.getString("status")
                if (currentStatus == "resolved" || currentStatus == "new") {
                     messageRef.update("status", "in-progress").await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupportRepository", "Error sending reply", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a support message by its ID
     */
    suspend fun deleteSupportMessage(messageId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
                
            // First, verify the message belongs to the current user
            val messageDoc = supportMessagesCollection.document(messageId).get().await()
            val message = messageDoc.toObject(SupportMessage::class.java)
            
            if (message == null) {
                return Result.failure(Exception("Message not found"))
            }
            
            if (message.userId != currentUser.uid) {
                return Result.failure(Exception("You don't have permission to delete this message"))
            }
            
            // Delete the message
            supportMessagesCollection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 