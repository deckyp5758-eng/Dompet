package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.logger.AppLogger
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddIncomeDialog
import com.example.ui.components.BonusAllocationDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.SystemDiagnosticsDialog
import com.example.ui.navigation.MainDestination
import com.example.ui.screens.BonusAllocationScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencyFundScreen
import com.example.ui.screens.InvestmentScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: FinanceViewModel) {
    var currentDestinationIndex by remember { mutableIntStateOf(0) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // Dialog States
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<IncomeEntity?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var bonusForAllocation by remember { mutableStateOf<IncomeEntity?>(null) }
    var showSalaryCustomizerDialog by remember { mutableStateOf(false) }

    var incomeToDelete by remember { mutableStateOf<IncomeEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Reactive states from ViewModel
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val budgetCategories by viewModel.budgetCategories.collectAsStateWithLifecycle()
    val emergencyFundEntries by viewModel.emergencyFundEntries.collectAsStateWithLifecycle()
    val investmentAssets by viewModel.investmentAssets.collectAsStateWithLifecycle()
    val investmentTx by viewModel.investmentTransactions.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()

    val errorCount by viewModel.errorCount.collectAsStateWithLifecycle()
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()
    val lastCrashReport by AppLogger.lastCrashReport.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val lastOperationError by viewModel.lastOperationError.collectAsStateWithLifecycle()

    // Handle normal messages
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Handle error notifications with quick action to open diagnostics
    LaunchedEffect(lastOperationError) {
        lastOperationError?.let { err ->
            val result = snackbarHostState.showSnackbar(
                message = "Error: $err",
                actionLabel = "Lihat Detail",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                showDiagnosticsDialog = true
            }
            viewModel.clearOperationError()
        }
    }

    // Android Predictive / System Back Navigation Handler:
    if (currentDestinationIndex != 0) {
        BackHandler {
            currentDestinationIndex = 0
        }
    }

    val destinations = MainDestination.entries

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentDestinationIndex == 0) "Dompet Pintar" else destinations[currentDestinationIndex].title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentDestinationIndex != 0) {
                            Text(
                                text = "Kas Aktual: ${CurrencyFormatter.formatRupiah(summary.actualCashBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Aplikasi Keuangan Pribadi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentDestinationIndex != 0) {
                        IconButton(onClick = { currentDestinationIndex = 0 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Beranda")
                        }
                    }
                },
                actions = {
                    // Tombol Cepat Diagnostik & Pendeteksi Error Sistem
                    IconButton(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.testTag("system_diagnostics_button")
                    ) {
                        if (errorCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$errorCount")
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.BugReport,
                                    contentDescription = "Diagnostik Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = "Diagnostik Sistem",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { showMoreMenuSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu Navigasi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentDestinationIndex == 0,
                    onClick = { currentDestinationIndex = 0 },
                    icon = { Icon(destinations[0].icon, contentDescription = "Beranda") },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = currentDestinationIndex == 1,
                    onClick = { currentDestinationIndex = 1 },
                    icon = { Icon(destinations[1].icon, contentDescription = "Transaksi") },
                    label = { Text("Transaksi") }
                )
                NavigationBarItem(
                    selected = currentDestinationIndex == 2,
                    onClick = { currentDestinationIndex = 2 },
                    icon = { Icon(destinations[2].icon, contentDescription = "Anggaran") },
                    label = { Text("Anggaran") }
                )
                NavigationBarItem(
                    selected = currentDestinationIndex == 3,
                    onClick = { currentDestinationIndex = 3 },
                    icon = {
                        if (summary.unallocatedBonusCount > 0) {
                            BadgedBox(badge = { Badge { Text("${summary.unallocatedBonusCount}") } }) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = "Bonus")
                            }
                        } else {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "Bonus")
                        }
                    },
                    label = { Text("Bonus") }
                )
                val isSecondarySelected = currentDestinationIndex >= 4
                NavigationBarItem(
                    selected = isSecondarySelected,
                    onClick = { showMoreMenuSheet = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Lainnya") },
                    label = {
                        Text(if (isSecondarySelected) destinations[currentDestinationIndex].title else "Lainnya")
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestinationIndex) {
                0 -> DashboardScreen(
                    summary = summary,
                    budgetCategories = budgetCategories,
                    expenses = expenses,
                    incomes = incomes,
                    onAddIncomeClick = {
                        incomeToEdit = null
                        showAddIncomeDialog = true
                    },
                    onAddExpenseClick = { showAddExpenseDialog = true },
                    onBonusAllocationClick = { currentDestinationIndex = 3 },
                    onCustomizeSalaryClick = { showSalaryCustomizerDialog = true },
                    onMarkSalaryReceivedClick = { id, source ->
                        viewModel.markIncomeReceived(id, source)
                    },
                    onNavigateToTab = { index -> currentDestinationIndex = index }
                )

                1 -> TransactionsScreen(
                    incomes = incomes,
                    expenses = expenses,
                    onAddIncomeClick = {
                        incomeToEdit = null
                        showAddIncomeDialog = true
                    },
                    onAddExpenseClick = { showAddExpenseDialog = true },
                    onEditIncomeClick = { inc ->
                        incomeToEdit = inc
                        showAddIncomeDialog = true
                    },
                    onDeleteIncomeClick = { inc -> incomeToDelete = inc },
                    onDeleteExpenseClick = { exp -> expenseToDelete = exp },
                    onToggleIncomeStatus = { id -> viewModel.toggleIncomeStatus(id) },
                    onAllocateBonusClick = { inc -> bonusForAllocation = inc }
                )

                2 -> BudgetScreen(
                    budgetCategories = budgetCategories,
                    expenses = expenses,
                    profile = profile ?: com.example.data.local.ProfileEntity(),
                    isSalaryReceivedThisMonth = summary.totalReceivedSalaryMonth > 0,
                    onCustomizeSalaryClick = { showSalaryCustomizerDialog = true },
                    onSaveBudgetCategories = { updatedList ->
                        viewModel.updateBudgetCategories(updatedList)
                    }
                )

                3 -> BonusAllocationScreen(
                    summary = summary,
                    bonusIncomes = incomes,
                    investmentAssets = investmentAssets,
                    onGetRecommendation = { bonusAmt ->
                        viewModel.getBonusAllocationRecommendation(bonusAmt)
                    },
                    onApplyAllocation = { id, darurat, invest, pribadi, assetId ->
                        viewModel.applyBonusAllocation(id, darurat, invest, pribadi, assetId)
                    }
                )

                4 -> EmergencyFundScreen(
                    summary = summary,
                    profile = profile ?: com.example.data.local.ProfileEntity(),
                    entries = emergencyFundEntries,
                    onAddEntry = { type, amount, source, notes ->
                        viewModel.addEmergencyFundEntry(type, amount, source, notes)
                    },
                    onDeleteEntry = { entry ->
                        viewModel.deleteEmergencyFundEntry(entry)
                    }
                )

                5 -> InvestmentScreen(
                    summary = summary,
                    profile = profile ?: com.example.data.local.ProfileEntity(),
                    assets = investmentAssets,
                    transactions = investmentTx,
                    onAddAsset = { name, type, capital, notes ->
                        viewModel.addInvestmentAsset(name, type, capital, notes)
                    },
                    onUpdateMarketValue = { assetId, newVal ->
                        viewModel.updateAssetMarketValue(assetId, newVal)
                    },
                    onAddDeposit = { assetId, amount, notes ->
                        viewModel.addInvestmentDeposit(assetId, amount, notes)
                    }
                )

                6 -> ReportsScreen(
                    summary = summary,
                    incomes = incomes,
                    expenses = expenses,
                    budgetCategories = budgetCategories,
                    onShowMessage = { msg -> viewModel.showMessage(msg) }
                )

                7 -> SettingsScreen(
                    profile = profile ?: com.example.data.local.ProfileEntity(),
                    onUpdateProfile = { newProf -> viewModel.updateProfile(newProf) },
                    onClearAllData = { viewModel.clearAllData() },
                    onExportJson = { viewModel.repository.exportDataAsJson() },
                    onShowMessage = { msg -> viewModel.showMessage(msg) },
                    onOpenDiagnostics = { showDiagnosticsDialog = true }
                )
            }
        }
    }

    // Modal Bottom Sheet for "Lainnya" / Full Menu
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Semua Menu Dompet Pintar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                destinations.forEachIndexed { index, dest ->
                    NavigationDrawerItem(
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.title) },
                        selected = currentDestinationIndex == index,
                        onClick = {
                            currentDestinationIndex = index
                            showMoreMenuSheet = false
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                // Item khusus diagnostik & log
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = if (errorCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    },
                    label = {
                        Row {
                            Text("Diagnostik & Log Sistem")
                            if (errorCount > 0) {
                                Text(" ($errorCount Error)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    selected = false,
                    onClick = {
                        showMoreMenuSheet = false
                        showDiagnosticsDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog Diagnostik & Log Error
    if (showDiagnosticsDialog) {
        SystemDiagnosticsDialog(
            logs = logEntries,
            errorCount = errorCount,
            lastCrashReport = lastCrashReport,
            onRunDiagnostic = { viewModel.runSystemDiagnostics() },
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    // Global Action Dialogs
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            initialIncome = incomeToEdit,
            onDismiss = {
                showAddIncomeDialog = false
                incomeToEdit = null
            },
            onSave = { amount, type, source, status, notes ->
                if (incomeToEdit != null) {
                    viewModel.updateIncome(
                        incomeToEdit!!.copy(
                            amount = amount,
                            incomeType = type,
                            source = source,
                            status = status,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addIncome(amount, type, source, status, notes)
                }
                showAddIncomeDialog = false
                incomeToEdit = null
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amount, category, method, notes ->
                viewModel.addExpense(amount, category, method, notes)
                showAddExpenseDialog = false
            }
        )
    }

    bonusForAllocation?.let { bonus ->
        val plan = viewModel.getBonusAllocationRecommendation(bonus.amount)
        BonusAllocationDialog(
            bonusIncome = bonus,
            plan = plan,
            availableAssets = investmentAssets,
            onDismiss = { bonusForAllocation = null },
            onConfirmAllocation = { darurat, invest, pribadi, assetId ->
                viewModel.applyBonusAllocation(bonus.id, darurat, invest, pribadi, assetId)
                bonusForAllocation = null
            }
        )
    }

    incomeToDelete?.let { inc ->
        DeleteConfirmDialog(
            itemDescription = "Pendapatan: ${inc.source} (${CurrencyFormatter.formatRupiah(inc.amount)})",
            onDismiss = { incomeToDelete = null },
            onConfirm = {
                viewModel.deleteIncome(inc)
                incomeToDelete = null
            }
        )
    }

    expenseToDelete?.let { exp ->
        DeleteConfirmDialog(
            itemDescription = "Pengeluaran: ${exp.category} (${CurrencyFormatter.formatRupiah(exp.amount)})",
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                viewModel.deleteExpense(exp)
                expenseToDelete = null
            }
        )
    }
}
