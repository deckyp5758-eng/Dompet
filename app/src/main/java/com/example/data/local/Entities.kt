package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object PaydayMode {
    const val START_OF_MONTH = "START_OF_MONTH"
    const val END_OF_MONTH = "END_OF_MONTH"
    const val SPECIFIC_DAY = "SPECIFIC_DAY"
}

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val monthlySalary: Long = 0L,
    val livingSituation: String = "",
    val initialSavings: Long = 0L,
    val initialDebt: Long = 0L,
    val financialPriority: String = "",
    val emergencyTargetMonths: Int = 3,
    val currency: String = "IDR",
    val timezone: String = "Asia/Jakarta",
    val paydayMode: String = PaydayMode.END_OF_MONTH,
    val paydayDayOfMonth: Int = 25
)

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val amount: Long,
    val incomeType: String,
    val source: String,
    val status: String,
    val notes: String = "",
    val isAllocated: Boolean = false,
    val allocationDetails: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val amount: Long,
    val category: String,
    val paymentMethod: String,
    val notes: String = ""
)

@Entity(tableName = "budget_categories")
data class BudgetCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val percentage: Double,
    val plannedAmount: Long,
    val orderIndex: Int
)

@Entity(tableName = "emergency_fund_history")
data class EmergencyFundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val type: String, // SETORAN, PENARIKAN
    val amount: Long,
    val sourceDescription: String,
    val notes: String = ""
)

@Entity(tableName = "investment_assets")
data class InvestmentAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val assetType: String,
    val totalCapitalInvested: Long,
    val currentMarketValue: Long,
    val lastUpdatedMillis: Long,
    val notes: String = ""
)

@Entity(tableName = "investment_transactions")
data class InvestmentTxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val dateMillis: Long,
    val type: String, // SETORAN, PENARIKAN
    val amount: Long,
    val notes: String = ""
)

/**
 * Entitas untuk riwayat transaksi keuangan umum (Income, Expense, Transfer, Savings).
 */
@Entity(tableName = "transaction_history")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER", "SAVINGS"
    val category: String,
    val paymentMethod: String = "TUNAI",
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val relatedEntityId: Long? = null
)
typealias TransactionHistoryEntity = TransactionEntity

/**
 * Entitas untuk target dan anggaran bulanan.
 */
@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int, // 1 - 12
    val year: Int,  // Contoh: 2026
    val totalBudget: Long,
    val allocatedNeeds: Long = 0L,
    val allocatedWants: Long = 0L,
    val allocatedSavings: Long = 0L,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Entitas untuk target tabungan (Savings Goals).
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val currentAmount: Long = 0L,
    val targetDateMillis: Long,
    val category: String = "Tabungan",
    val priority: String = "SEDANG", // "RENDAH", "SEDANG", "TINGGI"
    val iconName: String = "savings",
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

