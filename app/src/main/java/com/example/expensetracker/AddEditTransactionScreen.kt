package com.example.expensetracker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    onDismiss: () -> Unit,
    viewModel: TransactionViewModel,
    transactionId: Int,
    mode: Int = 0 // 0 = Transaction, 1 = Transfer
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Shared States
    var amount by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    // Transaction States
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var originalTransaction by remember { mutableStateOf<Transaction?>(null) }
    var allowLearning by remember { mutableStateOf(false) }
    var isAutoDetected by remember { mutableStateOf(false) }
    var merchantSource by remember { mutableStateOf(MerchantSource.SENDER) }

    // Transfer States
    var fromMethod by remember { mutableStateOf("Cash") }
    var toMethod by remember { mutableStateOf("UPI") }

    val paymentMethods by viewModel.allPaymentMethods.collectAsState()
    val dbCategories by viewModel.allCategories.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    // 1. EXISTING: Loads data when editing
    LaunchedEffect(transactionId) {
        if (transactionId != -1) {
            val transaction = viewModel.getTransactionById(transactionId)
            transaction?.let {
                amount = if (it.amount % 1.0 == 0.0) it.amount.toInt()
                    .toString() else it.amount.toString()
                dateInMillis = it.date
                originalTransaction = it
                description = it.description
                isAutoDetected = it.isAutoDetected
                merchantSource = it.merchantSource

                if (it.type == "TRANSFER") {
                    fromMethod = it.paymentMethod
                    toMethod = it.toPaymentMethod ?: ""
                } else {
                    title = it.title
                    category = it.category
                    type = it.type
                    paymentMethod = it.paymentMethod
                    allowLearning = it.allowLearning
                }
            }
        }
    }

    // 2. NEW: Watches 'title' to Auto-Fill Category (Only for NEW transactions)
    LaunchedEffect(title) {
        // We check 'transactionId == -1' so we don't accidentally change the category
        // when you are just opening an existing transaction to edit it.
        if (transactionId == -1 && title.length > 2 && !isAutoDetected) {
            val match = viewModel.findAutoCategory(title)
            if (match != null) {
                category = match.category
                isAutoDetected = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            // --- HEADER ---
            Text(
                text = if (mode == 1) "New Transfer" else if (transactionId != -1) "Edit Transaction" else "New Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- COMMON: AMOUNT INPUT ---
            Text(
                "Enter Amount",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₹",
                    style = MaterialTheme.typography.displayMedium,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    textStyle = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Start
                    ),
                    placeholder = {
                        Text(
                            "0",
                            fontSize = 48.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.width(IntrinsicSize.Min)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CONDITIONAL UI BASED ON MODE ---
            if (mode == 0) {
                // ==========================
                //      TRANSACTION FORM
                // ==========================

                // 1. Type Switcher
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(if (type == "EXPENSE") primaryColor else Color.Transparent)
                                .clickable { type = "EXPENSE" }) {
                            Text(
                                "Expense",
                                fontWeight = FontWeight.Bold,
                                color = if (type == "EXPENSE") onPrimaryColor else Color.Gray
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(if (type == "INCOME") primaryColor else Color.Transparent)
                                .clickable { type = "INCOME" }) {
                            Text(
                                "Income",
                                fontWeight = FontWeight.Bold,
                                color = if (type == "INCOME") onPrimaryColor else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Details
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Merchant") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                CategoryDropdown(category, dbCategories) { category = it }
                Spacer(modifier = Modifier.height(16.dp))

                DatePickerField(dateInMillis) { showDatePicker = true }
                Spacer(modifier = Modifier.height(16.dp))

                PaymentMethodSelector(
                    "Payment Method",
                    paymentMethod,
                    paymentMethods
                ) { paymentMethod = it }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3
                )

                if (originalTransaction?.needsReview == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Remember category?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Future transactions for \"$title\" will use \"$category\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = allowLearning,
                                onCheckedChange = { allowLearning = it }
                            )
                        }
                    }
                }

            } else {
                // ==========================
                //       TRANSFER FORM
                // ==========================

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PaymentMethodSelector(
                            "From Account",
                            fromMethod,
                            paymentMethods
                        ) { fromMethod = it }

                        Box(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ), contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, tint = primaryColor)
                        }

                        PaymentMethodSelector(
                            "To Account",
                            toMethod,
                            paymentMethods
                        ) { toMethod = it }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                DatePickerField(dateInMillis) { showDatePicker = true }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- ACTION BUTTON ---
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val transaction = if (mode == 0) {
                        // Transaction Logic
                        if (title.isBlank() || category.isBlank()) {
                            Toast.makeText(context, "Title and Category are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        Transaction(
                            id = if (transactionId == -1) 0 else transactionId,
                            title = title,
                            amount = amt,
                            category = category,
                            date = dateInMillis,
                            type = type,
                            paymentMethod = paymentMethod,
                            description = description,
                            needsReview = false,
                            allowLearning = allowLearning,
                            isAutoDetected = isAutoDetected,
                            merchantSource = merchantSource,
                            reference = originalTransaction?.reference
                        )
                    } else {
                        // Transfer Logic
                        if (fromMethod == toMethod) {
                            Toast.makeText(context, "Source and Destination cannot be the same", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        Transaction(
                            id = if (transactionId == -1) 0 else transactionId,
                            title = "Transfer: $fromMethod → $toMethod",
                            amount = amt,
                            category = "Transfer",
                            date = dateInMillis,
                            type = "TRANSFER",
                            paymentMethod = fromMethod,
                            toPaymentMethod = toMethod,
                            description = description,
                            needsReview = false,
                            isAutoDetected = isAutoDetected,
                            merchantSource = merchantSource,
                            reference = originalTransaction?.reference
                        )
                    }

                    viewModel.upsertTransaction(transaction)
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Confirm Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Padding for Nav Bar
            Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp))
        }
    }

    // --- DATE PICKER DIALOG ---
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

// --- Helper Composables (Kept mostly same, adjusted Row to be scrollable) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelector(
    label: String,
    selected: String,
    methods: List<PaymentMethod>,
    onSelect: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Added horizontalScroll to ensure chips don't get cut off in the sheet
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            methods.forEach { method ->
                FilterChip(
                    selected = selected == method.name,
                    onClick = { onSelect(method.name) },
                    label = { Text(method.name) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// Keep CategoryDropdown, DatePickerField, convertMillisToDate exactly as they were in your code
// I have omitted them here for brevity as they don't require changes for the sheet logic,
// but ensure they are present in the file.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(selected: String, categories: List<Category>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = { onSelect(cat.name); expanded = false })
            }
        }
    }
}

@Composable
fun DatePickerField(dateMillis: Long, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = convertMillisToDate(dateMillis), onValueChange = {}, readOnly = true,
            label = { Text("Date") }, trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier
            .matchParentSize()
            .clickable { onClick() })
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}