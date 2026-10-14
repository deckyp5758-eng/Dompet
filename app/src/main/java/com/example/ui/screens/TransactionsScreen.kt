package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald

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
    onEditExpenseClick: (ExpenseEntity) -> Unit,
    onDeleteIncomeClick: (IncomeEntity) -> Unit,
    onDeleteExpenseClick: (ExpenseEntity) -> Unit,
    onToggleIncomeStatus: (Long) -> Unit,
    onAllocateBonusClick: (IncomeEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Semua, 1: Masuk, 2: Keluar
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("Semua") }
    var selectedItemForDetail by remember { mutableStateOf<TransactionItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalInflow = remember(incomes) { incomes.filter { it.status == "DITERIMA" }.sumOf { it.amount } }
    val totalOutflow = remember(expenses) { expenses.sumOf { it.amount } }

    val filteredItems: List<TransactionItem> = remember(
        incomes, expenses, selectedTab, searchQuery, selectedFilterCategory
    ) {
        val q = searchQuery.trim().lowercase()

        val incomeList = incomes
            .filter { inc ->
                (selectedFilterCategory == "Semua" || inc.incomeType.equals(selectedFilterCategory, ignoreCase = true)) &&
                (q.isEmpty() || inc.source.lowercase().contains(q) || inc.notes.lowercase().contains(q))
            }
            .map { TransactionItem.Income(it) }

        val expenseList = expenses
            .filter { exp ->
                (selectedFilterCategory == "Semua" || exp.category.equals(selectedFilterCategory, ignoreCase = true)) &&
                (q.isEmpty() || exp.category.lowercase().contains(q) || exp.notes.lowercase().contains(q))
            }
            .map { TransactionItem.Expense(it) }

        when (selectedTab) {
            0 -> (incomeList + expenseList).sortedByDescending { it.timestamp }
            1 -> incomeList.sortedByDescending { it.timestamp }
            2 -> expenseList.sortedByDescending { it.timestamp }
            else -> emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Modern Segmented Tab Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabs = listOf("Semua", "Pemasukan", "Pengeluaran")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inflow vs Outflow Mini Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = PrimaryEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Total Masuk", fontSize = 11.sp, color = Color(0xFF065F46))
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalInflow),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryEmerald
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFE4E6),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFCDD2),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Total Keluar", fontSize = 11.sp, color = Color(0xFF9F1239))
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalOutflow),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentRose
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari catatan transaksi...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_transaction_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedFilterCategory != "Semua") "Tidak ada transaksi yang cocok" else "Belum ada transaksi tercatat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedFilterCategory != "Semua") "Coba ubah kata kunci pencarian atau reset filter kategori." else "Mulai catat pemasukan atau pengeluaran harian Anda agar keuangan terpantau rapi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (searchQuery.isNotEmpty() || selectedFilterCategory != "Semua") {
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilterCategory = "Semua"
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Filter & Pencarian")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onAddIncomeClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Pemasukan")
                                }
                                Button(
                                    onClick = onAddExpenseClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Pengeluaran")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredItems,
                        key = { item ->
                            when (item) {
                                is TransactionItem.Income -> "inc_${item.entity.id}"
                                is TransactionItem.Expense -> "exp_${item.entity.id}"
                            }
                        }
                    ) { item ->
                        ModernTransactionTile(
                            item = item,
                            onClick = { selectedItemForDetail = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
                    }
                }
            }
        }

        // Floating Action Buttons for quick entry
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = onAddIncomeClick,
                containerColor = PrimaryEmerald,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_income_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Masuk", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = AccentRose,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keluar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sleek Detail Bottom Sheet on Item Click
        selectedItemForDetail?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetail = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                TransactionDetailSheetContent(
                    item = item,
                    onEditIncome = {
                        selectedItemForDetail = null
                        if (item is TransactionItem.Income) onEditIncomeClick(item.entity)
                    },
                    onEditExpense = {
                        selectedItemForDetail = null
                        if (item is TransactionItem.Expense) onEditExpenseClick(item.entity)
                    },
                    onDeleteIncome = {
                        selectedItemForDetail = null
                        if (item is TransactionItem.Income) onDeleteIncomeClick(item.entity)
                    },
                    onDeleteExpense = {
                        selectedItemForDetail = null
                        if (item is TransactionItem.Expense) onDeleteExpenseClick(item.entity)
                    },
                    onToggleIncomeStatus = {
                        selectedItemForDetail = null
                        if (item is TransactionItem.Income) onToggleIncomeStatus(item.entity.id)
                    },
                    onAllocateBonus = {
                        selectedItemForDetail = null
                        if (item is TransactionItem.Income) onAllocateBonusClick(item.entity)
                    },
                    onClose = { selectedItemForDetail = null }
                )
            }
        }
    }
}

