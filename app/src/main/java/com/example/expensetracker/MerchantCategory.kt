package com.example.expensetracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_category",
    indices = [Index(value = ["merchant"], unique = true)]
)
data class MerchantCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,
    val category: String,
    val keywords: String = ""
)
