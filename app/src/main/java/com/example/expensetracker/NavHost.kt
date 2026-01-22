package com.example.expensetracker

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: TransactionViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Import Confirmation States
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedImportMonths by remember { mutableIntStateOf(6) }
    var showCustomRangePicker by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine Title and Navigation Icon based on current route
    val title = when {
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.Home>() } == true -> "Expense Tracker"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.Analytics>() } == true -> "Monthly Analytics"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.PaymentMethods>() } == true -> "Payment Methods"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.Categories>() } == true -> "Categories"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.AutoLearned>() } == true -> "Auto-Learned"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.RecurringPayments>() } == true -> "Recurring Payments"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.Dues>() } == true -> "Dues"
        currentDestination?.hierarchy?.any { it.hasRoute<Routs.AddEdit>() } == true -> "Transaction"
        else -> "Expense Tracker"
    }

    val isHome = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Home>() } == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isHome,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Expense Tracker",
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                NavigationDrawerItem(
                    label = { Text("Analytics") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Analytics>() } == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routs.Analytics)
                    },
                    icon = { Icon(Icons.Default.Analytics, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Payment Methods") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.PaymentMethods>() } == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routs.PaymentMethods)
                    },
                    icon = { Icon(Icons.Default.Payments, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Categories") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Categories>() } == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routs.Categories)
                    },
                    icon = { Icon(Icons.Default.Category, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Recurring Payments") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.RecurringPayments>() } == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routs.RecurringPayments)
                    },
                    icon = { Icon(Icons.Default.Repeat, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Dues") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.Dues>() } == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routs.Dues)
                    },
                    icon = { Icon(Icons.Default.People, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Auto-Learned") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<Routs.AutoLearned>() } == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routs.AutoLearned)
                    },
                    icon = { Icon(Icons.Default.History, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                NavigationDrawerItem(
                    label = { Text("Import SMS History") },
                    selected = false,
                    onClick = {
                        scope.launch { 
                            drawerState.close() 
                            showImportDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Sms, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
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
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    TopAppBar(
                        title = { Text(title, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            if (isHome) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            } else {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isHome,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routs.AddEdit()) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Surface(
                modifier = Modifier.padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                // Multi-Option Import Dialog
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
                                        modifier = Modifier.fillMaxWidth().clickable { selectedImportMonths = months }
                                    ) {
                                        RadioButton(selected = selectedImportMonths == months, onClick = { selectedImportMonths = months })
                                        Text("Last $months Month${if(months>1) "s" else ""}")
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

                // Custom Range Picker Dialog
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
                    startDestination = Routs.Home,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    }
                ) {
                    composable<Routs.Home> {
                        val transactions by viewModel.filteredSortedTransactions.collectAsState(emptyList())
                        val typeFilter by viewModel.typeFilter.collectAsState()
                        val sortOption by viewModel.sortOption.collectAsState()
                        val categoryFilters by viewModel.categoryFilters.collectAsState()
                        val paymentMethodFilters by viewModel.paymentMethodFilters.collectAsState()
                        val reviewTransactions by viewModel.reviewTransactions.collectAsState()
                        
                        val allCategories by viewModel.allCategories.collectAsState()
                        val allPaymentMethods by viewModel.allPaymentMethods.collectAsState()

                        HomePagerScreen(
                            transactions = transactions,
                            reviewTransactions = reviewTransactions,
                            totalBalance = 0.0,
                            onAddClick = { navController.navigate(Routs.AddEdit()) },
                            onItemClick = { txn ->
                                navController.navigate(Routs.AddEdit(transactionId = txn.id))
                            },
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
                            typeFilter = typeFilter,
                            sortOption = sortOption,
                            categoryFilters = categoryFilters,
                            paymentMethodFilters = paymentMethodFilters,
                            availableCategories = allCategories.map { it.name },
                            availablePaymentMethods = allPaymentMethods.map { it.name },
                            onTypeFilterChange = viewModel::setTypeFilter,
                            onSortChange = viewModel::setSortOption,
                            onCategoryToggle = viewModel::toggleCategoryFilter,
                            onPaymentMethodToggle = viewModel::togglePaymentMethodFilter,
                            onClearCategories = viewModel::clearCategoryFilters,
                            onClearPaymentMethods = viewModel::clearPaymentMethodFilters,
                            onReviewAllClick = viewModel::reviewAllTransactions,
                            onReviewSelectedClick = { ids -> viewModel.reviewTransactions(ids) },
                            viewModel = viewModel
                        )
                    }

                    composable<Routs.AddEdit> { backStackEntry ->
                        val args = backStackEntry.toRoute<Routs.AddEdit>()
                        AddEditTransactionScreen(
                            navController = navController,
                            viewModel = viewModel,
                            transactionId = args.transactionId
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

                    composable<Routs.Analytics> {
                        AnalyticsScreen(viewModel = viewModel)
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

                    composable<Routs.AutoLearned> {
                        AutoLearnedScreen(viewModel = viewModel)
                    }

                    composable<Routs.RecurringPayments> {
                        RecurringPaymentsScreen(viewModel = viewModel)
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
                }
            }
        }
    }
}