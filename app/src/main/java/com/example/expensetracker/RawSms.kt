package com.example.expensetracker

data class RawSms(
    val sender: String,
    val body: String,
    val timestamp: Long
)