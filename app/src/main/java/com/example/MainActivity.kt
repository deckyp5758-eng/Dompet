package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.logger.AppLogger
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddIncomeDialog
import com.example.ui.components.BonusAllocationDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.SalaryCustomizerDialog
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
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryEmerald
import com.example.viewmodel.FinanceViewModel
import java.util.Calendar

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
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
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

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(lastOperationError) {
        lastOperationError?.let { err ->
            val result = snackbarHostState.showSnackbar(
                message = "Perhatian: $err",
                actionLabel = "Lihat Detail",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                showDiagnosticsDialog = true
            }
            viewModel.clearOperationError()
        }
    }

    // Predictive back navigation: safely return to Home if on secondary tab
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
                    if (currentDestinationIndex == 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "DP",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Dompet Pintar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (summary.isPaydayToday) Color(0xFFD1FAE5) else Color(0xFFEEF2FF)
                                    ) {
                                        Text(
                                            text = if (summary.isPaydayToday) "🎉 Gajian!" else "H-${summary.daysUntilNextPayday} Gajian",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (summary.isPaydayToday) PrimaryEmerald else AccentIndigo,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyFormatter.formatDayMonth(Calendar.getInstance().timeInMillis) + " • " + CurrencyFormatter.formatMonthYear(Calendar.getInstance().timeInMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = destinations[currentDestinationIndex].title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    if (currentDestinationIndex != 0) {
                        IconButton(onClick = { currentDestinationIndex = 0 }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Beranda",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.testTag("top_diagnostics_button")
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
                                    contentDescription = "Diagnostik Sistem",
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

                    IconButton(
                        onClick = { showMoreMenuSheet = true },
                        modifier = Modifier.testTag("top_menu_button")
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Semua Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // 1. Beranda
                NavigationBarItem(
                    selected = currentDestinationIndex == 0,
                    onClick = { currentDestinationIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                    label = { Text("Beranda", fontSize = 11.sp, fontWeight = if (currentDestinationIndex == 0) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 2. Transaksi
                NavigationBarItem(
                    selected = currentDestinationIndex == 1,
                    onClick = { currentDestinationIndex = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transaksi") },
                    label = { Text("Transaksi", fontSize = 11.sp, fontWeight = if (currentDestinationIndex == 1) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 3. Anggaran
                NavigationBarItem(
                    selected = currentDestinationIndex == 2,
                    onClick = { currentDestinationIndex = 2 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Anggaran") },
                    label = { Text("Anggaran", fontSize = 11.sp, fontWeight = if (currentDestinationIndex == 2) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 4. Bonus
                NavigationBarItem(
                    selected = currentDestinationIndex == 3,
                    onClick = { currentDestinationIndex = 3 },
                    icon = {
                        if (summary.unallocatedBonusCount > 0) {
                            BadgedBox(badge = { Badge(containerColor = AccentAmber) { Text("${summary.unallocatedBonusCount}") } }) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = "Bonus")
                            }
                        } else {
                            Icon(Icons.Default.CardGiftcard, contentDescription = "Bonus")
                        }
                    },
                    label = { Text("Bonus", fontSize = 11.sp, fontWeight = if (currentDestinationIndex == 3) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 5. Lainnya (Darurat, Investasi, Laporan, Pengaturan)
                val isSecondarySelected = currentDestinationIndex >= 4
                NavigationBarItem(
                    selected = isSecondarySelected,
                    onClick = { showMoreMenuSheet = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Lainnya") },
                    label = {
                        Text(
                            text = if (isSecondarySelected) destinations[currentDestinationIndex].title else "Lainnya",
                            fontSize = 11.sp,
                            fontWeight = if (isSecondarySelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
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
                    onAddExpenseClick = {
                        expenseToEdit = null
                        showAddExpenseDialog = true
                    },
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
                    onAddExpenseClick = {
                        expenseToEdit = null
                        showAddExpenseDialog = true
                    },
                    onEditIncomeClick = { inc ->
                        incomeToEdit = inc
                        showAddIncomeDialog = true
                    },
                    onEditExpenseClick = { exp ->
                        expenseToEdit = exp
                        showAddExpenseDialog = true
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
                    onExportJson = { viewModel.exportDataAsJson() },
                    onRestoreJson = { jsonStr -> viewModel.restoreDataFromJson(jsonStr) },
                    onShowMessage = { msg -> viewModel.showMessage(msg) },
                    onOpenDiagnostics = { showDiagnosticsDialog = true }
                )
            }
        }
    }

    // Modern Modal Bottom Sheet untuk Semua Menu Navigasi & Fitur
    if (showMoreMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenuSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Semua Fitur Dompet Pintar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Akses cepat ke seluruh modul pengelolaan keuangan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature grid / cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(
                        icon = Icons.Default.Shield,
                        title = "Dana Darurat",
                        subtitle = CurrencyFormatter.formatRupiah(summary.emergencyFundBalance),
                        color = PrimaryEmerald,
                        bgColor = Color(0xFFECFDF5),
                        isSelected = currentDestinationIndex == 4,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentDestinationIndex = 4
                            showMoreMenuSheet = false
                        }
                    )

                    MenuTile(
                        icon = Icons.Default.TrendingUp,
                        title = "Investasi",
                        subtitle = CurrencyFormatter.formatRupiah(summary.investmentCurrentValue),
                        color = AccentIndigo,
                        bgColor = Color(0xFFEEF2FF),
                        isSelected = currentDestinationIndex == 5,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentDestinationIndex = 5
                            showMoreMenuSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(
                        icon = Icons.Default.Assessment,
                        title = "Laporan",
                        subtitle = "Arus kas & grafik",
                        color = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        isSelected = currentDestinationIndex == 6,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentDestinationIndex = 6
                            showMoreMenuSheet = false
                        }
                    )

                    MenuTile(
                        icon = Icons.Default.Settings,
                        title = "Pengaturan",
                        subtitle = "Profil, gaji, & data",
                        color = Color(0xFF475569),
                        bgColor = Color(0xFFF1F5F9),
                        isSelected = currentDestinationIndex == 7,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentDestinationIndex = 7
                            showMoreMenuSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(12.dp))

                // Item khusus diagnostik & pemecahan error
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (errorCount > 0) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showMoreMenuSheet = false
                            showDiagnosticsDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (errorCount > 0) Color(0xFFFFE4E6) else Color(0xFFDBEAFE),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = if (errorCount > 0) AccentRose else Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Diagnostik & Log Sistem",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (errorCount > 0) "$errorCount error tercatat" else "Sistem berjalan normal",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (errorCount > 0) AccentRose else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (errorCount > 0) {
                            Badge(containerColor = AccentRose) {
                                Text("$errorCount")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
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

    // Dialog Tambah / Edit Pendapatan
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

    // Dialog Tambah / Edit Pengeluaran
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            initialExpense = expenseToEdit,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { amount, category, method, notes ->
                if (expenseToEdit != null) {
                    viewModel.updateExpense(
                        expenseToEdit!!.copy(
                            amount = amount,
                            category = category,
                            paymentMethod = method,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addExpense(amount, category, method, notes)
                }
                showAddExpenseDialog = false
                expenseToEdit = null
            }
        )
    }

    // Dialog Alokasi Bonus
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

    // Dialog Konfirmasi Hapus Pendapatan
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

    // Dialog Konfirmasi Hapus Pengeluaran
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

    // Dialog Sesuaikan Gaji Bulanan
    if (showSalaryCustomizerDialog) {
        val currentSalary = profile?.monthlySalary ?: 2_700_000L
        SalaryCustomizerDialog(
            currentSalary = currentSalary,
            onDismiss = { showSalaryCustomizerDialog = false },
            onApply = { newSalary, preset ->
                viewModel.updateMonthlySalary(newSalary, preset)
                showSalaryCustomizerDialog = false
            }
        )
    }
}

@Composable
fun MenuTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    bgColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
