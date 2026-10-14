package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets ORDER BY year DESC, month DESC")
    fun getAllMonthlyBudgets(): Flow<List<MonthlyBudgetEntity>>

    @Query("SELECT * FROM monthly_budgets WHERE month = :month AND year = :year LIMIT 1")
    fun getBudgetForMonth(month: Int, year: Int): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForMonthSync(month: Int, year: Int): MonthlyBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudget(budget: MonthlyBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudgets(budgets: List<MonthlyBudgetEntity>)

    @Update
    suspend fun updateMonthlyBudget(budget: MonthlyBudgetEntity)

    @Delete
    suspend fun deleteMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("DELETE FROM monthly_budgets WHERE id = :id")
    suspend fun deleteMonthlyBudgetById(id: Long)

    @Query("DELETE FROM monthly_budgets")
    suspend fun clearAllMonthlyBudgets()

    @Query("SELECT * FROM monthly_budgets ORDER BY year DESC, month DESC")
    suspend fun getAllMonthlyBudgetsSync(): List<MonthlyBudgetEntity>
}
