package com.example.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Transaction::class, MerchantCategory::class, PaymentMethod::class, Category::class, RecurringPayment::class, Due::class, DuePayment::class],
    version = 1,
    exportSchema = false
)
abstract class TransactionDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    abstract val paymentMethodDao: PaymentMethodDao
    abstract val categoryDao: CategoryDao
    abstract val recurringPaymentDao: RecurringPaymentDao
    abstract val dueDao: DueDao
    abstract val duePaymentDao: DuePaymentDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null
        fun getDatabase(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "expense_tracker_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                
                                // Pre-populate default payment methods
                                database.paymentMethodDao.let { dao ->
                                    if (dao.getCount() == 0) {
                                        dao.upsertPaymentMethod(PaymentMethod(name = "Cash", initialBalance = 0.0, colorHex = "#4CAF50"))
                                        dao.upsertPaymentMethod(PaymentMethod(name = "UPI", initialBalance = 0.0, colorHex = "#2196F3"))
                                        dao.upsertPaymentMethod(PaymentMethod(name = "Card", initialBalance = 0.0, colorHex = "#FF9800"))
                                    }
                                }

                                // Pre-populate default categories
                                database.categoryDao.let { dao ->
                                    if (dao.getCount() == 0) {
                                        val defaults = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Salary", "Investment", "Other")
                                        defaults.forEach { dao.upsertCategory(Category(name = it)) }
                                    }
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}