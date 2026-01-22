package com.example.expensetracker

import TransactionSortOption
import TransactionTypeFilter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerScreen(
    transactions: List<Transaction>,
    reviewTransactions: List<Transaction>,
    totalBalance: Double,
    onAddClick: () -> Unit,
    onItemClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit,
    onDeleteSelectedClick: (Set<Int>) -> Unit,
    typeFilter: TransactionTypeFilter,
    sortOption: TransactionSortOption,
    categoryFilters: Set<String>,
    paymentMethodFilters: Set<String>,
    availableCategories: List<String>,
    availablePaymentMethods: List<String>,
    onTypeFilterChange: (TransactionTypeFilter) -> Unit,
    onSortChange: (TransactionSortOption) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onPaymentMethodToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    onClearPaymentMethods: () -> Unit,
    onReviewAllClick: () -> Unit,
    onReviewSelectedClick: (Set<Int>) -> Unit,
    viewModel: TransactionViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Selection states lifted up for BackHandler support
    var homeSelectedIds by remember { mutableStateOf(setOf<Int>()) }
    var reviewSelectedIds by remember { mutableStateOf(setOf<Int>()) }

    val isHomeSelectionMode = homeSelectedIds.isNotEmpty() && pagerState.currentPage == 0
    val isReviewSelectionMode = reviewSelectedIds.isNotEmpty() && pagerState.currentPage == 1
    val isReviewTabActive = pagerState.currentPage == 1

    // BACK BUTTON LOGIC
    BackHandler(enabled = isHomeSelectionMode || isReviewSelectionMode || isReviewTabActive) {
        if (isHomeSelectionMode) {
            homeSelectedIds = emptySet()
        } else if (isReviewSelectionMode) {
            reviewSelectedIds = emptySet()
        } else if (isReviewTabActive) {
            scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = { 
                    Text(
                        "Home", 
                        color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { 
                    Text(
                        "Review", 
                        color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                }
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
                        typeFilter = typeFilter,
                        sortOption = sortOption,
                        categoryFilters = categoryFilters,
                        paymentMethodFilters = paymentMethodFilters,
                        availableCategories = availableCategories,
                        availablePaymentMethods = availablePaymentMethods,
                        onAddClick = onAddClick,
                        onItemClick = onItemClick,
                        onDeleteClick = onDeleteClick,
                        onDeleteSelectedClick = onDeleteSelectedClick,
                        onTypeFilterChange = onTypeFilterChange,
                        onSortChange = onSortChange,
                        onCategoryToggle = onCategoryToggle,
                        onPaymentMethodToggle = onPaymentMethodToggle,
                        onClearCategories = onClearCategories,
                        onClearPaymentMethods = onClearPaymentMethods,
                        onMenuClick = {},
                        onReviewSelectedClick = onReviewSelectedClick,
                        externalSelectedIds = homeSelectedIds,
                        onSelectionChange = { homeSelectedIds = it },
                        viewModel = viewModel
                    )
                }
                1 -> {
                    ReviewScreen(
                        reviewTransactions = reviewTransactions,
                        onItemClick = onItemClick,
                        onReviewAllClick = onReviewAllClick,
                        onReviewSelectedClick = onReviewSelectedClick,
                        externalSelectedIds = reviewSelectedIds,
                        onSelectionChange = { reviewSelectedIds = it }
                    )
                }
            }
        }
    }
}
