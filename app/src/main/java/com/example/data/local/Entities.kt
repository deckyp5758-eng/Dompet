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
    val monthlySalary: Long = 2_700_000L,
    val livingSituation: String = "Tinggal bersama keluarga",
    val initialSavings: Long = 0L,
    val initialDebt: Long = 0L,
    val financialPriority: String = "Investasi jangka panjang",
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
