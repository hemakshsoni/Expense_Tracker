package com.example.expensetracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: TransactionViewModel
) {
    val statsData by viewModel.statsData.collectAsState(initial = emptyMap())
    val timeframe by viewModel.analyticsTimeframe.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // --- TIMEFRAME FILTER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsTimeframe.entries.forEach { t ->
                FilterChip(
                    selected = timeframe == t,
                    onClick = { viewModel.setAnalyticsTimeframe(t) },
                    label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        // --- ANALYTICS TABS ---
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Categories") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Payment Methods") }
            )
        }

        if (statsData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transaction data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { page ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val sortedKeys = statsData.keys.sortedDescending()
                    
                    items(sortedKeys) { key ->
                        val stats = statsData[key]
                        if (stats != null) {
                            SummaryCard(
                                title = formatTimeframeKey(key, timeframe),
                                income = stats.income,
                                expense = stats.expense,
                                breakdown = if (page == 0) stats.categoryBreakdown else stats.paymentMethodBreakdown,
                                breakdownTitle = if (page == 0) "Spending by Category" else "Spending by Payment Method"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    income: Double,
    expense: Double,
    breakdown: Map<String, Double>,
    breakdownTitle: String
) {
    val total = income + expense
    val expenseWeight = if (total > 0) (expense / (income + expense)).toFloat() else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Overview",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DonutChart(expenseWeight)
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRow("Income", income, Color(0xFF4CAF50))
                    StatRow("Expense", expense, Color(0xFFF44336))
                    StatRow("Savings", income - expense, MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = breakdownTitle, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (breakdown.isEmpty()) {
                Text("No data for this view.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                breakdown.entries.sortedByDescending { it.value }.forEach { (label, amount) ->
                    BreakdownBar(label, amount, expense)
                }
            }
        }
    }
}

@Composable
fun DonutChart(expenseWeight: Float) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(expenseWeight) {
        animatedProgress.animateTo(expenseWeight, animationSpec = tween(1000))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Canvas(modifier = Modifier.size(80.dp)) {
            drawArc(
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFF44336),
                startAngle = -90f,
                sweepAngle = animatedProgress.value * 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(expenseWeight * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatRow(label: String, amount: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("₹${String.format(Locale.getDefault(), "%.0f", amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BreakdownBar(label: String, amount: Double, totalExpense: Double) {
    val percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("₹${String.format(Locale.getDefault(), "%.0f", amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.Transparent, RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}

fun formatTimeframeKey(key: String, timeframe: AnalyticsTimeframe): String {
    return try {
        when (timeframe) {
            AnalyticsTimeframe.MONTHLY -> {
                val parts = key.split("-")
                val year = parts[0]
                val monthInt = parts[1].toInt()
                val monthNames = arrayOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                "${monthNames[monthInt - 1]} $year"
            }
            AnalyticsTimeframe.YEARLY -> key
        }
    } catch (e: Exception) {
        key
    }
}
