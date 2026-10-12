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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.components.InvestmentAssetDialog
import com.example.ui.components.UpdateMarketValueDialog
import com.example.viewmodel.DashboardSummary

@Composable
fun InvestmentScreen(
    summary: DashboardSummary,
    profile: ProfileEntity,
    assets: List<InvestmentAssetEntity>,
    transactions: List<InvestmentTxEntity>,
    onAddAsset: (name: String, assetType: String, initialCapital: Long, notes: String) -> Unit,
    onUpdateMarketValue: (assetId: Long, newValue: Long) -> Unit,
    onAddDeposit: (assetId: Long, amount: Long, notes: String) -> Unit
) {
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var assetForValueUpdate by remember { mutableStateOf<InvestmentAssetEntity?>(null) }
    var assetForDeposit by remember { mutableStateOf<InvestmentAssetEntity?>(null) }

    val monthlyTarget = profile.monthlySalary * 5 / 100 // 5% = Rp 135.000
    val isEmergencyFundUnsafe = summary.emergencyFundBalance < 1_000_000L
    val isEssentialShortfall = summary.essentialShortfall > 0

    val totalCapital = summary.investmentTotalCapital
    val currentValue = summary.investmentCurrentValue
    val gainLoss = summary.investmentGainLossAmount
    val gainLossPct = summary.investmentGainLossPct

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Portofolio Investasi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Prioritas: ${profile.financialPriority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddAssetDialog = true },
                    modifier = Modifier.testTag("add_investment_asset_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aset Baru")
                }
            }
        }

        // Summary Performance Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nilai Pasar Terkini (Total)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = if (gainLoss >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "${if (gainLoss >= 0) "+" else ""}${String.format("%.1f", gainLossPct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (gainLoss >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.formatRupiah(currentValue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Modal Disetor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalCapital),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Keuntungan / Kerugian",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (gainLoss >= 0) "+" else ""}${CurrencyFormatter.formatRupiah(gainLoss)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (gainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Monthly Target & Safety Check Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEmergencyFundUnsafe || isEssentialShortfall) Color(0xFFFEF3C7)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isEmergencyFundUnsafe || isEssentialShortfall) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isEmergencyFundUnsafe || isEssentialShortfall) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Target Bulanan: ${CurrencyFormatter.formatRupiah(monthlyTarget)} (5% Gaji)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEmergencyFundUnsafe || isEssentialShortfall) Color(0xFF78350F) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isEmergencyFundUnsafe) {
                        Text(
                            text = "Rekomendasi Keamanan: Dana darurat Anda saat ini masih di bawah Rp1.000.000. Anda disarankan melewati atau menunda target investasi bulan ini untuk mengamankan dana darurat terlebih dahulu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                    } else if (isEssentialShortfall) {
                        Text(
                            text = "Rekomendasi Keamanan: Kebutuhan pokok bulan ini belum terpenuhi penuh. Jangan memaksakan investasi sebelum kebutuhan primer aman.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                    } else {
                        Text(
                            text = "Kondisi keuangan aman! Dana darurat telah memenuhi target awal sehingga target investasi bulanan siap dijalankan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Risk Disclaimer Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catatan: Nilai aset investasi berfluktuasi sesuai pergerakan pasar. Nilai pasar dimasukkan secara manual oleh pengguna tanpa janji keuntungan tetap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Assets List
        item {
            Text(
                text = "Daftar Aset Investasi (${assets.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (assets.isEmpty()) {
            item {
                Text(
                    text = "Belum ada aset investasi terdaftar. Ketuk 'Aset Baru' untuk mulai mencatat portofolio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(assets) { asset ->
                val diff = asset.currentMarketValue - asset.totalCapitalInvested
                val diffPct = if (asset.totalCapitalInvested > 0) ((diff.toFloat() / asset.totalCapitalInvested) * 100f) else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = asset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${asset.assetType} • Diperbarui ${CurrencyFormatter.formatShortDate(asset.lastUpdatedMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.formatRupiah(asset.currentMarketValue),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "${if (diff >= 0) "+" else ""}${CurrencyFormatter.formatRupiah(diff)} (${String.format("%.1f", diffPct)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Modal: ${CurrencyFormatter.formatRupiah(asset.totalCapitalInvested)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row {
                                OutlinedButton(
                                    onClick = { assetForDeposit = asset },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+ Setoran", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { assetForValueUpdate = asset },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Update Nilai", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showAddAssetDialog) {
        InvestmentAssetDialog(
            onDismiss = { showAddAssetDialog = false },
            onSave = { name, type, capital, notes ->
                onAddAsset(name, type, capital, notes)
                showAddAssetDialog = false
            }
        )
    }

    assetForValueUpdate?.let { asset ->
        UpdateMarketValueDialog(
            asset = asset,
            onDismiss = { assetForValueUpdate = null },
            onSave = { newVal ->
                onUpdateMarketValue(asset.id, newVal)
                assetForValueUpdate = null
            }
        )
    }

    assetForDeposit?.let { asset ->
        DepositDialog(
            asset = asset,
            onDismiss = { assetForDeposit = null },
            onConfirm = { amount, notes ->
                onAddDeposit(asset.id, amount, notes)
                assetForDeposit = null
            }
        )
    }
}

@Composable
private fun DepositDialog(
    asset: InvestmentAssetEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Setoran: ${asset.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Nominal Setoran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) onConfirm(amount, notes)
                }
            ) {
                Text("Setor Modal")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
