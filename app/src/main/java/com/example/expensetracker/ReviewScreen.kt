package com.example.expensetracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun ReviewScreen(
    reviewTransactions: List<Transaction>,
    onItemClick: (Transaction) -> Unit
) {
    if (reviewTransactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No transactions to review 🎉")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(reviewTransactions) { txn ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onItemClick(txn) },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3CD)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(txn.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        "₹${txn.amount}",
                        color = if (txn.type == "Expense") Color.Red else Color.Green
                    )
                    Text(
                        "Needs review",
                        color = Color(0xFF856404),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
