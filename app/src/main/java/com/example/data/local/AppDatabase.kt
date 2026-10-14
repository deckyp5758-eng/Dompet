package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migrasi dari Versi 1 ke Versi 2:
         * Menambahkan tabel investment_assets dan investment_transactions
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `investment_assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `assetType` TEXT NOT NULL,
                        `totalCapitalInvested` INTEGER NOT NULL,
                        `currentMarketValue` INTEGER NOT NULL,
                        `lastUpdatedMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `investment_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migrasi dari Versi 2 ke Versi 3:
         * Menambahkan kolom alokasi pada tabel incomes jika belum ada
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `incomes` ADD COLUMN `isAllocated` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `incomes` ADD COLUMN `allocationDetails` TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        /**
         * Migrasi dari Versi 3 ke Versi 4:
         * Menambahkan tabel transaction_history, monthly_budgets, dan savings_goals secara aman
         * tanpa menghapus data yang sudah ada di tabel sebelumnya.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL DEFAULT 'TUNAI',
                        `dateMillis` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `relatedEntityId` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `totalBudget` INTEGER NOT NULL,
                        `allocatedNeeds` INTEGER NOT NULL DEFAULT 0,
                        `allocatedWants` INTEGER NOT NULL DEFAULT 0,
                        `allocatedSavings` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmount` INTEGER NOT NULL,
                        `currentAmount` INTEGER NOT NULL DEFAULT 0,
                        `targetDateMillis` INTEGER NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'Tabungan',
                        `priority` TEXT NOT NULL DEFAULT 'SEDANG',
                        `iconName` TEXT NOT NULL DEFAULT 'savings',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dompet_pintar_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // PADA INSTALASI BARU: Buat profil awal bersih dengan saldo Rp0 dan nilai default kosong.
                // TIDAK ADA data transaksi, gaji palsu, tabungan contoh, atau investasi yang otomatis dimasukkan.
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO user_profile (
                        id, monthlySalary, livingSituation, initialSavings, initialDebt,
                        financialPriority, emergencyTargetMonths, currency, timezone,
                        paydayMode, paydayDayOfMonth
                    ) VALUES (
                        1, 0, '', 0, 0, '', 3, 'IDR', 'Asia/Jakarta', 'END_OF_MONTH', 25
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * FUNGSI KHUSUS MODE DEMO: Hanya dipanggil secara manual jika pengguna sengaja mengaktifkan mode demo
         * atau di dalam unit test. Jangan dipanggil otomatis saat inisialisasi aplikasi normal.
         */
        suspend fun populateDemoData(
            dao: FinanceDao,
            transactionDao: TransactionDao? = null,
            monthlyBudgetDao: MonthlyBudgetDao? = null,
            savingsGoalDao: SavingsGoalDao? = null
        ) {
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

            dao.insertIncome(
                IncomeEntity(
                    dateMillis = System.currentTimeMillis(),
                    amount = 2_700_000L,
                    incomeType = "GAJI_TETAP",
                    source = "Gaji Tetap Bulanan (Demo)",
                    status = "DITERIMA",
                    notes = "Data contoh untuk demonstrasi fitur"
                )
            )

            monthlyBudgetDao?.let { mDao ->
                val cal = java.util.Calendar.getInstance()
                val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
                val currentYear = cal.get(java.util.Calendar.YEAR)
                mDao.insertMonthlyBudget(
                    MonthlyBudgetEntity(
                        month = currentMonth,
                        year = currentYear,
                        totalBudget = 2_700_000L,
                        allocatedNeeds = 1_620_000L,
                        allocatedWants = 405_000L,
                        allocatedSavings = 675_000L,
                        notes = "Anggaran bulanan demo"
                    )
                )
            }

            savingsGoalDao?.let { sDao ->
                val now = System.currentTimeMillis()
                val sixMonthsLater = now + (180L * 24 * 60 * 60 * 1000)
                sDao.insertSavingsGoal(
                    SavingsGoalEntity(
                        title = "Laptop Kerja (Demo)",
                        targetAmount = 8_000_000L,
                        currentAmount = 1_500_000L,
                        targetDateMillis = sixMonthsLater,
                        category = "Produktivitas",
                        priority = "TINGGI",
                        iconName = "laptop",
                        isCompleted = false,
                        notes = "Target tabungan demo"
                    )
                )
            }

            transactionDao?.let { tDao ->
                tDao.insertTransaction(
                    TransactionEntity(
                        title = "Gaji Tetap (Demo)",
                        amount = 2_700_000L,
                        type = "INCOME",
                        category = "Gaji Tetap",
                        paymentMethod = "TRANSFER_BANK",
                        notes = "Transaksi riwayat demo"
                    )
                )
            }
        }
    }
}
