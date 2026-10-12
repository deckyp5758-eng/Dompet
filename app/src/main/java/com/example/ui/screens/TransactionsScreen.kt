package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.data.model.IncomeStatus

sealed class TransactionItem(val timestamp: Long) {
    data class Income(val entity: IncomeEntity) : TransactionItem(entity.dateMillis)
    data class Expense(val entity: ExpenseEntity) : TransactionItem(entity.dateMillis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    incomes: List<IncomeEntity>,
    expenses: List<ExpenseEntity>,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditIncomeClick: (IncomeEntity) -> Unit,
    onDeleteIncomeClick: (IncomeEntity) -> Unit,
    onDeleteExpenseClick: (ExpenseEntity) -> Unit,
    onToggleIncomeStatus: (Long) -> Unit,
    onAllocateBonusClick: (IncomeEntity) -> Unit
) {
    var selectedMainTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: Pemasukan, 2: Pengeluaran
    var incomeTypeFilter by remember { mutableStateOf("SEMUA") }
    var searchQuery by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }

    val filteredItems: List<TransactionItem> = remember(
        incomes, expenses, selectedMainTab, incomeTypeFilter, searchQuery
    ) {
        val q = searchQuery.trim().lowercase()

        val incomeItems = incomes
            .filter { inc ->
                (incomeTypeFilter == "SEMUA" || inc.incomeType == incomeTypeFilter) &&
                (q.isEmpty() || inc.source.lowercase().contains(q) || inc.notes.lowercase().contains(q))
            }
            .map { TransactionItem.Income(it) }

        val expenseItems = expenses
            .filter { exp ->
                q.isEmpty() || exp.category.lowercase().contains(q) || exp.notes.lowercase().contains(q)
            }
            .map { TransactionItem.Expense(it) }

        when (selectedMainTab) {
            0 -> (incomeItems + expenseItems).sortedByDescending { it.timestamp }
            1 -> incomeItems.sortedByDescending { it.timestamp }
            2 -> expenseItems.sortedByDescending { it.timestamp }
            else -> emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedMainTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = { Text("Semua (${incomes.size + expenses.size})") }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = { Text("Pendapatan (${incomes.size})") }
                )
                Tab(
                    selected = selectedMainTab == 2,
                    onClick = { selectedMainTab = 2 },
                    text = { Text("Pengeluaran (${expenses.size})") }
                )
            }

            // Compact Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari sumber, kategori, atau catatan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Sub-filter Chips for Income
            if (selectedMainTab == 0 || selectedMainTab == 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = incomeTypeFilter == "SEMUA",
                            onClick = { incomeTypeFilter = "SEMUA" },
                            label = { Text("Semua Sumber", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = incomeTypeFilter == "GAJI_TETAP",
                            onClick = { incomeTypeFilter = "GAJI_TETAP" },
                            label = { Text("Gaji Tetap", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = incomeTypeFilter == "BONUS",
                            onClick = { incomeTypeFilter = "BONUS" },
                            label = { Text("Bonus / Pemberian", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // List of Transactions
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Pencarian Tidak Ditemukan" else "Belum Ada Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Coba kata kunci lain."
                            else "Gunakan tombol (+) di bawah untuk mencatat transaksi baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(filteredItems, key = { item ->
                        when (item) {
                            is TransactionItem.Income -> "inc_${item.entity.id}"
                            is TransactionItem.Expense -> "exp_${item.entity.id}"
                        }
                    }) { item ->
                        when (item) {
                            is TransactionItem.Income -> {
                                CompactIncomeCardItem(
                                    income = item.entity,
                                    onToggleStatus = { onToggleIncomeStatus(item.entity.id) },
                                    onEdit = { onEditIncomeClick(item.entity) },
                                    onDelete = { onDeleteIncomeClick(item.entity) },
                                    onAllocateBonus = { onAllocateBonusClick(item.entity) }
                                )
                            }
                            is TransactionItem.Expense -> {
                                CompactExpenseCardItem(
                                    expense = item.entity,
                                    onDelete = { onDeleteExpenseClick(item.entity) }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        }

        // Modern Speed-Dial Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(visible = showFabMenu) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onAddIncomeClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("fab_add_income")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Pemasukan")
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onAddExpenseClick()
                        },
                        containerColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("fab_add_expense")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Pengeluaran")
                    }
                }
            }

            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_main_toggle")
            ) {
                Icon(
                    if (showFabMenu) Icons.Default.Clear else Icons.Default.Add,
                    contentDescription = "Menu Tambah Transaksi"
                )
            }
        }
    }
}

@Composable
private fun CompactIncomeCardItem(
    income: IncomeEntity,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAllocateBonus: () -> Unit
) {
    val isReceived = income.status == IncomeStatus.DITERIMA.name
    val isBonus = income.incomeType == "BONUS"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("income_item_${income.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReceived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            else Color(0xFFFEF3C7).copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isBonus) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isBonus) Icons.Default.CardGiftcard else Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isBonus) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = income.source,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${when (income.incomeType) {
                                "GAJI_TETAP" -> "Gaji Tetap"
                                "BONUS" -> "Bonus"
                                else -> "Lainnya"
                            }} • ${CurrencyFormatter.formatDate(income.dateMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${CurrencyFormatter.formatRupiah(income.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isReceived) MaterialTheme.colorScheme.primary else Color(0xFFB45309)
                    )
                    // Status Pill (Clickable to quickly toggle status!)
                    Surface(
                        color = if (isReceived) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFDE68A),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(onClick = onToggleStatus)
                    ) {
                        Text(
                            text = if (isReceived) "✓ Diterima" else "⏳ Belum Cair",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReceived) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (income.notes.isNotBlank() || income.allocationDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                if (income.notes.isNotBlank()) {
                    Text(
                        text = income.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (income.allocationDetails.isNotBlank()) {
                    Text(
                        text = "Alokasi: ${income.allocationDetails}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Compact Action Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBonus && isReceived && !income.isAllocated) {
                    Button(
                        onClick = onAllocateBonus,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Alokasikan", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactExpenseCardItem(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_item_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${expense.paymentMethod} • ${CurrencyFormatter.formatDate(expense.dateMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expense.notes.isNotBlank()) {
                        Text(
                            text = expense.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${CurrencyFormatter.formatRupiah(expense.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
