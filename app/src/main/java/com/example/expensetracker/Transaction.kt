package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "transactions") // entity = database table
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,       // "Salary" or "Ice Cream"
    val amount: Double,      // 5000.00
    val category: String,    // "Food", "Salary", "Rent"
    val date: Long,          // Timestamp
    val type: String,         // "INCOME" or "EXPENSE"
    val paymentMethod: String, // NEW: e.g., "Cash", "UPI", "Card"
    val reference: String? = null,
    val needsReview: Boolean = true
)