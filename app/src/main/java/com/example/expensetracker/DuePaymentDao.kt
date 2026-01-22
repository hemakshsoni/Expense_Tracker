package com.example.expensetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DuePaymentDao {
    @Query("SELECT * FROM due_payments WHERE dueId = :dueId ORDER BY date DESC")
    fun getPaymentsForDue(dueId: Int): Flow<List<DuePayment>>

    @Insert
    suspend fun insertPayment(payment: DuePayment)

    @Delete
    suspend fun deletePayment(payment: DuePayment)
}
