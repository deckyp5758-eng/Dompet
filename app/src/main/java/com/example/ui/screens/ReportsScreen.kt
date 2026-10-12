package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.viewmodel.DashboardSummary

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ReportsScreen(
    summary: DashboardSummary,
    incomes: List<IncomeEntity>,
    expenses: List<ExpenseEntity>,
    budgetCategories: List<BudgetCategoryEntity>,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedReportPeriod by remember { mutableIntStateOf(0) } // 0: Bulanan, 1: Mingguan, 2: Tahunan

    val totalSalary = summary.totalReceivedSalaryMonth
    val totalBonus = summary.totalReceivedBonusMonth
    val totalExpense = summary.totalActualExpensesMonth

    val savingRatio = if (summary.totalReceivedIncomeMonth > 0) {
        val saved = (summary.totalReceivedIncomeMonth - totalExpense).coerceAtLeast(0L)
        ((saved.toFloat() / summary.totalReceivedIncomeMonth) * 100f).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Laporan & Analisis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Evaluasi kebiasaan belanja, perbandingan gaji vs bonus, dan ekspor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Period Tab
        item {
            SecondaryTabRow(selectedTabIndex = selectedReportPeriod) {
                Tab(
                    selected = selectedReportPeriod == 0,
                    onClick = { selectedReportPeriod = 0 },
                    text = { Text("Bulanan") }
                )
                Tab(
                    selected = selectedReportPeriod == 1,
                    onClick = { selectedReportPeriod = 1 },
                    text = { Text("Mingguan") }
                )
                Tab(
                    selected = selectedReportPeriod == 2,
                    onClick = { selectedReportPeriod = 2 },
                    text = { Text("Tahunan") }
                )
            }
        }

        // Smart Financial Health Summary
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Ringkasan Kebiasaan Finansial",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Rasio Tabungan & Investasi: $savingRatio% dari pemasukan cair bulan ini.\n" +
                                "• Status Dana Darurat: Tahap ${summary.emergencyStage} (${CurrencyFormatter.formatRupiah(summary.emergencyFundBalance)}).\n" +
                                "• Rasio Belanja: ${if (totalSalary > 0) ((totalExpense.toFloat() / totalSalary) * 100).toInt() else 0}% dari gaji tetap telah dibelanjakan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Comparison: Gaji Tetap vs Bonus
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Perbandingan Gaji Tetap vs Bonus/Pemberian",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gaji Tetap Diterima", style = MaterialTheme.typography.labelSmall)
                            Text(
                                CurrencyFormatter.formatRupiah(totalSalary),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bonus Diterima", style = MaterialTheme.typography.labelSmall)
                            Text(
                                CurrencyFormatter.formatRupiah(totalBonus),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val totalInc = (totalSalary + totalBonus).coerceAtLeast(1L)
                    val salaryShare = (totalSalary.toFloat() / totalInc).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { salaryShare },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(salaryShare * 100).toInt()}% Gaji Tetap • ${((1 - salaryShare) * 100).toInt()}% Bonus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top Expense Categories Breakdown
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Distribusi Pengeluaran Per Kategori",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val groupedExpenses = expenses.groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }

                    if (groupedExpenses.isEmpty()) {
                        Text(
                            text = "Belum ada pengeluaran yang dicatat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        groupedExpenses.take(5).forEach { (catName, amount) ->
                            val pct = if (totalExpense > 0) ((amount.toFloat() / totalExpense) * 100).toInt() else 0
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(catName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${CurrencyFormatter.formatRupiah(amount)} ($pct%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.error,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Data to CSV
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ekspor Laporan (CSV)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ekspor riwayat pendapatan dan pengeluaran ke format CSV standar untuk dibuka di Excel atau Google Sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val csvContent = buildCsvExport(incomes, expenses)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Dompet Pintar CSV", csvContent)
                            clipboard.setPrimaryClip(clip)
                            onShowMessage("Data CSV berhasil disalin ke Clipboard! Anda dapat menempelkannya di aplikasi spreadsheet.")
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Laporan CSV")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

private fun buildCsvExport(incomes: List<IncomeEntity>, expenses: List<ExpenseEntity>): String {
    val sb = StringBuilder()
    sb.append("Tipe,Tanggal,Kategori/Sumber,Jenis Pendapatan,Metode/Status,Nominal (IDR),Catatan\n")

    incomes.forEach { inc ->
        sb.append("Pendapatan,")
        sb.append(CurrencyFormatter.formatDate(inc.dateMillis)).append(",")
        sb.append("\"${inc.source}\",")
        sb.append(inc.incomeType).append(",")
        sb.append(inc.status).append(",")
        sb.append(inc.amount).append(",")
        sb.append("\"${inc.notes}\"\n")
    }

    expenses.forEach { exp ->
        sb.append("Pengeluaran,")
        sb.append(CurrencyFormatter.formatDate(exp.dateMillis)).append(",")
        sb.append("\"${exp.category}\",")
        sb.append("-,")
        sb.append(exp.paymentMethod).append(",")
        sb.append(exp.amount).append(",")
        sb.append("\"${exp.notes}\"\n")
    }

    return sb.toString()
}
