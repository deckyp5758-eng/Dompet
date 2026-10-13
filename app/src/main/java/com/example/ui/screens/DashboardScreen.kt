package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald
import com.example.viewmodel.DashboardSummary

sealed class RecentActivityItem(val timestamp: Long) {
    data class IncomeItem(val entity: IncomeEntity) : RecentActivityItem(entity.dateMillis)
    data class ExpenseItem(val entity: ExpenseEntity) : RecentActivityItem(entity.dateMillis)
}

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
    var hideBalance by remember { mutableStateOf(false) }
    val pendingSalary = incomes.firstOrNull { it.incomeType == "GAJI_TETAP" && it.status == "BELUM_DITERIMA" }

    val recentActivities: List<RecentActivityItem> = remember(incomes, expenses) {
        val incList = incomes.map { RecentActivityItem.IncomeItem(it) }
        val expList = expenses.map { RecentActivityItem.ExpenseItem(it) }
        (incList + expList).sortedByDescending { it.timestamp }.take(4)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Sleek Modern Hero Balance Card
        item {
            ModernHeroWalletCard(
                actualCashBalance = summary.actualCashBalance,
                totalNetWorth = summary.totalNetWorth,
                emergencyFund = summary.emergencyFundBalance,
                investmentValue = summary.investmentCurrentValue,
                hideBalance = hideBalance,
                onToggleHideBalance = { hideBalance = !hideBalance }
            )
        }

        // 2. Innovative 4-Action Quick Bar
        item {
            ModernQuickActionsBar(
                onAddIncome = onAddIncomeClick,
                onAddExpense = onAddExpenseClick,
                onCustomizeSalary = onCustomizeSalaryClick,
                onBonusAllocation = onBonusAllocationClick,
                unallocatedBonusCount = summary.unallocatedBonusCount
            )
        }

        // 3. Priority Action Banners (Pending Salary / Unallocated Bonus)
        if (pendingSalary != null) {
            item {
                PendingSalarySmartBanner(
                    salary = pendingSalary,
                    onCustomize = onCustomizeSalaryClick,
                    onMarkReceived = {
                        onMarkSalaryReceivedClick(pendingSalary.id, pendingSalary.source)
                    }
                )
            }
        }

        if (summary.unallocatedBonusCount > 0) {
            item {
                UnallocatedBonusSmartBanner(
                    bonusCount = summary.unallocatedBonusCount,
                    onClick = onBonusAllocationClick
                )
            }
        }

        // 4. Hitung Mundur Siklus & Kalender Gajian
        item {
            PaydayCountdownSmartCard(
                paydayDay = summary.paydayDayOfMonth,
                daysUntilPayday = summary.daysUntilNextPayday,
                nextPaydayMillis = summary.nextPaydayDateMillis,
                isPaydayToday = summary.isPaydayToday,
                cycleProgressPct = summary.payCycleProgressPct,
                dailySafeLimit = summary.safeDailyFlexibleLimit,
                onOpenSettings = { onNavigateToTab(7) }
            )
        }

        // 5. Financial Pulse: Batas Belanja Harian Fleksibel
        item {
            SafeDailyLimitModernCard(
                dailyLimit = summary.safeDailyFlexibleLimit,
                daysRemaining = summary.daysRemainingInMonth,
                daysUntilPayday = summary.daysUntilNextPayday,
                paydayDay = summary.paydayDayOfMonth,
                isWarning = summary.isDailyLimitWarning,
                actualCash = summary.actualCashBalance,
                onManageBudget = { onNavigateToTab(2) } // Anggaran tab
            )
        }

        // 5. Ringkasan Anggaran Bulanan
        item {
            BudgetSnapshotCard(
                totalSalary = summary.totalReceivedSalaryMonth,
                totalPlannedBudget = summary.totalPlannedSalaryMonth,
                totalActualExpense = summary.totalActualExpensesMonth,
                onViewDetails = { onNavigateToTab(2) }
            )
        }

        // 6. Transaksi Terbaru (Recent Activities)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktivitas Terkini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToTab(1) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (recentActivities.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada transaksi bulan ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentActivities) { item ->
                RecentActivityTile(item = item, onClick = { onNavigateToTab(1) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ModernHeroWalletCard(
    actualCashBalance: Long,
    totalNetWorth: Long,
    emergencyFund: Long,
    investmentValue: Long,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF1E293B)  // Slate 800
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                // Top row: Label + Eye toggle + Net Worth badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Saldo Kas Tersedia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onToggleHideBalance,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideBalance) "Tampilkan Saldo" else "Sembunyikan Saldo",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF334155).copy(alpha = 0.6f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aktif",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Large Balance
                Text(
                    text = if (hideBalance) "••••••••••" else CurrencyFormatter.formatRupiah(actualCashBalance),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f))

                Spacer(modifier = Modifier.height(14.dp))

                // Sub Portfolio Row: Dana Darurat, Investasi & Total Kekayaan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dana Darurat",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hideBalance) "••••••" else CurrencyFormatter.formatRupiah(emergencyFund),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34D399) // Mint
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Investasi",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hideBalance) "••••••" else CurrencyFormatter.formatRupiah(investmentValue),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF818CF8) // Indigo
                        )
                    }

                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Kekayaan",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hideBalance) "••••••" else CurrencyFormatter.formatRupiah(totalNetWorth),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24) // Gold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernQuickActionsBar(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onCustomizeSalary: () -> Unit,
    onBonusAllocation: () -> Unit,
    unallocatedBonusCount: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_action_grid")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Pemasukan",
                containerColor = Color(0xFFECFDF5),
                contentColor = PrimaryEmerald,
                testTag = "quick_income_btn",
                onClick = onAddIncome
            )

            QuickActionButton(
                icon = Icons.Default.Remove,
                label = "Pengeluaran",
                containerColor = Color(0xFFFFE4E6),
                contentColor = AccentRose,
                testTag = "quick_expense_btn",
                onClick = onAddExpense
            )

            QuickActionButton(
                icon = Icons.Default.Tune,
                label = "Atur Gaji",
                containerColor = Color(0xFFEEF2FF),
                contentColor = AccentIndigo,
                testTag = "quick_salary_btn",
                onClick = onCustomizeSalary
            )

            QuickActionButton(
                icon = Icons.Default.CardGiftcard,
                label = "Bagi Bonus",
                containerColor = Color(0xFFFEF3C7),
                contentColor = AccentAmber,
                badgeCount = unallocatedBonusCount,
                testTag = "quick_bonus_btn",
                onClick = onBonusAllocation
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    badgeCount: Int = 0,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(AccentRose),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PendingSalarySmartBanner(
    salary: IncomeEntity,
    onCustomize: () -> Unit,
    onMarkReceived: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), // Soft blue
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_salary_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFDBEAFE),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gaji Bulanan Terencana",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = "${CurrencyFormatter.formatRupiah(salary.amount)} • Belum cair",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF3B82F6)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onMarkReceived,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.testTag("mark_salary_received_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cairkan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun UnallocatedBonusSmartBanner(
    bonusCount: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("unallocated_bonus_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF3C7),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$bonusCount Penghasilan Tambahan Siap Dibagi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = "Alokasikan ke Dana Darurat, Investasi, & Pribadi",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF92400E)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AccentAmber,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PaydayCountdownSmartCard(
    paydayDay: Int,
    daysUntilPayday: Int,
    nextPaydayMillis: Long,
    isPaydayToday: Boolean,
    cycleProgressPct: Float,
    dailySafeLimit: Long,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaydayToday) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payday_countdown_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPaydayToday) Color(0xFF10B981) else Color(0xFFEEF2FF),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPaydayToday) Icons.Default.Celebration else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isPaydayToday) Color.White else AccentIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isPaydayToday) "Hari Ini Gajian! 🎉" else "Hitung Mundur Gajian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPaydayToday) "Siklus gajian aktif (Tgl $paydayDay)" else "Jadwal rutin setiap tanggal $paydayDay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPaydayToday) Color(0xFFD1FAE5) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenSettings)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ubah Tanggal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPaydayToday) PrimaryEmerald else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isPaydayToday) PrimaryEmerald else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Countdown Numbers & Next Date
            if (isPaydayToday) {
                Text(
                    text = "Gaji Anda Terjadwal Cair Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cek mutasi rekening Anda dan segera alokasikan dana sesuai porsi anggaran (kebutuhan, darurat, dan investasi).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "H-$daysUntilPayday",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentIndigo
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hari Lagi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Gajian berikutnya: ${CurrencyFormatter.formatDate(nextPaydayMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Batas Belanja Aman",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${CurrencyFormatter.formatRupiah(dailySafeLimit)}/hari",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cycle Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progres Siklus Bulanan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(cycleProgressPct * 100).toInt()}% terlalui",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentIndigo
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { cycleProgressPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentIndigo,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
fun SafeDailyLimitModernCard(
    dailyLimit: Long,
    daysRemaining: Int,
    daysUntilPayday: Int,
    paydayDay: Int,
    isWarning: Boolean,
    actualCash: Long,
    onManageBudget: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("safe_daily_limit_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isWarning) Color(0xFFFFE4E6) else Color(0xFFECFDF5),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (isWarning) AccentRose else PrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Batas Belanja Fleksibel Harian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (daysUntilPayday > 0) {
                                "Sisa $daysUntilPayday hari s.d. gajian (Tgl $paydayDay)"
                            } else {
                                "Sisa $daysRemaining hari bulan ini"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isWarning) Color(0xFFFFE4E6) else Color(0xFFECFDF5)
                ) {
                    Text(
                        text = if (isWarning) "Perhatian" else "Aman",
                        color = if (isWarning) AccentRose else PrimaryEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(dailyLimit),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) AccentRose else PrimaryEmerald
                )
                Text(
                    text = if (daysUntilPayday > 0) "per hari sampai gajian" else "per hari fleksibel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetSnapshotCard(
    totalSalary: Long,
    totalPlannedBudget: Long,
    totalActualExpense: Long,
    onViewDetails: () -> Unit
) {
    val usagePercent = if (totalPlannedBudget > 0) {
        ((totalActualExpense.toFloat() / totalPlannedBudget.toFloat()) * 100f).coerceIn(0f, 150f)
    } else 0f

    val remaining = (totalPlannedBudget - totalActualExpense).coerceAtLeast(0L)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pemanfaatan Anggaran",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sisa: ${CurrencyFormatter.formatRupiah(remaining)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (usagePercent > 90f) AccentRose else PrimaryEmerald
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (usagePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    usagePercent > 95f -> AccentRose
                    usagePercent > 80f -> AccentAmber
                    else -> PrimaryEmerald
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai: ${CurrencyFormatter.formatRupiah(totalActualExpense)} (${usagePercent.toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pagu: ${CurrencyFormatter.formatRupiah(totalPlannedBudget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentActivityTile(
    item: RecentActivityItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is RecentActivityItem.IncomeItem -> {
                    val isBonus = item.entity.incomeType == "BONUS"
                    Surface(
                        shape = CircleShape,
                        color = if (isBonus) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isBonus) Icons.Default.CardGiftcard else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isBonus) AccentAmber else PrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.entity.source,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${CurrencyFormatter.formatDate(item.entity.dateMillis)} • ${item.entity.incomeType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${CurrencyFormatter.formatRupiah(item.entity.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                        if (item.entity.status == "BELUM_DITERIMA") {
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                is RecentActivityItem.ExpenseItem -> {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFE4E6),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.entity.category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${CurrencyFormatter.formatDate(item.entity.dateMillis)} • ${item.entity.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "-${CurrencyFormatter.formatRupiah(item.entity.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentRose
                    )
                }
            }
        }
    }
}
