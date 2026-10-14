package com.example.data.backup

import com.example.core.logger.AppLogger
import com.example.core.result.AppResult
import com.example.core.result.runCatchingApp
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.DatabaseProvider
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.MonthlyBudgetEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manajer ekspor dan pemulihan (restore) penuh data aplikasi Dompet Pintar.
 * Menjamin 100% data keuangan (profil, pos anggaran, pendapatan, pengeluaran,
 * dana darurat, portofolio investasi, riwayat transaksi, anggaran bulanan, dan target tabungan)
 * tercadangkan dan dapat dipulihkan secara aman dan atomik.
 */
object DataBackupManager {

    const val BACKUP_SCHEMA_VERSION = 2
    const val APP_IDENTIFIER = "DompetPintar"

    suspend fun exportDataAsJson(db: AppDatabase): AppResult<String> {
        return runCatchingApp("DataBackupExport") {
            val financeDao = db.financeDao()
            val transactionDao = db.transactionDao()
            val monthlyBudgetDao = db.monthlyBudgetDao()
            val savingsGoalDao = db.savingsGoalDao()

            val root = JSONObject()
            root.put("app", APP_IDENTIFIER)
            root.put("version", BACKUP_SCHEMA_VERSION)
            root.put("exportedAt", System.currentTimeMillis())

            // 1. Profile
            val profile = financeDao.getProfileSync() ?: ProfileEntity()
            root.put("profile", JSONObject().apply {
                put("monthlySalary", profile.monthlySalary)
                put("livingSituation", profile.livingSituation)
                put("initialSavings", profile.initialSavings)
                put("initialDebt", profile.initialDebt)
                put("financialPriority", profile.financialPriority)
                put("emergencyTargetMonths", profile.emergencyTargetMonths)
                put("currency", profile.currency)
                put("timezone", profile.timezone)
                put("paydayMode", profile.paydayMode)
                put("paydayDayOfMonth", profile.paydayDayOfMonth)
            })

            // 2. Budget Categories
            val budgetCategories = financeDao.getBudgetCategoriesSync()
            val catArray = JSONArray()
            budgetCategories.forEach { cat ->
                catArray.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("percentage", cat.percentage)
                    put("plannedAmount", cat.plannedAmount)
                    put("orderIndex", cat.orderIndex)
                })
            }
            root.put("budgetCategories", catArray)

            // 3. Incomes
            val incomes = financeDao.getAllIncomesSync()
            val incomeArray = JSONArray()
            incomes.forEach { inc ->
                incomeArray.put(JSONObject().apply {
                    put("id", inc.id)
                    put("dateMillis", inc.dateMillis)
                    put("amount", inc.amount)
                    put("incomeType", inc.incomeType)
                    put("source", inc.source)
                    put("status", inc.status)
                    put("isAllocated", inc.isAllocated)
                    put("allocationDetails", inc.allocationDetails)
                    put("notes", inc.notes)
                })
            }
            root.put("incomes", incomeArray)

            // 4. Expenses
            val expenses = financeDao.getAllExpensesSync()
            val expenseArray = JSONArray()
            expenses.forEach { exp ->
                expenseArray.put(JSONObject().apply {
                    put("id", exp.id)
                    put("dateMillis", exp.dateMillis)
                    put("amount", exp.amount)
                    put("category", exp.category)
                    put("paymentMethod", exp.paymentMethod)
                    put("notes", exp.notes)
                })
            }
            root.put("expenses", expenseArray)

            // 5. Emergency Fund Entries
            val emergencyEntries = financeDao.getAllEmergencyFundEntriesSync()
            val efArray = JSONArray()
            emergencyEntries.forEach { ef ->
                efArray.put(JSONObject().apply {
                    put("id", ef.id)
                    put("dateMillis", ef.dateMillis)
                    put("type", ef.type)
                    put("amount", ef.amount)
                    put("sourceDescription", ef.sourceDescription)
                    put("notes", ef.notes)
                })
            }
            root.put("emergencyFundEntries", efArray)

