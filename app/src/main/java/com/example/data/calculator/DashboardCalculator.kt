package com.example.data.calculator

import com.example.core.logger.AppLogger
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.ExpenseCategory
import java.util.Calendar

data class DashboardSummary(
    val actualCashBalance: Long = 0L,
    val totalNetWorth: Long = 0L,
    val totalReceivedIncomeMonth: Long = 0L,
    val totalReceivedSalaryMonth: Long = 0L,
    val totalPlannedSalaryMonth: Long = 0L,
    val totalReceivedBonusMonth: Long = 0L,
    val totalReceivedOtherMonth: Long = 0L,
    val totalActualExpensesMonth: Long = 0L,
    val emergencyFundBalance: Long = 0L,
    val emergencyTargetInitial: Long = 1_000_000L,
    val emergencyTargetFull: Long = 3_645_000L,
    val emergencyStage: Int = 1,
    val emergencyProgressInitialPct: Float = 0f,
    val emergencyProgressFullPct: Float = 0f,
    val investmentTotalCapital: Long = 0L,
    val investmentCurrentValue: Long = 0L,
    val investmentGainLossAmount: Long = 0L,
    val investmentGainLossPct: Float = 0f,
    val safeDailyFlexibleLimit: Long = 0L,
    val daysRemainingInMonth: Int = 20,
    val isDailyLimitWarning: Boolean = false,
    val pendingSalaryExists: Boolean = false,
    val unallocatedBonusCount: Int = 0,
    val essentialShortfall: Long = 0L
)

/**
 * Kalkulator murni untuk memproses ringkasan metrik dashboard keuangan.
 * Terpisah dari ViewModel untuk mempermudah pengujian unit (unit testing)
 * dan perawatan jangka panjang.
 */
object DashboardCalculator {

