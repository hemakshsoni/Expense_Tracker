package com.example.expensetracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute


@Composable
fun AppNavigation(viewModel: TransactionViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routs.Home
    ) {
        // SCREEN 1: Home
        composable<Routs.Home> {
            // Collect the "Live" data from ViewModel
            val transactions by viewModel.allTransactions.collectAsState()
            val reviewTransactions by viewModel.reviewTransactions.collectAsState()
            val balance by viewModel.totalBalance.collectAsState()

            HomePagerScreen(
                transactions = transactions,
                reviewTransactions = reviewTransactions,
                totalBalance = balance,
                onAddClick = { navController.navigate(Routs.AddEdit()) },
                onItemClick = { txn ->
                    navController.navigate(Routs.AddEdit(transactionId = txn.id))
                },
                onDeleteClick = { viewModel.deleteTransaction(it) }
            )

        }

        // SCREEN 2: Add / Edit Form
        composable<Routs.AddEdit> { backStackEntry ->
            // Extract the 'transactionId' passed from the previous screen
            val args = backStackEntry.toRoute<Routs.AddEdit>()

            AddEditTransactionScreen(
                navController = navController,
                viewModel = viewModel,
                transactionId = args.transactionId
            )
        }
    }
}