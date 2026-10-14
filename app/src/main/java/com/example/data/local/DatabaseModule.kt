package com.example.data.local

import android.content.Context

/**
 * Modul abstraksi penyedia database dan DAO untuk dependency injection dan pengujian independen.
 */
interface DatabaseModule {
    fun getDatabase(context: Context): AppDatabase
    fun getTransactionDao(context: Context): TransactionDao
    fun getMonthlyBudgetDao(context: Context): MonthlyBudgetDao
    fun getSavingsGoalDao(context: Context): SavingsGoalDao
    fun getFinanceDao(context: Context): FinanceDao
}

/**
 * Implementasi default DatabaseModule yang mendelegasikan pemanggilan ke DatabaseProvider.
 */
class DefaultDatabaseModule : DatabaseModule {
    override fun getDatabase(context: Context): AppDatabase =
        DatabaseProvider.provideDatabase(context)

    override fun getTransactionDao(context: Context): TransactionDao =
        DatabaseProvider.provideTransactionDao(context)

    override fun getMonthlyBudgetDao(context: Context): MonthlyBudgetDao =
        DatabaseProvider.provideMonthlyBudgetDao(context)

    override fun getSavingsGoalDao(context: Context): SavingsGoalDao =
        DatabaseProvider.provideSavingsGoalDao(context)

    override fun getFinanceDao(context: Context): FinanceDao =
        DatabaseProvider.provideFinanceDao(context)
}
