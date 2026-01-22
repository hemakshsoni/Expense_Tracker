package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_payments")
data class RecurringPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val frequency: String, // "Daily", "Weekly", "Monthly", "Yearly"
    val nextDate: Long,    // Timestamp for next occurrence
    val lastProcessed: Long? = null,
    val isActive: Boolean = true
)
