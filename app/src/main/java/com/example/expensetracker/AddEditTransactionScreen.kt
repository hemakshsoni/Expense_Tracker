package com.example.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel,
    transactionId: Int
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Shared States
    var amount by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
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

    LaunchedEffect(transactionId) {
        if (transactionId != -1) {
            val transaction = viewModel.getTransactionById(transactionId)
            transaction?.let {
                amount = if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()
                dateInMillis = it.date
                originalTransaction = it
                description = it.description
                isAutoDetected = it.isAutoDetected
                merchantSource = it.merchantSource
                
                if (it.type == "TRANSFER") {
                    scope.launch { pagerState.scrollToPage(1) }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // --- HEADER TABS ---
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = primaryColor
                    )
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Transaction") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Transfer") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Enter Amount", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", style = MaterialTheme.typography.headlineLarge, color = primaryColor, fontWeight = FontWeight.Bold)
                        TextField(
                            value = amount,
                            onValueChange = { amount = it },
                            textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = primaryColor, textAlign = TextAlign.Start),
                            placeholder = { Text("0", fontSize = 48.sp, color = Color.LightGray, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            modifier = Modifier.width(IntrinsicSize.Min)
                        )
                    }

                    if (page == 0) {
                        // --- TRANSACTION UI ---
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(50), modifier = Modifier.height(48.dp).fillMaxWidth()) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (type == "EXPENSE") primaryColor else Color.Transparent).clickable { type = "EXPENSE" }) {
                                    Text("Expense", fontWeight = FontWeight.Bold, color = if (type == "EXPENSE") onPrimaryColor else Color.Gray)
                                }
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).background(if (type == "INCOME") primaryColor else Color.Transparent).clickable { type = "INCOME" }) {
                                    Text("Income", fontWeight = FontWeight.Bold, color = if (type == "INCOME") onPrimaryColor else Color.Gray)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title / Merchant") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        CategoryDropdown(category, dbCategories) { category = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        DatePickerField(dateInMillis) { showDatePicker = true }
                        Spacer(modifier = Modifier.height(16.dp))
                        PaymentMethodSelector("Payment Method", paymentMethod, paymentMethods) { paymentMethod = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)
                        
                        if (originalTransaction?.needsReview == true) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Remember category for \"${title}\"?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = allowLearning,
                                        onCheckedChange = { allowLearning = it },
                                        modifier = Modifier.graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        // --- TRANSFER UI ---
                        Spacer(modifier = Modifier.height(32.dp))
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                PaymentMethodSelector("From Account", fromMethod, paymentMethods) { fromMethod = it }
                                Icon(Icons.Default.SwapHoriz, null, tint = primaryColor, modifier = Modifier.padding(vertical = 8.dp).size(32.dp))
                                PaymentMethodSelector("To Account", toMethod, paymentMethods) { toMethod = it }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        DatePickerField(dateInMillis) { showDatePicker = true }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                scope.launch { snackbarHostState.showSnackbar("Please enter a valid amount") }
                                return@Button
                            }
                            
                            val transaction = if (pagerState.currentPage == 0) {
                                if (title.isBlank() || category.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Title and Category are required") }
                                    return@Button
                                }
                                Transaction(
                                    id = if (transactionId == -1) 0 else transactionId,
                                    title = title, amount = amt, category = category,
                                    date = dateInMillis, type = type, paymentMethod = paymentMethod,
                                    description = description, needsReview = false, allowLearning = allowLearning,
                                    isAutoDetected = isAutoDetected,
                                    merchantSource = merchantSource,
                                    reference = originalTransaction?.reference
                                )
                            } else {
                                if (fromMethod == toMethod) {
                                    scope.launch { snackbarHostState.showSnackbar("Source and Destination cannot be the same") }
                                    return@Button
                                }
                                Transaction(
                                    id = if (transactionId == -1) 0 else transactionId,
                                    title = "Transfer: $fromMethod → $toMethod",
                                    amount = amt, category = "Transfer", date = dateInMillis,
                                    type = "TRANSFER", paymentMethod = fromMethod, toPaymentMethod = toMethod,
                                    description = description,
                                    needsReview = false,
                                    isAutoDetected = isAutoDetected,
                                    merchantSource = merchantSource,
                                    reference = originalTransaction?.reference
                                )
                            }
                            
                            viewModel.upsertTransaction(transaction)
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save ${if(pagerState.currentPage == 0) "Transaction" else "Transfer"}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(selected: String, categories: List<Category>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, label = { Text("Category") },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(text = { Text(cat.name) }, onClick = { onSelect(cat.name); expanded = false })
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
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable { onClick() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelector(label: String, selected: String, methods: List<PaymentMethod>, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.forEach { method ->
                FilterChip(
                    selected = selected == method.name,
                    onClick = { onSelect(method.name) },
                    label = { Text(method.name) },
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}


fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
