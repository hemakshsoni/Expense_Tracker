package com.example.expensetracker


import java.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
    transactionId: Int
) {
    // 1. State Holders
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var paymentMethod by remember { mutableStateOf("Cash") }

    // Date States
    var dateInMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 2. Load Data Logic
    LaunchedEffect(transactionId) {
        if (transactionId != -1) {
            val transaction = viewModel.getTransactionById(transactionId)
            transaction?.let {
                title = it.title
                amount = it.amount.toString()
                category = it.category
                type = it.type
                paymentMethod = it.paymentMethod
                dateInMillis = it.date
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val action = if (transactionId == -1) "Add" else "Edit"
                    val typeText = if (type == "EXPENSE") "Expense" else "Income"
                    Text("$action $typeText")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            // 1. Define your list of categories
            val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Salary", "Investment", "Other")

            // 2. State for the menu (Is it open or closed?)
            var expanded by remember { mutableStateOf(false) }

            // 3. The Dropdown Component
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                // The Text Field that shows the current selection
                OutlinedTextField(
                    value = category,
                    onValueChange = {}, // Read-only, changes happen in the menu items
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor() // CRITICAL: This links the popup to this text field
                )

                // The Popup Menu
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { selection ->
                        DropdownMenuItem(
                            text = { Text(selection) },
                            onClick = {
                                category = selection // Update the state
                                expanded = false     // Close the menu
                            }
                        )
                    }
                }
            }


            Text("Payment Method", style = MaterialTheme.typography.labelMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val methods = listOf("Cash", "UPI", "Card")

                methods.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) },
                        leadingIcon = {
                            if (paymentMethod == method) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        }
                    )
                }
            }

            // ... (Above your Date Field) ...

            // Type Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == "EXPENSE",
                    onClick = { type = "EXPENSE" },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == "INCOME",
                    onClick = { type = "INCOME" },
                    label = { Text("Income") },
                    modifier = Modifier.weight(1f)
                )
            }

            // --- DATE FIELD (Fixed Box Logic) ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = convertMillisToDate(dateInMillis),
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    },
                    enabled = false, // Visual only
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                // The Invisible Overlay that captures the click
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }
            // ------------------------------------

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (title.isNotEmpty() && amountDouble != null) {
                        val transaction = Transaction(
                            id = if (transactionId == -1) 0 else transactionId,
                            title = title,
                            amount = amountDouble,
                            category = category,
                            type = type,
                            date = dateInMillis, // Fixed: Using the SELECTED date, not current time
                            paymentMethod = paymentMethod,
                            needsReview = false
                        )
                        viewModel.upsertTransaction(transaction)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }

        // --- DIALOG LOGIC (This MUST be here) ---
        // This reads the 'showDatePicker' variable. Without this, the warning appears.
        // ... inside AddEditTransactionScreen ...

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dateInMillis
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        // Only update if the user actually picked a date
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            dateInMillis = selectedDate
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// Fixed Helper function using generic Java import (Works on all Android versions)
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}