package com.example.expensetracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StatsScreen(transactions: List<Transaction>) {
    val scrollState = rememberScrollState()

    // 1. Calculate Category Totals (Expenses Only)
    val categoryTotals = remember(transactions) {
        transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalExpense = categoryTotals.sumOf { it.second }

    // 2. Calculate Monthly Totals
    val monthlyTotals = remember(transactions) {
        val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        transactions
            .groupBy { fmt.format(java.util.Date(it.date)) }
            .mapValues { entry ->
                val income = entry.value.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = entry.value.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                income to expense
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        // --- PIE CHART SECTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Expense Breakdown", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(24.dp))

                if (totalExpense > 0) {
                    SimplePieChart(categoryTotals, totalExpense)
                    Spacer(modifier = Modifier.height(24.dp))
                    // Legend
                    categoryTotals.take(5).forEachIndexed { index, (cat, amt) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, color = getChartColor(index))
                            Text("₹${amt.toInt()}", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("No expenses yet", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- MONTHLY SUMMARY SECTION ---
        Text("Monthly Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        monthlyTotals.forEach { (month, data) ->
            val (income, expense) = data
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(month, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("+ ₹${income.toInt()}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        Text("- ₹${expense.toInt()}", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
    }
}

@Composable
fun SimplePieChart(data: List<Pair<String, Double>>, total: Double) {
    val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF2196F3))

    Canvas(modifier = Modifier.size(200.dp)) {
        var startAngle = -90f
        val radius = size.minDimension / 2
        val strokeWidth = 40f

        data.forEachIndexed { index, (_, value) ->
            val sweepAngle = (value / total * 360).toFloat()
            val color = colors.getOrElse(index) { Color.Gray }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

fun getChartColor(index: Int): Color {
    val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF2196F3))
    return colors.getOrElse(index) { Color.Gray }
}