@Composable
fun ModernTransactionTile(
    item: TransactionItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is TransactionItem.Income -> {
                    val isBonus = item.entity.incomeType == "BONUS"
                    val isGaji = item.entity.incomeType == "GAJI_TETAP"
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isBonus -> Color(0xFFFEF3C7)
                            isGaji -> Color(0xFFDBEAFE)
                            else -> Color(0xFFECFDF5)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    isBonus -> Icons.Default.CardGiftcard
                                    isGaji -> Icons.Default.Payments
                                    else -> Icons.Default.ArrowDownward
                                },
                                contentDescription = null,
                                tint = when {
                                    isBonus -> AccentAmber
                                    isGaji -> Color(0xFF2563EB)
                                    else -> PrimaryEmerald
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.entity.source,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${CurrencyFormatter.formatDate(item.entity.dateMillis)} • ${item.entity.incomeType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${CurrencyFormatter.formatRupiah(item.entity.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                        if (item.entity.status == "BELUM_DITERIMA") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Pending",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isBonus && !item.entity.isAllocated) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "Belum Bagi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                is TransactionItem.Expense -> {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFE4E6),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.entity.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${CurrencyFormatter.formatDate(item.entity.dateMillis)} • ${item.entity.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "-${CurrencyFormatter.formatRupiah(item.entity.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentRose
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDetailSheetContent(
    item: TransactionItem,
    onEditIncome: () -> Unit,
    onEditExpense: () -> Unit,
    onDeleteIncome: () -> Unit,
    onDeleteExpense: () -> Unit,
    onToggleIncomeStatus: () -> Unit,
    onAllocateBonus: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rincian Transaksi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (item) {
            is TransactionItem.Income -> {
                val inc = item.entity
                DetailRow(label = "Jenis Transaksi", value = "Pemasukan (${inc.incomeType})")
                DetailRow(label = "Sumber", value = inc.source)
                DetailRow(label = "Nominal", value = CurrencyFormatter.formatRupiah(inc.amount), highlightColor = PrimaryEmerald)
                DetailRow(label = "Tanggal", value = CurrencyFormatter.formatDate(inc.dateMillis))
                DetailRow(label = "Status", value = if (inc.status == "DITERIMA") "Sudah Masuk Rekening" else "Belum Diterima (Pending)")
                if (inc.notes.isNotBlank()) {
                    DetailRow(label = "Catatan", value = inc.notes)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inc.status == "BELUM_DITERIMA") {
                        Button(
                            onClick = onToggleIncomeStatus,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tandai Sudah Masuk Rekening")
                        }
                    }

                    if (inc.incomeType == "BONUS" && inc.status == "DITERIMA" && !inc.isAllocated) {
                        Button(
                            onClick = onAllocateBonus,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alokasikan Bonus Ini")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditIncome,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ubah")
                        }

                        Button(
                            onClick = onDeleteIncome,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus")
                        }
                    }
                }
            }

            is TransactionItem.Expense -> {
                val exp = item.entity
                DetailRow(label = "Jenis Transaksi", value = "Pengeluaran")
                DetailRow(label = "Kategori Pos", value = exp.category)
                DetailRow(label = "Nominal", value = CurrencyFormatter.formatRupiah(exp.amount), highlightColor = AccentRose)
                DetailRow(label = "Tanggal", value = CurrencyFormatter.formatDate(exp.dateMillis))
                DetailRow(label = "Metode Pembayaran", value = exp.paymentMethod)
                if (exp.notes.isNotBlank()) {
                    DetailRow(label = "Catatan", value = exp.notes)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditExpense,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ubah")
                    }

                    Button(
                        onClick = onDeleteExpense,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String, highlightColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
