package com.example.data.calculator

import com.example.core.logger.AppLogger
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.PaydayMode
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
    val essentialShortfall: Long = 0L,
    val paydayDayOfMonth: Int = 25,
    val paydayMode: String = PaydayMode.END_OF_MONTH,
    val paydayDescription: String = "Akhir Bulan",
    val daysUntilNextPayday: Int = 0,
    val nextPaydayDateMillis: Long = 0L,
    val isPaydayToday: Boolean = false,
    val payCycleProgressPct: Float = 0f
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

            // Payday & Calendar Cycle Synchronization (Awal Bulan, Akhir Bulan Otomatis, atau Tanggal Spesifik)
            val todayZero = (referenceCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val maxDaysInCurrentMonth = todayZero.getActualMaximum(Calendar.DAY_OF_MONTH)
            val effectivePaydayThisMonth = when (prof.paydayMode) {
                PaydayMode.START_OF_MONTH -> 1
                PaydayMode.END_OF_MONTH -> maxDaysInCurrentMonth
                else -> prof.paydayDayOfMonth.coerceIn(1, 31).coerceAtMost(maxDaysInCurrentMonth)
            }

            val paydayDescription = when (prof.paydayMode) {
                PaydayMode.START_OF_MONTH -> "Awal Bulan (Tgl 1)"
                PaydayMode.END_OF_MONTH -> "Akhir Bulan (Tgl $maxDaysInCurrentMonth)"
                else -> "Tgl ${prof.paydayDayOfMonth}"
            }

            val (daysUntilPayday, nextPaydayMillis, isTodayPayday) = when {
                currentDay == effectivePaydayThisMonth -> {
                    Triple(0, todayZero.timeInMillis, true)
                }
                currentDay < effectivePaydayThisMonth -> {
                    val nextPayCal = (todayZero.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, effectivePaydayThisMonth)
                    }
                    val diff = ((nextPayCal.timeInMillis - todayZero.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
                    Triple(diff.coerceAtLeast(1), nextPayCal.timeInMillis, false)
                }
                else -> {
                    val nextMonthCal = (todayZero.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                        val maxInNext = getActualMaximum(Calendar.DAY_OF_MONTH)
                        val nextPaydayDay = when (prof.paydayMode) {
                            PaydayMode.START_OF_MONTH -> 1
                            PaydayMode.END_OF_MONTH -> maxInNext
                            else -> prof.paydayDayOfMonth.coerceIn(1, 31).coerceAtMost(maxInNext)
                        }
                        set(Calendar.DAY_OF_MONTH, nextPaydayDay)
                    }
                    val diff = ((nextMonthCal.timeInMillis - todayZero.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
                    Triple(diff.coerceAtLeast(1), nextMonthCal.timeInMillis, false)
                }
            }

            // Pay cycle progress (menyesuaikan siklus bulan)
            val nominalCycle = if (prof.paydayMode == PaydayMode.END_OF_MONTH) maxDaysInCurrentMonth else 30
            val cycleProgressPct = if (isTodayPayday) {
                1.0f
            } else {
                ((nominalCycle - daysUntilPayday).coerceAtLeast(0).toFloat() / nominalCycle.toFloat()).coerceIn(0.05f, 0.95f)
            }

            // Safe Daily Flexible Limit: Synchronized with days until next payday
            val daysToBudget = if (daysUntilPayday > 0) daysUntilPayday else daysRemaining
            val plannedEssentialTotal = budList.filter {
                it.name.contains("pokok", ignoreCase = true) || it.name.contains("tagihan", ignoreCase = true)
            }.sumOf { it.plannedAmount }
            val remainingEssentialObligation = (plannedEssentialTotal - totalEssentialExpensesMonth).coerceAtLeast(0L)
            val availableForFlexible = (actualCash - remainingEssentialObligation).coerceAtLeast(0L)
            val dailyLimit = availableForFlexible / daysToBudget.coerceAtLeast(1)
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
                essentialShortfall = shortfall,
                paydayDayOfMonth = effectivePaydayThisMonth,
                paydayMode = prof.paydayMode,
                paydayDescription = paydayDescription,
                daysUntilNextPayday = daysUntilPayday,
                nextPaydayDateMillis = nextPaydayMillis,
                isPaydayToday = isTodayPayday,
                payCycleProgressPct = cycleProgressPct
            )
        } catch (e: Throwable) {
            AppLogger.error("DashboardCalculator", "Gagal menghitung metrik dashboard: ${e.localizedMessage}", e)
            return DashboardSummary()
        }
    }
}
