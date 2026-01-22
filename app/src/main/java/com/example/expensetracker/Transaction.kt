package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,       // "Salary" or "Ice Cream"
    val amount: Double,      // 5000.00
    val category: String,    // "Food", "Salary", "Rent"
    val date: Long,          // Timestamp
    val type: String,         // "INCOME", "EXPENSE", "TRANSFER"
    val paymentMethod: String, // e.g., "Cash", "UPI" (Source for Transfer)
    val toPaymentMethod: String? = null, // Destination for Transfer
    val reference: String? = null,
    val needsReview: Boolean = true,
    val merchantSource: MerchantSource = MerchantSource.SENDER,
    val allowLearning: Boolean = false,
    val isAutoDetected: Boolean = false,
    val description: String = ""
)