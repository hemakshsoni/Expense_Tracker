package com.example.expensetracker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: TransactionViewModel,
                  startDestination: Any) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Import Confirmation States
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedImportMonths by remember { mutableIntStateOf(6) }
    var showCustomRangePicker by remember { mutableStateOf(false) }

    val bottomBarRoutes =
        listOf(Routs.Home, Routs.Review, Routs.Analytics, Routs.More, Routs.Activity)
    val isMainDestination = currentDestination?.hierarchy?.any { dest ->
        bottomBarRoutes.any { route -> dest.hasRoute(route::class) }
    } == true

    val isHome = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Home>() } == true

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            if (isMainDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    // --- HELPER FUNCTION FOR NAVIGATION ---
                    // This ensures "Back" always returns to Home, and Home is never destroyed.
                    fun navigateToTab(route: Any) {
                        navController.navigate(route) {
                            // Pop up to the start destination (Home) to avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when re-selecting the same item
                            launchSingleTop = true
                            // Restore state when re-selecting a previously selected item
                            restoreState = true
                        }
                    }

                    NavigationBarItem(
                        selected = isHome,
                        onClick = { navigateToTab(Routs.Home) },
                        icon = { Icon(Icons.Default.Dashboard, "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Activity>() } == true,
                        onClick = { navigateToTab(Routs.Activity) },
                        icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, "Activity") },
                        label = { Text("Activity") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Review>() } == true,
                        onClick = { navigateToTab(Routs.Review) },
                        icon = { Icon(Icons.Default.RateReview, "Review") },
                        label = { Text("Review") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Analytics>() } == true,
                        onClick = { navigateToTab(Routs.Analytics) },
                        icon = { Icon(Icons.Default.BarChart, "Analytics") },
                        label = { Text("Analytics") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.More>() } == true,
                        onClick = { navigateToTab(Routs.More) },
                        icon = { Icon(Icons.Default.MoreHoriz, "More") },
                        label = { Text("More") }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // ... (Your Dialog Code remains exactly the same) ...
            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    icon = { Icon(Icons.Default.History, null) },
                    title = { Text("Import Transaction History") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select the timeframe to scan your SMS messages for bank transactions:")
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf(1, 3, 6).forEach { months ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedImportMonths = months }
                                ) {
                                    RadioButton(
                                        selected = selectedImportMonths == months,
                                        onClick = { selectedImportMonths = months })
                                    Text("Last $months Month${if (months > 1) "s" else ""}")
                                }
                            }
                            TextButton(
                                onClick = {
                                    showImportDialog = false
                                    showCustomRangePicker = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DateRange, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Custom Range")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showImportDialog = false
                                viewModel.importOldTransactions(context, selectedImportMonths)
                                scope.launch { snackbarHostState.showSnackbar("Importing last $selectedImportMonths months...") }
                            }
                        ) { Text("Import") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showCustomRangePicker) {
                val dateRangePickerState = rememberDateRangePickerState()
                DatePickerDialog(
                    onDismissRequest = { showCustomRangePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val start = dateRangePickerState.selectedStartDateMillis
                                val end = dateRangePickerState.selectedEndDateMillis
                                if (start != null && end != null) {
                                    showCustomRangePicker = false
                                    viewModel.importCustomRange(context, start, end)
                                    scope.launch { snackbarHostState.showSnackbar("Importing custom range...") }
                                }
                            }
                        ) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomRangePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        title = { Text("Select Import Dates", modifier = Modifier.padding(16.dp)) },
                        showModeToggle = false,
                        modifier = Modifier.height(400.dp)
                    )
                }
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                // ... (Transitions remain the same) ...
                enterTransition = {
                    if (targetState.destination.hasRoute<Routs.AddEdit>()) {
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn()
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(400)) + fadeIn()
                    }
                },
                exitTransition = {
                    if (initialState.destination.hasRoute<Routs.AddEdit>()) {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut()
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(400)) + fadeOut()
                    }
                },
                popEnterTransition = {
                    if (targetState.destination.hasRoute<Routs.AddEdit>()) {
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn()
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(400)) + fadeIn()
                    }
                },
                popExitTransition = {
                    if (initialState.destination.hasRoute<Routs.AddEdit>()) {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut()
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(400)) + fadeOut()
                    }
                }
            ) {
                composable<Routs.Setup> {
                    SetupScreen(
                        onSetupComplete = { name ->
                            viewModel.completeSetup(name) // Pass name to VM
                            navController.navigate(Routs.Home) {
                                popUpTo(Routs.Setup) { inclusive = true }
                            }
                        }
                    )
                }


                composable<Routs.Home> {
                    DashboardScreen(
                        viewModel = viewModel,
                        // Fix internal navigations to keep stack clean if needed
                        onNavigateToReview = { navController.navigate(Routs.Review) },
                        onNavigateToTransactions = { navController.navigate(Routs.Activity) },
                        onNavigateToDues = { navController.navigate(Routs.Dues) },
                        onNavigateToBills = { navController.navigate(Routs.RecurringPayments) }
                    )
                }

                // ... (Rest of composables remain exactly the same) ...

                composable<Routs.Review> {
                    val reviewTransactions by viewModel.reviewTransactions.collectAsState()
                    var reviewSelectedIds by remember { mutableStateOf(setOf<Int>()) }
                    // --- SHEET STATES ---
                    var showEditSheet by remember { mutableStateOf(false) }
                    var editingTransactionId by remember { mutableIntStateOf(-1) }

                    // If items are selected, Back button clears selection.
                    // If nothing selected, Back button goes Home (default behavior).
                    BackHandler(enabled = reviewSelectedIds.isNotEmpty()) {
                        reviewSelectedIds = emptySet()
                    }

                    ReviewScreen(
                        reviewTransactions = reviewTransactions,
                        onItemClick = { txn ->
                            editingTransactionId = txn.id
                            showEditSheet = true
                        },
                        onReviewAllClick = viewModel::reviewAllTransactions,
                        onReviewSelectedClick = { ids -> viewModel.reviewTransactions(ids) },
                        externalSelectedIds = reviewSelectedIds,
                        onSelectionChange = { reviewSelectedIds = it }
                    )

                    // --- THE SHEET ---
                    if (showEditSheet) {
                        AddEditTransactionSheet(
                            onDismiss = { showEditSheet = false },
                            viewModel = viewModel,
                            transactionId = editingTransactionId,
                            mode = 0 // Default to Transaction mode for editing
                        )
                    }
                }

                composable<Routs.Activity> {
                    val transactions by viewModel.filteredSortedTransactions.collectAsState(emptyList())
                    val typeFilter by viewModel.typeFilter.collectAsState()
                    val sortOption by viewModel.sortOption.collectAsState()
                    val categoryFilters by viewModel.categoryFilters.collectAsState()
                    val paymentMethodFilters by viewModel.paymentMethodFilters.collectAsState()
                    var homeSelectedIds by remember { mutableStateOf(setOf<Int>()) }
                    val allCategories by viewModel.allCategories.collectAsState()
                    val allPaymentMethods by viewModel.allPaymentMethods.collectAsState()
                    // If items are selected, Back button clears selection.

                    BackHandler(enabled = homeSelectedIds.isNotEmpty()) {
                        homeSelectedIds = emptySet()
                    }

                    TransactionScreen(
                        transactions = transactions,
                        typeFilter = typeFilter,
                        sortOption = sortOption,
                        categoryFilters = categoryFilters,
                        paymentMethodFilters = paymentMethodFilters,
                        availableCategories = allCategories.map { it.name },
                        availablePaymentMethods = allPaymentMethods.map { it.name },
                        onDeleteClick = { txn ->
                            viewModel.deleteTransaction(txn)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Transaction deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.upsertTransaction(txn)
                                }
                            }
                        },
                        onDeleteSelectedClick = { ids ->
                            val selectedTxns = transactions.filter { it.id in ids }
                            viewModel.deleteTransactions(selectedTxns)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "${selectedTxns.size} transactions deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    selectedTxns.forEach { viewModel.upsertTransaction(it) }
                                }
                            }
                        },
                        onTypeFilterChange = viewModel::setTypeFilter,
                        onSortChange = viewModel::setSortOption,
                        onCategoryToggle = viewModel::toggleCategoryFilter,
                        onPaymentMethodToggle = viewModel::togglePaymentMethodFilter,
                        onClearCategories = viewModel::clearCategoryFilters,
                        onClearPaymentMethods = viewModel::clearPaymentMethodFilters,
                        onReviewSelectedClick = { ids -> viewModel.reviewTransactions(ids) },
                        externalSelectedIds = homeSelectedIds,
                        onSelectionChange = { homeSelectedIds = it },
                        viewModel = viewModel
                    )
                }

                composable<Routs.Analytics> {
                    AnalyticsScreen(viewModel = viewModel)
                }

                composable<Routs.More> {
                    MoreScreen(
                        onNavigateToCategories = { navController.navigate(Routs.Categories) },
                        onNavigateToPaymentMethods = { navController.navigate(Routs.PaymentMethods) },
                        onNavigateToDues = { navController.navigate(Routs.Dues) },
                        onNavigateToRecurring = { navController.navigate(Routs.RecurringPayments) },
                        onNavigateToAutoLearned = { navController.navigate(Routs.AutoLearned) },
                        onImportSms = { showImportDialog = true }
                    )
                }

                composable<Routs.Categories> {
                    CategoriesScreen(
                        viewModel = viewModel,
                        onDeleteCategory = { category ->
                            viewModel.deleteCategory(category)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Category deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.upsertCategory(category)
                                }
                            }
                        }
                    )
                }

                composable<Routs.PaymentMethods> {
                    PaymentMethodsScreen(
                        viewModel = viewModel,
                        onDeleteMethod = { method ->
                            viewModel.deletePaymentMethod(method)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Payment method deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.upsertPaymentMethod(method)
                                }
                            }
                        }
                    )
                }

                composable<Routs.Dues> {
                    DuesScreen(
                        viewModel = viewModel,
                        onDeleteDue = { due ->
                            viewModel.deleteDue(due)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Due deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.upsertDue(due)
                                }
                            }
                        }
                    )
                }

                composable<Routs.RecurringPayments> {
                    RecurringPaymentsScreen(viewModel = viewModel)
                }

                composable<Routs.AutoLearned> {
                    AutoLearnedScreen(viewModel = viewModel)
                }
            }
        }
    }
}