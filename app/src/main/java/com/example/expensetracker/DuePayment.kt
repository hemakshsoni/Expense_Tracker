package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "due_payments")
data class DuePayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dueId: Int,
    val amount: Double,
    val date: Long,
    val note: String = ""
)
