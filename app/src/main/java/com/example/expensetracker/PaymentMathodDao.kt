package com.example.expensetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods")
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>

    @Upsert
    suspend fun upsertPaymentMethod(method: PaymentMethod)

    @Delete
    suspend fun deletePaymentMethod(method: PaymentMethod)

    // Initialize defaults if empty
    @Query("SELECT COUNT(*) FROM payment_methods")
    suspend fun getCount(): Int
}