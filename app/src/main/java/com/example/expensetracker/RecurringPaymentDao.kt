package com.example.expensetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments")
    fun getAllRecurringPayments(): Flow<List<RecurringPayment>>

    @Upsert
    suspend fun upsertRecurringPayment(recurringPayment: RecurringPayment)

    @Delete
    suspend fun deleteRecurringPayment(recurringPayment: RecurringPayment)

    @Query("SELECT * FROM recurring_payments WHERE isActive = 1")
    suspend fun getActiveRecurringPayments(): List<RecurringPayment>
}
