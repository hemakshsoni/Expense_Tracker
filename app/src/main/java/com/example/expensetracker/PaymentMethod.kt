package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,          // "HDFC", "SBI", "Cash", "Wallet"
    val initialBalance: Double = 0.0,
    val colorHex: String = "#6200EE", // Store color as hex string
    val isDefault: Boolean = false
)