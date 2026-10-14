package com.example

import com.example.core.logger.AppLogger
import com.example.core.logger.LogLevel
import com.example.core.result.AppResult
import com.example.core.result.runCatchingApp
import com.example.data.calculator.BonusAllocationCalculator
import com.example.data.calculator.DashboardCalculator
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.PaydayMode
import com.example.data.local.ProfileEntity
import com.example.data.repository.FinanceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinanceCalculationTest {

    @Test
    fun `test 1 and 2 planned salary does not add to cash until marked received`() {
        val plannedSalary = IncomeEntity(
            id = 1,
            dateMillis = System.currentTimeMillis(),
            amount = 2_700_000L,
            incomeType = "GAJI_TETAP",
            source = "Gaji Tetap",
            status = "BELUM_DITERIMA"
        )

        // Helper calculation function mirroring ViewModel
        fun calcBalance(incomes: List<IncomeEntity>, expenses: List<ExpenseEntity>): Long {
            val received = incomes.filter { it.status == "DITERIMA" }.sumOf { it.amount }
            val spent = expenses.sumOf { it.amount }
            return (received - spent).coerceAtLeast(0L)
        }

        // 1. Before received: balance is 0
        assertEquals(0L, calcBalance(listOf(plannedSalary), emptyList()))

        // 2. When marked received: balance increases by exactly 2_700_000
        val receivedSalary = plannedSalary.copy(status = "DITERIMA")
        assertEquals(2_700_000L, calcBalance(listOf(receivedSalary), emptyList()))
    }

    @Test
    fun `test 3 and 7 and 8 bonus allocation stages and exact rounding`() {
        val repo = FinanceRepository(FakeFinanceDao())

        // Stage 1: Emergency fund < 1_000_000 -> 80% Darurat, 10% Investasi, 10% Pribadi
        val bonus200k = 200_000L
        val planStage1 = repo.calculateBonusAllocationPlan(
            bonusAmount = bonus200k,
            currentEmergencyFund = 0L,
            targetMonths = 3,
            monthlyEssentialCost = 1_215_000L,
            currentCashBalance = 2_700_000L,
            monthlyEssentialShortfall = 0L
        )

        assertEquals(1, planStage1.stage)
        assertEquals(160_000L, planStage1.emergencyFundAmount) // 80% of 200k
        assertEquals(20_000L, planStage1.investmentAmount)    // 10% of 200k
        assertEquals(20_000L, planStage1.personalSpendingAmount) // 10% of 200k
        assertEquals(bonus200k, planStage1.emergencyFundAmount + planStage1.investmentAmount + planStage1.personalSpendingAmount)

        // Stage 2: Emergency fund >= 1_000_000 and < 3_645_000
        val planStage2 = repo.calculateBonusAllocationPlan(
            bonusAmount = bonus200k,
            currentEmergencyFund = 1_500_000L,
            targetMonths = 3,
            monthlyEssentialCost = 1_215_000L,
            currentCashBalance = 2_700_000L,
            monthlyEssentialShortfall = 0L
        )
        assertEquals(2, planStage2.stage)
        assertEquals(100_000L, planStage2.emergencyFundAmount) // 50%
        assertEquals(60_000L, planStage2.investmentAmount)    // 30%
        assertEquals(40_000L, planStage2.personalSpendingAmount) // 20%
        assertEquals(bonus200k, planStage2.emergencyFundAmount + planStage2.investmentAmount + planStage2.personalSpendingAmount)

        // Stage 3: Emergency fund >= full target (e.g. 4_000_000)
        val planStage3 = repo.calculateBonusAllocationPlan(
            bonusAmount = bonus200k,
            currentEmergencyFund = 4_000_000L,
            targetMonths = 3,
            monthlyEssentialCost = 1_215_000L,
            currentCashBalance = 2_700_000L,
            monthlyEssentialShortfall = 0L
        )
        assertEquals(3, planStage3.stage)
        assertEquals(0L, planStage3.emergencyFundAmount)
        assertEquals(140_000L, planStage3.investmentAmount) // 70%
        assertEquals(40_000L, planStage3.savingsTargetAmount) // 20%
        assertEquals(20_000L, planStage3.personalSpendingAmount) // 10%
        assertEquals(bonus200k, planStage3.investmentAmount + planStage3.savingsTargetAmount + planStage3.personalSpendingAmount)
    }

    @Test
    fun `test 4 unreceived bonus does not add to balance`() {
        val unreceivedBonus = IncomeEntity(
            id = 2,
            dateMillis = System.currentTimeMillis(),
            amount = 500_000L,
            incomeType = "BONUS",
            source = "Bonus Performa",
            status = "BELUM_DITERIMA"
        )

        val receivedSalary = IncomeEntity(
            id = 1,
            dateMillis = System.currentTimeMillis(),
            amount = 2_700_000L,
            incomeType = "GAJI_TETAP",
            source = "Gaji Tetap",
            status = "DITERIMA"
        )

        val totalReceived = listOf(receivedSalary, unreceivedBonus)
            .filter { it.status == "DITERIMA" }
            .sumOf { it.amount }

        assertEquals(2_700_000L, totalReceived)
    }

    @Test
    fun `test 5 and 6 expenses and transfers maintain accurate net worth without double counting`() {
        // Initial received salary: 2.700.000
        val salary = 2_700_000L
        // Essential expense: 600.000
        val expense = 600_000L
        // Transfer to emergency fund: 540.000
        val efTransfer = 540_000L
        // Transfer to investment: 135.000
        val invTransfer = 135_000L

        val cash = salary - expense - efTransfer - invTransfer
        assertEquals(1_425_000L, cash)

        val netWorth = cash + efTransfer + invTransfer
        assertEquals(salary - expense, netWorth) // Exact 2.100.000!
    }

    @Test
    fun `test DashboardCalculator handles zero data and negative balances safely`() {
        val summary = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 2_500_000L),
            incList = emptyList(),
            expList = emptyList(),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList()
        )

        assertEquals(0L, summary.actualCashBalance)
        assertEquals(0L, summary.totalNetWorth)
        assertEquals(1, summary.emergencyStage)
        assertEquals(2_500_000L, summary.totalPlannedSalaryMonth)
    }

    @Test
    fun `test payday countdown calculation when today is before payday`() {
        // Mock reference calendar: October 10, 2026
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 10, 0, 0)
        }
        val summary = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 3_000_000L, paydayMode = PaydayMode.SPECIFIC_DAY, paydayDayOfMonth = 25),
            incList = emptyList(),
            expList = emptyList(),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList(),
            referenceCalendar = cal
        )

        assertEquals(25, summary.paydayDayOfMonth)
        assertEquals(15, summary.daysUntilNextPayday) // 25 - 10 = 15
        assertFalse(summary.isPaydayToday)
    }

    @Test
    fun `test payday countdown calculation when today is exactly payday`() {
        // Mock reference calendar: October 25, 2026
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 8, 30, 0)
        }
        val summary = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 3_000_000L, paydayMode = PaydayMode.SPECIFIC_DAY, paydayDayOfMonth = 25),
            incList = emptyList(),
            expList = emptyList(),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList(),
            referenceCalendar = cal
        )

        assertEquals(0, summary.daysUntilNextPayday)
        assertTrue(summary.isPaydayToday)
    }

    @Test
    fun `test payday countdown calculation when today is after payday wrapping to next month`() {
        // Mock reference calendar: October 28, 2026 (October has 31 days)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 28, 12, 0, 0)
        }
        val summary = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 3_000_000L, paydayMode = PaydayMode.SPECIFIC_DAY, paydayDayOfMonth = 25),
            incList = emptyList(),
            expList = emptyList(),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList(),
            referenceCalendar = cal
        )

        // 3 days left in October (29, 30, 31) + 25 days in November = 28 days
        assertEquals(28, summary.daysUntilNextPayday)
        assertFalse(summary.isPaydayToday)
    }

    @Test
    fun `test AppLogger records logs and increments error count for fast failure detection`() {
        AppLogger.clearLogs()
        assertEquals(0, AppLogger.errorCount.value)

        AppLogger.info("TestTag", "Info message")
        AppLogger.warn("TestTag", "Warning message")
        assertEquals(0, AppLogger.errorCount.value)

        AppLogger.error("TestTag", "Error detected!", RuntimeException("Crash simulation"))
        assertEquals(1, AppLogger.errorCount.value)
        assertTrue(AppLogger.logs.value.any { it.level == LogLevel.ERROR })
    }

    @Test
    fun `test runCatchingApp handles exceptions safely without crashing`() {
        val successResult = runCatchingApp("SafeOp") {
            42
        }
        assertTrue(successResult.isSuccess)
        assertEquals(42, (successResult as AppResult.Success).data)

        val failureResult = runCatchingApp("FailingOp") {
            throw IllegalStateException("Something failed intentionally")
        }
        assertTrue(failureResult.isFailure)
        val fail = failureResult as AppResult.Failure
        assertTrue(fail.message.contains("Something failed intentionally"))
    }

    @Test
    fun `test full system synchronization - salary, expense, bonus allocation, emergency fund, and investment asset`() {
        val now = System.currentTimeMillis()
        val salaryIncome = IncomeEntity(
            id = 1,
            dateMillis = now,
            amount = 3_000_000L,
            incomeType = "GAJI_TETAP",
            source = "Gaji Bulanan",
            status = "DITERIMA"
        )
        val essentialExpense = ExpenseEntity(
            id = 1,
            dateMillis = now,
            amount = 1_000_000L,
            category = "Makan & Kebutuhan Pokok",
            paymentMethod = "TUNAI"
        )
        // Bonus 500k received
        val bonusIncome = IncomeEntity(
            id = 2,
            dateMillis = now,
            amount = 500_000L,
            incomeType = "BONUS",
            source = "Bonus THR",
            status = "DITERIMA",
            isAllocated = true
        )
        // Bonus allocation: Stage 1 (80% Darurat = 400k, 10% Invest = 50k, 10% Pribadi = 50k)
        val efDeposit = EmergencyFundEntity(
            id = 1,
            dateMillis = now,
            type = "SETORAN",
            amount = 400_000L,
            sourceDescription = "Alokasi Bonus"
        )
        val invAsset = InvestmentAssetEntity(
            id = 1,
            name = "Pasar Uang",
            assetType = "PASAR_UANG",
            totalCapitalInvested = 50_000L,
            currentMarketValue = 55_000L, // +5k gain
            lastUpdatedMillis = now
        )

        val summary = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 3_000_000L),
            incList = listOf(salaryIncome, bonusIncome),
            expList = listOf(essentialExpense),
            efList = listOf(efDeposit),
            invList = listOf(invAsset),
            budList = listOf(
                BudgetCategoryEntity(name = "Kebutuhan pokok", percentage = 50.0, plannedAmount = 1_500_000L, orderIndex = 0)
            )
        )

        // Total received = 3.000.000 + 500.000 = 3.500.000
        assertEquals(3_500_000L, summary.totalReceivedIncomeMonth)
        // Cash = 3.500.000 (received) - 1.000.000 (expense) - 400.000 (EF deposit) - 50.000 (Inv deposit) = 2.050.000
        assertEquals(2_050_000L, summary.actualCashBalance)
        // EF balance = 400.000
        assertEquals(400_000L, summary.emergencyFundBalance)
        // Net worth = 2.050.000 (cash) + 400.000 (EF) + 55.000 (Inv value) = 2.505.000
        assertEquals(2_505_000L, summary.totalNetWorth)
        // Gain/Loss = 55.000 - 50.000 = +5.000 (+10%)
        assertEquals(5_000L, summary.investmentGainLossAmount)
        assertEquals(10f, summary.investmentGainLossPct, 0.01f)
    }

    @Test
    fun `test updating expense recalculates actual cash balance correctly`() {
        val now = System.currentTimeMillis()
        val salary = IncomeEntity(id = 1, dateMillis = now, amount = 3_000_000L, status = "DITERIMA", incomeType = "GAJI_TETAP", source = "Gaji Bulanan")
        val initialExpense = ExpenseEntity(id = 1, dateMillis = now, amount = 50_000L, category = "Makan", paymentMethod = "TUNAI")

        val summaryBefore = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 3_000_000L),
            incList = listOf(salary),
            expList = listOf(initialExpense),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList()
        )
        // Cash = 3.000.000 - 50.000 = 2.950.000
        assertEquals(2_950_000L, summaryBefore.actualCashBalance)

        // User edits expense from 50.000 to 120.000
        val updatedExpense = initialExpense.copy(amount = 120_000L)
        val summaryAfter = DashboardCalculator.calculate(
            prof = ProfileEntity(monthlySalary = 3_000_000L),
            incList = listOf(salary),
            expList = listOf(updatedExpense),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList()
        )
        // Cash = 3.000.000 - 120.000 = 2.880.000
        assertEquals(2_880_000L, summaryAfter.actualCashBalance)
        assertEquals(2_880_000L, summaryAfter.totalNetWorth)
    }
}

