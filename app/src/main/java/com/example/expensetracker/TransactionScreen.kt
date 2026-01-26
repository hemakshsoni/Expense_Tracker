package com.example.expensetracker

import TransactionSortOption
import TransactionTypeFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionScreen(
    transactions: List<Transaction>,
    typeFilter: TransactionTypeFilter,
    sortOption: TransactionSortOption,
    categoryFilters: Set<String>,
    paymentMethodFilters: Set<String>,
    availableCategories: List<String>,
    availablePaymentMethods: List<String>,
    onDeleteClick: (Transaction) -> Unit,
    onDeleteSelectedClick: (Set<Int>) -> Unit = {},
    onReviewSelectedClick: (Set<Int>) -> Unit = {},
    onTypeFilterChange: (TransactionTypeFilter) -> Unit,
    onSortChange: (TransactionSortOption) -> Unit,
    onCategoryToggle: (String) -> Unit = {},
    onPaymentMethodToggle: (String) -> Unit = {},
    onClearCategories: () -> Unit = {},
    onClearPaymentMethods: () -> Unit = {},
    externalSelectedIds: Set<Int> = emptySet(),
    onSelectionChange: (Set<Int>) -> Unit = {},
    viewModel: TransactionViewModel
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val categoryIconMap by viewModel.categoryIconMap.collectAsState()

    // Sheet States
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }

    // Add/Edit Sheet Logic
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingTransactionId by remember { mutableIntStateOf(-1) }
    var sheetInitialTab by remember { mutableIntStateOf(0) } // 0 = Transaction, 1 = Transfer

    val isSelectionMode = externalSelectedIds.isNotEmpty()

    val filterSheetState = rememberModalBottomSheetState()
    val detailSheetState = rememberModalBottomSheetState()

    // Main Layout
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- TOP BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSelectionMode) "${externalSelectedIds.size} Selected" else "Activity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelectionMode) {
                        TextButton(
                            onClick = { onSelectionChange(emptySet()) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Cancel Selection",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val allIds = transactions.map { it.id }.toSet()
                            onSelectionChange(if (externalSelectedIds.size == transactions.size) emptySet() else allIds)
                        }) {
                            Icon(
                                Icons.Default.Checklist,
                                "Select All",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            onReviewSelectedClick(externalSelectedIds)
                            onSelectionChange(emptySet())
                        }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Review Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            onDeleteSelectedClick(externalSelectedIds)
                            onSelectionChange(emptySet())
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete Selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LIST CONTENT ---
            if (transactions.isEmpty()) {
                EmptyState()
            } else {
                val groupedTransactions = remember(transactions) {
                    transactions.groupBy { getRelativeDateLabel(it.date) }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTransactions.forEach { (label, txns) ->
                        item(key = label) {
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }
                        items(
                            items = txns,
                            key = { it.id }
                        ) { transaction ->
                            val isSelected = externalSelectedIds.contains(transaction.id)
                            Box(modifier = Modifier.animateItem()) {
                                SwipeToDeleteItem(
                                    transaction = transaction,
                                    onDelete = onDeleteClick,
                                    onReview = { onReviewSelectedClick(setOf(transaction.id)) },
                                    enabled = !isSelectionMode,
                                    content = {
                                        TransactionItem(
                                            transaction = transaction,
                                            isSelected = isSelected,
                                            icon = if (transaction.type == "TRANSFER") Icons.Default.SwapHoriz else IconUtils.getIcon(
                                                categoryIconMap[transaction.category]
                                            ),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    onSelectionChange(if (isSelected) externalSelectedIds - transaction.id else externalSelectedIds + transaction.id)
                                                } else {
                                                    selectedTransactionForDetail = transaction
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    onSelectionChange(setOf(transaction.id))
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- FAB (Bottom Right) ---
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                SpeedDialFAB(
                    onAddTransaction = {
                        editingTransactionId = -1
                        sheetInitialTab = 0
                        showAddEditSheet = true
                    },
                    onDoTransfer = {
                        editingTransactionId = -1
                        sheetInitialTab = 1
                        showAddEditSheet = true
                    }
                )
            }
        }
    }

    // --- ADD / EDIT / TRANSFER SHEET ---
    if (showAddEditSheet) {
        AddEditTransactionSheet(
            onDismiss = { showAddEditSheet = false },
            viewModel = viewModel,
            transactionId = editingTransactionId,
            mode = sheetInitialTab
        )
    }

    // --- FILTER SHEET ---
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            FilterContent(
                typeFilter = typeFilter,
                sortOption = sortOption,
                categoryFilters = categoryFilters,
                paymentMethodFilters = paymentMethodFilters,
                availableCategories = availableCategories,
                availablePaymentMethods = availablePaymentMethods,
                onTypeFilterChange = onTypeFilterChange,
                onSortChange = onSortChange,
                onCategoryToggle = onCategoryToggle,
                onPaymentMethodToggle = onPaymentMethodToggle,
                onClearCategories = onClearCategories,
                onClearPaymentMethods = onClearPaymentMethods,
                onApply = {
                    scope.launch { filterSheetState.hide() }
                        .invokeOnCompletion { showFilterSheet = false }
                }
            )
        }
    }

    // --- DETAIL SHEET ---
    if (selectedTransactionForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTransactionForDetail = null },
            sheetState = detailSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            TransactionDetailContent(
                transaction = selectedTransactionForDetail!!,
                icon = if (selectedTransactionForDetail!!.type == "TRANSFER") Icons.Default.SwapHoriz else IconUtils.getIcon(
                    categoryIconMap[selectedTransactionForDetail!!.category]
                ),
                onEditClick = {
                    val txn = selectedTransactionForDetail!!
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                        selectedTransactionForDetail = null
                        // Configure Edit State
                        editingTransactionId = txn.id
                        sheetInitialTab = if(txn.type == "TRANSFER") 1 else 0
                        showAddEditSheet = true
                    }
                }
            )
        }
    }
}

