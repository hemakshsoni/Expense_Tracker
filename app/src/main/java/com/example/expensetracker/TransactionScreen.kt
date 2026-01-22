package com.example.expensetracker

import TransactionSortOption
import TransactionTypeFilter
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
    onAddClick: () -> Unit,
    onItemClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    onDeleteSelectedClick: (Set<Int>) -> Unit = {},
    onReviewSelectedClick: (Set<Int>) -> Unit = {},
    onTypeFilterChange: (TransactionTypeFilter) -> Unit,
    onSortChange: (TransactionSortOption) -> Unit,
    onCategoryToggle: (String) -> Unit = {},
    onPaymentMethodToggle: (String) -> Unit = {},
    onClearCategories: () -> Unit = {},
    onClearPaymentMethods: () -> Unit = {},
    onMenuClick: () -> Unit,
    externalSelectedIds: Set<Int> = emptySet(),
    onSelectionChange: (Set<Int>) -> Unit = {},
    viewModel: TransactionViewModel
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val categoryIconMap by viewModel.categoryIconMap.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }

    val isSelectionMode = externalSelectedIds.isNotEmpty()

    val filterSheetState = rememberModalBottomSheetState()
    val detailSheetState = rememberModalBottomSheetState()

    val totalIncome = remember(transactions) {
        transactions.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
    }
    val dynamicBalance = totalIncome - totalExpense

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            BalanceCard(
                totalBalance = dynamicBalance,
                income = totalIncome,
                expense = totalExpense
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSelectionMode) "${externalSelectedIds.size} Selected" else "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelectionMode) {
                        TextButton(onClick = { onSelectionChange(emptySet()) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Cancel", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val allIds = transactions.map { it.id }.toSet()
                            onSelectionChange(if (externalSelectedIds.size == transactions.size) emptySet() else allIds)
                        }) {
                            Icon(Icons.Default.Checklist, "Select All")
                        }
                        IconButton(onClick = {
                            onDeleteSelectedClick(externalSelectedIds)
                            onSelectionChange(emptySet())
                        }) {
                            Icon(Icons.Default.Delete, "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    AssistChip(
                        onClick = { showFilterSheet = true },
                        label = { Text("Filters") },
                        leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                EmptyState()
            } else {
                val groupedTransactions = remember(transactions) {
                    transactions.groupBy {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.date
                        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                        val year = cal.get(Calendar.YEAR)
                        "$monthName $year"
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedTransactions.forEach { (month, txns) ->
                        item(key = month) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
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
                                            icon = IconUtils.getIcon(categoryIconMap[transaction.category]),
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

        // --- FILTER SHEET ---
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = filterSheetState,
                containerColor = MaterialTheme.colorScheme.surface
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
                    onApply = { scope.launch { filterSheetState.hide() }.invokeOnCompletion { showFilterSheet = false } }
                )
            }
        }

        // --- DETAIL SHEET ---
        if (selectedTransactionForDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedTransactionForDetail = null },
                sheetState = detailSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                TransactionDetailContent(
                    transaction = selectedTransactionForDetail!!,
                    icon = IconUtils.getIcon(categoryIconMap[selectedTransactionForDetail!!.category]),
                    onEditClick = {
                        val txn = selectedTransactionForDetail!!
                        scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                            selectedTransactionForDetail = null
                            onItemClick(txn)
                        }
                    }
                )
            }
        }
    }
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
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Filters & Sort", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionTypeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { onTypeFilterChange(filter) },
                    label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoryFilters.isEmpty(),
                onClick = { onClearCategories() },
                label = { Text("All") }
            )
            availableCategories.forEach { category ->
                FilterChip(
                    selected = category in categoryFilters,
                    onClick = { onCategoryToggle(category) },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Payment Method", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = paymentMethodFilters.isEmpty(),
                onClick = { onClearPaymentMethods() },
                label = { Text("All") }
            )
            availablePaymentMethods.forEach { method ->
                FilterChip(
                    selected = method in paymentMethodFilters,
                    onClick = { onPaymentMethodToggle(method) },
                    label = { Text(method) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Sort By", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            TransactionSortOption.entries.forEach { option ->
                val displayName = when (option) {
                    TransactionSortOption.DATE_DESC -> "Newest First"
                    TransactionSortOption.DATE_ASC -> "Oldest First"
                    TransactionSortOption.AMOUNT_DESC -> "Highest Amount"
                    TransactionSortOption.AMOUNT_ASC -> "Lowest Amount"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortChange(option) }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = sortOption == option, onClick = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(displayName)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply")
        }
    }
}

@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    icon: ImageVector,
    onEditClick: () -> Unit
) {
    val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    val isExpense = transaction.type.equals("Expense", ignoreCase = true)

    Column(
        modifier = Modifier
            .padding(16.dp)
            .padding(bottom = 40.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transaction Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isExpense) "-₹${"%,.2f".format(transaction.amount)}" else "+₹${"%,.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isExpense) ExpenseRed else IncomeGreen
                )
                Text(
                    text = transaction.type.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description Container (Visible if not blank)
        if (transaction.description.isNotBlank()) {
            Text(
                "Description",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Details List
        DetailItem(icon = icon, label = "Title", value = transaction.title)
        DetailItem(icon = Icons.Default.Category, label = "Category", value = transaction.category)
        DetailItem(icon = Icons.Default.CalendarMonth, label = "Date & Time", value = formattedDate)
        DetailItem(icon = Icons.Default.Payments, label = "Payment Method", value = transaction.paymentMethod)

        if (!transaction.reference.isNullOrBlank()) {
            DetailItem(icon = Icons.Default.ShoppingBag, label = "Reference", value = transaction.reference)
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BalanceCard(totalBalance: Double, income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
        ) {
            Text(
                "Total Balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Text(
                "₹ ${"%,.2f".format(totalBalance)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowUpward, "Income", tint = Color.Green, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        Text("₹${"%.0f".format(income)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowDownward, "Expense", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        Text("₹${"%.0f".format(expense)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    icon: ImageVector,
    onLongClick: () -> Unit = {}
) {
    val formattedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transaction.date))
    val isExpense = transaction.type.equals("Expense", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    if (transaction.isAutoDetected && !transaction.needsReview) Badge("Auto", MaterialTheme.colorScheme.primary)
                    Text(
                        text = "${transaction.category} • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (isExpense) "-₹${"%.0f".format(transaction.amount)}" else "+₹${"%.0f".format(transaction.amount)}",
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

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
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
                    SwipeToDismissBoxValue.StartToEnd -> if (transaction.needsReview) Color(0xFFE8F5E9) else Color.Transparent
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
                        .clip(RoundedCornerShape(16.dp))
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
