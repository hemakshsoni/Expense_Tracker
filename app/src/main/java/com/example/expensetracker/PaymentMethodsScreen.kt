package com.example.expensetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentMethodsScreen(
    viewModel: TransactionViewModel,
    onDeleteMethod: (PaymentMethod) -> Unit
) {
    val paymentMethods by viewModel.allPaymentMethods.collectAsState()
    val balances by viewModel.paymentMethodBalances.collectAsState()
    val spending by viewModel.paymentMethodSpending.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMethod by remember { mutableStateOf<PaymentMethod?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Your Wallets & Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(paymentMethods) { method ->
                PaymentMethodCard(
                    method = method,
                    balance = balances[method] ?: 0.0,
                    totalSpent = spending[method] ?: 0.0,
                    onDelete = { onDeleteMethod(method) },
                    onEdit = { editingMethod = method }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Method") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )

        if (showAddDialog) {
            AddEditPaymentMethodDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, balance, color ->
                    viewModel.upsertPaymentMethod(PaymentMethod(name = name, initialBalance = balance, colorHex = color))
                    showAddDialog = false
                }
            )
        }

        if (editingMethod != null) {
            AddEditPaymentMethodDialog(
                initialMethod = editingMethod,
                onDismiss = { editingMethod = null },
                onConfirm = { name, balance, color ->
                    viewModel.upsertPaymentMethod(editingMethod!!.copy(name = name, initialBalance = balance, colorHex = color))
                    editingMethod = null
                }
            )
        }
    }
}

@Composable
fun PaymentMethodCard(
    method: PaymentMethod,
    balance: Double,
    totalSpent: Double,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(method.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(cardColor, cardColor.copy(alpha = 0.7f))
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "Available Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹ ${"%,.2f".format(balance)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Total Spent",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹ ${"%,.0f".format(totalSpent)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            // Decorative Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 40.dp, y = 40.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
        }
    }
}

@Composable
fun AddEditPaymentMethodDialog(
    initialMethod: PaymentMethod? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initialMethod?.name ?: "") }
    var initialBalance by remember { mutableStateOf(initialMethod?.initialBalance?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    
    val presetColors = listOf(
        "#1E40AF", // Blue
        "#059669", // Emerald
        "#D97706", // Amber
        "#7C3AED", // Violet
        "#DB2777", // Pink
        "#4B5563", // Slate
        "#DC2626", // Red
        "#0891B2"  // Cyan
    )
    
    var selectedColor by remember { 
        mutableStateOf(initialMethod?.colorHex ?: presetColors.random()) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (initialMethod == null) "New Payment Method" else "Update Payment Method",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Account Name (e.g. HDFC Bank)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialBalance, 
                    onValueChange = { initialBalance = it }, 
                    label = { Text("Opening Balance") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("₹", modifier = Modifier.padding(start = 8.dp)) }
                )
                
                Column {
                    Text("Card Theme", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetColors.take(4).forEach { colorHex ->
                            ColorOption(
                                colorHex = colorHex,
                                isSelected = selectedColor == colorHex,
                                onClick = { selectedColor = colorHex }
                            )
                        }
                        IconButton(onClick = { selectedColor = presetColors.random() }) {
                            Icon(Icons.Default.Refresh, "Randomize", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetColors.drop(4).forEach { colorHex ->
                            ColorOption(
                                colorHex = colorHex,
                                isSelected = selectedColor == colorHex,
                                onClick = { selectedColor = colorHex }
                            )
                        }
                        Spacer(modifier = Modifier.size(40.dp)) // Alignment spacer
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, initialBalance.toDoubleOrNull() ?: 0.0, selectedColor) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (initialMethod == null) "Create" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            }
        }
    )
}

@Composable
fun ColorOption(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(android.graphics.Color.parseColor(colorHex))
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
