package com.example.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [Transaction::class],
    version = 1
)
abstract class TransactionDatabase: RoomDatabase() {
    abstract val transactionDao: TransactionDao

    //We want the database to be a Singleton (Single Instance).
    // We don't want to create a new database connection every time we click a button
    // We want one shared connection for the whole app.
    // The companion object holds this shared connection.
    companion object{ // acts like static in java
        //We want the database to be a Singleton (Single Instance).
        // We don't want to create a new database connection every time we click a button.
        // We want one shared connection for the whole app. The companion object holds this shared connection.

    @Volatile //This is a thread-safety keyword. It ensures that if "Thread A" opens the database, "Thread B" sees the change immediately.
    // It prevents two threads from accidentally creating two different databases at the same time.
    private var INSTANCE: TransactionDatabase? = null

    fun getDatabase(context: Context): TransactionDatabase {
        return INSTANCE
            ?: synchronized(this) //t locks the code so only one part of the app can try to build the database at a time.
            {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "expense_tracker_db"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}
