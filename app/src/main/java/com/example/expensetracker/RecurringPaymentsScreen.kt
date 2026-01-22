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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecurringPaymentsScreen(
    viewModel: TransactionViewModel
) {
    val recurringPayments by viewModel.allRecurringPayments.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<RecurringPayment?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (recurringPayments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recurring payments yet.", color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Manage Subscriptions & Bills",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(recurringPayments, key = { it.id }) { payment ->
                    RecurringPaymentCard(
                        payment = payment,
                        onEdit = { editingPayment = payment },
                        onDelete = { viewModel.deleteRecurringPayment(payment) },
                        onToggleActive = { viewModel.upsertRecurringPayment(payment.copy(isActive = !payment.isActive)) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Bill/Sub") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )

        if (showAddDialog) {
            AddEditRecurringDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onConfirm = { payment ->
                    viewModel.upsertRecurringPayment(payment)
                    showAddDialog = false
                }
            )
        }

        if (editingPayment != null) {
            AddEditRecurringDialog(
                viewModel = viewModel,
                initialPayment = editingPayment,
                onDismiss = { editingPayment = null },
                onConfirm = { payment ->
                    viewModel.upsertRecurringPayment(payment)
                    editingPayment = null
                }
            )
        }
    }
}

@Composable
fun RecurringPaymentCard(
    payment: RecurringPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(payment.nextDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (payment.isActive) 0.5f else 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(payment.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(payment.frequency, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Switch(
                    checked = payment.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.scale(0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Next Payment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formattedDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "₹${"%.2f".format(payment.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringDialog(
    viewModel: TransactionViewModel,
    initialPayment: RecurringPayment? = null,
    onDismiss: () -> Unit,
    onConfirm: (RecurringPayment) -> Unit
) {
    var title by remember { mutableStateOf(initialPayment?.title ?: "") }
    var amount by remember { mutableStateOf(initialPayment?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(initialPayment?.frequency ?: "Monthly") }
    var category by remember { mutableStateOf(initialPayment?.category ?: "") }
    var paymentMethod by remember { mutableStateOf(initialPayment?.paymentMethod ?: "") }
    var nextDateInMillis by remember { mutableLongStateOf(initialPayment?.nextDate ?: System.currentTimeMillis()) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")
    val categories by viewModel.allCategories.collectAsState()
    val methods by viewModel.allPaymentMethods.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPayment == null) "Add Recurring Payment" else "Edit Recurring Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Amount") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Frequency Dropdown
                var freqExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = !freqExpanded }) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        frequencies.forEach { f ->
                            DropdownMenuItem(text = { Text(f) }, onClick = { frequency = f; freqExpanded = false })
                        }
                    }
                }

                // Start Date Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(nextDateInMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Date") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }

                // Category Dropdown
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { category = c.name; catExpanded = false })
                        }
                    }
                }

                // Method Dropdown
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = !methodExpanded }) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        methods.forEach { m ->
                            DropdownMenuItem(text = { Text(m.name) }, onClick = { paymentMethod = m.name; methodExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                onConfirm(
                    RecurringPayment(
                        id = initialPayment?.id ?: 0,
                        title = title,
                        amount = amt,
                        category = category,
                        paymentMethod = paymentMethod,
                        frequency = frequency,
                        nextDate = nextDateInMillis,
                        isActive = initialPayment?.isActive ?: true
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

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