@Composable
fun SpeedDialFAB(
    onAddTransaction: () -> Unit,
    onDoTransfer: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Transfer Button
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(),
            exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Transfer",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                SmallFloatingActionButton(
                    onClick = {
                        expanded = false
                        onDoTransfer()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer")
                }
            }
        }

        // Add Transaction Button
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(),
            exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Transaction",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                SmallFloatingActionButton(
                    onClick = {
                        expanded = false
                        onAddTransaction()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Transaction")
                }
            }
        }

        // Main FAB
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            label = "fabRotation"
        )

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

// ... (Rest of your UI components: TransactionItem, FilterContent, TransactionDetailContent, etc. remain the same)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    icon: ImageVector,
    onLongClick: () -> Unit = {}
) {
    val formattedDate =
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transaction.date))
    val isExpense = transaction.type.equals("Expense", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Done, null, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (transaction.needsReview) Badge("Review", MaterialTheme.colorScheme.error)
                    if (transaction.isAutoDetected && !transaction.needsReview) Badge(
                        "Auto",
                        MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${transaction.category} • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (isExpense) "-₹${"%.0f".format(transaction.amount)}" else "+₹${
                    "%.0f".format(
                        transaction.amount
                    )
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) ExpenseRed else IncomeGreen
            )
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
fun getRelativeDateLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, time) -> "Today"
        isYesterday(now, time) -> "Yesterday"
        isSameWeek(now, time) -> "Earlier This Week"
        isSameMonth(now, time) -> "Earlier This Month"
        else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    val yesterday = (cal1.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return isSameDay(yesterday, cal2)
}

fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
}

fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterContent(
    typeFilter: TransactionTypeFilter,
    sortOption: TransactionSortOption,
    categoryFilters: Set<String>,
    paymentMethodFilters: Set<String>,
    availableCategories: List<String>,
    availablePaymentMethods: List<String>,
    onTypeFilterChange: (TransactionTypeFilter) -> Unit,
    onSortChange: (TransactionSortOption) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onPaymentMethodToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    onClearPaymentMethods: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Filter",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "TRANSACTION TYPE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionTypeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { onTypeFilterChange(filter) },
                    label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "CATEGORIES",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        FlowRow(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoryFilters.isEmpty(),
                onClick = { onClearCategories() },
                label = { Text("All") },
                shape = RoundedCornerShape(12.dp)
            )
            availableCategories.forEach { category ->
                FilterChip(
                    selected = category in categoryFilters,
                    onClick = { onCategoryToggle(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "PAYMENT METHODS",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        FlowRow(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = paymentMethodFilters.isEmpty(),
                onClick = { onClearPaymentMethods() },
                label = { Text("All") },
                shape = RoundedCornerShape(12.dp)
            )
            availablePaymentMethods.forEach { method ->
                FilterChip(
                    selected = method in paymentMethodFilters,
                    onClick = { onPaymentMethodToggle(method) },
                    label = { Text(method) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "SORT BY",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TransactionSortOption.entries.forEach { option ->
                val displayName = when (option) {
                    TransactionSortOption.DATE_DESC -> "Newest First"
                    TransactionSortOption.DATE_ASC -> "Oldest First"
                    TransactionSortOption.AMOUNT_DESC -> "Highest Amount"
                    TransactionSortOption.AMOUNT_ASC -> "Lowest Amount"
                }
                Surface(
                    onClick = { onSortChange(option) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (sortOption == option) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        RadioButton(selected = sortOption == option, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Apply Filters", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    icon: ImageVector,
    onEditClick: () -> Unit
) {
    val formattedDate = SimpleDateFormat(
        "dd MMMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(transaction.date))
    val isExpense = transaction.type.equals("Expense", ignoreCase = true)

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onEditClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isExpense) "-₹${"%,.2f".format(transaction.amount)}" else if (transaction.type == "TRANSFER") "₹${
                    "%,.2f".format(
                        transaction.amount
                    )
                }" else "+₹${"%,.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = if (isExpense) ExpenseRed else if (transaction.type == "TRANSFER") MaterialTheme.colorScheme.primary else IncomeGreen
            )
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DetailItemNew(Icons.Default.Category, "Category", transaction.category)
            DetailItemNew(Icons.Default.CalendarToday, "Date & Time", formattedDate)
            DetailItemNew(
                Icons.Default.Payments,
                "Account",
                if (transaction.type == "TRANSFER") "${transaction.paymentMethod} → ${transaction.toPaymentMethod}" else transaction.paymentMethod
            )
            if (!transaction.reference.isNullOrBlank()) {
                DetailItemNew(Icons.Default.Tag, "Reference ID", transaction.reference)
            }
        }

        if (transaction.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "NOTES",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp),
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
fun DetailItemNew(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}


@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No New Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All your transactions will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    transaction: Transaction,
    onDelete: (Transaction) -> Unit,
    onReview: (Transaction) -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete(transaction)
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    if (transaction.needsReview) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReview(transaction)
                    }
                    false
                }

                else -> false
            }
        }
    )

    if (enabled) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = true,
            enableDismissFromStartToEnd = transaction.needsReview,
            backgroundContent = {
                val direction = dismissState.dismissDirection

                val color = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.StartToEnd -> if (transaction.needsReview) Color(
                        0xFFE8F5E9
                    ) else Color.Transparent

                    else -> Color.Transparent
                }

                val icon = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    SwipeToDismissBoxValue.StartToEnd -> if (transaction.needsReview) Icons.Default.CheckCircle else null
                    else -> null
                }

                val alignment = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.Center
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (direction == SwipeToDismissBoxValue.StartToEnd)
                                Color(0xFF2E7D32)
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            content = { content() }
        )
    } else {
        content()
    }
}
