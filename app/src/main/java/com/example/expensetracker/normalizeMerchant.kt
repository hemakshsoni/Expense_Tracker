package com.example.expensetracker

fun normalizeMerchant(name: String): String {
    return name
        .uppercase()
        .replace(Regex("[^A-Z0-9 ]"), "") // remove punctuation/symbols
        .replace(Regex("\\s+"), " ")      // collapse multiple spaces
        .trim()
}
