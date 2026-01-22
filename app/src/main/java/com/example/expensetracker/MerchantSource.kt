package com.example.expensetracker

enum class MerchantSource {
    BODY,    // extracted from SMS text (Amazon, Swiggy, etc.)
    SENDER   // fallback (SBI, HDFC, etc.)
}
