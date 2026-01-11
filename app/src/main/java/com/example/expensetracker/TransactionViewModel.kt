package com.example.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val dao: TransactionDao
) : ViewModel() {

    val allTransactions = dao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reviewTransactions = dao.getTransactionsNeedingReview()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    // FIX: Using 'equals(..., ignoreCase = true)' fixes the calculation issue
    val totalBalance = allTransactions.map { list ->
        val income = list
            .filter { it.type.equals("Income", ignoreCase = true) }
            .sumOf { it.amount }

        val expense = list
            .filter { it.type.equals("Expense", ignoreCase = true) }
            .sumOf { it.amount }

        income - expense
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Also fix these if you use them elsewhere
    val totalIncome = allTransactions.map { list ->
        list.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense = allTransactions.map { list ->
        list.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.upsertTransaction(transaction) }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.deleteTransaction(transaction) }
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return dao.getTransactionById(id)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = (application as ExpenseApp).database
                return TransactionViewModel(database.transactionDao) as T
            }
        }
    }
}