    fun calculate(
        prof: ProfileEntity,
        incList: List<IncomeEntity>,
        expList: List<ExpenseEntity>,
        efList: List<EmergencyFundEntity>,
        invList: List<InvestmentAssetEntity>,
        budList: List<BudgetCategoryEntity>,
        referenceCalendar: Calendar = Calendar.getInstance()
    ): DashboardSummary {
        try {
            val currentMonth = referenceCalendar.get(Calendar.MONTH)
            val currentYear = referenceCalendar.get(Calendar.YEAR)
            val currentDay = referenceCalendar.get(Calendar.DAY_OF_MONTH)
            val totalDaysInMonth = referenceCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val daysRemaining = (totalDaysInMonth - currentDay + 1).coerceAtLeast(1)

            fun isCurrentMonth(timeMillis: Long): Boolean {
                val c = Calendar.getInstance().apply { timeInMillis = timeMillis }
                return c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
            }

            // Incomes
            var totalReceivedAllTime = 0L
            var totalReceivedMonth = 0L
            var totalReceivedSalaryMonth = 0L
            var totalPlannedSalaryMonth = 0L
            var totalReceivedBonusMonth = 0L
            var totalReceivedOtherMonth = 0L
            var pendingSalaryCount = 0
            var unallocatedBonuses = 0

            incList.forEach { inc ->
                if (inc.status == "DITERIMA") {
                    totalReceivedAllTime += inc.amount
                    if (isCurrentMonth(inc.dateMillis)) {
                        totalReceivedMonth += inc.amount
                        when (inc.incomeType) {
                            "GAJI_TETAP" -> totalReceivedSalaryMonth += inc.amount
                            "BONUS" -> totalReceivedBonusMonth += inc.amount
                            else -> totalReceivedOtherMonth += inc.amount
                        }
                    }
                    if (inc.incomeType == "BONUS" && !inc.isAllocated) {
                        unallocatedBonuses++
                    }
                } else {
                    if (inc.incomeType == "GAJI_TETAP") {
                        pendingSalaryCount++
                    }
                }

                if (inc.incomeType == "GAJI_TETAP" && isCurrentMonth(inc.dateMillis)) {
                    totalPlannedSalaryMonth += inc.amount
                }
            }

            // Default planned salary from profile if none exists
            if (totalPlannedSalaryMonth == 0L) {
                totalPlannedSalaryMonth = prof.monthlySalary
            }

            // Expenses
            var totalExpensesAllTime = 0L
            var totalExpensesMonth = 0L
            var totalEssentialExpensesMonth = 0L

            expList.forEach { exp ->
                totalExpensesAllTime += exp.amount
                if (isCurrentMonth(exp.dateMillis)) {
                    totalExpensesMonth += exp.amount
                    val cat = ExpenseCategory.fromDisplayName(exp.category)
                    if (cat.isEssential) {
                        totalEssentialExpensesMonth += exp.amount
                    }
                }
            }

            // Emergency Fund
            var efDeposits = 0L
            var efWithdrawals = 0L
            efList.forEach { ef ->
                if (ef.type == "SETORAN") efDeposits += ef.amount
                else efWithdrawals += ef.amount
            }
            val emergencyBalance = (efDeposits - efWithdrawals).coerceAtLeast(0L)

            // Investment
            var invCapital = 0L
            var invValue = 0L
            invList.forEach { inv ->
                invCapital += inv.totalCapitalInvested
                invValue += inv.currentMarketValue
            }
            val invGainLoss = invValue - invCapital
            val invGainLossPct = if (invCapital > 0) ((invGainLoss.toFloat() / invCapital) * 100f) else 0f

            // Actual Liquid Cash Balance
            // Cash = Total Received Incomes - Total Expenses - EF Deposits + EF Withdrawals - Inv Capital
            val actualCash = (totalReceivedAllTime - totalExpensesAllTime - emergencyBalance - invCapital)
                .coerceAtLeast(0L)

            val totalNetWorth = actualCash + emergencyBalance + invValue

            // Emergency Fund Target
            val initialTarget = 1_000_000L
            val essentialPlanned = budList.firstOrNull { it.name.contains("pokok", ignoreCase = true) }?.plannedAmount
                ?: (prof.monthlySalary * 45 / 100)
            val fullTarget = (prof.emergencyTargetMonths * essentialPlanned).coerceAtLeast(initialTarget)

            val stage = when {
                emergencyBalance < initialTarget -> 1
                emergencyBalance < fullTarget -> 2
                else -> 3
            }

            val progressInitial = (emergencyBalance.toFloat() / initialTarget).coerceIn(0f, 1f)
            val progressFull = (emergencyBalance.toFloat() / fullTarget).coerceIn(0f, 1f)

            // Safe Daily Flexible Limit
            val plannedEssentialTotal = budList.filter {
                it.name.contains("pokok", ignoreCase = true) || it.name.contains("tagihan", ignoreCase = true)
            }.sumOf { it.plannedAmount }
            val remainingEssentialObligation = (plannedEssentialTotal - totalEssentialExpensesMonth).coerceAtLeast(0L)
            val availableForFlexible = (actualCash - remainingEssentialObligation).coerceAtLeast(0L)
            val dailyLimit = availableForFlexible / daysRemaining
            val isWarning = availableForFlexible <= 0L || actualCash < 100_000L

            val shortfall = if (totalReceivedSalaryMonth < plannedEssentialTotal && totalReceivedSalaryMonth > 0) {
                plannedEssentialTotal - totalReceivedSalaryMonth
            } else if (totalReceivedSalaryMonth == 0L) {
                plannedEssentialTotal
            } else 0L

            return DashboardSummary(
                actualCashBalance = actualCash,
                totalNetWorth = totalNetWorth,
                totalReceivedIncomeMonth = totalReceivedMonth,
                totalReceivedSalaryMonth = totalReceivedSalaryMonth,
                totalPlannedSalaryMonth = totalPlannedSalaryMonth,
                totalReceivedBonusMonth = totalReceivedBonusMonth,
                totalReceivedOtherMonth = totalReceivedOtherMonth,
                totalActualExpensesMonth = totalExpensesMonth,
                emergencyFundBalance = emergencyBalance,
                emergencyTargetInitial = initialTarget,
                emergencyTargetFull = fullTarget,
                emergencyStage = stage,
                emergencyProgressInitialPct = progressInitial,
                emergencyProgressFullPct = progressFull,
                investmentTotalCapital = invCapital,
                investmentCurrentValue = invValue,
                investmentGainLossAmount = invGainLoss,
                investmentGainLossPct = invGainLossPct,
                safeDailyFlexibleLimit = dailyLimit,
                daysRemainingInMonth = daysRemaining,
                isDailyLimitWarning = isWarning,
                pendingSalaryExists = pendingSalaryCount > 0,
                unallocatedBonusCount = unallocatedBonuses,
                essentialShortfall = shortfall
            )
        } catch (e: Throwable) {
            AppLogger.error("DashboardCalculator", "Gagal menghitung metrik dashboard: ${e.localizedMessage}", e)
            return DashboardSummary()
        }
    }
}
