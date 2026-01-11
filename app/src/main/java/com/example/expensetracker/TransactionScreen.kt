package com.example.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class) // Needed for SwipeToDismiss
@Composable
fun TransactionScreen(
    transactions: List<Transaction>,
    totalBalance: Double,
    onAddClick: () -> Unit,
    onItemClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    reviewTransactions: List<Transaction>
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // 1. Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Balance", style = MaterialTheme.typography.labelMedium)
                    // FIX: Changed '$' to 'Rs' to match your SMS data
                    Text(
                        text = "₹ ${"%.2f".format(totalBalance)}", // Formats to 2 decimal places
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 2. Transaction List
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet. Tap + to add one!")
                }
            } else {
                LazyColumn {
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteClick(transaction)
                                    return@rememberSwipeToDismissBoxState true
                                }
                                return@rememberSwipeToDismissBoxState false
                            },
                            positionalThreshold = { it * 0.4f }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = MaterialTheme.colorScheme.errorContainer
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp)
                                        .clip(CardDefaults.shape)
                                        .background(color),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            },
                            content = {
                                TransactionItem(
                                    transaction = transaction,
                                    onClick = { onItemClick(transaction) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) {
    val formattedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transaction.date))

    // FIX: Check for "Expense" (case-insensitive) to fix the colors
    val isExpense = transaction.type.equals("Expense", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (transaction.needsReview) {
                    Text(
                        "Needs Review",
                        color = Color(0xFF856404),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    text = "${transaction.paymentMethod} • ${transaction.category} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // FIX: Dynamic Color & Sign based on isExpense
            Text(
                text = if (isExpense) "- ₹${transaction.amount}" else "+ ₹${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isExpense) Color.Red else Color(0xFF00C853), // Red for Spent, Green for Income
                fontWeight = FontWeight.Bold
            )


        }
    }
}