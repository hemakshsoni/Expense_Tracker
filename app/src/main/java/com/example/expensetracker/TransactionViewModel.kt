package com.example.expensetracker

import TransactionSortOption
import TransactionTypeFilter
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthlyStatsData(
    val income: Double,
    val expense: Double,
    val categoryBreakdown: Map<String, Double>,
    val paymentMethodBreakdown: Map<String, Double>
)

enum class AnalyticsTimeframe {
    MONTHLY, YEARLY
}

class TransactionViewModel(
    private val dao: TransactionDao,
    private val merchantCategoryDao: MerchantCategoryDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val categoryDao: CategoryDao,
    private val recurringPaymentDao: RecurringPaymentDao,
    private val dueDao: DueDao,
    private val duePaymentDao: DuePaymentDao
) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reviewTransactions: StateFlow<List<Transaction>> = dao.getTransactionsNeedingReview()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPaymentMethods: StateFlow<List<PaymentMethod>> = paymentMethodDao.getAllPaymentMethods()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Map of category name to icon name for easy lookup in lists
    val categoryIconMap: StateFlow<Map<String, String>> = allCategories.map { list ->
        list.associate { it.name to it.iconName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val autoLearnedMappings: StateFlow<List<MerchantCategory>> = merchantCategoryDao.getAllMappings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRecurringPayments = recurringPaymentDao.getAllRecurringPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDues = dueDao.getAllDues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLent = dueDao.getTotalLent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBorrowed = dueDao.getTotalBorrowed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun getPaymentsForDue(dueId: Int) = duePaymentDao.getPaymentsForDue(dueId)

    val paymentMethodBalances: StateFlow<Map<PaymentMethod, Double>> = combine(allTransactions, allPaymentMethods) { transactions, methods ->
        methods.associateWith { method ->
            val income = transactions
                .filter { it.paymentMethod == method.name && it.type.equals("Income", ignoreCase = true) }
                .sumOf { it.amount }
            val expense = transactions
                .filter { it.paymentMethod == method.name && it.type.equals("Expense", ignoreCase = true) }
                .sumOf { it.amount }
            
            // Transfers out of this method
            val transferOut = transactions
                .filter { it.paymentMethod == method.name && it.type.equals("TRANSFER", ignoreCase = true) }
                .sumOf { it.amount }
            
            // Transfers into this method
            val transferIn = transactions
                .filter { it.toPaymentMethod == method.name && it.type.equals("TRANSFER", ignoreCase = true) }
                .sumOf { it.amount }

            method.initialBalance + income - expense - transferOut + transferIn
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val paymentMethodSpending: StateFlow<Map<PaymentMethod, Double>> = combine(allTransactions, allPaymentMethods) { transactions, methods ->
        methods.associateWith { method ->
            transactions
                .filter { it.paymentMethod == method.name && it.type.equals("Expense", ignoreCase = true) }
                .sumOf { it.amount }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    val totalBalance: StateFlow<Double> = allTransactions.map { list ->
        val income = list.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
        val expense = list.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
        income - expense
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        checkAndProcessRecurringPayments()
    }

    private fun checkAndProcessRecurringPayments() {
        viewModelScope.launch(Dispatchers.IO) {
            val activePayments = recurringPaymentDao.getActiveRecurringPayments()
            val now = System.currentTimeMillis()
            
            activePayments.forEach { payment ->
                var currentNextDate = payment.nextDate
                val processedTransactions = mutableListOf<Transaction>()
                
                while (currentNextDate <= now) {
                    processedTransactions.add(
                        Transaction(
                            title = payment.title,
                            amount = payment.amount,
                            category = payment.category,
                            date = currentNextDate,
                            type = "Expense",
                            paymentMethod = payment.paymentMethod,
                            needsReview = false,
                            isAutoDetected = true,
                            description = "Recurring Payment: ${payment.frequency}"
                        )
                    )
                    currentNextDate = calculateNextOccurrence(currentNextDate, payment.frequency)
                }
                
                if (processedTransactions.isNotEmpty()) {
                    processedTransactions.forEach { dao.upsertTransaction(it) }
                    recurringPaymentDao.upsertRecurringPayment(
                        payment.copy(
                            nextDate = currentNextDate,
                            lastProcessed = now
                        )
                    )
                }
            }
        }
    }

    private fun calculateNextOccurrence(currentDate: Long, frequency: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentDate
        when (frequency) {
            "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> cal.add(Calendar.MONTH, 1)
            "Yearly" -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun upsertTransaction(transaction: Transaction) {
        viewModelScope.launch { 
            dao.upsertTransaction(transaction)
            
            // 🔥 CRITICAL: Handle learning when saving a reviewed transaction
            if (transaction.allowLearning && transaction.merchantSource == MerchantSource.BODY) {
                merchantCategoryDao.insertMapping(
                    MerchantCategory(
                        merchant = normalizeMerchant(transaction.title),
                        category = transaction.category
                    )
                )
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.deleteTransaction(transaction) }
    }

    fun deleteTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            transactions.forEach { dao.deleteTransaction(it) }
        }
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return dao.getTransactionById(id)
    }

    fun upsertPaymentMethod(method: PaymentMethod) {
        viewModelScope.launch {
            val oldMethod = allPaymentMethods.value.find { it.id == method.id }
            if (oldMethod != null && oldMethod.name != method.name) {
                dao.updatePaymentMethodNameInTransactions(oldMethod.name, method.name)
            }
            paymentMethodDao.upsertPaymentMethod(method)
        }
    }

    fun deletePaymentMethod(method: PaymentMethod) {
        viewModelScope.launch { paymentMethodDao.deletePaymentMethod(method) }
    }

    fun upsertCategory(category: Category) {
        viewModelScope.launch {
            val oldCategory = allCategories.value.find { it.id == category.id }
            if (oldCategory != null && oldCategory.name != category.name) {
                dao.updateCategoryNameInTransactions(oldCategory.name, category.name)
            }
            categoryDao.upsertCategory(category)
        }
    }

    fun updateCategoryOrder(categories: List<Category>) {
        viewModelScope.launch {
            categoryDao.upsertCategories(categories.mapIndexed { index, category -> 
                category.copy(order = index) 
            })
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryDao.deleteCategory(category) }
    }

    fun upsertRecurringPayment(payment: RecurringPayment) {
        viewModelScope.launch { 
            recurringPaymentDao.upsertRecurringPayment(payment)
            checkAndProcessRecurringPayments()
        }
    }

    fun deleteRecurringPayment(payment: RecurringPayment) {
        viewModelScope.launch { recurringPaymentDao.deleteRecurringPayment(payment) }
    }

    fun upsertDue(due: Due, additionalPaymentAmount: Double = 0.0) {
        viewModelScope.launch { 
            dueDao.upsertDue(due)
            if (additionalPaymentAmount > 0) {
                duePaymentDao.insertPayment(
                    DuePayment(
                        dueId = due.id,
                        amount = additionalPaymentAmount,
                        date = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteDue(due: Due) {
        viewModelScope.launch { dueDao.deleteDue(due) }
    }

    fun upsertAutoLearnedMapping(mapping: MerchantCategory) {
        viewModelScope.launch { merchantCategoryDao.insertMapping(mapping) }
    }

    fun deleteAutoLearnedMapping(mapping: MerchantCategory) {
        viewModelScope.launch { merchantCategoryDao.deleteMapping(mapping) }
    }

    fun importOldTransactions(context: Context, months: Int) {
        viewModelScope.launch {
            SmsImporter(context).importTransactions(months)
        }
    }

    fun importCustomRange(context: Context, start: Long, end: Long) {
        viewModelScope.launch {
            SmsImporter(context).importTransactionsRange(start, end)
        }
    }

    fun reviewAllTransactions() {
        viewModelScope.launch {
            reviewTransactions.value.forEach { txn ->
                dao.upsertTransaction(txn.copy(needsReview = false))
            }
        }
    }

    fun reviewTransactions(transactionIds: Set<Int>) {
        viewModelScope.launch {
            // Safer way: iterate IDs and update if found in allTransactions
            transactionIds.forEach { id ->
                allTransactions.value.find { it.id == id }?.let { txn ->
                    dao.upsertTransaction(txn.copy(needsReview = false))
                }
            }
        }
    }

    fun learnMerchantCategoryIfNeeded(
        oldTransaction: Transaction,
        updatedTransaction: Transaction
    ) {
        // Obsolete: Integrated directly into upsertTransaction
    }

    private val _analyticsTimeframe = MutableStateFlow(AnalyticsTimeframe.MONTHLY)
    val analyticsTimeframe = _analyticsTimeframe.asStateFlow()

    fun setAnalyticsTimeframe(timeframe: AnalyticsTimeframe) {
        _analyticsTimeframe.value = timeframe
    }

    val statsData: StateFlow<Map<String, MonthlyStatsData>> = 
        combine(allTransactions, analyticsTimeframe) { transactions, timeframe ->
            transactions.groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.date
                when (timeframe) {
                    AnalyticsTimeframe.MONTHLY -> {
                        val year = cal.get(Calendar.YEAR)
                        val month = cal.get(Calendar.MONTH) + 1
                        "$year-${String.format("%02d", month)}"
                    }
                    AnalyticsTimeframe.YEARLY -> {
                        cal.get(Calendar.YEAR).toString()
                    }
                }
            }.mapValues { entry ->
                val income = entry.value.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
                val expense = entry.value.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
                
                val catBreakdown = entry.value.filter { it.type.equals("Expense", ignoreCase = true) }
                    .groupBy { it.category }
                    .mapValues { catEntry -> catEntry.value.sumOf { it.amount } }
                
                val pmBreakdown = entry.value.filter { it.type.equals("Expense", ignoreCase = true) }
                    .groupBy { it.paymentMethod }
                    .mapValues { pmEntry -> pmEntry.value.sumOf { it.amount } }
                
                MonthlyStatsData(income, expense, catBreakdown, pmBreakdown)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val monthlyStats = statsData // For backward compatibility if needed

    private val _typeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    val typeFilter = _typeFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(TransactionSortOption.DATE_DESC)
    val sortOption = _sortOption.asStateFlow()

    private val _categoryFilters = MutableStateFlow<Set<String>>(emptySet())
    val categoryFilters = _categoryFilters.asStateFlow()

    private val _paymentMethodFilters = MutableStateFlow<Set<String>>(emptySet())
    val paymentMethodFilters = _paymentMethodFilters.asStateFlow()

    fun setTypeFilter(filter: TransactionTypeFilter) { _typeFilter.value = filter }
    fun setSortOption(option: TransactionSortOption) { _sortOption.value = option }
    
    fun toggleCategoryFilter(category: String) {
        _categoryFilters.value = if (_categoryFilters.value.contains(category)) {
            _categoryFilters.value - category
        } else {
            _categoryFilters.value + category
        }
    }
    
    fun clearCategoryFilters() { _categoryFilters.value = emptySet() }

    fun togglePaymentMethodFilter(method: String) {
        _paymentMethodFilters.value = if (_paymentMethodFilters.value.contains(method)) {
            _paymentMethodFilters.value - method
        } else {
            _paymentMethodFilters.value + method
        }
    }
    
    fun clearPaymentMethodFilters() { _paymentMethodFilters.value = emptySet() }

    val filteredSortedTransactions: StateFlow<List<Transaction>> =
        combine(allTransactions, typeFilter, sortOption, categoryFilters, paymentMethodFilters) {
                transactions, typeF, sort, catFs, methodFs ->
            var result = when (typeF) {
                TransactionTypeFilter.ALL -> transactions
                TransactionTypeFilter.INCOME -> transactions.filter { it.type.trim().equals("Income", ignoreCase = true) }
                TransactionTypeFilter.EXPENSE -> transactions.filter { it.type.trim().equals("Expense", ignoreCase = true) }
                TransactionTypeFilter.TRANSFER -> transactions.filter { it.type.trim().equals("TRANSFER", ignoreCase = true) }
            }
            if (catFs.isNotEmpty()) result = result.filter { it.category in catFs }
            if (methodFs.isNotEmpty()) result = result.filter { it.paymentMethod in methodFs }
            
            result = when (sort) {
                TransactionSortOption.DATE_DESC -> result.sortedByDescending { it.date }
                TransactionSortOption.DATE_ASC -> result.sortedBy { it.date }
                TransactionSortOption.AMOUNT_DESC -> result.sortedByDescending { it.amount }
                TransactionSortOption.AMOUNT_ASC -> result.sortedBy { it.amount }
            }
            result
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = (application as ExpenseApp).database
                return TransactionViewModel(
                    database.transactionDao,
                    database.merchantCategoryDao(),
                    database.paymentMethodDao,
                    database.categoryDao,
                    database.recurringPaymentDao,
                    database.dueDao,
                    database.duePaymentDao
                ) as T
            }
        }
    }
}