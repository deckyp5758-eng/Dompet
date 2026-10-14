package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.components.DonutExpenseChart
import com.example.ui.components.IncomeComparisonBarChart
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald
import com.example.viewmodel.DashboardSummary
import java.util.Calendar

@Composable
fun ReportsScreen(
    summary: DashboardSummary,
    incomes: List<IncomeEntity>,
    expenses: List<ExpenseEntity>,
    budgetCategories: List<BudgetCategoryEntity>,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedReportPeriod by remember { mutableIntStateOf(0) } // 0: Bulan Ini, 1: 3 Bulan Terakhir, 2: Tahun Ini, 3: Semua Waktu

    val now = remember { Calendar.getInstance() }
    val currentMonth = now.get(Calendar.MONTH)
    val currentYear = now.get(Calendar.YEAR)
    val ninetyDaysAgo = remember { System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000L }

    val filteredIncomes = remember(incomes, selectedReportPeriod) {
        incomes.filter { inc ->
            if (inc.status != "DITERIMA") return@filter false
            when (selectedReportPeriod) {
                0 -> {
                    val c = Calendar.getInstance().apply { timeInMillis = inc.dateMillis }
                    c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                }
                1 -> inc.dateMillis >= ninetyDaysAgo
                2 -> {
                    val c = Calendar.getInstance().apply { timeInMillis = inc.dateMillis }
                    c.get(Calendar.YEAR) == currentYear
                }
                else -> true
            }
        }
    }

    val filteredExpenses = remember(expenses, selectedReportPeriod) {
        expenses.filter { exp ->
            when (selectedReportPeriod) {
                0 -> {
                    val c = Calendar.getInstance().apply { timeInMillis = exp.dateMillis }
                    c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                }
                1 -> exp.dateMillis >= ninetyDaysAgo
                2 -> {
                    val c = Calendar.getInstance().apply { timeInMillis = exp.dateMillis }
                    c.get(Calendar.YEAR) == currentYear
                }
                else -> true
            }
        }
    }

    val totalIncome = remember(filteredIncomes) { filteredIncomes.sumOf { it.amount } }
    val totalSalary = remember(filteredIncomes) { filteredIncomes.filter { it.incomeType == "GAJI_TETAP" }.sumOf { it.amount } }
    val totalBonus = remember(filteredIncomes) { filteredIncomes.filter { it.incomeType == "BONUS" }.sumOf { it.amount } }
    val totalOther = remember(filteredIncomes) { filteredIncomes.filter { it.incomeType != "GAJI_TETAP" && it.incomeType != "BONUS" }.sumOf { it.amount } }
    val totalExpense = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

    val savingRatio = if (totalIncome > 0) {
        val saved = (totalIncome - totalExpense).coerceAtLeast(0L)
        ((saved.toFloat() / totalIncome.toFloat()) * 100f).toInt()
    } else 0

    val periodLabel = when (selectedReportPeriod) {
        0 -> "Bulan Ini"
        1 -> "3 Bulan Terakhir"
        2 -> "Tahun Ini"
        else -> "Semua Waktu"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Laporan & Analisis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Evaluasi arus kas, rasio tabungan, dan distribusi pengeluaran",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Periode Selector Chips
        item {
            val periods = listOf("Bulan Ini", "3 Bulan Terakhir", "Tahun Ini", "Semua Waktu")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(periods.size) { idx ->
                    FilterChip(
                        selected = selectedReportPeriod == idx,
                        onClick = { selectedReportPeriod = idx },
                        label = {
                            Text(
                                text = periods[idx],
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedReportPeriod == idx) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        // Saving Ratio Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rasio Tabungan ($periodLabel)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (savingRatio >= 20) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (savingRatio >= 20) "Sangat Sehat" else "Bisa Ditingkatkan",
                                color = if (savingRatio >= 20) PrimaryEmerald else AccentAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$savingRatio%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "dari total pendapatan tersimpan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { (savingRatio / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryEmerald,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Cashflow Summary
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Arus Kas ($periodLabel)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pemasukan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.formatRupiah(totalIncome), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pengeluaran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.formatRupiah(totalExpense), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentRose)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val net = totalIncome - totalExpense
                            Text("Surplus / Sisa", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.formatRupiah(net),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) PrimaryEmerald else AccentRose
                            )
                        }
                    }
                }
            }
        }

        // Charts
        item {
            DonutExpenseChart(expenses = filteredExpenses)
        }

        item {
            IncomeComparisonBarChart(
                salaryReceived = totalSalary,
                bonusReceived = totalBonus,
                otherReceived = totalOther
            )
        }

        // Quick Export / Copy
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Salin Ringkasan Laporan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Salin ikhtisar keuangan periode $periodLabel ke clipboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedButton(
                        onClick = {
                            val summaryText = buildString {
                                appendLine("=== LAPORAN KEUANGAN DOMPET PINTAR ===")
                                appendLine("Periode: $periodLabel")
                                appendLine("Total Pemasukan: ${CurrencyFormatter.formatRupiah(totalIncome)}")
                                appendLine("Total Pengeluaran: ${CurrencyFormatter.formatRupiah(totalExpense)}")
                                appendLine("Surplus / Sisa: ${CurrencyFormatter.formatRupiah(totalIncome - totalExpense)}")
                                appendLine("Rasio Tabungan: $savingRatio%")
                                appendLine("Dana Darurat Terkumpul: ${CurrencyFormatter.formatRupiah(summary.emergencyFundBalance)}")
                                appendLine("Investasi Saat Ini: ${CurrencyFormatter.formatRupiah(summary.investmentCurrentValue)}")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Laporan Keuangan", summaryText))
                            onShowMessage("Ringkasan keuangan ($periodLabel) berhasil disalin ke clipboard!")
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
