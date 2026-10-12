package com.example.data.calculator

import com.example.core.logger.AppLogger

data class BonusAllocationPlan(
    val stage: Int,
    val stageName: String,
    val stageDescription: String,
    val emergencyFundAmount: Long,
    val investmentAmount: Long,
    val personalSpendingAmount: Long,
    val savingsTargetAmount: Long = 0L,
    val emergencyFundPct: Double,
    val investmentPct: Double,
    val personalSpendingPct: Double,
    val savingsTargetPct: Double = 0.0,
    val isEssentialShortfall: Boolean,
    val shortfallAmount: Long
)

/**
 * Kalkulator murni untuk alokasi dana bonus bertahap (Stage 1, 2, 3)
 * Menjamin integritas akuntansi dan akurasi pembulatan nominal hingga satuan Rupiah terkecil.
 */
object BonusAllocationCalculator {

    fun calculate(
        bonusAmount: Long,
        currentEmergencyFund: Long,
        targetMonths: Int,
        monthlyEssentialCost: Long,
        currentCashBalance: Long,
        monthlyEssentialShortfall: Long
    ): BonusAllocationPlan {
        try {
            val safeBonus = bonusAmount.coerceAtLeast(0L)
            val initialThreshold = 1_000_000L
            val fullThreshold = targetMonths * monthlyEssentialCost.coerceAtLeast(1_215_000L)
            val isShortfall = monthlyEssentialShortfall > 0

            return when {
                currentEmergencyFund < initialThreshold -> {
                    val darurat = (safeBonus * 80) / 100
                    val invest = (safeBonus * 10) / 100
                    val pribadi = safeBonus - (darurat + invest) // Penyesuaian pembulatan presisi
                    BonusAllocationPlan(
                        stage = 1,
                        stageName = "Tahap 1 (Dana Darurat Awal < Rp1.000.000)",
                        stageDescription = "Prioritas membangun bantalan darurat pertama sebelum memperbesar investasi.",
                        emergencyFundAmount = darurat,
                        investmentAmount = invest,
                        personalSpendingAmount = pribadi,
                        emergencyFundPct = 80.0,
                        investmentPct = 10.0,
                        personalSpendingPct = 10.0,
                        isEssentialShortfall = isShortfall,
                        shortfallAmount = monthlyEssentialShortfall
                    )
                }
                currentEmergencyFund < fullThreshold -> {
                    val darurat = (safeBonus * 50) / 100
                    val invest = (safeBonus * 30) / 100
                    val pribadi = safeBonus - (darurat + invest)
                    BonusAllocationPlan(
                        stage = 2,
                        stageName = "Tahap 2 (Dana Darurat Menuju Target Penuh)",
                        stageDescription = "Dana darurat Rp1jt tercapai! Pembagian lebih seimbang antara dana darurat dan investasi.",
                        emergencyFundAmount = darurat,
                        investmentAmount = invest,
                        personalSpendingAmount = pribadi,
                        emergencyFundPct = 50.0,
                        investmentPct = 30.0,
                        personalSpendingPct = 20.0,
                        isEssentialShortfall = isShortfall,
                        shortfallAmount = monthlyEssentialShortfall
                    )
                }
                else -> {
                    val invest = (safeBonus * 70) / 100
                    val tabungan = (safeBonus * 20) / 100
                    val pribadi = safeBonus - (invest + tabungan)
                    BonusAllocationPlan(
                        stage = 3,
                        stageName = "Tahap 3 (Target Penuh Dana Darurat Tercapai)",
                        stageDescription = "Pondasi keuangan kokoh! Porsi terbesar dialokasikan untuk investasi masa depan.",
                        emergencyFundAmount = 0L,
                        investmentAmount = invest,
                        savingsTargetAmount = tabungan,
                        personalSpendingAmount = pribadi,
                        emergencyFundPct = 0.0,
                        investmentPct = 70.0,
                        savingsTargetPct = 20.0,
                        personalSpendingPct = 10.0,
                        isEssentialShortfall = isShortfall,
                        shortfallAmount = monthlyEssentialShortfall
                    )
                }
            }
        } catch (e: Throwable) {
            AppLogger.error("BonusAllocationCalculator", "Gagal menghitung alokasi bonus: ${e.localizedMessage}", e)
            return BonusAllocationPlan(
                stage = 1,
                stageName = "Tahap Default (Error Fallback)",
                stageDescription = "Terjadi kegagalan kalkulasi",
                emergencyFundAmount = bonusAmount,
                investmentAmount = 0L,
                personalSpendingAmount = 0L,
                emergencyFundPct = 100.0,
                investmentPct = 0.0,
                personalSpendingPct = 0.0,
                isEssentialShortfall = false,
                shortfallAmount = 0L
            )
        }
    }
}
