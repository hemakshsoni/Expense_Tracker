package com.example.expensetracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch


@Composable
fun HomePagerScreen(
    transactions: List<Transaction>,
    reviewTransactions: List<Transaction>,
    totalBalance: Double,
    onAddClick: () -> Unit,
    onItemClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Move statusBarsPadding to the Column to ensure it starts at the very top
    Column(modifier = Modifier.statusBarsPadding()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                text = { Text("Home") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                text = { Text("Review") }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    TransactionScreen(
                        transactions = transactions,
                        totalBalance = totalBalance,
                        onAddClick = onAddClick,
                        onItemClick = onItemClick,
                        onDeleteClick = onDeleteClick,
                        reviewTransactions = reviewTransactions
                    )
                }

                1 -> {
                    ReviewScreen(
                        reviewTransactions = reviewTransactions,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}
