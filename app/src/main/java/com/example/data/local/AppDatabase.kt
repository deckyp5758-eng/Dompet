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
        InvestmentTxEntity::class,
        TransactionEntity::class,
        MonthlyBudgetEntity::class,
        SavingsGoalEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao

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
                    .fallbackToDestructiveMigration()
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
                        populateInitialData(
                            database.financeDao(),
                            database.transactionDao(),
                            database.monthlyBudgetDao(),
                            database.savingsGoalDao()
                        )
                    }
                }
            }
        }

        suspend fun populateInitialData(
            dao: FinanceDao,
            transactionDao: TransactionDao? = null,
            monthlyBudgetDao: MonthlyBudgetDao? = null,
            savingsGoalDao: SavingsGoalDao? = null
        ) {
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
                    timezone = "Asia/Jakarta",
                    paydayMode = PaydayMode.END_OF_MONTH,
                    paydayDayOfMonth = 25
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

            // Initial Monthly Budget
            monthlyBudgetDao?.let { mDao ->
                val cal = java.util.Calendar.getInstance()
                val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
                val currentYear = cal.get(java.util.Calendar.YEAR)
                mDao.insertMonthlyBudget(
                    MonthlyBudgetEntity(
                        month = currentMonth,
                        year = currentYear,
                        totalBudget = 2_700_000L,
                        allocatedNeeds = 1_620_000L, // 60%
                        allocatedWants = 405_000L,   // 15%
                        allocatedSavings = 675_000L, // 25%
                        notes = "Anggaran bulanan default berdasarkan strategi alokasi Dompet Pintar"
                    )
                )
            }

            // Initial Savings Goals
            savingsGoalDao?.let { sDao ->
                val now = System.currentTimeMillis()
                val sixMonthsLater = now + (180L * 24 * 60 * 60 * 1000)
                val threeMonthsLater = now + (90L * 24 * 60 * 60 * 1000)

                sDao.insertSavingsGoals(
                    listOf(
                        SavingsGoalEntity(
                            title = "Laptop Kerja & Produktivitas",
                            targetAmount = 8_000_000L,
                            currentAmount = 1_500_000L,
                            targetDateMillis = sixMonthsLater,
                            category = "Produktivitas",
                            priority = "TINGGI",
                            iconName = "laptop",
                            isCompleted = false,
                            notes = "Upgrade laptop untuk mendukung pekerjaan dan freelance"
                        ),
                        SavingsGoalEntity(
                            title = "Liburan & Pulang Kampung",
                            targetAmount = 2_000_000L,
                            currentAmount = 600_000L,
                            targetDateMillis = threeMonthsLater,
                            category = "Keluarga & Liburan",
                            priority = "SEDANG",
                            iconName = "flight",
                            isCompleted = false,
                            notes = "Tabungan tiket dan oleh-oleh untuk keluarga"
                        )
                    )
                )
            }

            // Initial Transaction History Entries
            transactionDao?.let { tDao ->
                tDao.insertTransactions(
                    listOf(
                        TransactionEntity(
                            title = "Gaji Tetap Bulanan",
                            amount = 2_700_000L,
                            type = "INCOME",
                            category = "Gaji Tetap",
                            paymentMethod = "TRANSFER_BANK",
                            notes = "Gaji pokok bulanan awal"
                        ),
                        TransactionEntity(
                            title = "Alokasi Awal Tabungan Laptop",
                            amount = 250_000L,
                            type = "SAVINGS",
                            category = "Tabungan",
                            paymentMethod = "TRANSFER_BANK",
                            notes = "Setoran otomatis ke target tabungan laptop"
                        )
                    )
                )
            }
        }
    }
}

