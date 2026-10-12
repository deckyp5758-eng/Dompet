package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.CurrencyFormatter

@Composable
fun BudgetScreen(
    budgetCategories: List<BudgetCategoryEntity>,
    expenses: List<ExpenseEntity>,
    profile: ProfileEntity,
    isSalaryReceivedThisMonth: Boolean,
    onCustomizeSalaryClick: () -> Unit,
    onSaveBudgetCategories: (List<BudgetCategoryEntity>) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val monthlySalary = profile.monthlySalary
    val totalBudgetPercent = budgetCategories.sumOf { it.percentage }
    val totalPlannedNominal = budgetCategories.sumOf { it.plannedAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anggaran Bulanan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Alokasi 100% dari Gaji Tetap (${CurrencyFormatter.formatRupiah(monthlySalary)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCustomizeSalaryClick,
                        modifier = Modifier.testTag("customize_salary_budget_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atur Gaji")
                    }
                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_budget_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ubah Pos")
                    }
                }
            }
        }

        // Salary Status Banner (Requirement 3)
        item {
            if (!isSalaryReceivedThisMonth) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Status: Anggaran Terencana (Gaji Belum Cair)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Dana belum tersedia di kas aktual hingga gaji ditandai sudah diterima di Beranda atau Transaksi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gaji Bulan Ini Sudah Diterima",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Dana telah tersedia di kas aktual untuk dialokasikan sesuai pos anggaran.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Total Summary Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Anggaran Terencana",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(totalPlannedNominal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = if (Math.abs(totalBudgetPercent - 100.0) < 0.1) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "${String.format("%.0f", totalBudgetPercent)}% Gaji",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (Math.abs(totalBudgetPercent - 100.0) < 0.1) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Rincian 6 Pos Anggaran Utama:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // List of Budget Items with Enhanced Progress
        items(budgetCategories) { cat ->
            val spent = expenses.filter { it.category.contains(cat.name, ignoreCase = true) }.sumOf { it.amount }
            val remaining = cat.plannedAmount - spent
            val progress = if (cat.plannedAmount > 0) (spent.toFloat() / cat.plannedAmount).coerceIn(0f, 1f) else 0f
            val isOver = remaining < 0
            val isWarning = progress > 0.75f && !isOver

            val progressColor = when {
                isOver -> MaterialTheme.colorScheme.error
                isWarning -> Color(0xFFD97706)
                else -> MaterialTheme.colorScheme.primary
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alokasi ${String.format("%.0f", cat.percentage)}% = ${CurrencyFormatter.formatRupiah(cat.plannedAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isOver) "Lebih ${CurrencyFormatter.formatRupiah(-remaining)}"
                                else "Sisa ${CurrencyFormatter.formatRupiah(remaining)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Terpakai: ${CurrencyFormatter.formatRupiah(spent)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditDialog) {
        BudgetEditorDialog(
            salary = monthlySalary,
            initialCategories = budgetCategories,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                onSaveBudgetCategories(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun BudgetEditorDialog(
    salary: Long,
    initialCategories: List<BudgetCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (List<BudgetCategoryEntity>) -> Unit
) {
    val editableList = remember {
        initialCategories.map {
            mutableStateOf(String.format("%.0f", it.percentage))
        }
    }

    val totalPct = editableList.sumOf { it.value.toDoubleOrNull() ?: 0.0 }
    val isValid = Math.abs(totalPct - 100.0) < 0.1
    val diffPct = 100.0 - totalPct

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Persentase Anggaran Gaji") },
        text = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Gaji Tetap: ${CurrencyFormatter.formatRupiah(salary)}. Total harus tepat 100%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                initialCategories.forEachIndexed { index, cat ->
                    val pctState = editableList[index]
                    val pct = pctState.value.toDoubleOrNull() ?: 0.0
                    val planned = ((salary * pct) / 100.0).toLong()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(planned),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = pctState.value,
                                onValueChange = {
                                    pctState.value = it.filter { c -> c.isDigit() }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(60.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time Sum Check Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isValid) "Total Tepat 100%" else "Sisa: ${if (diffPct > 0) "+${String.format("%.0f", diffPct)}%" else "${String.format("%.0f", diffPct)}%"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${String.format("%.0f", totalPct)}% / 100%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val result = initialCategories.mapIndexed { idx, cat ->
                            val pct = editableList[idx].value.toDoubleOrNull() ?: 0.0
                            val nominal = ((salary * pct) / 100.0).toLong()
                            cat.copy(percentage = pct, plannedAmount = nominal)
                        }
                        onSave(result)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Simpan Anggaran")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
