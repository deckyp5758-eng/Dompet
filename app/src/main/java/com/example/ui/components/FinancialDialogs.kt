package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.model.AssetType
import com.example.data.model.BudgetPreset
import com.example.data.model.CurrencyFormatter
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeStatus
import com.example.data.model.IncomeType
import com.example.data.model.PaymentMethod
import com.example.data.repository.BonusAllocationPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    initialIncome: IncomeEntity? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Long, incomeType: String, source: String, status: String, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf(initialIncome?.amount?.toString() ?: "") }
    var incomeType by remember { mutableStateOf(initialIncome?.incomeType ?: IncomeType.BONUS.name) }
    var source by remember { mutableStateOf(initialIncome?.source ?: "") }
    var status by remember { mutableStateOf(initialIncome?.status ?: IncomeStatus.DITERIMA.name) }
    var notes by remember { mutableStateOf(initialIncome?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialIncome == null) "Tambah Pendapatan" else "Edit Pendapatan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Catat gaji, bonus, atau pendapatan sampingan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Jenis Pendapatan - Horizontal Scrollable Chips (Prevents vertical squishing!)
                Text(
                    text = "Jenis Pendapatan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = incomeType == IncomeType.GAJI_TETAP.name,
                            onClick = {
                                incomeType = IncomeType.GAJI_TETAP.name
                                if (source.isEmpty()) source = "Gaji Bulanan"
                            },
                            label = { Text("Gaji Tetap", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = incomeType == IncomeType.BONUS.name,
                            onClick = {
                                incomeType = IncomeType.BONUS.name
                                if (source.isEmpty()) source = "Bonus Proyek"
                            },
                            label = { Text("Bonus / Hadiah", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = incomeType == IncomeType.LAINNYA.name,
                            onClick = {
                                incomeType = IncomeType.LAINNYA.name
                                if (source.isEmpty()) source = "Pendapatan Sampingan"
                            },
                            label = { Text("Lainnya", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nominal Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Nominal (Rp)") },
                    placeholder = { Text("Contoh: 200000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (amountText.isNotEmpty()) {
                    val previewVal = amountText.toLongOrNull() ?: 0L
                    Text(
                        text = CurrencyFormatter.formatRupiah(previewVal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sumber / Keterangan Input
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Sumber / Keterangan") },
                    placeholder = { Text("Contoh: Bonus Kinerja / Gaji Kantor") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_source_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Penerimaan - Horizontal Scrollable Chips
                Text(
                    text = "Status Penerimaan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Uang yang 'Belum Cair' tidak akan menambah saldo kas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = status == IncomeStatus.DITERIMA.name,
                            onClick = { status = IncomeStatus.DITERIMA.name },
                            leadingIcon = {
                                if (status == IncomeStatus.DITERIMA.name) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            label = { Text("✓ Diterima (Aktual)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = status == IncomeStatus.BELUM_DITERIMA.name,
                            onClick = { status = IncomeStatus.BELUM_DITERIMA.name },
                            label = { Text("⏳ Belum Cair (Terencana)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Opsional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull()
                            if (amount == null || amount <= 0L) {
                                errorMessage = "Nominal harus berupa angka positif lebih dari 0."
                                return@Button
                            }
                            if (source.isBlank()) {
                                errorMessage = "Sumber atau keterangan tidak boleh kosong."
                                return@Button
                            }
                            onSave(amount, incomeType, source.trim(), status, notes.trim())
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_income_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Long, category: String, paymentMethod: String, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.MAKAN_POKOK) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var paymentDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Catat Pengeluaran",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Langsung memotong saldo kas & pos anggaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Nominal Pengeluaran (Rp)") },
                    placeholder = { Text("Contoh: 35000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (amountText.isNotEmpty()) {
                    val previewVal = amountText.toLongOrNull() ?: 0L
                    Text(
                        text = CurrencyFormatter.formatRupiah(previewVal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kategori Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori Pengeluaran") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat.displayName, style = MaterialTheme.typography.bodyMedium)
                                        if (cat.isEssential) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "(Wajib)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metode Pembayaran Dropdown
                ExposedDropdownMenuBox(
                    expanded = paymentDropdownExpanded,
                    onExpandedChange = { paymentDropdownExpanded = !paymentDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metode Pembayaran") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = paymentDropdownExpanded,
                        onDismissRequest = { paymentDropdownExpanded = false }
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayName, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    paymentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Keterangan") },
                    placeholder = { Text("Contoh: Nasi padang, bensin motor") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull()
                            if (amount == null || amount <= 0L) {
                                errorMessage = "Nominal harus berupa angka positif lebih dari 0."
                                return@Button
                            }
                            onSave(amount, selectedCategory.displayName, selectedPaymentMethod.name, notes.trim())
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

/**
 * Bonus Allocation Dialog per Section 4 of user spec
 */
@Composable
fun BonusAllocationDialog(
    bonusIncome: IncomeEntity,
    plan: BonusAllocationPlan,
    availableAssets: List<InvestmentAssetEntity>,
    onDismiss: () -> Unit,
    onConfirmAllocation: (daruratAmount: Long, investAmount: Long, pribadiAmount: Long, assetId: Long?) -> Unit
) {
    val bonusAmount = bonusIncome.amount

    var daruratPct by remember { mutableDoubleStateOf(plan.emergencyFundPct) }
    var investPct by remember { mutableDoubleStateOf(plan.investmentPct) }
    var pribadiPct by remember { mutableDoubleStateOf(plan.personalSpendingPct) }

    val calculatedDarurat = ((bonusAmount * daruratPct) / 100.0).toLong()
    val calculatedInvest = ((bonusAmount * investPct) / 100.0).toLong()
    val calculatedPribadi = (bonusAmount - calculatedDarurat - calculatedInvest).coerceAtLeast(0L)

    var selectedAssetId by remember { mutableStateOf<Long?>(availableAssets.firstOrNull()?.id) }

    val totalPct = daruratPct + investPct + pribadiPct
    val isPctValid = Math.abs(totalPct - 100.0) < 0.1

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Alokasi Bonus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${bonusIncome.source} (${CurrencyFormatter.formatRupiah(bonusAmount)})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = plan.stageName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = plan.stageDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (plan.isEssentialShortfall) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perhatian: Kebutuhan pokok bulan ini belum cukup. Disarankan amankan kebutuhan pokok terlebih dahulu.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Rekomendasi Pembagian (% dapat diedit):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                AllocationRow(
                    label = "Dana Darurat",
                    percent = daruratPct,
                    nominal = calculatedDarurat,
                    onPercentChange = { daruratPct = it }
                )

                AllocationRow(
                    label = "Investasi",
                    percent = investPct,
                    nominal = calculatedInvest,
                    onPercentChange = { investPct = it }
                )

                val thirdLabel = if (plan.stage == 3) "Tujuan Tabungan" else "Keperluan Pribadi"
                AllocationRow(
                    label = thirdLabel,
                    percent = pribadiPct,
                    nominal = calculatedPribadi,
                    onPercentChange = { pribadiPct = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Total Check Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPctValid) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
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
                            text = "Total: ${CurrencyFormatter.formatRupiah(calculatedDarurat + calculatedInvest + calculatedPribadi)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.0f", totalPct)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPctValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!isPctValid) {
                    Text(
                        text = "Total persentase harus tepat 100%.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isPctValid) {
                                onConfirmAllocation(
                                    calculatedDarurat,
                                    calculatedInvest,
                                    calculatedPribadi,
                                    selectedAssetId
                                )
                            }
                        },
                        enabled = isPctValid,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_bonus_allocation_button")
                    ) {
                        Text("Setujui Alokasi")
                    }
                }
            }
        }
    }
}

@Composable
private fun AllocationRow(
    label: String,
    percent: Double,
    nominal: Long,
    onPercentChange: (Double) -> Unit
) {
    var textVal by remember(percent) { mutableStateOf(String.format("%.0f", percent)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(
                CurrencyFormatter.formatRupiah(nominal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textVal,
                onValueChange = {
                    textVal = it.filter { char -> char.isDigit() }
                    val d = textVal.toDoubleOrNull() ?: 0.0
                    onPercentChange(d)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(60.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EmergencyFundDialog(
    currentBalance: Long,
    cashBalance: Long,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Long, source: String, notes: String) -> Unit
) {
    var type by remember { mutableStateOf("SETORAN") }
    var amountText by remember { mutableStateOf("") }
    var sourceDesc by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transaksi Dana Darurat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Saldo Darurat: ${CurrencyFormatter.formatRupiah(currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = type == "SETORAN",
                            onClick = {
                                type = "SETORAN"
                                if (sourceDesc.isEmpty() || sourceDesc == "Penarikan Darurat") {
                                    sourceDesc = "Setoran Tabungan Darurat"
                                }
                            },
                            label = { Text("Setor Dana (+)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = type == "PENARIKAN",
                            onClick = {
                                type = "PENARIKAN"
                                sourceDesc = "Penarikan Darurat"
                            },
                            label = { Text("Tarik Dana (-)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (type == "PENARIKAN") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gunakan hanya untuk kondisi darurat kritis/medis.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = sourceDesc,
                    onValueChange = { sourceDesc = it },
                    label = { Text("Keterangan") },
                    placeholder = { Text("Contoh: Setoran rutin / Servis darurat") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Opsional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull()
                            if (amount == null || amount <= 0L) {
                                errorMessage = "Nominal harus angka lebih dari 0."
                                return@Button
                            }
                            if (type == "SETORAN" && amount > cashBalance) {
                                errorMessage = "Saldo kas aktif (${CurrencyFormatter.formatRupiah(cashBalance)}) tidak cukup."
                                return@Button
                            }
                            if (type == "PENARIKAN" && amount > currentBalance) {
                                errorMessage = "Saldo dana darurat (${CurrencyFormatter.formatRupiah(currentBalance)}) tidak cukup."
                                return@Button
                            }
                            onSave(type, amount, sourceDesc.ifBlank { "Transaksi Dana Darurat" }, notes)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun InvestmentAssetDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, assetType: String, initialCapital: Long, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AssetType.PASAR_UANG) }
    var capitalText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tambah Aset Investasi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Pencatatan aset manual tanpa janji imbal hasil tetap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Aset") },
                    placeholder = { Text("Contoh: Reksadana Sucor, BBRI, Emas Antam") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Jenis Aset:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssetType.entries.forEach { at ->
                        item {
                            FilterChip(
                                selected = selectedType == at,
                                onClick = { selectedType = at },
                                label = { Text(at.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = capitalText,
                    onValueChange = { capitalText = it.filter { char -> char.isDigit() } },
                    label = { Text("Modal Disetor Awal (Rp)") },
                    placeholder = { Text("0 jika belum ada setoran") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Investasi") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama aset tidak boleh kosong."
                                return@Button
                            }
                            val cap = capitalText.toLongOrNull() ?: 0L
                            onSave(name.trim(), selectedType.name, cap, notes.trim())
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateMarketValueDialog(
    asset: InvestmentAssetEntity,
    onDismiss: () -> Unit,
    onSave: (newMarketValue: Long) -> Unit
) {
    var valText by remember { mutableStateOf(asset.currentMarketValue.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembaruan Nilai: ${asset.name}") },
        text = {
            Column {
                Text(
                    text = "Modal Disetor: ${CurrencyFormatter.formatRupiah(asset.totalCapitalInvested)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = valText,
                    onValueChange = { valText = it.filter { char -> char.isDigit() } },
                    label = { Text("Nilai Terkini (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (valText.isNotEmpty()) {
                    val newVal = valText.toLongOrNull() ?: 0L
                    val diff = newVal - asset.totalCapitalInvested
                    val diffPct = if (asset.totalCapitalInvested > 0) ((diff.toFloat() / asset.totalCapitalInvested) * 100f) else 0f
                    val sign = if (diff >= 0) "+" else ""
                    Text(
                        text = "Imbal Hasil: $sign${CurrencyFormatter.formatRupiah(diff)} (${String.format("%.2f", diffPct)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newVal = valText.toLongOrNull()
                    if (newVal == null || newVal < 0L) {
                        errorMessage = "Nilai harus berupa angka tidak negatif."
                        return@Button
                    }
                    onSave(newVal)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Nilai")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    itemDescription: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Hapus") },
        text = { Text("Apakah Anda yakin ingin menghapus '$itemDescription'? Saldo dan riwayat akan diperbarui secara otomatis.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryCustomizerDialog(
    currentSalary: Long,
    onDismiss: () -> Unit,
    onApply: (newSalary: Long, preset: BudgetPreset?) -> Unit
) {
    var salaryInput by remember { mutableStateOf(currentSalary.toString()) }
    var selectedPreset by remember { mutableStateOf(BudgetPreset.DOMPET_PINTAR) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val numericSalary = salaryInput.toLongOrNull() ?: 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sesuaikan Gaji & Anggaran",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Otomatisasi pembagian pos dengan aturan keuangan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Gaji
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = {
                        val cleaned = it.filter { char -> char.isDigit() }
                        salaryInput = cleaned
                        errorMessage = null
                    },
                    label = { Text("Nominal Gaji Tetap Bulanan (Rp)") },
                    supportingText = {
                        Text("Terformat: ${CurrencyFormatter.formatRupiah(numericSalary)}")
                    },
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("salary_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Pilihan Cepat Nominal Gaji:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Chips
                val quickSalaries = listOf(
                    2_700_000L to "Rp 2,7 Jt",
                    3_500_000L to "Rp 3,5 Jt",
                    5_000_000L to "Rp 5,0 Jt",
                    7_500_000L to "Rp 7,5 Jt",
                    10_000_000L to "Rp 10 Jt",
                    15_000_000L to "Rp 15 Jt"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSalaries.size) { index ->
                        val (amount, label) = quickSalaries[index]
                        val isSelected = numericSalary == amount
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                salaryInput = amount.toString()
                                errorMessage = null
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Model Alokasi Anggaran (Referensi Finansial):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets list
                val presets = listOf(
                    BudgetPreset.DOMPET_PINTAR,
                    BudgetPreset.RULE_50_30_20,
                    BudgetPreset.FIRE_AGGRESSIVE,
                    BudgetPreset.CURRENT_PERCENTAGES
                )

                presets.forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedPreset = preset },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPreset = preset },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Simulation Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulasi Pembagian Otomatis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% = ${CurrencyFormatter.formatRupiah(numericSalary)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val simulatedBreakdown: List<Pair<String, Pair<Double, Long>>> = when (selectedPreset) {
                            BudgetPreset.RULE_50_30_20 -> listOf(
                                "Kebutuhan Pokok & Tagihan (50%)" to (50.0 to (numericSalary * 50 / 100)),
                                "Keperluan Pribadi & Gaya Hidup (30%)" to (30.0 to (numericSalary * 30 / 100)),
                                "Tabungan, Darurat & Investasi (20%)" to (20.0 to (numericSalary * 20 / 100))
                            )
                            BudgetPreset.FIRE_AGGRESSIVE -> listOf(
                                "Kebutuhan Pokok (35%)" to (35.0 to (numericSalary * 35 / 100)),
                                "Transport & Tagihan (15%)" to (15.0 to (numericSalary * 15 / 100)),
                                "Investasi Masa Depan (30%)" to (30.0 to (numericSalary * 30 / 100)),
                                "Dana Darurat (10%)" to (10.0 to (numericSalary * 10 / 100)),
                                "Keperluan Pribadi (5%)" to (5.0 to (numericSalary * 5 / 100)),
                                "Cadangan Fleksibel (5%)" to (5.0 to (numericSalary * 5 / 100))
                            )
                            BudgetPreset.DOMPET_PINTAR -> listOf(
                                "Kebutuhan Pokok & Keluarga (45%)" to (45.0 to (numericSalary * 45 / 100)),
                                "Transport, Pulsa & Tagihan (15%)" to (15.0 to (numericSalary * 15 / 100)),
                                "Dana Darurat (20%)" to (20.0 to (numericSalary * 20 / 100)),
                                "Keperluan Pribadi (10%)" to (10.0 to (numericSalary * 10 / 100)),
                                "Investasi (5%)" to (5.0 to (numericSalary * 5 / 100)),
                                "Cadangan Fleksibel (5%)" to (5.0 to (numericSalary * 5 / 100))
                            )
                            BudgetPreset.CURRENT_PERCENTAGES -> listOf(
                                "Rasio aktif saat ini (Otomatis)" to (100.0 to numericSalary)
                            )
                        }

                        simulatedBreakdown.forEach { (catName, pair) ->
                            val (_, amt) = pair
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(amt),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (numericSalary <= 0L) {
                                errorMessage = "Gaji harus lebih dari Rp 0."
                                return@Button
                            }
                            onApply(numericSalary, selectedPreset)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("apply_salary_preset_button")
                    ) {
                        Text("Terapkan & Hitung Ulang")
                    }
                }
            }
        }
    }
}
