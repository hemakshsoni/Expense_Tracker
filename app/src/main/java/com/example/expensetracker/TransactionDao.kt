package com.example.expensetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Upsert
    suspend fun upsertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // 1. Get EVERYTHING (for the main list)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // 2. Get ONLY Expenses (for charts/graphs)
    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' ORDER BY date DESC")
    fun getOnlyExpenses(): Flow<List<Transaction>>

    // 3. Get ONLY Income
    @Query("SELECT * FROM transactions WHERE type = 'INCOME' ORDER BY date DESC")
    fun getOnlyIncomes(): Flow<List<Transaction>>

    // COALESCE = if the first argument is null, return the second argument
    // triple quotes for multi-line strings.
    // if it were single quotes then we would have to write everything in a single line.
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
}