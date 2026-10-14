package com.example.data.repository

import com.example.core.logger.AppLogger
import com.example.core.result.AppResult
import com.example.core.result.runCatchingApp
import com.example.data.backup.DataBackupManager
import com.example.data.calculator.BonusAllocationCalculator
import com.example.data.calculator.BonusAllocationPlan
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.BudgetPreset
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

// Re-export BonusAllocationPlan for repository layer consumers
typealias BonusAllocationPlan = com.example.data.calculator.BonusAllocationPlan

class FinanceRepository(val dao: FinanceDao) {
    val profile: Flow<ProfileEntity?> = dao.getProfile()
    val allIncomes: Flow<List<IncomeEntity>> = dao.getAllIncomes()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val allBudgetCategories: Flow<List<BudgetCategoryEntity>> = dao.getAllBudgetCategories()
    val allEmergencyFundEntries: Flow<List<EmergencyFundEntity>> = dao.getAllEmergencyFundEntries()
    val allInvestmentAssets: Flow<List<InvestmentAssetEntity>> = dao.getAllInvestmentAssets()
    val allInvestmentTransactions: Flow<List<InvestmentTxEntity>> = dao.getAllInvestmentTransactions()

    suspend fun getProfileSync(): ProfileEntity {
        return runCatchingApp("getProfileSync") {
            dao.getProfileSync() ?: ProfileEntity()
        }.getOrDefault(ProfileEntity())
    }

    suspend fun updateProfile(profile: ProfileEntity): AppResult<Unit> {
        return runCatchingApp("updateProfile") {
            dao.insertProfile(profile)
            AppLogger.info("FinanceRepository", "Profil pengguna diperbarui: gaji Rp ${profile.monthlySalary}")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal memperbarui profil: ${it.message}", it.throwable)
        }
    }

    suspend fun insertIncome(income: IncomeEntity): AppResult<Long> {
        return runCatchingApp("insertIncome") {
            val id = dao.insertIncome(income)
            AppLogger.info("FinanceRepository", "Pendapatan baru ditambahkan [ID $id]: ${income.source} (${income.amount})")
            id
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menyimpan pendapatan: ${it.message}", it.throwable)
        }
    }

    suspend fun updateIncome(income: IncomeEntity): AppResult<Unit> {
        return runCatchingApp("updateIncome") {
            dao.updateIncome(income)
            AppLogger.info("FinanceRepository", "Pendapatan diperbarui [ID ${income.id}]: ${income.source}")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal memperbarui pendapatan: ${it.message}", it.throwable)
        }
    }

    suspend fun deleteIncome(income: IncomeEntity): AppResult<Unit> {
        return runCatchingApp("deleteIncome") {
            dao.deleteIncome(income)
            AppLogger.info("FinanceRepository", "Pendapatan dihapus [ID ${income.id}]: ${income.source}")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menghapus pendapatan: ${it.message}", it.throwable)
        }
    }

    suspend fun deleteIncomeById(id: Long): AppResult<Unit> {
        return runCatchingApp("deleteIncomeById") {
            dao.deleteIncomeById(id)
            AppLogger.info("FinanceRepository", "Pendapatan dihapus berdasarkan ID: $id")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menghapus pendapatan ID $id: ${it.message}", it.throwable)
        }
    }

    suspend fun toggleIncomeStatus(incomeId: Long): AppResult<Unit> {
        return runCatchingApp("toggleIncomeStatus") {
            val income = dao.getIncomeById(incomeId)
                ?: throw IllegalArgumentException("Pendapatan ID $incomeId tidak ditemukan")
            val newStatus = if (income.status == "DITERIMA") "BELUM_DITERIMA" else "DITERIMA"
            dao.updateIncome(income.copy(status = newStatus))
            if (income.incomeType == "GAJI_TETAP" && newStatus == "DITERIMA") {
                applySalaryAndBudgetPreset(income.amount, null)
            }
            AppLogger.info("FinanceRepository", "Status pendapatan [ID $incomeId] diubah ke $newStatus")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal mengubah status pendapatan: ${it.message}", it.throwable)
        }
    }

