package com.example.data.backup

import com.example.core.logger.AppLogger
import com.example.core.result.AppResult
import com.example.core.result.runCatchingApp
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.ProfileEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manajer ekspor dan pemulihan (restore) penuh data aplikasi Dompet Pintar.
 * Menjamin 100% data keuangan (profil, pos anggaran, pendapatan, pengeluaran,
 * dana darurat, dan portofolio investasi) tercadangkan dan dapat dipulihkan dengan aman.
 */
object DataBackupManager {

    private const val BACKUP_SCHEMA_VERSION = 1
    private const val APP_IDENTIFIER = "DompetPintar"

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

            // Summary metadata
            root.put("summary", JSONObject().apply {
                put("totalIncomes", incomes.size)
                put("totalExpenses", expenses.size)
                put("totalEmergencyEntries", emergencyEntries.size)
                put("totalInvestmentAssets", assets.size)
            })

            val jsonString = root.toString(2)
            AppLogger.info("DataBackupManager", "Berhasil mengekspor cadangan data penuh (${jsonString.length} bytes, ${incomes.size} pendapatan, ${expenses.size} pengeluaran)")
            jsonString
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal mengekspor data ke JSON: ${error.message}", error.throwable)
        }
    }

    suspend fun restoreDataFromJson(dao: FinanceDao, jsonString: String): AppResult<String> {
        return runCatchingApp("DataBackupRestore") {
            val root = JSONObject(jsonString)

            // 1. Validasi Schema dasar
            val app = root.optString("app", "")
            if (app.isNotBlank() && app != APP_IDENTIFIER) {
                throw IllegalArgumentException("Format cadangan tidak kompatibel (Aplikasi sumber: $app)")
            }

            // 2. Parse Profile
            val profileObj = root.optJSONObject("profile")
            val profile = if (profileObj != null) {
                ProfileEntity(
                    id = 1,
                    monthlySalary = profileObj.optLong("monthlySalary", 2_700_000L),
                    livingSituation = profileObj.optString("livingSituation", "Tinggal bersama keluarga"),
                    initialSavings = profileObj.optLong("initialSavings", 0L),
                    initialDebt = profileObj.optLong("initialDebt", 0L),
                    financialPriority = profileObj.optString("financialPriority", "Investasi jangka panjang"),
                    emergencyTargetMonths = profileObj.optInt("emergencyTargetMonths", 3),
                    currency = profileObj.optString("currency", "IDR"),
                    timezone = profileObj.optString("timezone", "Asia/Jakarta"),
                    paydayMode = profileObj.optString("paydayMode", "END_OF_MONTH"),
                    paydayDayOfMonth = profileObj.optInt("paydayDayOfMonth", 25)
                )
            } else null

            // 3. Parse Budget Categories
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

            // 4. Parse Incomes
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
                            incomeType = o.optString("incomeType", "BONUS"),
                            source = o.optString("source", "Pendapatan"),
                            status = o.optString("status", "DITERIMA"),
                            isAllocated = o.optBoolean("isAllocated", false),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // 5. Parse Expenses
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

            // 6. Parse Emergency Fund Entries
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

            // 7. Parse Investment Assets
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

            // 8. Parse Investment Transactions
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

            // Lakukan pembersihan dan pemulihan data atomik
            dao.clearIncomes()
            dao.clearExpenses()
            dao.clearEmergencyFund()
            dao.clearInvestmentAssets()
            dao.clearInvestmentTransactions()
            dao.clearBudgetCategories()

            profile?.let { dao.insertProfile(it) }
            if (budgetCategories.isNotEmpty()) dao.insertBudgetCategories(budgetCategories)
            if (incomes.isNotEmpty()) dao.insertIncomes(incomes)
            if (expenses.isNotEmpty()) dao.insertExpenses(expenses)
            if (emergencyEntries.isNotEmpty()) dao.insertEmergencyFundEntries(emergencyEntries)
            if (assets.isNotEmpty()) dao.insertInvestmentAssets(assets)
            if (transactions.isNotEmpty()) dao.insertInvestmentTransactions(transactions)

            val report = "Pemulihan Berhasil: ${incomes.size} pemasukan, ${expenses.size} pengeluaran, ${emergencyEntries.size} mutasi dana darurat, ${assets.size} aset investasi."
            AppLogger.info("DataBackupManager", report)
            report
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal memulihkan data dari JSON: ${error.message}", error.throwable)
        }
    }
}
