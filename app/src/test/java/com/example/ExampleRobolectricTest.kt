package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.result.AppResult
import com.example.data.backup.DataBackupManager
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.ProfileEntity
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dompet Pintar", appName)
    }

    @Test
    fun `test full backup manager export and restore roundtrip`() = runBlocking {
        val now = System.currentTimeMillis()
        val testProfile = ProfileEntity(id = 1, monthlySalary = 4_000_000L, livingSituation = "Kost Mandiri")
        val testIncomes = mutableListOf(IncomeEntity(id = 1, dateMillis = now, amount = 4_000_000L, incomeType = "GAJI_TETAP", source = "Gaji", status = "DITERIMA"))
        val testExpenses = mutableListOf(ExpenseEntity(id = 1, dateMillis = now, amount = 250_000L, category = "Makan", paymentMethod = "TUNAI"))
        val testEf = mutableListOf(EmergencyFundEntity(id = 1, dateMillis = now, type = "SETORAN", amount = 300_000L, sourceDescription = "Gaji"))
        val testAssets = mutableListOf(InvestmentAssetEntity(id = 1, name = "Reksadana", assetType = "PASAR_UANG", totalCapitalInvested = 100_000L, currentMarketValue = 105_000L, lastUpdatedMillis = now))

        var storedProfile: ProfileEntity? = testProfile
        val inMemoryDao = object : FinanceDao {
            override fun getProfile() = emptyFlow<ProfileEntity?>()
            override suspend fun getProfileSync() = storedProfile
            override suspend fun insertProfile(profile: ProfileEntity) { storedProfile = profile }
            override fun getAllIncomes() = emptyFlow<List<IncomeEntity>>()
            override suspend fun insertIncome(income: IncomeEntity) = 1L
            override suspend fun updateIncome(income: IncomeEntity) {}
            override suspend fun deleteIncome(income: IncomeEntity) {}
            override suspend fun deleteIncomeById(id: Long) {}
            override suspend fun getIncomeById(id: Long) = null
            override suspend fun getPendingSalaries(): List<IncomeEntity> = emptyList()
            override suspend fun updatePendingSalaryAmount(newAmount: Long) {}
            override fun getAllExpenses() = emptyFlow<List<ExpenseEntity>>()
            override suspend fun insertExpense(expense: ExpenseEntity) = 1L
            override suspend fun updateExpense(expense: ExpenseEntity) {}
            override suspend fun deleteExpense(expense: ExpenseEntity) {}
            override suspend fun deleteExpenseById(id: Long) {}
            override fun getAllBudgetCategories() = emptyFlow<List<com.example.data.local.BudgetCategoryEntity>>()
            override suspend fun getBudgetCategoriesSync() = emptyList<com.example.data.local.BudgetCategoryEntity>()
            override suspend fun insertBudgetCategories(categories: List<com.example.data.local.BudgetCategoryEntity>) {}
            override suspend fun updateBudgetCategory(category: com.example.data.local.BudgetCategoryEntity) {}
            override suspend fun clearBudgetCategories() {}
            override fun getAllEmergencyFundEntries() = emptyFlow<List<EmergencyFundEntity>>()
            override suspend fun insertEmergencyFundEntry(entry: EmergencyFundEntity) = 1L
            override suspend fun deleteEmergencyFundEntry(entry: EmergencyFundEntity) {}
            override fun getAllInvestmentAssets() = emptyFlow<List<InvestmentAssetEntity>>()
            override suspend fun insertInvestmentAsset(asset: InvestmentAssetEntity) = 1L
            override suspend fun updateInvestmentAsset(asset: InvestmentAssetEntity) {}
            override suspend fun deleteInvestmentAsset(asset: InvestmentAssetEntity) {}
            override suspend fun deleteTransactionsByAssetId(assetId: Long) {}
            override suspend fun getInvestmentAssetById(id: Long) = null
            override fun getAllInvestmentTransactions() = emptyFlow<List<InvestmentTxEntity>>()
            override suspend fun insertInvestmentTx(tx: InvestmentTxEntity) = 1L
            override suspend fun getAllIncomesSync() = testIncomes.toList()
            override suspend fun getAllExpensesSync() = testExpenses.toList()
            override suspend fun getAllEmergencyFundEntriesSync() = testEf.toList()
            override suspend fun getAllInvestmentAssetsSync() = testAssets.toList()
            override suspend fun getAllInvestmentTransactionsSync() = emptyList<InvestmentTxEntity>()
            override suspend fun clearIncomes() { testIncomes.clear() }
            override suspend fun clearExpenses() { testExpenses.clear() }
            override suspend fun clearEmergencyFund() { testEf.clear() }
            override suspend fun clearInvestmentAssets() { testAssets.clear() }
            override suspend fun clearInvestmentTransactions() {}
            override suspend fun insertIncomes(incomes: List<IncomeEntity>) { testIncomes.addAll(incomes) }
            override suspend fun insertExpenses(expenses: List<ExpenseEntity>) { testExpenses.addAll(expenses) }
            override suspend fun insertEmergencyFundEntries(entries: List<EmergencyFundEntity>) { testEf.addAll(entries) }
            override suspend fun insertInvestmentAssets(assets: List<InvestmentAssetEntity>) { testAssets.addAll(assets) }
            override suspend fun insertInvestmentTransactions(transactions: List<InvestmentTxEntity>) {}
        }

        // 1. Export
        val exportResult = DataBackupManager.exportDataAsJson(inMemoryDao)
        assertTrue(exportResult.isSuccess)
        val json = (exportResult as AppResult.Success).data
        assertTrue(json.contains("DompetPintar"))
        assertTrue(json.contains("4000000"))

        // 2. Clear state in memory
        testIncomes.clear()
        testExpenses.clear()
        testEf.clear()
        testAssets.clear()
        assertEquals(0, testIncomes.size)

        // 3. Restore
        val restoreResult = DataBackupManager.restoreDataFromJson(inMemoryDao, json)
        assertTrue(restoreResult.isSuccess)
        assertEquals(1, testIncomes.size)
        assertEquals(4_000_000L, testIncomes[0].amount)
        assertEquals(1, testExpenses.size)
        assertEquals(250_000L, testExpenses[0].amount)
        assertEquals(1, testEf.size)
        assertEquals(300_000L, testEf[0].amount)
        assertEquals(1, testAssets.size)
        assertEquals("Reksadana", testAssets[0].name)
    }
}
