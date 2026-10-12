package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // Incomes
    @Query("SELECT * FROM incomes ORDER BY dateMillis DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long)

    @Query("SELECT * FROM incomes WHERE id = :id LIMIT 1")
    suspend fun getIncomeById(id: Long): IncomeEntity?

    @Query("SELECT * FROM incomes WHERE incomeType = 'GAJI_TETAP' AND status = 'BELUM_DITERIMA'")
    suspend fun getPendingSalaries(): List<IncomeEntity>

    @Query("UPDATE incomes SET amount = :newAmount WHERE incomeType = 'GAJI_TETAP' AND status = 'BELUM_DITERIMA'")
    suspend fun updatePendingSalaryAmount(newAmount: Long)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    // Budget Categories
    @Query("SELECT * FROM budget_categories ORDER BY orderIndex ASC")
    fun getAllBudgetCategories(): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories ORDER BY orderIndex ASC")
    suspend fun getBudgetCategoriesSync(): List<BudgetCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategories(categories: List<BudgetCategoryEntity>)

    @Update
    suspend fun updateBudgetCategory(category: BudgetCategoryEntity)

    @Query("DELETE FROM budget_categories")
    suspend fun clearBudgetCategories()

    // Emergency Fund History
    @Query("SELECT * FROM emergency_fund_history ORDER BY dateMillis DESC")
    fun getAllEmergencyFundEntries(): Flow<List<EmergencyFundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyFundEntry(entry: EmergencyFundEntity): Long

    @Delete
    suspend fun deleteEmergencyFundEntry(entry: EmergencyFundEntity)

    // Investment Assets
    @Query("SELECT * FROM investment_assets ORDER BY id ASC")
    fun getAllInvestmentAssets(): Flow<List<InvestmentAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestmentAsset(asset: InvestmentAssetEntity): Long

    @Update
    suspend fun updateInvestmentAsset(asset: InvestmentAssetEntity)

    @Delete
    suspend fun deleteInvestmentAsset(asset: InvestmentAssetEntity)

    @Query("DELETE FROM investment_transactions WHERE assetId = :assetId")
    suspend fun deleteTransactionsByAssetId(assetId: Long)

    @Query("SELECT * FROM investment_assets WHERE id = :id LIMIT 1")
    suspend fun getInvestmentAssetById(id: Long): InvestmentAssetEntity?

    // Investment Transactions
    @Query("SELECT * FROM investment_transactions ORDER BY dateMillis DESC")
    fun getAllInvestmentTransactions(): Flow<List<InvestmentTxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestmentTx(tx: InvestmentTxEntity): Long

    // Clear All (for reset/restore)
    @Query("DELETE FROM incomes")
    suspend fun clearIncomes()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM emergency_fund_history")
    suspend fun clearEmergencyFund()

    @Query("DELETE FROM investment_assets")
    suspend fun clearInvestmentAssets()

    @Query("DELETE FROM investment_transactions")
    suspend fun clearInvestmentTransactions()
}
