package com.example.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onNavigateToReview: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDues: () -> Unit,
    onNavigateToBills: () -> Unit
) {

    val userName by viewModel.userName.collectAsState()
    // --- 1. COLLECT REAL DATA FROM VIEWMODEL ---
    val totalBalance by viewModel.totalBalance.collectAsState(initial = 0.0)
    val totalIncome by viewModel.totalIncome.collectAsState(initial = 0.0)
    val totalExpense by viewModel.totalExpense.collectAsState(initial = 0.0)

    // Pending Transactions (Review Banner)
    val reviewTransactions by viewModel.reviewTransactions.collectAsState(initial = emptyList())

    // Bills (Sorted by nearest date)
    val allBills by viewModel.allRecurringPayments.collectAsState(initial = emptyList())
    val upcomingBills = remember(allBills) {
        allBills
            .sortedBy { it.nextDate }
            .take(3) // Show only top 3
    }

    // Dues Summary
    val totalLent by viewModel.totalLent.collectAsState(initial = 0.0)
    val totalBorrowed by viewModel.totalBorrowed.collectAsState(initial = 0.0)

    // Chart Data (Mon-Sun)
    val weeklySpending by viewModel.weeklySpending.collectAsState(initial = List(7) { 0f })

    // Top Categories (Calculated from recent transactions)
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val categoryIconMap by viewModel.categoryIconMap.collectAsState()
    val topCategories = remember(allTransactions) {
        calculateTopCategories(allTransactions, categoryIconMap)
    }

    var showProfileSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Header
            item {
                DashboardHeader(userName = userName, onProfileClick = {showProfileSheet = true})
            }

            // 2. Balance Card
            item {
                BalanceCard(
                    totalBalance = totalBalance,
                    income = totalIncome,
                    expense = totalExpense
                )
            }

            // 3. Alert Banner
            if (reviewTransactions.isNotEmpty()) {
                item {
                    PendingReviewBanner(
                        count = reviewTransactions.size,
                        onClick = onNavigateToReview
                    )
                }
            }

            // 4. Weekly Chart
            item {
                Column {
                    SectionHeader(title = "This Week's Spending", actionText = "History", onAction = onNavigateToTransactions)
                    Spacer(modifier = Modifier.height(12.dp))
                    WeeklyChart(data = weeklySpending)
                }
            }

            // 5. Top Categories
            if (topCategories.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Where your money went",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(topCategories) { category ->
                                CategoryCard(category)
                            }
                        }
                    }
                }
            }

            // 6. Upcoming Bills Section (Now visible even if categories are empty)
            item {
                Column {
                    SectionHeader(title = "Upcoming Bills", actionText = "See All", onAction = onNavigateToBills)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (upcomingBills.isEmpty()) {
                        EmptySectionPlaceholder("No upcoming bills")
                    } else {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                upcomingBills.forEachIndexed { index, bill ->
                                    UpcomingBillRow(bill)
                                    if (index < upcomingBills.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Dues & Debts Section
            item {
                Column {
                    SectionHeader(title = "Dues & Debts", actionText = "Manage", onAction = onNavigateToDues)
                    Spacer(modifier = Modifier.height(12.dp))
                    DuesCard(toReceive = totalLent, toPay = totalBorrowed)
                }
            }
        }
    }
    if (showProfileSheet) {
        EditProfileSheet(
            currentName = userName,
            onDismiss = { showProfileSheet = false },
            onSave = { newName ->
                viewModel.updateUserName(newName)
                showProfileSheet = false
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        TextButton(onClick = onAction) { Text(actionText, color = MaterialTheme.colorScheme.primary) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeader(userName: String,onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = onProfileClick) {
                Text(userName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(currentName) }

    // 1. ADD THIS: Focus Manager to control keyboard
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.isNotBlank()) {
                            // 4. FIX: Close keyboard -> Try Hide -> Finally Save
                            focusManager.clearFocus()
                            scope.launch {
                                sheetState.hide()
                                onSave(name)
                            }
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        // 5. FIX: Same safe logic for the button
                        focusManager.clearFocus()
                        scope.launch {
                            try { sheetState.hide() } catch (e: Exception) { /* Ignore */ }
                            finally { onSave(name) }
                        }
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



// ---COMPONENT: Balance Card
@Composable
fun BalanceCard(
    totalBalance: Double,
    income: Double,
    expense: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            // 1. DECORATIVE BACKGROUND ICON (The "Watermark")
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f), // Very subtle
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterEnd) // Positioned on the right
                    .offset(x = 30.dp, y = 20.dp) // Shifted slightly off-screen
            )

            // 2. MAIN CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- Top Section: Balance ---
                Column {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${"%,.2f".format(totalBalance)}",
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp), // Bigger text
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // --- Bottom Section: Income & Expense Panel ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)) // Glass effect
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income
                    StatItem(
                        label = "Income",
                        amount = income,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = IncomeGreen
                    )

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    )

                    // Expense
                    StatItem(
                        label = "Expense",
                        amount = expense,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = ExpenseRed // Bright Red
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    amount: Double,
    icon: ImageVector,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Text Info
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = "₹${"%.0f".format(amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// --- COMPONENT: Weekly Chart (Dynamic) ---
@Composable
fun WeeklyChart(data: List<Float>) {
    // 1. Find MAX height using ABSOLUTE value (so -5000 is as tall as +5000)
    val maxVal = data.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Weekly Net Flow", // Changed Title
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.zip(days).forEach { (netAmount, dayLabel) ->

                    // 2. Use ABSOLUTE value for height fraction
                    val absAmount = kotlin.math.abs(netAmount)
                    val heightFraction = (absAmount / maxVal).coerceIn(0f, 1f)

                    // 3. Pick Color: Green for Profit, Red for Loss
                    val isPositive = netAmount >= 0
                    val barColor = if (isPositive) IncomeGreen else ExpenseRed

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // The Bar
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                // Height relies on ABSOLUTE amount
                                .fillMaxHeight(0.8f * (if (absAmount > 0) heightFraction else 0.02f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (absAmount > 0) barColor else MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // The Label
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENT: Pending Review Banner ---
@Composable
fun PendingReviewBanner(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("$count New Transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Tap to review and categorize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
            }
        }
    }
}

// --- HELPER: Category Logic ---
data class CategorySummary(val name: String, val amount: Double, val color: Color, val icon: ImageVector)

fun calculateTopCategories(transactions: List<Transaction>, categoryIconMap: Map<String, String>): List<CategorySummary> {
    // Group expenses by category and sum amounts
    val expenses = transactions.filter { it.type == "EXPENSE" }
    val grouped = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
        .take(5)

    // Map to UI model with dynamic colors
    val colors = listOf(Color(0xFFFFB74D), Color(0xFF64B5F6), Color(0xFFE57373), Color(0xFF81C784), Color(0xFFBA68C8))

    return grouped.mapIndexed { index, entry ->
        CategorySummary(
            name = entry.key,
            amount = entry.value,
            color = colors.getOrElse(index) { Color.Gray },
            icon = IconUtils.getIcon(categoryIconMap[entry.key])
        )
    }
}

@Composable
fun CategoryCard(category: CategorySummary) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(category.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, null, tint = category.color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(category.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("₹${"%,.0f".format(category.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// --- COMPONENT: Upcoming Bill Row (Dynamic) ---
@Composable
fun UpcomingBillRow(bill: RecurringPayment) {
    val daysUntil = getDaysUntil(bill.nextDate)
    val urgencyColor = if (daysUntil <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val dateText = if (daysUntil == 0L) "Due Today" else if (daysUntil == 1L) "Due Tomorrow" else "Due in $daysUntil days"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bill.title.trim().replace("\\s+","").take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = urgencyColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bill.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = urgencyColor
            )
        }

        Text(
            text = "₹${"%,.0f".format(bill.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- COMPONENT: Dues Card (Wired) ---
@Composable
fun DuesCard(toReceive: Double?, toPay: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DuesStatusBox(
            modifier = Modifier.weight(1f),
            label = "To Receive",
            amount = "₹${"%,.0f".format(toReceive)}",
            color = IncomeGreen,
            icon = Icons.Default.ArrowDownward
        )

        DuesStatusBox(
            modifier = Modifier.weight(1f),
            label = "To Pay",
            amount = "₹${"%,.0f".format(toPay)}",
            color = ExpenseRed,
            icon = Icons.Default.ArrowUpward
        )
    }
}

@Composable
fun DuesStatusBox(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun EmptySectionPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}