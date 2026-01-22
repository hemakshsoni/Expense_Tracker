package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dues")
data class Due(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val amount: Double,
    val paidAmount: Double = 0.0, // NEW: Track partial payments
    val isLent: Boolean, // true if user lent money, false if user borrowed
    val date: Long,
    val isPaid: Boolean = false,
    val note: String = ""
)
