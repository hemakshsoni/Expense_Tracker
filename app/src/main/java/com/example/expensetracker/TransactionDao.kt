package com.example.expensetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Upsert
    suspend fun upsertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' ORDER BY date DESC")
    fun getOnlyExpenses(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'INCOME' ORDER BY date DESC")
    fun getOnlyIncomes(): Flow<List<Transaction>>

    @Query("""
        SELECT 
            (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME') - 
            (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE')
    """)
    fun getTotalBalance(): Flow<Double>

    @Query("SELECT * FROM transactions WHERE needsReview = 1 ORDER BY date DESC")
    fun getTransactionsNeedingReview(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE reference = :reference)")
    suspend fun existsByReference(reference: String): Boolean

    @Query("UPDATE transactions SET paymentMethod = :newName WHERE paymentMethod = :oldName")
    suspend fun updatePaymentMethodNameInTransactions(oldName: String, newName: String)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryNameInTransactions(oldName: String, newName: String)

    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM transactions
        WHERE 
            reference = :reference
            OR (
                amount = :amount
                AND title = :merchant
                AND type = :type
                AND date >= :sinceTime
            )
    )
""")
    suspend fun existsDuplicate(
        reference: String,
        amount: Double,
        merchant: String,
        type: String,
        sinceTime: Long
    ): Boolean



}