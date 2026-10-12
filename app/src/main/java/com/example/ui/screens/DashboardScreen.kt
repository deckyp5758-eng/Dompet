package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.components.DonutExpenseChart
import com.example.ui.components.IncomeComparisonBarChart
import com.example.viewmodel.DashboardSummary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    budgetCategories: List<BudgetCategoryEntity>,
    expenses: List<ExpenseEntity>,
    incomes: List<IncomeEntity>,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onBonusAllocationClick: () -> Unit,
    onCustomizeSalaryClick: () -> Unit,
    onMarkSalaryReceivedClick: (incomeId: Long, source: String) -> Unit,
    onNavigateToTab: (Int) -> Unit
) {
    val pendingSalary = incomes.firstOrNull { it.incomeType == "GAJI_TETAP" && it.status == "BELUM_DITERIMA" }
    var selectedDashboardTab by remember { mutableIntStateOf(0) } // 0: Ringkasan, 1: Grafik & Analisis, 2: Pos Anggaran

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
            HeaderSection(
                plannedSalary = summary.totalPlannedSalaryMonth,
                onCustomizeSalary = onCustomizeSalaryClick
            )
        }

        // Hero Balance & Financial Health Card
        item {
            HeroBalanceCard(
                actualCashBalance = summary.actualCashBalance,
                totalNetWorth = summary.totalNetWorth,
                emergencyFund = summary.emergencyFundBalance,
                investmentValue = summary.investmentCurrentValue
            )
        }

        // Quick Action Buttons
        item {
            QuickActionsRow(
                onAddIncome = onAddIncomeClick,
                onAddExpense = onAddExpenseClick,
                onBonusAllocation = onBonusAllocationClick,
                onCustomizeSalary = onCustomizeSalaryClick,
                unallocatedBonusCount = summary.unallocatedBonusCount
            )
        }

        // Pending Salary Banner (Requirement 1 & 2 - Gaji Terencana Belum Diterima)
        if (pendingSalary != null) {
            item {
                PendingSalaryCard(
                    salaryIncome = pendingSalary,
                    onCustomizeSalary = onCustomizeSalaryClick,
                    onMarkReceived = {
                        onMarkSalaryReceivedClick(pendingSalary.id, pendingSalary.source)
                    }
                )
            }
        }

        // Unallocated Bonus Notification Banner (Requirement 4)
        if (summary.unallocatedBonusCount > 0) {
            item {
                UnallocatedBonusBanner(
                    count = summary.unallocatedBonusCount,
                    onClick = onBonusAllocationClick
                )
            }
        }

        // Safe Daily Flexible Spending Bar (Requirement 8)
        item {
            SafeDailyLimitCard(
                dailyLimit = summary.safeDailyFlexibleLimit,
                daysRemaining = summary.daysRemainingInMonth,
                isWarning = summary.isDailyLimitWarning
            )
        }

        // Interactive Segmented Tabs: Organizes content compactly to eliminate vertical scrolling
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                SecondaryTabRow(
                    selectedTabIndex = selectedDashboardTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Tab(
                        selected = selectedDashboardTab == 0,
                        onClick = { selectedDashboardTab = 0 },
                        text = { Text("Ringkasan", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedDashboardTab == 1,
                        onClick = { selectedDashboardTab = 1 },
                        text = { Text("Grafik & Analisis", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedDashboardTab == 2,
                        onClick = { selectedDashboardTab = 2 },
                        text = { Text("Pos Anggaran", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Dynamic Tab Content
        when (selectedDashboardTab) {
            0 -> {
                // TAB 0: Ringkasan Arus Kas & Pilar Finansial
                item {
                    MonthlyHighlightsSection(summary = summary)
                }

                item {
                    PillarsOverviewSection(
                        summary = summary,
                        onEmergencyClick = { onNavigateToTab(4) },
                        onInvestmentClick = { onNavigateToTab(5) }
                    )
                }

                item {
                    DirectShortcutRow(onNavigateToTab = onNavigateToTab)
                }
            }
            1 -> {
                // TAB 1: Visual Charts (Pengeluaran & Pemasukan)
                item {
                    DonutExpenseChart(expenses = expenses)
                }

                item {
                    IncomeComparisonBarChart(
                        salaryReceived = summary.totalReceivedSalaryMonth,
                        bonusReceived = summary.totalReceivedBonusMonth,
                        otherReceived = summary.totalReceivedOtherMonth
                    )
                }
            }
            2 -> {
                // TAB 2: Pos Anggaran & Realisasi
                item {
                    BudgetRealizationPreview(
                        budgetCategories = budgetCategories,
                        expenses = expenses,
                        plannedSalary = summary.totalPlannedSalaryMonth,
                        onCustomizeSalary = onCustomizeSalaryClick,
                        onViewAllBudgetClick = { onNavigateToTab(2) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderSection(
    plannedSalary: Long,
    onCustomizeSalary: () -> Unit
) {
    val cal = Calendar.getInstance()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Dompet Pintar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = CurrencyFormatter.formatMonthYear(cal.timeInMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Salary & Preset Config Pill
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier
                .clickable(onClick = onCustomizeSalary)
                .testTag("dashboard_salary_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Sesuaikan Gaji",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Gaji: ${CurrencyFormatter.formatRupiah(plannedSalary)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    actualCashBalance: Long,
    totalNetWorth: Long,
    emergencyFund: Long,
    investmentValue: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F6E49),
                            Color(0xFF07482F)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saldo Kas Aktual (Tersedia)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Siap Pakai",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = CurrencyFormatter.formatRupiah(actualCashBalance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown Strip
                Surface(
                    color = Color.Black.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kekayaan Bersih",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalNetWorth),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Darurat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(emergencyFund),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF68DCA0)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Investasi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(investmentValue),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFD166)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onBonusAllocation: () -> Unit,
    onCustomizeSalary: () -> Unit,
    unallocatedBonusCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onAddIncome,
            modifier = Modifier
                .weight(1f)
                .testTag("action_add_income"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Masuk", style = MaterialTheme.typography.labelSmall)
        }

        Button(
            onClick = onAddExpense,
            modifier = Modifier
                .weight(1f)
                .testTag("action_add_expense"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Keluar", style = MaterialTheme.typography.labelSmall)
        }

        FilledTonalButton(
            onClick = onBonusAllocation,
            modifier = Modifier
                .weight(1f)
                .testTag("action_bonus_allocation"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(if (unallocatedBonusCount > 0) "Bonus ($unallocatedBonusCount)" else "Alokasi", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
            onClick = onCustomizeSalary,
            modifier = Modifier
                .weight(1f)
                .testTag("action_customize_salary"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Atur Gaji", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DirectShortcutRow(onNavigateToTab: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            AssistChip(
                onClick = { onNavigateToTab(4) },
                label = { Text("Dana Darurat") },
                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = RoundedCornerShape(100.dp)
            )
        }
        item {
            AssistChip(
                onClick = { onNavigateToTab(5) },
                label = { Text("Investasi") },
                leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = RoundedCornerShape(100.dp)
            )
        }
        item {
            AssistChip(
                onClick = { onNavigateToTab(6) },
                label = { Text("Laporan & CSV") },
                leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = RoundedCornerShape(100.dp)
            )
        }
        item {
            AssistChip(
                onClick = { onNavigateToTab(7) },
                label = { Text("Pengaturan Profil") },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = RoundedCornerShape(100.dp)
            )
        }
    }
}

@Composable
private fun PendingSalaryCard(
    salaryIncome: IncomeEntity,
    onCustomizeSalary: () -> Unit,
    onMarkReceived: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFFEF3C7)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFD97706),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Gaji Terencana: ${CurrencyFormatter.formatRupiah(salaryIncome.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F)
                    )
                    Text(
                        text = "Belum masuk rekening • Klik jika sudah cair",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onCustomizeSalary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ubah", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onMarkReceived,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    modifier = Modifier.testTag("mark_salary_received_button")
                ) {
                    Text("Diterima", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun UnallocatedBonusBanner(
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ada $count Bonus Belum Dialokasikan!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F)
                    )
                    Text(
                        text = "Terapkan aturan pembagian dana darurat & investasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFB45309)
            )
        }
    }
}

@Composable
private fun SafeDailyLimitCard(
    dailyLimit: Long,
    daysRemaining: Int,
    isWarning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Batas Belanja Harian Fleksibel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isWarning) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = if (isWarning) "Peringatan: Dana fleksibel menipis atau tagihan pokok belum aman!"
                    else "Tersedia aman untuk $daysRemaining hari tersisa",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatRupiah(dailyLimit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/ hari",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyHighlightsSection(summary: DashboardSummary) {
    Column {
        Text(
            text = "Arus Keuangan Bulan Ini",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HighlightMetricCard(
                title = "Gaji Diterima",
                value = CurrencyFormatter.formatRupiah(summary.totalReceivedSalaryMonth),
                subtitle = "Dari ${CurrencyFormatter.formatRupiah(summary.totalPlannedSalaryMonth)} terencana",
                icon = Icons.Default.Payments,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            HighlightMetricCard(
                title = "Bonus Diterima",
                value = CurrencyFormatter.formatRupiah(summary.totalReceivedBonusMonth),
                subtitle = "Pendapatan tambahan",
                icon = Icons.Default.CardGiftcard,
                iconColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HighlightMetricCard(
                title = "Pengeluaran Aktual",
                value = CurrencyFormatter.formatRupiah(summary.totalActualExpensesMonth),
                subtitle = "Total belanja keluar",
                icon = Icons.Default.ArrowUpward,
                iconColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )

            HighlightMetricCard(
                title = "Total Kas Masuk",
                value = CurrencyFormatter.formatRupiah(summary.totalReceivedIncomeMonth),
                subtitle = "Semua yang sudah cair",
                icon = Icons.Default.ArrowDownward,
                iconColor = Color(0xFF059669),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HighlightMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PillarsOverviewSection(
    summary: DashboardSummary,
    onEmergencyClick: () -> Unit,
    onInvestmentClick: () -> Unit
) {
    Column {
        Text(
            text = "Pilar Perlindungan & Pertumbuhan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Emergency Fund Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEmergencyClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dana Darurat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Tahap ${summary.emergencyStage}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(summary.emergencyFundBalance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Target Awal: ${CurrencyFormatter.formatRupiah(summary.emergencyTargetInitial)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { summary.emergencyProgressInitialPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(summary.emergencyProgressInitialPct * 100).toInt()}% menuju target awal Rp1.000.000",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Investment Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onInvestmentClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Investasi Jangka Panjang",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Kelola Portofolio >",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = CurrencyFormatter.formatRupiah(summary.investmentCurrentValue),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "Nilai Pasar Terkini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatRupiah(summary.investmentTotalCapital),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Modal Disetor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetRealizationPreview(
    budgetCategories: List<BudgetCategoryEntity>,
    expenses: List<ExpenseEntity>,
    plannedSalary: Long,
    onCustomizeSalary: () -> Unit,
    onViewAllBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Realisasi Pos Anggaran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Alokasi 100% Gaji: ${CurrencyFormatter.formatRupiah(plannedSalary)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    OutlinedButton(
                        onClick = onCustomizeSalary,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Atur Gaji/Model", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            budgetCategories.forEach { cat ->
                val spent = expenses.filter { it.category.contains(cat.name, ignoreCase = true) }.sumOf { it.amount }
                val progress = if (cat.plannedAmount > 0) (spent.toFloat() / cat.plannedAmount).coerceIn(0f, 1f) else 0f
                val isOver = spent > cat.plannedAmount

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${cat.name} (${String.format("%.0f", cat.percentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${CurrencyFormatter.formatRupiah(spent)} / ${CurrencyFormatter.formatRupiah(cat.plannedAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onViewAllBudgetClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Buka Pengelolaan Lengkap Anggaran >", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
