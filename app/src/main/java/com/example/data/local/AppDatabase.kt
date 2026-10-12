package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        IncomeEntity::class,
        ExpenseEntity::class,
        BudgetCategoryEntity::class,
        EmergencyFundEntity::class,
        InvestmentAssetEntity::class,
        InvestmentTxEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dompet_pintar_db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.financeDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: FinanceDao) {
            // Profile
            dao.insertProfile(
                ProfileEntity(
                    id = 1,
                    monthlySalary = 2_700_000L,
                    livingSituation = "Tinggal bersama keluarga",
                    initialSavings = 0L,
                    initialDebt = 0L,
                    financialPriority = "Investasi jangka panjang",
                    emergencyTargetMonths = 3,
                    currency = "IDR",
                    timezone = "Asia/Jakarta"
                )
            )

            // Initial Budget Categories (Exact 100% of Rp 2.700.000)
            val defaultCategories = listOf(
                BudgetCategoryEntity(
                    name = "Kebutuhan pokok dan kontribusi keluarga",
                    percentage = 45.0,
                    plannedAmount = 1_215_000L,
                    orderIndex = 0
                ),
                BudgetCategoryEntity(
                    name = "Transportasi, pulsa, dan tagihan",
                    percentage = 15.0,
                    plannedAmount = 405_000L,
                    orderIndex = 1
                ),
                BudgetCategoryEntity(
                    name = "Dana darurat",
                    percentage = 20.0,
                    plannedAmount = 540_000L,
                    orderIndex = 2
                ),
                BudgetCategoryEntity(
                    name = "Investasi",
                    percentage = 5.0,
                    plannedAmount = 135_000L,
                    orderIndex = 3
                ),
                BudgetCategoryEntity(
                    name = "Keperluan pribadi",
                    percentage = 10.0,
                    plannedAmount = 270_000L,
                    orderIndex = 4
                ),
                BudgetCategoryEntity(
                    name = "Cadangan fleksibel",
                    percentage = 5.0,
                    plannedAmount = 135_000L,
                    orderIndex = 5
                )
            )
            dao.insertBudgetCategories(defaultCategories)

            // Initial planned salary for current month: NOT YET RECEIVED (Status: BELUM_DITERIMA)
            dao.insertIncome(
                IncomeEntity(
                    dateMillis = System.currentTimeMillis(),
                    amount = 2_700_000L,
                    incomeType = "GAJI_TETAP",
                    source = "Gaji Tetap Bulanan (Terencana)",
                    status = "BELUM_DITERIMA",
                    notes = "Gaji bulanan terencana. Tandai diterima ketika dana sudah masuk ke rekening."
                )
            )
        }
    }
}
