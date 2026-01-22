package com.example.expensetracker

data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val merchant: String,
    val paymentMethod: String,
    val reference: String,
    val merchantSource: MerchantSource = MerchantSource.SENDER,
    val date: Long // NEW: Original SMS date
)