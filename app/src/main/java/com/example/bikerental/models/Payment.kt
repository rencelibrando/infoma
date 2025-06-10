package com.example.bikerental.models

import com.google.firebase.Timestamp
import java.util.UUID

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val mobileNumber: String = "",
    val referenceNumber: String = "",
    val amount: Double = 0.0,
    val screenshotUrl: String = "",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val businessName: String = "Bambike Cycles",
    val gcashNumber: String = "09123456789",
    val bikeType: String = "",
    val duration: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null,
    val processedBy: String? = null,
    val notes: String = ""
)

enum class PaymentStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

data class PaymentRequest(
    val mobileNumber: String,
    val referenceNumber: String,
    val amount: Double,
    val bikeType: String,
    val duration: String,
    val screenshotUri: String? = null
)

// New data class for unpaid bookings in payment tab
data class UnpaidBooking(
    val bookingId: String,
    val bikeId: String,
    val bikeName: String,
    val bikeType: String,
    val bikeImageUrl: String = "",
    val startDate: Long,
    val endDate: Long,
    val totalPrice: Double,
    val duration: String,
    val status: BookingStatus,
    val isHourly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Get formatted duration string
     */
    fun getFormattedDuration(): String {
        return if (isHourly) {
            val hours = (endDate - startDate) / (1000 * 60 * 60)
            if (hours <= 1) "1 hour" else "$hours hours"
        } else {
            val days = ((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1
            if (days <= 1) "1 day" else "$days days"
        }
    }
    
    /**
     * Get formatted price string
     */
    fun getFormattedPrice(): String {
        return "₱%.2f".format(totalPrice)
    }

    
    /**
     * Get payment urgency level
     */
    fun getPaymentUrgency(): PaymentUrgency {
        val currentTime = System.currentTimeMillis()
        val timeLeft = endDate - currentTime
        val hoursLeft = timeLeft / (1000 * 60 * 60)
        
        return when {
            currentTime > endDate -> PaymentUrgency.OVERDUE
            hoursLeft <= 2 -> PaymentUrgency.URGENT
            hoursLeft <= 24 -> PaymentUrgency.MODERATE
            else -> PaymentUrgency.LOW
        }
    }
}

enum class PaymentUrgency {
    LOW,
    MODERATE, 
    URGENT,
    OVERDUE
} 