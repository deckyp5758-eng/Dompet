package com.example.data.local

import android.content.Context

/**
 * DatabaseProvider singleton module providing application-wide access to AppDatabase and its DAOs.
 * Implements thread-safe singleton initialization and provides direct DAO access points.
 */
object DatabaseProvider {
    @Volatile
    private var databaseInstance: AppDatabase? = null

    /**
     * Menyediakan instance AppDatabase singleton.
     */
    fun provideDatabase(context: Context): AppDatabase {
        return databaseInstance ?: synchronized(this) {
            databaseInstance ?: AppDatabase.getDatabase(context.applicationContext).also {
                databaseInstance = it
            }
        }
    }

    fun provideDatabase(): AppDatabase {
        return databaseInstance ?: throw IllegalStateException("Database not initialized. Call provideDatabase(context) first.")
    }

    /**
     * Menyediakan TransactionDao untuk riwayat transaksi.
     */
    fun provideTransactionDao(context: Context): TransactionDao {
        return provideDatabase(context).transactionDao()
    }

    /**
     * Menyediakan MonthlyBudgetDao untuk perencanaan anggaran bulanan.
     */
    fun provideMonthlyBudgetDao(context: Context): MonthlyBudgetDao {
        return provideDatabase(context).monthlyBudgetDao()
    }

    /**
     * Menyediakan SavingsGoalDao untuk pengelolaan target tabungan.
     */
    fun provideSavingsGoalDao(context: Context): SavingsGoalDao {
        return provideDatabase(context).savingsGoalDao()
    }

    /**
     * Menyediakan FinanceDao utama aplikasi.
     */
    fun provideFinanceDao(context: Context): FinanceDao {
        return provideDatabase(context).financeDao()
    }

    /**
     * Helper untuk pengujian (mengganti atau mereset instance database saat unit testing).
     */
    fun setDatabaseForTesting(database: AppDatabase?) {
        synchronized(this) {
            databaseInstance = database
        }
    }
}