            // 6. Investment Assets
            val assets = financeDao.getAllInvestmentAssetsSync()
            val assetArray = JSONArray()
            assets.forEach { ast ->
                assetArray.put(JSONObject().apply {
                    put("id", ast.id)
                    put("name", ast.name)
                    put("assetType", ast.assetType)
                    put("totalCapitalInvested", ast.totalCapitalInvested)
                    put("currentMarketValue", ast.currentMarketValue)
                    put("lastUpdatedMillis", ast.lastUpdatedMillis)
                    put("notes", ast.notes)
                })
            }
            root.put("investmentAssets", assetArray)

            // 7. Investment Transactions
            val transactions = financeDao.getAllInvestmentTransactionsSync()
            val txArray = JSONArray()
            transactions.forEach { tx ->
                txArray.put(JSONObject().apply {
                    put("id", tx.id)
                    put("assetId", tx.assetId)
                    put("dateMillis", tx.dateMillis)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("notes", tx.notes)
                })
            }
            root.put("investmentTransactions", txArray)

            // 8. Transaction History (Baru di v2)
            val history = transactionDao.getAllTransactionsSync()
            val historyArray = JSONArray()
            history.forEach { item ->
                historyArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("amount", item.amount)
                    put("type", item.type)
                    put("category", item.category)
                    put("paymentMethod", item.paymentMethod)
                    put("dateMillis", item.dateMillis)
                    put("notes", item.notes)
                    if (item.relatedEntityId != null) {
                        put("relatedEntityId", item.relatedEntityId)
                    }
                })
            }
            root.put("transactionHistory", historyArray)

            // 9. Monthly Budgets (Baru di v2)
            val monthlyBudgets = monthlyBudgetDao.getAllMonthlyBudgetsSync()
            val mbArray = JSONArray()
            monthlyBudgets.forEach { mb ->
                mbArray.put(JSONObject().apply {
                    put("id", mb.id)
                    put("month", mb.month)
                    put("year", mb.year)
                    put("totalBudget", mb.totalBudget)
                    put("allocatedNeeds", mb.allocatedNeeds)
                    put("allocatedWants", mb.allocatedWants)
                    put("allocatedSavings", mb.allocatedSavings)
                    put("notes", mb.notes)
                    put("createdAtMillis", mb.createdAtMillis)
                })
            }
            root.put("monthlyBudgets", mbArray)

            // 10. Savings Goals (Baru di v2)
            val savingsGoals = savingsGoalDao.getAllSavingsGoalsSync()
            val sgArray = JSONArray()
            savingsGoals.forEach { sg ->
                sgArray.put(JSONObject().apply {
                    put("id", sg.id)
                    put("title", sg.title)
                    put("targetAmount", sg.targetAmount)
                    put("currentAmount", sg.currentAmount)
                    put("targetDateMillis", sg.targetDateMillis)
                    put("category", sg.category)
                    put("priority", sg.priority)
                    put("iconName", sg.iconName)
                    put("isCompleted", sg.isCompleted)
                    put("notes", sg.notes)
                    put("createdAtMillis", sg.createdAtMillis)
                })
            }
            root.put("savingsGoals", sgArray)

            // Summary metadata
            root.put("summary", JSONObject().apply {
                put("totalIncomes", incomes.size)
                put("totalExpenses", expenses.size)
                put("totalEmergencyEntries", emergencyEntries.size)
                put("totalInvestmentAssets", assets.size)
                put("totalTransactionHistory", history.size)
                put("totalMonthlyBudgets", monthlyBudgets.size)
                put("totalSavingsGoals", savingsGoals.size)
            })

            val jsonString = root.toString(2)
            AppLogger.info("DataBackupManager", "Berhasil mengekspor cadangan data penuh (${jsonString.length} bytes)")
            jsonString
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal mengekspor data ke JSON: ${error.message}", error.throwable)
        }
    }

    suspend fun exportDataAsJson(dao: FinanceDao): AppResult<String> {
        return runCatchingApp("DataBackupExport") {
            val root = JSONObject()
            root.put("app", APP_IDENTIFIER)
            root.put("version", BACKUP_SCHEMA_VERSION)
            root.put("exportedAt", System.currentTimeMillis())

            // 1. Profile
            val profile = dao.getProfileSync() ?: ProfileEntity()
            root.put("profile", JSONObject().apply {
                put("monthlySalary", profile.monthlySalary)
                put("livingSituation", profile.livingSituation)
                put("initialSavings", profile.initialSavings)
                put("initialDebt", profile.initialDebt)
                put("financialPriority", profile.financialPriority)
                put("emergencyTargetMonths", profile.emergencyTargetMonths)
                put("currency", profile.currency)
                put("timezone", profile.timezone)
                put("paydayMode", profile.paydayMode)
                put("paydayDayOfMonth", profile.paydayDayOfMonth)
            })

            // 2. Budget Categories
            val budgetCategories = dao.getBudgetCategoriesSync()
            val catArray = JSONArray()
            budgetCategories.forEach { cat ->
                catArray.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("percentage", cat.percentage)
                    put("plannedAmount", cat.plannedAmount)
                    put("orderIndex", cat.orderIndex)
                })
            }
            root.put("budgetCategories", catArray)

            // 3. Incomes
            val incomes = dao.getAllIncomesSync()
            val incomeArray = JSONArray()
            incomes.forEach { inc ->
                incomeArray.put(JSONObject().apply {
                    put("id", inc.id)
                    put("dateMillis", inc.dateMillis)
                    put("amount", inc.amount)
                    put("incomeType", inc.incomeType)
                    put("source", inc.source)
                    put("status", inc.status)
                    put("isAllocated", inc.isAllocated)
                    put("allocationDetails", inc.allocationDetails)
                    put("notes", inc.notes)
                })
            }
            root.put("incomes", incomeArray)

            // 4. Expenses
            val expenses = dao.getAllExpensesSync()
            val expenseArray = JSONArray()
            expenses.forEach { exp ->
                expenseArray.put(JSONObject().apply {
                    put("id", exp.id)
                    put("dateMillis", exp.dateMillis)
                    put("amount", exp.amount)
                    put("category", exp.category)
                    put("paymentMethod", exp.paymentMethod)
                    put("notes", exp.notes)
                })
            }
            root.put("expenses", expenseArray)

            // 5. Emergency Fund Entries
            val emergencyEntries = dao.getAllEmergencyFundEntriesSync()
            val efArray = JSONArray()
            emergencyEntries.forEach { ef ->
                efArray.put(JSONObject().apply {
                    put("id", ef.id)
                    put("dateMillis", ef.dateMillis)
                    put("type", ef.type)
                    put("amount", ef.amount)
                    put("sourceDescription", ef.sourceDescription)
                    put("notes", ef.notes)
                })
            }
            root.put("emergencyFundEntries", efArray)

            // 6. Investment Assets
            val assets = dao.getAllInvestmentAssetsSync()
            val assetArray = JSONArray()
            assets.forEach { ast ->
                assetArray.put(JSONObject().apply {
                    put("id", ast.id)
                    put("name", ast.name)
                    put("assetType", ast.assetType)
                    put("totalCapitalInvested", ast.totalCapitalInvested)
                    put("currentMarketValue", ast.currentMarketValue)
                    put("lastUpdatedMillis", ast.lastUpdatedMillis)
                    put("notes", ast.notes)
                })
            }
            root.put("investmentAssets", assetArray)

            // 7. Investment Transactions
            val transactions = dao.getAllInvestmentTransactionsSync()
            val txArray = JSONArray()
            transactions.forEach { tx ->
                txArray.put(JSONObject().apply {
                    put("id", tx.id)
                    put("assetId", tx.assetId)
                    put("dateMillis", tx.dateMillis)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("notes", tx.notes)
                })
            }
            root.put("investmentTransactions", txArray)

            root.put("transactionHistory", JSONArray())
            root.put("monthlyBudgets", JSONArray())
            root.put("savingsGoals", JSONArray())

            // Summary metadata
            root.put("summary", JSONObject().apply {
                put("totalIncomes", incomes.size)
                put("totalExpenses", expenses.size)
                put("totalEmergencyEntries", emergencyEntries.size)
                put("totalInvestmentAssets", assets.size)
                put("totalTransactionHistory", 0)
                put("totalMonthlyBudgets", 0)
                put("totalSavingsGoals", 0)
            })

            val jsonString = root.toString(2)
            AppLogger.info("DataBackupManager", "Berhasil mengekspor cadangan data dao (${jsonString.length} bytes)")
            jsonString
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal mengekspor data dao ke JSON: ${error.message}", error.throwable)
        }
    }

    suspend fun restoreDataFromJson(db: AppDatabase, jsonString: String): AppResult<String> {
        return runCatchingApp("DataBackupRestore") {
            if (jsonString.isBlank()) {
                throw IllegalArgumentException("Data cadangan kosong atau tidak valid.")
            }

            // 1. Parsing & Validasi Schema sebelum mengubah database
            val root = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                throw IllegalArgumentException("Format file rusak atau bukan format JSON yang valid: ${e.message}")
            }

            val app = root.optString("app", "")
            if (app.isNotBlank() && app != APP_IDENTIFIER) {
                throw IllegalArgumentException("Format cadangan tidak kompatibel (Aplikasi sumber: $app)")
            }

            val schemaVersion = root.optInt("version", 1)
            if (schemaVersion < 1) {
                throw IllegalArgumentException("Versi skema cadangan tidak valid: $schemaVersion")
            }

            // 2. Validasi Profile
            val profileObj = root.optJSONObject("profile")
            val profile = if (profileObj != null) {
                val salary = profileObj.optLong("monthlySalary", 0L)
                val initSavings = profileObj.optLong("initialSavings", 0L)
                val initDebt = profileObj.optLong("initialDebt", 0L)
                if (salary < 0 || initSavings < 0 || initDebt < 0) {
                    throw IllegalArgumentException("Nilai numerik pada profil tidak boleh negatif.")
                }
                ProfileEntity(
                    id = 1,
                    monthlySalary = salary,
                    livingSituation = profileObj.optString("livingSituation", ""),
                    initialSavings = initSavings,
                    initialDebt = initDebt,
                    financialPriority = profileObj.optString("financialPriority", ""),
                    emergencyTargetMonths = profileObj.optInt("emergencyTargetMonths", 3).coerceAtLeast(1),
                    currency = profileObj.optString("currency", "IDR"),
                    timezone = profileObj.optString("timezone", "Asia/Jakarta"),
                    paydayMode = profileObj.optString("paydayMode", "END_OF_MONTH"),
                    paydayDayOfMonth = profileObj.optInt("paydayDayOfMonth", 25)
                )
            } else null

            // 3. Validasi Pos Anggaran
            val catArray = root.optJSONArray("budgetCategories")
            val budgetCategories = mutableListOf<BudgetCategoryEntity>()
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    val name = c.getString("name").trim()
                    if (name.isEmpty()) throw IllegalArgumentException("Nama pos anggaran pada indeks $i tidak boleh kosong.")
                    budgetCategories.add(
                        BudgetCategoryEntity(
                            id = c.optLong("id", 0L),
                            name = name,
                            percentage = c.optDouble("percentage", 0.0).coerceIn(0.0, 100.0),
                            plannedAmount = c.optLong("plannedAmount", 0L).coerceAtLeast(0L),
                            orderIndex = c.optInt("orderIndex", i)
                        )
                    )
                }
            }

            // 4. Validasi Incomes
            val incArray = root.optJSONArray("incomes")
            val incomes = mutableListOf<IncomeEntity>()
            if (incArray != null) {
                for (i in 0 until incArray.length()) {
                    val o = incArray.getJSONObject(i)
                    val amount = o.getLong("amount")
                    if (amount < 0) throw IllegalArgumentException("Nominal pendapatan tidak boleh negatif (indeks $i).")
                    incomes.add(
                        IncomeEntity(
                            id = o.optLong("id", 0L),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            amount = amount,
                            incomeType = o.optString("incomeType", "LAINNYA"),
                            source = o.optString("source", "Pendapatan"),
                            status = o.optString("status", "DITERIMA"),
                            isAllocated = o.optBoolean("isAllocated", false),
                            allocationDetails = o.optString("allocationDetails", ""),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 5. Validasi Expenses
            val expArray = root.optJSONArray("expenses")
            val expenses = mutableListOf<ExpenseEntity>()
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val o = expArray.getJSONObject(i)
                    val amount = o.getLong("amount")
                    if (amount < 0) throw IllegalArgumentException("Nominal pengeluaran tidak boleh negatif (indeks $i).")
                    expenses.add(
                        ExpenseEntity(
                            id = o.optLong("id", 0L),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            amount = amount,
                            category = o.optString("category", "Lainnya"),
                            paymentMethod = o.optString("paymentMethod", "TUNAI"),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 6. Validasi Emergency Fund Entries
            val efArray = root.optJSONArray("emergencyFundEntries")
            val emergencyEntries = mutableListOf<EmergencyFundEntity>()
            if (efArray != null) {
                for (i in 0 until efArray.length()) {
                    val o = efArray.getJSONObject(i)
                    val amount = o.getLong("amount")
                    if (amount < 0) throw IllegalArgumentException("Nominal dana darurat tidak boleh negatif (indeks $i).")
                    emergencyEntries.add(
                        EmergencyFundEntity(
                            id = o.optLong("id", 0L),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            type = o.optString("type", "SETORAN"),
                            amount = amount,
                            sourceDescription = o.optString("sourceDescription", ""),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 7. Validasi Investment Assets
            val astArray = root.optJSONArray("investmentAssets")
            val assets = mutableListOf<InvestmentAssetEntity>()
            val assetIds = mutableSetOf<Long>()
            if (astArray != null) {
                for (i in 0 until astArray.length()) {
                    val o = astArray.getJSONObject(i)
                    val name = o.getString("name").trim()
                    if (name.isEmpty()) throw IllegalArgumentException("Nama aset investasi pada indeks $i tidak boleh kosong.")
                    val id = o.optLong("id", 0L)
                    if (id > 0) assetIds.add(id)
                    assets.add(
                        InvestmentAssetEntity(
                            id = id,
                            name = name,
                            assetType = o.optString("assetType", "PASAR_UANG"),
                            totalCapitalInvested = o.optLong("totalCapitalInvested", 0L).coerceAtLeast(0L),
                            currentMarketValue = o.optLong("currentMarketValue", 0L).coerceAtLeast(0L),
                            lastUpdatedMillis = o.optLong("lastUpdatedMillis", System.currentTimeMillis()),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 8. Validasi Investment Transactions (Integritas Relasional)
            val txArray = root.optJSONArray("investmentTransactions")
            val transactions = mutableListOf<InvestmentTxEntity>()
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    val assetId = o.getLong("assetId")
                    val amount = o.getLong("amount")
                    if (amount < 0) throw IllegalArgumentException("Nominal transaksi investasi tidak boleh negatif (indeks $i).")
                    if (assets.isNotEmpty() && assetIds.isNotEmpty() && !assetIds.contains(assetId)) {
                        throw IllegalArgumentException("Integritas relasi gagal: Transaksi investasi merujuk ke assetId $assetId yang tidak ada.")
                    }
                    transactions.add(
                        InvestmentTxEntity(
                            id = o.optLong("id", 0L),
                            assetId = assetId,
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            type = o.optString("type", "SETORAN"),
                            amount = amount,
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 9. Validasi Transaction History (Kompatibel dengan backup lama v1)
            val histArray = root.optJSONArray("transactionHistory")
            val transactionHistory = mutableListOf<TransactionEntity>()
            if (histArray != null) {
                for (i in 0 until histArray.length()) {
                    val o = histArray.getJSONObject(i)
                    val title = o.optString("title", "Transaksi")
                    val amount = o.getLong("amount")
                    if (amount < 0) throw IllegalArgumentException("Nominal riwayat transaksi tidak boleh negatif (indeks $i).")
                    transactionHistory.add(
                        TransactionEntity(
                            id = o.optLong("id", 0L),
                            title = title,
                            amount = amount,
                            type = o.optString("type", "EXPENSE"),
                            category = o.optString("category", "Lainnya"),
                            paymentMethod = o.optString("paymentMethod", "TUNAI"),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            notes = o.optString("notes", ""),
                            relatedEntityId = if (o.has("relatedEntityId") && !o.isNull("relatedEntityId")) o.getLong("relatedEntityId") else null
                        )
                    )
                }
            }

            // 10. Validasi Monthly Budgets (Kompatibel dengan backup lama v1)
            val mbArray = root.optJSONArray("monthlyBudgets")
            val monthlyBudgets = mutableListOf<MonthlyBudgetEntity>()
            if (mbArray != null) {
                for (i in 0 until mbArray.length()) {
                    val o = mbArray.getJSONObject(i)
                    val month = o.getInt("month")
                    val year = o.getInt("year")
                    val total = o.getLong("totalBudget")
                    if (month !in 1..12) throw IllegalArgumentException("Bulan anggaran tidak valid: $month (indeks $i).")
                    if (total < 0) throw IllegalArgumentException("Total anggaran tidak boleh negatif (indeks $i).")
                    monthlyBudgets.add(
                        MonthlyBudgetEntity(
                            id = o.optLong("id", 0L),
                            month = month,
                            year = year,
                            totalBudget = total,
                            allocatedNeeds = o.optLong("allocatedNeeds", 0L),
                            allocatedWants = o.optLong("allocatedWants", 0L),
                            allocatedSavings = o.optLong("allocatedSavings", 0L),
                            notes = o.optString("notes", ""),
                            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 11. Validasi Savings Goals (Kompatibel dengan backup lama v1)
            val sgArray = root.optJSONArray("savingsGoals")
            val savingsGoals = mutableListOf<SavingsGoalEntity>()
            if (sgArray != null) {
                for (i in 0 until sgArray.length()) {
                    val o = sgArray.getJSONObject(i)
                    val title = o.getString("title").trim()
                    val target = o.getLong("targetAmount")
                    val current = o.optLong("currentAmount", 0L)
                    if (title.isEmpty()) throw IllegalArgumentException("Judul target tabungan pada indeks $i tidak boleh kosong.")
                    if (target < 0 || current < 0) throw IllegalArgumentException("Target nominal tabungan tidak boleh negatif.")
                    savingsGoals.add(
                        SavingsGoalEntity(
                            id = o.optLong("id", 0L),
                            title = title,
                            targetAmount = target,
                            currentAmount = current,
                            targetDateMillis = o.optLong("targetDateMillis", System.currentTimeMillis()),
                            category = o.optString("category", "Tabungan"),
                            priority = o.optString("priority", "SEDANG"),
                            iconName = o.optString("iconName", "savings"),
                            isCompleted = o.optBoolean("isCompleted", false),
                            notes = o.optString("notes", ""),
                            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 12. Eksekusi Atomik Menggunakan Transaksi Database Room
            val financeDao = db.financeDao()
            val transactionDao = db.transactionDao()
            val monthlyBudgetDao = db.monthlyBudgetDao()
            val savingsGoalDao = db.savingsGoalDao()

            db.runInTransaction {
                // Hapus data lama hanya setelah seluruh validasi lolos 100%
                kotlinx.coroutines.runBlocking {
                    financeDao.clearIncomes()
                    financeDao.clearExpenses()
                    financeDao.clearEmergencyFund()
                    financeDao.clearInvestmentTransactions()
                    financeDao.clearInvestmentAssets()
                    financeDao.clearBudgetCategories()
                    transactionDao.clearAllTransactions()
                    monthlyBudgetDao.clearAllMonthlyBudgets()
                    savingsGoalDao.clearAllSavingsGoals()

                    // Masukkan seluruh data yang dipulihkan
                    if (profile != null) financeDao.insertProfile(profile)
                    if (budgetCategories.isNotEmpty()) financeDao.insertBudgetCategories(budgetCategories)
                    if (incomes.isNotEmpty()) financeDao.insertIncomes(incomes)
                    if (expenses.isNotEmpty()) financeDao.insertExpenses(expenses)
                    if (emergencyEntries.isNotEmpty()) financeDao.insertEmergencyFundEntries(emergencyEntries)
                    if (assets.isNotEmpty()) financeDao.insertInvestmentAssets(assets)
                    if (transactions.isNotEmpty()) financeDao.insertInvestmentTransactions(transactions)
                    if (transactionHistory.isNotEmpty()) transactionDao.insertTransactions(transactionHistory)
                    if (monthlyBudgets.isNotEmpty()) monthlyBudgetDao.insertMonthlyBudgets(monthlyBudgets)
                    if (savingsGoals.isNotEmpty()) savingsGoalDao.insertSavingsGoals(savingsGoals)
                }
            }

            val report = "Pemulihan Berhasil: ${incomes.size} pemasukan, ${expenses.size} pengeluaran, ${emergencyEntries.size} mutasi dana darurat, ${assets.size} aset investasi, ${savingsGoals.size} target tabungan."
            AppLogger.info("DataBackupManager", report)
            report
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal memulihkan data dari JSON: ${error.message}", error.throwable)
        }
    }

    suspend fun restoreDataFromJson(dao: FinanceDao, jsonString: String): AppResult<String> {
        return runCatchingApp("DataBackupRestoreDao") {
            if (jsonString.isBlank()) {
                throw IllegalArgumentException("Data cadangan kosong atau tidak valid.")
            }

            val root = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                throw IllegalArgumentException("Format file rusak atau bukan format JSON yang valid: ${e.message}")
            }

            val app = root.optString("app", "")
            if (app.isNotBlank() && app != APP_IDENTIFIER) {
                throw IllegalArgumentException("Format cadangan tidak kompatibel (Aplikasi sumber: $app)")
            }

            // 2. Validasi Profile
            val profileObj = root.optJSONObject("profile")
            val profile = if (profileObj != null) {
                ProfileEntity(
                    id = 1,
                    monthlySalary = profileObj.optLong("monthlySalary", 0L),
                    livingSituation = profileObj.optString("livingSituation", ""),
                    initialSavings = profileObj.optLong("initialSavings", 0L),
                    initialDebt = profileObj.optLong("initialDebt", 0L),
                    financialPriority = profileObj.optString("financialPriority", ""),
                    emergencyTargetMonths = profileObj.optInt("emergencyTargetMonths", 3),
                    currency = profileObj.optString("currency", "IDR"),
                    timezone = profileObj.optString("timezone", "Asia/Jakarta"),
                    paydayMode = profileObj.optString("paydayMode", "END_OF_MONTH"),
                    paydayDayOfMonth = profileObj.optInt("paydayDayOfMonth", 25)
                )
            } else null

            // 3. Pos Anggaran
            val catArray = root.optJSONArray("budgetCategories")
            val budgetCategories = mutableListOf<BudgetCategoryEntity>()
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    budgetCategories.add(
                        BudgetCategoryEntity(
                            id = c.optLong("id", 0L),
                            name = c.getString("name"),
                            percentage = c.optDouble("percentage", 0.0),
                            plannedAmount = c.optLong("plannedAmount", 0L),
                            orderIndex = c.optInt("orderIndex", i)
                        )
                    )
                }
            }

            // 4. Incomes
            val incArray = root.optJSONArray("incomes")
            val incomes = mutableListOf<IncomeEntity>()
            if (incArray != null) {
                for (i in 0 until incArray.length()) {
                    val o = incArray.getJSONObject(i)
                    incomes.add(
                        IncomeEntity(
                            id = o.optLong("id", 0L),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            amount = o.getLong("amount"),
                            incomeType = o.optString("incomeType", "LAINNYA"),
                            source = o.optString("source", "Pendapatan"),
                            status = o.optString("status", "DITERIMA"),
                            isAllocated = o.optBoolean("isAllocated", false),
                            allocationDetails = o.optString("allocationDetails", ""),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 5. Expenses
            val expArray = root.optJSONArray("expenses")
            val expenses = mutableListOf<ExpenseEntity>()
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val o = expArray.getJSONObject(i)
                    expenses.add(
                        ExpenseEntity(
                            id = o.optLong("id", 0L),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            amount = o.getLong("amount"),
                            category = o.optString("category", "Lainnya"),
                            paymentMethod = o.optString("paymentMethod", "TUNAI"),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 6. Emergency Fund
            val efArray = root.optJSONArray("emergencyFundEntries")
            val emergencyEntries = mutableListOf<EmergencyFundEntity>()
            if (efArray != null) {
                for (i in 0 until efArray.length()) {
                    val o = efArray.getJSONObject(i)
                    emergencyEntries.add(
                        EmergencyFundEntity(
                            id = o.optLong("id", 0L),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            type = o.optString("type", "SETORAN"),
                            amount = o.getLong("amount"),
                            sourceDescription = o.optString("sourceDescription", ""),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 7. Investment Assets
            val astArray = root.optJSONArray("investmentAssets")
            val assets = mutableListOf<InvestmentAssetEntity>()
            if (astArray != null) {
                for (i in 0 until astArray.length()) {
                    val o = astArray.getJSONObject(i)
                    assets.add(
                        InvestmentAssetEntity(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            assetType = o.optString("assetType", "PASAR_UANG"),
                            totalCapitalInvested = o.optLong("totalCapitalInvested", 0L),
                            currentMarketValue = o.optLong("currentMarketValue", 0L),
                            lastUpdatedMillis = o.optLong("lastUpdatedMillis", System.currentTimeMillis()),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 8. Investment Transactions
            val txArray = root.optJSONArray("investmentTransactions")
            val transactions = mutableListOf<InvestmentTxEntity>()
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    transactions.add(
                        InvestmentTxEntity(
                            id = o.optLong("id", 0L),
                            assetId = o.getLong("assetId"),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            type = o.optString("type", "SETORAN"),
                            amount = o.getLong("amount"),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            dao.clearIncomes()
            dao.clearExpenses()
            dao.clearEmergencyFund()
            dao.clearInvestmentTransactions()
            dao.clearInvestmentAssets()
            dao.clearBudgetCategories()

            if (profile != null) dao.insertProfile(profile)
            if (budgetCategories.isNotEmpty()) dao.insertBudgetCategories(budgetCategories)
            if (incomes.isNotEmpty()) dao.insertIncomes(incomes)
            if (expenses.isNotEmpty()) dao.insertExpenses(expenses)
            if (emergencyEntries.isNotEmpty()) dao.insertEmergencyFundEntries(emergencyEntries)
            if (assets.isNotEmpty()) dao.insertInvestmentAssets(assets)
            if (transactions.isNotEmpty()) dao.insertInvestmentTransactions(transactions)

            val report = "Pemulihan Berhasil: ${incomes.size} pemasukan, ${expenses.size} pengeluaran."
            AppLogger.info("DataBackupManager", report)
            report
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal memulihkan data dao dari JSON: ${error.message}", error.throwable)
        }
    }
}
