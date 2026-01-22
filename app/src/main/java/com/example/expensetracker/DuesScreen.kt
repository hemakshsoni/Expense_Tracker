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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuesScreen(
    viewModel: TransactionViewModel,
    onDeleteDue: (Due) -> Unit
) {
    val dues by viewModel.allDues.collectAsState()
    val totalLent by viewModel.totalLent.collectAsState(initial = 0.0)
    val totalBorrowed by viewModel.totalBorrowed.collectAsState(initial = 0.0)
    
    val scope = rememberCoroutineScope()
    val addEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingDue by remember { mutableStateOf<Due?>(null) }

    val historySheetState = rememberModalBottomSheetState()
    var selectedDueForHistory by remember { mutableStateOf<Due?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- SUMMARY HEADER ---
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Remaining to Receive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text("₹${"%.0f".format(totalLent ?: 0.0)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining to Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text("₹${"%.0f".format(totalBorrowed ?: 0.0)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
            }

            if (dues.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pending dues found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    items(dues, key = { it.id }) { due ->
                        DueItemCard(
                            due = due,
                            onClick = { selectedDueForHistory = due },
                            onEdit = { 
                                editingDue = due 
                                showAddEditSheet = true
                            },
                            onDelete = { onDeleteDue(due) },
                            onMarkAsPaid = { 
                                val updatedDue = if (!due.isPaid) {
                                    due.copy(isPaid = true, paidAmount = due.amount)
                                } else {
                                    due.copy(isPaid = false, paidAmount = 0.0)
                                }
                                viewModel.upsertDue(updatedDue)
                            }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { 
                editingDue = null
                showAddEditSheet = true 
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Due") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )

        // Add/Edit Bottom Sheet
        if (showAddEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddEditSheet = false },
                sheetState = addEditSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                AddEditDueSheet(
                    initialDue = editingDue,
                    onDismiss = {
                        scope.launch { addEditSheetState.hide() }.invokeOnCompletion {
                            showAddEditSheet = false
                        }
                    },
                    onConfirm = { due, addedAmount ->
                        viewModel.upsertDue(due, addedAmount)
                    }
                )
            }
        }

        // History Bottom Sheet
        if (selectedDueForHistory != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedDueForHistory = null },
                sheetState = historySheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                DueHistorySheet(
                    due = selectedDueForHistory!!,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun DueItemCard(
    due: Due,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsPaid: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(due.date))
    val remainingAmount = due.amount - due.paidAmount
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (due.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (due.isLent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (due.isLent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (due.isLent) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(due.personName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(if (due.isLent) "Lent on $dateStr" else "Borrowed on $dateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Text(
                    "₹${"%.0f".format(remainingAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (due.isPaid) Color.Gray else if (due.isLent) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            if (due.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = due.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }

            if (due.paidAmount > 0 && !due.isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.padding(start = 56.dp)) {
                    val progress = (due.paidAmount / due.amount).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (due.isLent) IncomeGreen else ExpenseRed,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paid: ₹${"%.0f".format(due.paidAmount)} / ₹${"%.0f".format(due.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMarkAsPaid) {
                    Icon(
                        imageVector = if (due.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Paid",
                        tint = if (due.isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = ExpenseRed.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun DueHistorySheet(
    due: Due,
    viewModel: TransactionViewModel
) {
    val payments by viewModel.getPaymentsForDue(due.id).collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
    ) {
        Text(
            text = "Due History - ${due.personName}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // History content inside a scrollable column
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            // Initial Transaction
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (due.isLent) "Money Lent (Initial)" else "Money Borrowed (Initial)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(due.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "₹${"%.0f".format(due.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (payments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Partial Payments",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                payments.forEach { payment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.6.dp)
                                .background(IncomeGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Repayment Received",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = dateFormat.format(Date(payment.date)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "₹${"%.0f".format(payment.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Remaining", fontWeight = FontWeight.Bold)
                val remaining = due.amount - due.paidAmount
                Text(
                    "₹${"%.0f".format(if (remaining < 0) 0.0 else remaining)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (due.isLent) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDueSheet(
    initialDue: Due? = null,
    onDismiss: () -> Unit,
    onConfirm: (Due, Double) -> Unit
) {
    var personName by remember { mutableStateOf(initialDue?.personName ?: "") }
    var totalAmount by remember { mutableStateOf(if (initialDue == null) "" else String.format("%.0f", initialDue.amount)) }
    var paidAmountState by remember { mutableStateOf(initialDue?.paidAmount ?: 0.0) }
    var newPaymentInput by remember { mutableStateOf("") }
    
    var isLent by remember { mutableStateOf(initialDue?.isLent ?: true) }
    var note by remember { mutableStateOf(initialDue?.note ?: "") }
    var dateInMillis by remember { mutableLongStateOf(initialDue?.date ?: System.currentTimeMillis()) }
    
    var showDatePicker by remember { mutableStateOf(false) }

    val updatedPaidAmount = remember(paidAmountState, newPaymentInput) {
        val additional = newPaymentInput.toDoubleOrNull() ?: 0.0
        paidAmountState + additional
    }
    val remainingAmount = remember(totalAmount, updatedPaidAmount) {
        val total = totalAmount.toDoubleOrNull() ?: 0.0
        (total - updatedPaidAmount).coerceAtLeast(0.0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (initialDue == null) "Add New Due" else "Manage Due",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = personName, 
            onValueChange = { personName = it }, 
            label = { Text("Person Name") }, 
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        OutlinedTextField(
            value = totalAmount, 
            onValueChange = { totalAmount = it }, 
            label = { Text("Total Amount") }, 
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) }
        )

        if (initialDue != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Repayment Status", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Already Paid", style = MaterialTheme.typography.bodySmall)
                            Text("₹${"%.0f".format(paidAmountState)}", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining", style = MaterialTheme.typography.bodySmall)
                            Text("₹${"%.0f".format(remainingAmount)}", fontWeight = FontWeight.Bold, color = if (isLent) IncomeGreen else ExpenseRed)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = newPaymentInput,
                        onValueChange = { newPaymentInput = it },
                        label = { Text("Add New Payment") },
                        placeholder = { Text("Enter amount to add") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = if(paidAmountState == 0.0) "" else String.format("%.0f", paidAmountState), 
                onValueChange = { paidAmountState = it.toDoubleOrNull() ?: 0.0 }, 
                label = { Text("Initial Paid Amount") }, 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { showDatePicker = true }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Date", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(dateInMillis)),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = isLent,
                onClick = { isLent = true },
                label = { Text("I Lent") },
                leadingIcon = if (isLent) { { Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp)) } } else null
            )
            Spacer(modifier = Modifier.width(16.dp))
            FilterChip(
                selected = !isLent,
                onClick = { isLent = false },
                label = { Text("I Borrowed") },
                leadingIcon = if (!isLent) { { Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp)) } } else null
            )
        }

        OutlinedTextField(
            value = note, 
            onValueChange = { note = it }, 
            label = { Text("Note / Description") }, 
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        Button(
            onClick = {
                val total = totalAmount.toDoubleOrNull() ?: 0.0
                val additionalAmount = newPaymentInput.toDoubleOrNull() ?: 0.0
                if (personName.isNotBlank() && total > 0) {
                    onConfirm(
                        Due(
                            id = initialDue?.id ?: 0,
                            personName = personName,
                            amount = total,
                            paidAmount = updatedPaidAmount,
                            isLent = isLent,
                            note = note,
                            date = dateInMillis,
                            isPaid = updatedPaidAmount >= total && total > 0
                        ),
                        additionalAmount
                    )
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateInMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
