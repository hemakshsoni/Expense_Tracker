package com.example.expensetracker

import android.app.Application

class ExpenseApp: Application() {
    val database by lazy {
        TransactionDatabase.getDatabase(this)
    }
}