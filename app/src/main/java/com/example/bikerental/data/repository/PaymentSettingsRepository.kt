package com.example.bikerental.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PaymentSettings(
    val gcashNumber: String = "09123456789",
    val businessName: String = "Bambike Cycles",
    val qrCodeUrl: String = ""
)

@Singleton
class PaymentSettingsRepository @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val settingsCollection = firestore.collection("settings")
    
    suspend fun getPaymentSettings(): PaymentSettings {
        return try {
            val document = settingsCollection.document("payment").get().await()
            if (document.exists()) {
                document.toObject(PaymentSettings::class.java) ?: PaymentSettings()
            } else {
                PaymentSettings()
            }
        } catch (e: Exception) {
            PaymentSettings()
        }
    }
    
    fun getPaymentSettingsFlow(): Flow<PaymentSettings> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            listenerRegistration = settingsCollection.document("payment")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(PaymentSettings())
                        return@addSnapshotListener
                    }
                    
                    val settings = if (snapshot?.exists() == true) {
                        snapshot.toObject(PaymentSettings::class.java) ?: PaymentSettings()
                    } else {
                        PaymentSettings()
                    }
                    
                    trySend(settings)
                }
        } catch (e: Exception) {
            trySend(PaymentSettings())
        }
        
        awaitClose {
            listenerRegistration?.remove()
        }
    }
} 