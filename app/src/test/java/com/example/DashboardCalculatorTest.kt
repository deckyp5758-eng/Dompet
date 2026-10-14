package com.example

import com.example.data.calculator.DashboardCalculator
import com.example.data.calculator.DashboardSummary
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DashboardCalculatorTest {

    @Test
    fun testInitialEmptyState() {
        val profile = ProfileEntity(monthlySalary = 0L)
        val summary = DashboardCalculator.calculate(
            prof = profile,
            incList = emptyList(),
            expList = emptyList(),
            efList = emptyList(),
            invList = emptyList(),
            budList = emptyList()
        )

        assertEquals(0L, summary.actualCashBalance)
        assertEquals(0L, summary.totalNetWorth)
        assertEquals(0L, summary.emergencyFundBalance)
        assertEquals(1, summary.emergencyStage)
    }

    @Test
    fun testIncomeAndExpenseCalculation() {
        val profile = ProfileEntity(monthlySalary = 3_000_000L)
        val now = Calendar.getInstance().timeInMillis

        val incomes = listOf(
            IncomeEntity(
                dateMillis = now,
                amount = 3_000_000L,
                incomeType = "GAJI_TETAP",
                source = "Gaji Bulanan",
                status = "DITERIMA"
            )
        )

        val expenses = listOf(
            ExpenseEntity(
                dateMillis = now,
                amount = 500_000L,
                category = "Makan & Kebutuhan Pokok",
                paymentMethod = "TUNAI"
            )
        )

        val budgetCategories = listOf(
            BudgetCategoryEntity(
                name = "Kebutuhan pokok dan kontribusi keluarga",
                percentage = 50.0,
                plannedAmount = 1_500_000L,
                orderIndex = 0
            )
        )

        val summary = DashboardCalculator.calculate(
            prof = profile,
            incList = incomes,
            expList = expenses,
            efList = emptyList(),
            invList = emptyList(),
            budList = budgetCategories
        )

        assertEquals(2_500_000L, summary.actualCashBalance)
        assertEquals(3_000_000L, summary.totalReceivedIncomeMonth)
        assertEquals(500_000L, summary.totalActualExpensesMonth)
    }

    @Test
    fun testEmergencyFundAndInvestmentSummary() {
        val profile = ProfileEntity(monthlySalary = 5_000_000L)
        val now = Calendar.getInstance().timeInMillis

        val efEntries = listOf(
            EmergencyFundEntity(
                dateMillis = now,
                type = "SETORAN",
                amount = 1_200_000L,
                sourceDescription = "Tabungan Awal"
            )
        )

        val assets = listOf(
            InvestmentAssetEntity(
                name = "Reksadana",
                assetType = "PASAR_UANG",
                totalCapitalInvested = 1_000_000L,
                currentMarketValue = 1_100_000L,
                lastUpdatedMillis = now
            )
        )

        val summary = DashboardCalculator.calculate(
            prof = profile,
            incList = emptyList(),
            expList = emptyList(),
            efList = efEntries,
            invList = assets,
            budList = emptyList()
        )

        assertEquals(1_200_000L, summary.emergencyFundBalance)
        assertEquals(2, summary.emergencyStage) // Berada di tahap 2 karena > 1.000.000
        assertEquals(1_100_000L, summary.investmentCurrentValue)
        assertEquals(100_000L, summary.investmentGainLossAmount)
    }
}
