package com.example.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringPaymentsScreen(
    viewModel: TransactionViewModel
) {
    val recurringPayments by viewModel.allRecurringPayments.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<RecurringPayment?>(null) }

    // Calculate Total Monthly Commitment
    val totalMonthly = remember(recurringPayments) {
        recurringPayments.sumOf {
            when (it.frequency.lowercase()) {
                "weekly" -> it.amount * 4
                "yearly" -> it.amount / 12
                "daily" -> it.amount * 30
                else -> it.amount
            }
        }
    }

    // Grouping Logic
    val groupedPayments = remember(recurringPayments) {
        recurringPayments.groupBy { it.frequency }
    }

    // Custom Sort Order for Frequencies
    val frequencyOrder = listOf("Daily", "Weekly", "Monthly", "Yearly")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Manage Subscriptions & Bills",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(all = 16.dp)
            )

            TotalCommitmentCard(totalMonthly)

            if (recurringPayments.isEmpty()) {
                EmptySubscriptionState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Sort groups by our custom order (Monthly first, etc.)
                    val sortedGroups = groupedPayments.toSortedMap(compareBy {
                        val index = frequencyOrder.indexOf(it)
                        if (index == -1) 999 else index
                    })

                    sortedGroups.forEach { (frequency, payments) ->
                        // Frequency Header
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = frequency, // e.g., "Monthly"
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        items(payments, key = { it.id }) { payment ->
                            RecurringPaymentTicketCard(
                                payment = payment,
                                onEdit = {
                                    editingPayment = payment
                                    showAddEditSheet = true
                                },
                                onDelete = { viewModel.deleteRecurringPayment(payment) }
                            )
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAddEditSheet = true
                      editingPayment = null},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp)
                .padding(24.dp),
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("New Payment") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )


        if (showAddEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddEditSheet = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                AddEditRecurringSheet(
                    viewModel = viewModel,
                    initialPayment = editingPayment,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showAddEditSheet = false
                        }
                    },
                    onConfirm = { payment ->
                        viewModel.upsertRecurringPayment(payment)
                    }
                )
            }
        }
    }
}

@Composable
fun RecurringPaymentTicketCard(
    payment: RecurringPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val daysUntil = getDaysUntil(payment.nextDate)

    // Urgency Colors
    val urgencyColor = when {
        daysUntil <= 3 -> MaterialTheme.colorScheme.error
        daysUntil <= 7 -> Color(0xFFFFB74D) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 1. LEFT: Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(urgencyColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = payment.title.trim().replace("\\s+", "").take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 2. CENTER: Title & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = payment.frequency,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Category • Method
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val catText = payment.category.ifEmpty { "Other" }
                        val methodText = payment.paymentMethod.ifEmpty { "Cash" }

                        Text(
                            text = "$catText • $methodText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // 3. RIGHT: Amount (Now stands alone and clear)
                Text(
                    text = "₹${"%.0f".format(payment.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically)
                )
            }

            // 4. FOOTER: Divider & Actions
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Due Date Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = urgencyColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (daysUntil == 0L) "Due Today" else "Due in $daysUntil days",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                }

                // Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onEdit)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Delete
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onDelete)
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TotalCommitmentCard(totalAmount: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Monthly Commitment",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹${"%.0f".format(totalAmount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            // ✅ CHANGED: Used Wallet icon instead of Calendar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet, // Or Savings
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EmptySubscriptionState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Subscriptions,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No subscriptions found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Add your bills to track them automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// --- Helper Functions ---
fun getDaysUntil(dateMillis: Long): Long {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val due = Calendar.getInstance().apply { timeInMillis = dateMillis }

    val diff = due.timeInMillis - today.timeInMillis
    return if (diff < 0) 0 else TimeUnit.MILLISECONDS.toDays(diff)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringSheet(
    viewModel: TransactionViewModel,
    initialPayment: RecurringPayment? = null,
    onDismiss: () -> Unit,
    onConfirm: (RecurringPayment) -> Unit
) {
    // 1. State Variables
    var title by remember { mutableStateOf(initialPayment?.title ?: "") }
    var amount by remember { mutableStateOf(initialPayment?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var frequency by remember { mutableStateOf(initialPayment?.frequency ?: "Monthly") }
    var nextDateInMillis by remember { mutableLongStateOf(initialPayment?.nextDate ?: System.currentTimeMillis()) }
    var category by remember { mutableStateOf(initialPayment?.category ?: "") }
    var paymentMethod by remember { mutableStateOf(initialPayment?.paymentMethod ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }

    // 2. Data Lists
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")
    // Collect data from ViewModel
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val methods by viewModel.allPaymentMethods.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (initialPayment == null) "New Subscription" else "Edit Subscription",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Service Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            prefix = { Text("₹ ") }
        )

        var freqExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = !freqExpanded }) {
            OutlinedTextField(
                value = frequency, onValueChange = {}, readOnly = true, label = { Text("Frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                frequencies.forEach { f ->
                    DropdownMenuItem(text = { Text(f) }, onClick = { frequency = f; freqExpanded = false })
                }
            }
        }

        var catExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
            OutlinedTextField(
                value = category, onValueChange = {}, readOnly = true, label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                categories.forEach { c ->
                    DropdownMenuItem(text = { Text(c.name) }, onClick = { category = c.name; catExpanded = false })
                }
            }
        }

        var methodExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
            OutlinedTextField(
                value = paymentMethod, onValueChange = {}, readOnly = true, label = { Text("Account") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                methods.forEach { m ->
                    DropdownMenuItem(text = { Text(m.name) }, onClick = { paymentMethod = m.name; methodExpanded = false })
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(nextDateInMillis)),
                onValueChange = {}, readOnly = true, label = { Text("Next Payment Date") },
                trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            )
            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
        }

        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                onConfirm(
                    RecurringPayment(
                        id = initialPayment?.id ?: 0,
                        title = title, amount = amt, category = category,
                        paymentMethod = paymentMethod, frequency = frequency,
                        nextDate = nextDateInMillis
                    )
                )
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Subscription", fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextDateInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { nextDateInMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
