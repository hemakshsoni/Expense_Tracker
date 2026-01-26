package com.example.expensetracker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.*



// --- Color Palette ---
val ChartColors = listOf(
    Color(0xFF5C6BC0), Color(0xFFEC407A), Color(0xFF26A69A),
    Color(0xFFFFA726), Color(0xFF7E57C2), Color(0xFF8D6E63),
    Color(0xFF78909C), Color(0xFF9CCC65), Color(0xFFAB47BC)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnalyticsScreen(viewModel: TransactionViewModel) {
    // 1. Collect Data
    val statsData by viewModel.statsData.collectAsState(initial = emptyMap())
    val timeframe by viewModel.analyticsTimeframe.collectAsState()

    // 2. Local State for Navigation
    // We sort keys descending (e.g., ["2026-02", "2026-01"]) to find the "current" index
    val availableKeys = remember(statsData) { statsData.keys.sortedDescending() }
    var currentIndex by remember { mutableIntStateOf(0) }

    // Reset index if timeframe changes
    LaunchedEffect(timeframe) { currentIndex = 0 }

    val currentKey = availableKeys.getOrNull(currentIndex)
    val currentStats = if (currentKey != null) statsData[currentKey] else null

    // Tabs State (0 = Categories, 1 = Methods)
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER: Timeframe & Navigation ---
        AnalyticsHeader(
            timeframe = timeframe,
            onTimeframeChange = { viewModel.setAnalyticsTimeframe(it) },
            currentLabel = currentKey?.let { formatTimeframeKey(it, timeframe) } ?: "No Data",
            hasPrevious = currentIndex < availableKeys.lastIndex,
            hasNext = currentIndex > 0,
            onPreviousClick = { currentIndex++ },
            onNextClick = { currentIndex-- }
        )

        // --- CONTENT ---
        if (currentStats == null) {
            AnalyticsEmptyState() // Renamed to avoid conflict
        } else {
            // Determine which data map to show
            val breakdownData = if (selectedTab == 0) currentStats.categoryBreakdown else currentStats.paymentMethodBreakdown
            val totalExpense = currentStats.expense

            Column(modifier = Modifier.fillMaxSize()) {

                // 1. CHART SECTION (Top Half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PieChartWithLegend(
                            data = breakdownData,
                            totalExpense = totalExpense
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Summary Row (Income vs Expense)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryChip(
                                label = "Income",
                                amount = currentStats.income,
                                color = Color(0xFF66BB6A), // Green
                                isIncome = true
                            )
                            SummaryChip(
                                label = "Expense",
                                amount = currentStats.expense,
                                color = Color(0xFFEF5350), // Red
                                isIncome = false
                            )
                        }
                    }
                }

                // 2. LIST SECTION (Bottom Half - White Card Effect)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)) {

                        // Segmented Control Tabs
                        CustomTabSelector(
                            selectedIndex = selectedTab,
                            options = listOf("Categories", "Payment Methods"),
                            onSelect = { selectedTab = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Animated List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            val sortedItems = breakdownData.entries.sortedByDescending { it.value }

                            items(sortedItems) { (name, amount) ->
                                // Calculate color index based on hash or position
                                val colorIndex = breakdownData.keys.indexOf(name) % ChartColors.size
                                val color = ChartColors.getOrElse(colorIndex) { MaterialTheme.colorScheme.primary }

                                BreakdownItem(
                                    name = name,
                                    amount = amount,
                                    total = totalExpense,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun AnalyticsHeader(
    timeframe: AnalyticsTimeframe,
    onTimeframeChange: (AnalyticsTimeframe) -> Unit,
    currentLabel: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timeframe Segmented Button (Mini)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.height(32.dp)) {
            AnalyticsTimeframe.entries.forEachIndexed { index, tf ->
                SegmentedButton(
                    selected = timeframe == tf,
                    onClick = { onTimeframeChange(tf) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AnalyticsTimeframe.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = tf.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Navigator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            IconButton(onClick = onPreviousClick, enabled = hasPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = if (hasPrevious) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.3f)
                )
            }

            // Text Animation
            AnimatedContent(
                targetState = currentLabel,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                            scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                            fadeOut(animationSpec = tween(90))
                },
                label = "DateChange"
            ) { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = onNextClick, enabled = hasNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = if (hasNext) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun PieChartWithLegend(
    data: Map<String, Double>,
    totalExpense: Double
) {
    val animatedProgress = remember { Animatable(0f) }

    // Animate chart on data change
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        if (totalExpense > 0) {
            Canvas(modifier = Modifier.size(160.dp)) {
                var startAngle = -90f
                val strokeWidth = 24.dp.toPx()

                // Sort to match color assignment
                val sortedEntries = data.entries.sortedByDescending { it.value }

                sortedEntries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / totalExpense) * 360f).toFloat() * animatedProgress.value
                    // Fix: Ensure we don't draw tiny slivers that look like glitches
                    if (sweepAngle > 1f) {
                        val color = ChartColors.getOrElse(index % ChartColors.size) { Color.Gray }

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // Gap between arcs
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += ((entry.value / totalExpense) * 360f).toFloat() * animatedProgress.value
                }
            }
        } else {
            // Empty Chart Ring
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.1f),
                    style = Stroke(width = 24.dp.toPx())
                )
            }
        }

        // Inner Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${String.format(Locale.getDefault(), "%.0f", totalExpense)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SummaryChip(label: String, amount: Double, color: Color, isIncome: Boolean) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(
            text = "${if (isIncome) "+" else "-"}₹${String.format(Locale.getDefault(), "%.0f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CustomTabSelector(selectedIndex: Int, options: List<String>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, text ->
            val isSelected = selectedIndex == index
            val animatedBgColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent, label = "bg"
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, label = "text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(animatedBgColor)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun BreakdownItem(name: String, amount: Double, total: Double, color: Color) {
    val percentage = if (total > 0) (amount / total).toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon / Color Indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.0f", amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${(percentage * 100).toInt()}% of total",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AnalyticsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No data for this period",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Robust Helper Function
fun formatTimeframeKey(key: String, timeframe: AnalyticsTimeframe): String {
    return try {
        when (timeframe) {
            AnalyticsTimeframe.MONTHLY -> {
                // Check format validity before splitting
                if (key.contains("-")) {
                    val parts = key.split("-")
                    if (parts.size >= 2) {
                        val year = parts[0]
                        val monthInt = parts[1].toInt()
                        val monthNames = arrayOf(
                            "January", "February", "March", "April", "May", "June",
                            "July", "August", "September", "October", "November", "December"
                        )
                        // Safe index access
                        if (monthInt in 1..12) {
                            "${monthNames[monthInt - 1]} $year"
                        } else {
                            key // Fallback if month is out of range
                        }
                    } else {
                        key
                    }
                } else {
                    key // Return key as is if format is unexpected
                }
            }
            AnalyticsTimeframe.YEARLY -> key // Usually just "YYYY"
        }
    } catch (_: Exception) {
        key // Fallback to raw key on any error
    }
}