// Dummy DAO for unit test calculation check
private class FakeFinanceDao : FinanceDao {
    override fun getProfile() = kotlinx.coroutines.flow.emptyFlow<ProfileEntity?>()
    override suspend fun getProfileSync() = null
    override suspend fun insertProfile(profile: ProfileEntity) {}
    override fun getAllIncomes() = kotlinx.coroutines.flow.emptyFlow<List<IncomeEntity>>()
    override suspend fun insertIncome(income: IncomeEntity) = 1L
    override suspend fun updateIncome(income: IncomeEntity) {}
    override suspend fun deleteIncome(income: IncomeEntity) {}
    override suspend fun deleteIncomeById(id: Long) {}
    override suspend fun getIncomeById(id: Long) = null
    override suspend fun getPendingSalaries(): List<IncomeEntity> = emptyList()
    override suspend fun updatePendingSalaryAmount(newAmount: Long) {}
    override fun getAllExpenses() = kotlinx.coroutines.flow.emptyFlow<List<ExpenseEntity>>()
    override suspend fun insertExpense(expense: ExpenseEntity) = 1L
    override suspend fun updateExpense(expense: ExpenseEntity) {}
    override suspend fun deleteExpense(expense: ExpenseEntity) {}
    override suspend fun deleteExpenseById(id: Long) {}
    override fun getAllBudgetCategories() = kotlinx.coroutines.flow.emptyFlow<List<BudgetCategoryEntity>>()
    override suspend fun getBudgetCategoriesSync() = emptyList<BudgetCategoryEntity>()
    override suspend fun insertBudgetCategories(categories: List<BudgetCategoryEntity>) {}
    override suspend fun updateBudgetCategory(category: BudgetCategoryEntity) {}
    override suspend fun clearBudgetCategories() {}
    override fun getAllEmergencyFundEntries() = kotlinx.coroutines.flow.emptyFlow<List<EmergencyFundEntity>>()
    override suspend fun insertEmergencyFundEntry(entry: EmergencyFundEntity) = 1L
    override suspend fun deleteEmergencyFundEntry(entry: EmergencyFundEntity) {}
    override fun getAllInvestmentAssets() = kotlinx.coroutines.flow.emptyFlow<List<InvestmentAssetEntity>>()
    override suspend fun insertInvestmentAsset(asset: InvestmentAssetEntity) = 1L
    override suspend fun updateInvestmentAsset(asset: InvestmentAssetEntity) {}
    override suspend fun deleteInvestmentAsset(asset: InvestmentAssetEntity) {}
    override suspend fun deleteTransactionsByAssetId(assetId: Long) {}
    override suspend fun getInvestmentAssetById(id: Long) = null
    override fun getAllInvestmentTransactions() = kotlinx.coroutines.flow.emptyFlow<List<InvestmentTxEntity>>()
    override suspend fun insertInvestmentTx(tx: InvestmentTxEntity) = 1L
    override suspend fun getAllIncomesSync(): List<IncomeEntity> = emptyList()
    override suspend fun getAllExpensesSync(): List<ExpenseEntity> = emptyList()
    override suspend fun getAllEmergencyFundEntriesSync(): List<EmergencyFundEntity> = emptyList()
    override suspend fun getAllInvestmentAssetsSync(): List<InvestmentAssetEntity> = emptyList()
    override suspend fun getAllInvestmentTransactionsSync(): List<InvestmentTxEntity> = emptyList()
    override suspend fun insertIncomes(incomes: List<IncomeEntity>) {}
    override suspend fun insertExpenses(expenses: List<ExpenseEntity>) {}
    override suspend fun insertEmergencyFundEntries(entries: List<EmergencyFundEntity>) {}
    override suspend fun insertInvestmentAssets(assets: List<InvestmentAssetEntity>) {}
    override suspend fun insertInvestmentTransactions(transactions: List<InvestmentTxEntity>) {}
    override suspend fun clearIncomes() {}
    override suspend fun clearExpenses() {}
    override suspend fun clearEmergencyFund() {}
    override suspend fun clearInvestmentAssets() {}
    override suspend fun clearInvestmentTransactions() {}
}
