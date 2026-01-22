package com.example.expensetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DueDao {
    @Query("SELECT * FROM dues ORDER BY date DESC")
    fun getAllDues(): Flow<List<Due>>

    @Upsert
    suspend fun upsertDue(due: Due)

    @Delete
    suspend fun deleteDue(due: Due)

    @Query("SELECT SUM(amount) FROM dues WHERE isLent = 1 AND isPaid = 0")
    fun getTotalLent(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM dues WHERE isLent = 0 AND isPaid = 0")
    fun getTotalBorrowed(): Flow<Double?>
}
