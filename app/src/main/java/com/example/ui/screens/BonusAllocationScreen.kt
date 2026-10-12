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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.model.CurrencyFormatter
import com.example.data.repository.BonusAllocationPlan
import com.example.ui.components.BonusAllocationDialog
import com.example.viewmodel.DashboardSummary

@Composable
fun BonusAllocationScreen(
    summary: DashboardSummary,
    bonusIncomes: List<IncomeEntity>,
    investmentAssets: List<InvestmentAssetEntity>,
    onGetRecommendation: (Long) -> BonusAllocationPlan,
    onApplyAllocation: (incomeId: Long, darurat: Long, invest: Long, pribadi: Long, assetId: Long?) -> Unit
) {
    var selectedBonusForAllocation by remember { mutableStateOf<IncomeEntity?>(null) }
    var simulationAmount by remember { mutableLongStateOf(200_000L) }

    val unallocatedBonuses = bonusIncomes.filter { it.incomeType == "BONUS" && it.status == "DITERIMA" && !it.isAllocated }
    val allocatedBonuses = bonusIncomes.filter { it.incomeType == "BONUS" && it.isAllocated }
    val pendingBonuses = bonusIncomes.filter { it.incomeType == "BONUS" && it.status == "BELUM_DITERIMA" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Aturan Alokasi Penghasilan Tambahan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sistem pembagian bonus & pemberian berdasarkan 3 tahap kesiapan dana darurat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stepper: 3 Tahap Alokasi
        item {
            StageStepperCard(activeStage = summary.emergencyStage, emergencyFund = summary.emergencyFundBalance)
        }

        // Interactive Bonus Simulator
        item {
            BonusSimulatorCard(
                simulatedBonus = simulationAmount,
                plan = onGetRecommendation(simulationAmount),
                onSelectPreset = { simulationAmount = it }
            )
        }

        // Section: Unallocated Received Bonuses
        item {
            Text(
                text = "Bonus Diterima Siap Dialokasikan (${unallocatedBonuses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (unallocatedBonuses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tidak Ada Bonus yang Menunggu Alokasi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Semua bonus yang sudah diterima telah dialokasikan, atau catat bonus baru di tab Transaksi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(unallocatedBonuses) { bonus ->
                BonusItemCard(
                    bonus = bonus,
                    isAllocated = false,
                    onActionClick = { selectedBonusForAllocation = bonus }
                )
            }
        }

        // Pending Bonuses Note
        if (pendingBonuses.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${pendingBonuses.size} Bonus Belum Cair",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Hanya bonus yang berstatus 'Diterima' yang dapat dialokasikan ke pos keuangan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        // Section: History of Allocated Bonuses
        if (allocatedBonuses.isNotEmpty()) {
            item {
                Text(
                    text = "Riwayat Bonus yang Sudah Dialokasikan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(allocatedBonuses) { bonus ->
                BonusItemCard(
                    bonus = bonus,
                    isAllocated = true,
                    onActionClick = { selectedBonusForAllocation = bonus }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    selectedBonusForAllocation?.let { bonus ->
        val plan = onGetRecommendation(bonus.amount)
        BonusAllocationDialog(
            bonusIncome = bonus,
            plan = plan,
            availableAssets = investmentAssets,
            onDismiss = { selectedBonusForAllocation = null },
            onConfirmAllocation = { darurat, invest, pribadi, assetId ->
                onApplyAllocation(bonus.id, darurat, invest, pribadi, assetId)
                selectedBonusForAllocation = null
            }
        )
    }
}

@Composable
private fun StageStepperCard(activeStage: Int, emergencyFund: Long) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status Kesiapan Dana Darurat",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(emergencyFund),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Step Horizontal Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepPill(
                    stepNum = 1,
                    title = "Tahap 1",
                    rule = "80% Darurat\n10% Invest\n10% Pribadi",
                    isActive = activeStage == 1,
                    modifier = Modifier.weight(1f)
                )
                StepPill(
                    stepNum = 2,
                    title = "Tahap 2",
                    rule = "50% Darurat\n30% Invest\n20% Pribadi",
                    isActive = activeStage == 2,
                    modifier = Modifier.weight(1f)
                )
                StepPill(
                    stepNum = 3,
                    title = "Tahap 3",
                    rule = "70% Invest\n20% Tabungan\n10% Pribadi",
                    isActive = activeStage == 3,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StepPill(
    stepNum: Int,
    title: String,
    rule: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isActive) "★ $title" else title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BonusSimulatorCard(
    simulatedBonus: Long,
    plan: BonusAllocationPlan,
    onSelectPreset: (Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simulasi Pembagian Otomatis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = CurrencyFormatter.formatRupiah(simulatedBonus),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Presets
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(100_000L, 200_000L, 500_000L, 1_000_000L).forEach { preset ->
                    item {
                        AssistChip(
                            onClick = { onSelectPreset(preset) },
                            label = { Text(CurrencyFormatter.formatRupiah(preset), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calculated 3 portions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dana Darurat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatRupiah(plan.emergencyFundAmount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Investasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatRupiah(plan.investmentAmount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Pribadi / Lainnya", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatRupiah(plan.personalSpendingAmount + plan.savingsTargetAmount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BonusItemCard(
    bonus: IncomeEntity,
    isAllocated: Boolean,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllocated) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else Color(0xFFFEF3C7)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = if (isAllocated) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFB45309),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = bonus.source,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(bonus.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllocated) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFB45309)
                    )
                    if (isAllocated && bonus.allocationDetails.isNotBlank()) {
                        Text(
                            text = bonus.allocationDetails,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllocated) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD97706)
                ),
                modifier = Modifier.testTag("apply_bonus_btn_${bonus.id}")
            ) {
                Text(
                    text = if (isAllocated) "Alokasi Ulang" else "Alokasikan",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAllocated) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }
        }
    }
}
