package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryDao {

    @Query("SELECT category FROM merchant_category WHERE merchant = :merchant LIMIT 1")
    suspend fun getCategoryForMerchant(merchant: String): String?

    @Query("SELECT * FROM merchant_category")
    fun getAllMappings(): Flow<List<MerchantCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantCategory)

    @Delete
    suspend fun deleteMapping(mapping: MerchantCategory)
}