    suspend fun markIncomeReceived(incomeId: Long): AppResult<Unit> {
        return runCatchingApp("markIncomeReceived") {
            val income = dao.getIncomeById(incomeId)
                ?: throw IllegalArgumentException("Pendapatan ID $incomeId tidak ditemukan")
            dao.updateIncome(income.copy(status = "DITERIMA"))
            if (income.incomeType == "GAJI_TETAP") {
                applySalaryAndBudgetPreset(income.amount, null)
            }
            AppLogger.info("FinanceRepository", "Pendapatan [ID $incomeId] ditandai DITERIMA")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menandai pendapatan diterima: ${it.message}", it.throwable)
        }
    }

    suspend fun insertExpense(expense: ExpenseEntity): AppResult<Long> {
        return runCatchingApp("insertExpense") {
            val id = dao.insertExpense(expense)
            AppLogger.info("FinanceRepository", "Pengeluaran baru dicatat [ID $id]: ${expense.category} (Rp ${expense.amount})")
            id
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal mencatat pengeluaran: ${it.message}", it.throwable)
        }
    }

    suspend fun updateExpense(expense: ExpenseEntity): AppResult<Unit> {
        return runCatchingApp("updateExpense") {
            dao.updateExpense(expense)
            AppLogger.info("FinanceRepository", "Pengeluaran diperbarui [ID ${expense.id}]: ${expense.category}")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal memperbarui pengeluaran: ${it.message}", it.throwable)
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity): AppResult<Unit> {
        return runCatchingApp("deleteExpense") {
            dao.deleteExpense(expense)
            AppLogger.info("FinanceRepository", "Pengeluaran dihapus [ID ${expense.id}]")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menghapus pengeluaran: ${it.message}", it.throwable)
        }
    }

    suspend fun deleteExpenseById(id: Long): AppResult<Unit> {
        return runCatchingApp("deleteExpenseById") {
            dao.deleteExpenseById(id)
            AppLogger.info("FinanceRepository", "Pengeluaran dihapus ID $id")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menghapus pengeluaran ID $id: ${it.message}", it.throwable)
        }
    }

    suspend fun updateBudgetCategories(categories: List<BudgetCategoryEntity>): AppResult<Unit> {
        return runCatchingApp("updateBudgetCategories") {
            dao.clearBudgetCategories()
            dao.insertBudgetCategories(categories)
            AppLogger.info("FinanceRepository", "Kategori anggaran diperbarui (${categories.size} pos)")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal memperbarui kategori anggaran: ${it.message}", it.throwable)
        }
    }

    suspend fun applySalaryAndBudgetPreset(newSalary: Long, preset: BudgetPreset?): AppResult<Unit> {
        return runCatchingApp("applySalaryAndBudgetPreset") {
            val currentProfile = getProfileSync()
            dao.insertProfile(currentProfile.copy(monthlySalary = newSalary))
            dao.updatePendingSalaryAmount(newSalary)

            val updatedCategories = when (preset) {
                BudgetPreset.RULE_50_30_20 -> listOf(
                    BudgetCategoryEntity(
                        name = "Kebutuhan pokok & tagihan wajib",
                        percentage = 50.0,
                        plannedAmount = ((newSalary * 50) / 100),
                        orderIndex = 0
                    ),
                    BudgetCategoryEntity(
                        name = "Keperluan pribadi & gaya hidup",
                        percentage = 30.0,
                        plannedAmount = ((newSalary * 30) / 100),
                        orderIndex = 1
                    ),
                    BudgetCategoryEntity(
                        name = "Tabungan, dana darurat & investasi",
                        percentage = 20.0,
                        plannedAmount = ((newSalary * 20) / 100),
                        orderIndex = 2
                    )
                )
                BudgetPreset.FIRE_AGGRESSIVE -> listOf(
                    BudgetCategoryEntity(
                        name = "Kebutuhan pokok dan kontribusi keluarga",
                        percentage = 35.0,
                        plannedAmount = ((newSalary * 35) / 100),
                        orderIndex = 0
                    ),
                    BudgetCategoryEntity(
                        name = "Transportasi, pulsa, dan tagihan",
                        percentage = 15.0,
                        plannedAmount = ((newSalary * 15) / 100),
                        orderIndex = 1
                    ),
                    BudgetCategoryEntity(
                        name = "Dana darurat",
                        percentage = 10.0,
                        plannedAmount = ((newSalary * 10) / 100),
                        orderIndex = 2
                    ),
                    BudgetCategoryEntity(
                        name = "Investasi masa depan (FIRE)",
                        percentage = 30.0,
                        plannedAmount = ((newSalary * 30) / 100),
                        orderIndex = 3
                    ),
                    BudgetCategoryEntity(
                        name = "Keperluan pribadi",
                        percentage = 5.0,
                        plannedAmount = ((newSalary * 5) / 100),
                        orderIndex = 4
                    ),
                    BudgetCategoryEntity(
                        name = "Cadangan fleksibel",
                        percentage = 5.0,
                        plannedAmount = ((newSalary * 5) / 100),
                        orderIndex = 5
                    )
                )
                BudgetPreset.DOMPET_PINTAR -> listOf(
                    BudgetCategoryEntity(
                        name = "Kebutuhan pokok dan kontribusi keluarga",
                        percentage = 45.0,
                        plannedAmount = ((newSalary * 45) / 100),
                        orderIndex = 0
                    ),
                    BudgetCategoryEntity(
                        name = "Transportasi, pulsa, dan tagihan",
                        percentage = 15.0,
                        plannedAmount = ((newSalary * 15) / 100),
                        orderIndex = 1
                    ),
                    BudgetCategoryEntity(
                        name = "Dana darurat",
                        percentage = 20.0,
                        plannedAmount = ((newSalary * 20) / 100),
                        orderIndex = 2
                    ),
                    BudgetCategoryEntity(
                        name = "Investasi",
                        percentage = 5.0,
                        plannedAmount = ((newSalary * 5) / 100),
                        orderIndex = 3
                    ),
                    BudgetCategoryEntity(
                        name = "Keperluan pribadi",
                        percentage = 10.0,
                        plannedAmount = ((newSalary * 10) / 100),
                        orderIndex = 4
                    ),
                    BudgetCategoryEntity(
                        name = "Cadangan fleksibel",
                        percentage = 5.0,
                        plannedAmount = ((newSalary * 5) / 100),
                        orderIndex = 5
                    )
                )
                else -> {
                    val existing = dao.getBudgetCategoriesSync()
                    if (existing.isNotEmpty()) {
                        existing.map { cat ->
                            cat.copy(plannedAmount = ((newSalary * cat.percentage) / 100.0).toLong())
                        }
                    } else {
                        listOf(
                            BudgetCategoryEntity(name = "Kebutuhan pokok dan kontribusi keluarga", percentage = 45.0, plannedAmount = ((newSalary * 45) / 100), orderIndex = 0),
                            BudgetCategoryEntity(name = "Transportasi, pulsa, dan tagihan", percentage = 15.0, plannedAmount = ((newSalary * 15) / 100), orderIndex = 1),
                            BudgetCategoryEntity(name = "Dana darurat", percentage = 20.0, plannedAmount = ((newSalary * 20) / 100), orderIndex = 2),
                            BudgetCategoryEntity(name = "Investasi", percentage = 5.0, plannedAmount = ((newSalary * 5) / 100), orderIndex = 3),
                            BudgetCategoryEntity(name = "Keperluan pribadi", percentage = 10.0, plannedAmount = ((newSalary * 10) / 100), orderIndex = 4),
                            BudgetCategoryEntity(name = "Cadangan fleksibel", percentage = 5.0, plannedAmount = ((newSalary * 5) / 100), orderIndex = 5)
                        )
                    }
                }
            }
            dao.clearBudgetCategories()
            dao.insertBudgetCategories(updatedCategories)
            AppLogger.info("FinanceRepository", "Preset anggaran disesuaikan untuk gaji Rp $newSalary")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal mengatur ulang preset anggaran: ${it.message}", it.throwable)
        }
    }

    suspend fun insertEmergencyFundEntry(entry: EmergencyFundEntity): AppResult<Long> {
        return runCatchingApp("insertEmergencyFundEntry") {
            val id = dao.insertEmergencyFundEntry(entry)
            AppLogger.info("FinanceRepository", "Catatan dana darurat ditambahkan: ${entry.type} Rp ${entry.amount}")
            id
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menyimpan entri dana darurat: ${it.message}", it.throwable)
        }
    }

    suspend fun deleteEmergencyFundEntry(entry: EmergencyFundEntity): AppResult<Unit> {
        return runCatchingApp("deleteEmergencyFundEntry") {
            dao.deleteEmergencyFundEntry(entry)
            AppLogger.info("FinanceRepository", "Catatan dana darurat dihapus [ID ${entry.id}]")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menghapus entri dana darurat: ${it.message}", it.throwable)
        }
    }

    suspend fun insertInvestmentAsset(asset: InvestmentAssetEntity): AppResult<Long> {
        return runCatchingApp("insertInvestmentAsset") {
            val id = dao.insertInvestmentAsset(asset)
            AppLogger.info("FinanceRepository", "Aset investasi baru ditambahkan [ID $id]: ${asset.name}")
            id
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menyimpan aset investasi: ${it.message}", it.throwable)
        }
    }

    suspend fun updateInvestmentAsset(asset: InvestmentAssetEntity): AppResult<Unit> {
        return runCatchingApp("updateInvestmentAsset") {
            dao.updateInvestmentAsset(asset)
            AppLogger.info("FinanceRepository", "Aset investasi diperbarui [ID ${asset.id}]: ${asset.name}")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal memperbarui aset investasi: ${it.message}", it.throwable)
        }
    }

    suspend fun deleteInvestmentAsset(asset: InvestmentAssetEntity): AppResult<Unit> {
        return runCatchingApp("deleteInvestmentAsset") {
            dao.deleteTransactionsByAssetId(asset.id)
            dao.deleteInvestmentAsset(asset)
            AppLogger.info("FinanceRepository", "Aset investasi [ID ${asset.id}] beserta transaksinya dihapus")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menghapus aset investasi: ${it.message}", it.throwable)
        }
    }

    suspend fun insertInvestmentTx(tx: InvestmentTxEntity): AppResult<Long> {
        return runCatchingApp("insertInvestmentTx") {
            val id = dao.insertInvestmentTx(tx)
            AppLogger.info("FinanceRepository", "Transaksi investasi dicatat [ID $id]: Rp ${tx.amount}")
            id
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal mencatat transaksi investasi: ${it.message}", it.throwable)
        }
    }

    suspend fun applyBonusAllocation(
        incomeId: Long,
        emergencyFundAmount: Long,
        investmentAmount: Long,
        personalAmount: Long,
        targetAssetId: Long? = null,
        targetAssetName: String = "Alokasi Bonus Investasi"
    ): AppResult<Unit> {
        return runCatchingApp("applyBonusAllocation") {
            val income = dao.getIncomeById(incomeId)
                ?: throw IllegalArgumentException("Bonus dengan ID $incomeId tidak ditemukan")
            val summary = "Dana Darurat: Rp $emergencyFundAmount, Investasi: Rp $investmentAmount, Pribadi: Rp $personalAmount"
            dao.updateIncome(income.copy(status = "DITERIMA", isAllocated = true, allocationDetails = summary))

            if (emergencyFundAmount > 0) {
                dao.insertEmergencyFundEntry(
                    EmergencyFundEntity(
                        dateMillis = System.currentTimeMillis(),
                        type = "SETORAN",
                        amount = emergencyFundAmount,
                        sourceDescription = "Alokasi Bonus: ${income.source}",
                        notes = "Alokasi otomatis dari ${income.source}"
                    )
                )
            }

            if (investmentAmount > 0) {
                val assetIdToUse: Long
                if (targetAssetId != null && targetAssetId > 0) {
                    assetIdToUse = targetAssetId
                    val existing = dao.getInvestmentAssetById(targetAssetId)
                    if (existing != null) {
                        val updatedCapital = existing.totalCapitalInvested + investmentAmount
                        val updatedValue = existing.currentMarketValue + investmentAmount
                        dao.updateInvestmentAsset(
                            existing.copy(
                                totalCapitalInvested = updatedCapital,
                                currentMarketValue = updatedValue,
                                lastUpdatedMillis = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    assetIdToUse = dao.insertInvestmentAsset(
                        InvestmentAssetEntity(
                            name = targetAssetName,
                            assetType = "PASAR_UANG",
                            totalCapitalInvested = investmentAmount,
                            currentMarketValue = investmentAmount,
                            lastUpdatedMillis = System.currentTimeMillis(),
                            notes = "Dibuat dari alokasi bonus"
                        )
                    )
                }

                dao.insertInvestmentTx(
                    InvestmentTxEntity(
                        assetId = assetIdToUse,
                        dateMillis = System.currentTimeMillis(),
                        type = "SETORAN",
                        amount = investmentAmount,
                        notes = "Alokasi bonus dari ${income.source}"
                    )
                )
            }
            AppLogger.info("FinanceRepository", "Alokasi bonus sukses diterapkan untuk bonus ID $incomeId")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal menerapkan alokasi bonus: ${it.message}", it.throwable)
        }
    }

    fun calculateBonusAllocationPlan(
        bonusAmount: Long,
        currentEmergencyFund: Long,
        targetMonths: Int,
        monthlyEssentialCost: Long,
        currentCashBalance: Long,
        monthlyEssentialShortfall: Long
    ): BonusAllocationPlan {
        return BonusAllocationCalculator.calculate(
            bonusAmount = bonusAmount,
            currentEmergencyFund = currentEmergencyFund,
            targetMonths = targetMonths,
            monthlyEssentialCost = monthlyEssentialCost,
            currentCashBalance = currentCashBalance,
            monthlyEssentialShortfall = monthlyEssentialShortfall
        )
    }

    suspend fun exportDataAsJson(): String {
        return when (val res = DataBackupManager.exportDataAsJson(dao)) {
            is AppResult.Success -> res.data
            is AppResult.Failure -> "{ \"error\": \"${res.message}\" }"
        }
    }

    suspend fun restoreDataFromJson(jsonString: String): AppResult<String> {
        return DataBackupManager.restoreDataFromJson(dao, jsonString)
    }

    suspend fun resetToDemoData(
        transactionDao: com.example.data.local.TransactionDao? = null,
        monthlyBudgetDao: com.example.data.local.MonthlyBudgetDao? = null,
        savingsGoalDao: com.example.data.local.SavingsGoalDao? = null
    ): AppResult<Unit> {
        return runCatchingApp("resetToDemoData") {
            dao.clearIncomes()
            dao.clearExpenses()
            dao.clearEmergencyFund()
            dao.clearInvestmentAssets()
            dao.clearInvestmentTransactions()
            dao.clearBudgetCategories()
            transactionDao?.clearAllTransactions()
            monthlyBudgetDao?.clearAllMonthlyBudgets()
            savingsGoalDao?.clearAllSavingsGoals()

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            cal.add(Calendar.DAY_OF_MONTH, -15)
            dao.insertIncome(
                IncomeEntity(
                    dateMillis = cal.timeInMillis,
                    amount = 2_700_000L,
                    incomeType = "GAJI_TETAP",
                    source = "Gaji Bulanan PT Maju Bersama",
                    status = "DITERIMA",
                    notes = "Gaji bulan berjalan sudah masuk."
                )
            )

            cal.add(Calendar.DAY_OF_MONTH, 5)
            dao.insertIncome(
                IncomeEntity(
                    dateMillis = cal.timeInMillis,
                    amount = 200_000L,
                    incomeType = "BONUS",
                    source = "Bonus Lembur Proyek A",
                    status = "DITERIMA",
                    notes = "Bonus kinerja kuartal."
                )
            )

            cal.add(Calendar.DAY_OF_MONTH, 2)
            dao.insertExpense(
                ExpenseEntity(
                    dateMillis = cal.timeInMillis,
                    amount = 600_000L,
                    category = "Makan & Kebutuhan Pokok",
                    paymentMethod = "TRANSFER_BANK",
                    notes = "Belanja bulanan sembako"
                )
            )

            cal.add(Calendar.DAY_OF_MONTH, 2)
            dao.insertExpense(
                ExpenseEntity(
                    dateMillis = cal.timeInMillis,
                    amount = 250_000L,
                    category = "Kontribusi Keluarga",
                    paymentMethod = "TRANSFER_BANK",
                    notes = "Titipan listrik & air keluarga"
                )
            )

            cal.add(Calendar.DAY_OF_MONTH, 1)
            dao.insertExpense(
                ExpenseEntity(
                    dateMillis = cal.timeInMillis,
                    amount = 100_000L,
                    category = "Pulsa & Internet",
                    paymentMethod = "E_WALLET",
                    notes = "Paket data bulanan"
                )
            )

            dao.insertEmergencyFundEntry(
                EmergencyFundEntity(
                    dateMillis = cal.timeInMillis,
                    type = "SETORAN",
                    amount = 540_000L,
                    sourceDescription = "Alokasi Anggaran Bulanan",
                    notes = "Tabungan darurat awal"
                )
            )

            val assetId = dao.insertInvestmentAsset(
                InvestmentAssetEntity(
                    name = "Reksadana Pasar Uang Sucorinvest",
                    assetType = "PASAR_UANG",
                    totalCapitalInvested = 135_000L,
                    currentMarketValue = 138_500L,
                    lastUpdatedMillis = now,
                    notes = "Investasi modal awal rutin"
                )
            )

            dao.insertInvestmentTx(
                InvestmentTxEntity(
                    assetId = assetId,
                    dateMillis = cal.timeInMillis,
                    type = "SETORAN",
                    amount = 135_000L,
                    notes = "Setoran rutin dari gaji"
                )
            )
            AppLogger.info("FinanceRepository", "Data demo berhasil dimuat secara manual oleh pengguna")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal memuat data demo: ${it.message}", it.throwable)
        }
    }

    suspend fun clearAllTransactions(
        transactionDao: com.example.data.local.TransactionDao? = null,
        monthlyBudgetDao: com.example.data.local.MonthlyBudgetDao? = null,
        savingsGoalDao: com.example.data.local.SavingsGoalDao? = null
    ): AppResult<Unit> {
        return runCatchingApp("clearAllTransactions") {
            dao.clearIncomes()
            dao.clearExpenses()
            dao.clearEmergencyFund()
            dao.clearInvestmentAssets()
            dao.clearInvestmentTransactions()
            dao.clearBudgetCategories()
            transactionDao?.clearAllTransactions()
            monthlyBudgetDao?.clearAllMonthlyBudgets()
            savingsGoalDao?.clearAllSavingsGoals()
            dao.insertProfile(
                ProfileEntity(
                    id = 1,
                    monthlySalary = 0L,
                    livingSituation = "",
                    initialSavings = 0L,
                    initialDebt = 0L,
                    financialPriority = "",
                    emergencyTargetMonths = 3,
                    currency = "IDR",
                    timezone = "Asia/Jakarta",
                    paydayMode = com.example.data.local.PaydayMode.END_OF_MONTH,
                    paydayDayOfMonth = 25
                )
            )
            AppLogger.info("FinanceRepository", "Semua data transaksi berhasil dibersihkan ke kondisi awal Rp0")
        }.onFailure {
            AppLogger.error("FinanceRepository", "Gagal membersihkan transaksi: ${it.message}", it.throwable)
        }
    }
}
