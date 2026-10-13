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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald

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
    val totalPlannedBudget = remember(budgetCategories) { budgetCategories.sumOf { it.plannedAmount } }
    val totalActualExpense = remember(expenses) { expenses.sumOf { it.amount } }
    val totalRemainingBudget = (totalPlannedBudget - totalActualExpense).coerceAtLeast(0L)

    val overallUsagePercent = if (totalPlannedBudget > 0) {
        ((totalActualExpense.toFloat() / totalPlannedBudget.toFloat()) * 100f).coerceIn(0f, 150f)
    } else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Header Info & Quick Salary Customizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Alokasi Anggaran",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dasar Gaji: ${CurrencyFormatter.formatRupiah(monthlySalary)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCustomizeSalaryClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("customize_salary_budget_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gaji", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_budget_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ubah Pos", fontSize = 12.sp)
                    }
                }
            }
        }

        // Master Overview Card
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
                            text = "Ringkasan Pagu Bulan Ini",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                overallUsagePercent > 95f -> Color(0xFFFFE4E6)
                                overallUsagePercent > 80f -> Color(0xFFFEF3C7)
                                else -> Color(0xFFECFDF5)
                            }
                        ) {
                            Text(
                                text = "${overallUsagePercent.toInt()}% Terpakai",
                                color = when {
                                    overallUsagePercent > 95f -> AccentRose
                                    overallUsagePercent > 80f -> AccentAmber
                                    else -> PrimaryEmerald
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (overallUsagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            overallUsagePercent > 95f -> AccentRose
                            overallUsagePercent > 80f -> AccentAmber
                            else -> PrimaryEmerald
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Pagu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.formatRupiah(totalPlannedBudget), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Terpakai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.formatRupiah(totalActualExpense), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentRose)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Sisa Pagu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.formatRupiah(totalRemainingBudget), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Daftar Pos Anggaran (${budgetCategories.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Category Cards
        items(budgetCategories) { cat ->
            val catExpenses = expenses.filter { it.category == cat.name }
            val catActualExpense = catExpenses.sumOf { it.amount }
            val catRemaining = (cat.plannedAmount - catActualExpense).coerceAtLeast(0L)
            val usagePct = if (cat.plannedAmount > 0) {
                ((catActualExpense.toFloat() / cat.plannedAmount.toFloat()) * 100f).coerceIn(0f, 150f)
            } else 0f

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_category_card_${cat.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cat.percentage}% dari gaji (${CurrencyFormatter.formatRupiah(cat.plannedAmount)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                usagePct > 95f -> Color(0xFFFFE4E6)
                                usagePct > 80f -> Color(0xFFFEF3C7)
                                else -> Color(0xFFECFDF5)
                            }
                        ) {
                            Text(
                                text = "${usagePct.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    usagePct > 95f -> AccentRose
                                    usagePct > 80f -> AccentAmber
                                    else -> PrimaryEmerald
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (usagePct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            usagePct > 95f -> AccentRose
                            usagePct > 80f -> AccentAmber
                            else -> PrimaryEmerald
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terpakai: ${CurrencyFormatter.formatRupiah(catActualExpense)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sisa: ${CurrencyFormatter.formatRupiah(catRemaining)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (usagePct > 95f) AccentRose else PrimaryEmerald
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditDialog) {
        EditBudgetDialog(
            categories = budgetCategories,
            salary = monthlySalary,
            onDismiss = { showEditDialog = false },
            onSave = { updatedList ->
                onSaveBudgetCategories(updatedList)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditBudgetDialog(
    categories: List<BudgetCategoryEntity>,
    salary: Long,
    onDismiss: () -> Unit,
    onSave: (List<BudgetCategoryEntity>) -> Unit
) {
    var editableCategories by remember {
        mutableStateOf(categories.map { it.copy() })
    }

    val totalPercentage = editableCategories.sumOf { it.percentage }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ubah Porsi Pos Anggaran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Total persentase saat ini: ${totalPercentage}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (totalPercentage == 100.0) PrimaryEmerald else AccentRose,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(editableCategories) { cat ->
                        var pctText by remember(cat.percentage) { mutableStateOf(cat.percentage.toString()) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pctText,
                                onValueChange = { newVal ->
                                    pctText = newVal
                                    val parsed = newVal.toDoubleOrNull() ?: 0.0
                                    editableCategories = editableCategories.map {
                                        if (it.id == cat.id) {
                                            it.copy(
                                                percentage = parsed,
                                                plannedAmount = (salary * parsed / 100.0).toLong()
                                            )
                                        } else it
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(90.dp),
                                singleLine = true,
                                trailingIcon = { Text("%", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editableCategories) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Batal")
            }
        }
    )
}
