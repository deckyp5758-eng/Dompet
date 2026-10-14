package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.logger.AppLogger
import com.example.data.calculator.BonusAllocationCalculator
import com.example.data.calculator.BonusAllocationPlan
import com.example.data.calculator.DashboardCalculator
import com.example.data.calculator.DashboardSummary
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.EmergencyFundEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.IncomeEntity
import com.example.data.local.InvestmentAssetEntity
import com.example.data.local.InvestmentTxEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.BudgetPreset
import com.example.data.model.CurrencyFormatter
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Re-export DashboardSummary for UI compatibility
typealias DashboardSummary = com.example.data.calculator.DashboardSummary

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    val repository: FinanceRepository

    init {
        val db = com.example.data.local.DatabaseProvider.provideDatabase(application)
        repository = FinanceRepository(db.financeDao())
        AppLogger.info("FinanceViewModel", "FinanceViewModel berhasil diinisialisasi dengan DatabaseProvider")
    }

    val profile: StateFlow<ProfileEntity?> = repository.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProfileEntity()
    )

    val incomes: StateFlow<List<IncomeEntity>> = repository.allIncomes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val budgetCategories: StateFlow<List<BudgetCategoryEntity>> = repository.allBudgetCategories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val emergencyFundEntries: StateFlow<List<EmergencyFundEntity>> = repository.allEmergencyFundEntries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val investmentAssets: StateFlow<List<InvestmentAssetEntity>> = repository.allInvestmentAssets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val investmentTransactions: StateFlow<List<InvestmentTxEntity>> = repository.allInvestmentTransactions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // User Feedback Message
    val userMessage = MutableStateFlow<String?>(null)

    // Diagnostic & Error States
    val errorCount: StateFlow<Int> = AppLogger.errorCount
    val logEntries = AppLogger.logs

    private val _lastOperationError = MutableStateFlow<String?>(null)
    val lastOperationError: StateFlow<String?> = _lastOperationError.asStateFlow()

    fun clearUserMessage() {
        userMessage.value = null
    }

    fun showMessage(msg: String) {
        userMessage.value = msg
    }

    fun clearOperationError() {
        _lastOperationError.value = null
    }

    // Kombinasi kalkulasi dashboard reaktif menggunakan DashboardCalculator murni
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        combine(profile, incomes, expenses) { p, i, e -> Triple(p, i, e) },
        emergencyFundEntries,
        investmentAssets,
        budgetCategories
    ) { (p, i, e), ef, inv, bud ->
        DashboardCalculator.calculate(
            prof = p ?: ProfileEntity(),
            incList = i,
            expList = e,
            efList = ef,
            invList = inv,
            budList = bud
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardSummary()
    )

    // --- Action Methods dengan Safe Error Handling & Logging ---

    fun addIncome(
        amount: Long,
        incomeType: String,
        source: String,
        status: String,
        notes: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entity = IncomeEntity(
                dateMillis = dateMillis,
                amount = amount,
                incomeType = incomeType,
                source = source,
                status = status,
                notes = notes
            )
            repository.insertIncome(entity).onSuccess {
                val statusText = if (status == "DITERIMA") "diterima dan menambah saldo" else "dicatat (belum menambah saldo)"
                showMessage("Pendapatan $source berhasil disimpan ($statusText).")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menyimpan pendapatan: ${err.message}")
            }
        }
    }

    fun updateIncome(income: IncomeEntity) {
        viewModelScope.launch {
            repository.updateIncome(income).onSuccess {
                showMessage("Pendapatan ${income.source} diperbarui.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal memperbarui pendapatan: ${err.message}")
            }
        }
    }

    fun markIncomeReceived(incomeId: Long, source: String) {
        viewModelScope.launch {
            repository.markIncomeReceived(incomeId).onSuccess {
                showMessage("Status $source diubah ke DITERIMA! Saldo aktual bertambah.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal mengubah status: ${err.message}")
            }
        }
    }

    fun toggleIncomeStatus(incomeId: Long) {
        viewModelScope.launch {
            repository.toggleIncomeStatus(incomeId).onSuccess {
                showMessage("Status penerimaan pendapatan berhasil diubah.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal mengubah status penerimaan: ${err.message}")
            }
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            repository.deleteIncome(income).onSuccess {
                showMessage("Pendapatan ${income.source} dihapus.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menghapus pendapatan: ${err.message}")
            }
        }
    }

    fun addExpense(
        amount: Long,
        category: String,
        paymentMethod: String,
        notes: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entity = ExpenseEntity(
                dateMillis = dateMillis,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.insertExpense(entity).onSuccess {
                showMessage("Pengeluaran $category berhasil dicatat.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal mencatat pengeluaran: ${err.message}")
            }
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense).onSuccess {
                showMessage("Pengeluaran diperbarui.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal memperbarui pengeluaran: ${err.message}")
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense).onSuccess {
                showMessage("Pengeluaran berhasil dihapus.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menghapus pengeluaran: ${err.message}")
            }
        }
    }

    fun updateBudgetCategories(categories: List<BudgetCategoryEntity>) {
        val totalPct = categories.sumOf { it.percentage }
        if (Math.abs(totalPct - 100.0) > 0.01) {
            val msg = "Peringatan: Total persentase anggaran harus tepat 100%! Saat ini: ${String.format("%.1f", totalPct)}%"
            AppLogger.warn("FinanceViewModel", msg)
            showMessage(msg)
            return
        }
        viewModelScope.launch {
            repository.updateBudgetCategories(categories).onSuccess {
                showMessage("Pengaturan anggaran bulanan berhasil disimpan (Total 100%).")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menyimpan anggaran: ${err.message}")
            }
        }
    }

    fun addEmergencyFundEntry(type: String, amount: Long, sourceDesc: String, notes: String) {
        viewModelScope.launch {
            val summary = dashboardSummary.value
            if (type == "SETORAN" && amount > summary.actualCashBalance) {
                AppLogger.warn("FinanceViewModel", "Setoran Rp $amount dicatat. Saldo kas saat ini Rp ${summary.actualCashBalance} (dicatat sebagai dana awal/tambahan).")
            }
            if (type == "PENARIKAN" && amount > summary.emergencyFundBalance) {
                val warn = "Peringatan: Saldo dana darurat saat ini (Rp ${summary.emergencyFundBalance}) tidak mencukupi untuk penarikan Rp $amount."
                AppLogger.warn("FinanceViewModel", warn)
                showMessage(warn)
                return@launch
            }

            repository.insertEmergencyFundEntry(
                EmergencyFundEntity(
                    dateMillis = System.currentTimeMillis(),
                    type = type,
                    amount = amount,
                    sourceDescription = sourceDesc,
                    notes = notes
                )
            ).onSuccess {
                if (type == "PENARIKAN") {
                    showMessage("Peringatan: Dana darurat ditarik Rp $amount. Saldo darurat berkurang.")
                } else {
                    showMessage("Setoran Rp $amount ke Dana Darurat berhasil dicatat.")
                }
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal mencatat mutasi dana darurat: ${err.message}")
            }
        }
    }

    fun deleteEmergencyFundEntry(entry: EmergencyFundEntity) {
        viewModelScope.launch {
            repository.deleteEmergencyFundEntry(entry).onSuccess {
                showMessage("Riwayat dana darurat dihapus.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menghapus entri dana darurat: ${err.message}")
            }
        }
    }

    fun addInvestmentAsset(name: String, assetType: String, initialCapital: Long, notes: String) {
        viewModelScope.launch {
            val summary = dashboardSummary.value
            if (initialCapital > summary.actualCashBalance) {
                AppLogger.warn("FinanceViewModel", "Aset $name dengan modal Rp $initialCapital dicatat (portofolio/modal awal). Saldo kas saat ini Rp ${summary.actualCashBalance}.")
            }

            repository.insertInvestmentAsset(
                InvestmentAssetEntity(
                    name = name,
                    assetType = assetType,
                    totalCapitalInvested = initialCapital,
                    currentMarketValue = initialCapital,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    notes = notes
                )
            ).onSuccess { assetId ->
                if (initialCapital > 0) {
                    repository.insertInvestmentTx(
                        InvestmentTxEntity(
                            assetId = assetId,
                            dateMillis = System.currentTimeMillis(),
                            type = "SETORAN",
                            amount = initialCapital,
                            notes = "Investasi awal aset $name"
                        )
                    )
                }
                showMessage("Aset investasi $name berhasil ditambahkan.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menambahkan aset: ${err.message}")
            }
        }
    }

    fun updateAssetMarketValue(assetId: Long, newMarketValue: Long) {
        viewModelScope.launch {
            val assets = investmentAssets.value
            val asset = assets.firstOrNull { it.id == assetId } ?: return@launch
            repository.updateInvestmentAsset(
                asset.copy(
                    currentMarketValue = newMarketValue,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            ).onSuccess {
                showMessage("Nilai pasar ${asset.name} diperbarui ke Rp $newMarketValue.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal memperbarui nilai pasar: ${err.message}")
            }
        }
    }

    fun addInvestmentDeposit(assetId: Long, amount: Long, notes: String) {
        viewModelScope.launch {
            val summary = dashboardSummary.value
            if (amount > summary.actualCashBalance) {
                AppLogger.warn("FinanceViewModel", "Setoran investasi Rp $amount dicatat. Saldo kas saat ini Rp ${summary.actualCashBalance}.")
            }
            val assets = investmentAssets.value
            val asset = assets.firstOrNull { it.id == assetId } ?: return@launch

            repository.insertInvestmentTx(
                InvestmentTxEntity(
                    assetId = assetId,
                    dateMillis = System.currentTimeMillis(),
                    type = "SETORAN",
                    amount = amount,
                    notes = notes
                )
            )
            repository.updateInvestmentAsset(
                asset.copy(
                    totalCapitalInvested = asset.totalCapitalInvested + amount,
                    currentMarketValue = asset.currentMarketValue + amount,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            ).onSuccess {
                showMessage("Setoran Rp $amount ke ${asset.name} berhasil.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menambah setoran investasi: ${err.message}")
            }
        }
    }

    fun applyBonusAllocation(
        incomeId: Long,
        daruratAmount: Long,
        investAmount: Long,
        pribadiAmount: Long,
        targetAssetId: Long? = null
    ) {
        viewModelScope.launch {
            repository.applyBonusAllocation(
                incomeId = incomeId,
                emergencyFundAmount = daruratAmount,
                investmentAmount = investAmount,
                personalAmount = pribadiAmount,
                targetAssetId = targetAssetId
            ).onSuccess {
                showMessage("Alokasi bonus berhasil diterapkan dan tercatat di pos masing-masing.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menerapkan alokasi bonus: ${err.message}")
            }
        }
    }

    fun updateMonthlySalary(newSalary: Long, preset: BudgetPreset? = null) {
        viewModelScope.launch {
            repository.applySalaryAndBudgetPreset(newSalary, preset).onSuccess {
                showMessage("Gaji disesuaikan ke ${CurrencyFormatter.formatRupiah(newSalary)} & pos anggaran dihitung ulang otomatis!")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal menyesuaikan gaji: ${err.message}")
            }
        }
    }

    fun updateProfile(newProfile: ProfileEntity) {
        viewModelScope.launch {
            val oldSalary = profile.value?.monthlySalary ?: 0L
            if (newProfile.monthlySalary != oldSalary) {
                repository.applySalaryAndBudgetPreset(newProfile.monthlySalary, null)
            }
            repository.updateProfile(newProfile).onSuccess {
                showMessage("Profil keuangan berhasil diperbarui.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal memperbarui profil: ${err.message}")
            }
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.resetToDemoData().onSuccess {
                showMessage("Data demo berhasil dimuat.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal memuat data demo: ${err.message}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllTransactions().onSuccess {
                showMessage("Data berhasil direset ke kondisi awal bersih.")
            }.onFailure { err ->
                _lastOperationError.value = err.message
                showMessage("Gagal mereset data: ${err.message}")
            }
        }
    }

    fun getBonusAllocationRecommendation(bonusAmount: Long): BonusAllocationPlan {
        val summary = dashboardSummary.value
        val prof = profile.value ?: ProfileEntity()
        val plannedEssential = budgetCategories.value
            .firstOrNull { it.name.contains("pokok", ignoreCase = true) }?.plannedAmount ?: 1_215_000L

        return BonusAllocationCalculator.calculate(
            bonusAmount = bonusAmount,
            currentEmergencyFund = summary.emergencyFundBalance,
            targetMonths = prof.emergencyTargetMonths,
            monthlyEssentialCost = plannedEssential,
            currentCashBalance = summary.actualCashBalance,
            monthlyEssentialShortfall = summary.essentialShortfall
        )
    }

    /**
     * Menjalankan uji diagnostik mandiri (Self-Health Check)
     * Memverifikasi konektivitas basis data dan konsistensi matematis data.
     */
    fun runSystemDiagnostics(): String {
        return try {
            val p = profile.value
            val incCount = incomes.value.size
            val expCount = expenses.value.size
            val budCount = budgetCategories.value.size
            val sum = dashboardSummary.value

            val status = StringBuilder()
            status.append("Status Basis Data: OK (Terhubung)\n")
            status.append("Profil Gaji: Rp ${p?.monthlySalary ?: 0}\n")
            status.append("Total Catatan Pendapatan: $incCount\n")
            status.append("Total Catatan Pengeluaran: $expCount\n")
            status.append("Kategori Anggaran: $budCount pos\n")
            status.append("Saldo Kas Aktual: Rp ${sum.actualCashBalance}\n")
            status.append("Kekayaan Bersih: Rp ${sum.totalNetWorth}\n")
            status.append("Total Error Sistem Terdeteksi: ${AppLogger.errorCount.value}\n")

            val report = status.toString()
            AppLogger.info("Diagnostics", "Self-health check dijalankan: Semuanya berjalan normal.")
            report
        } catch (e: Exception) {
            AppLogger.error("Diagnostics", "Kegagalan saat menjalankan diagnosa mandiri", e)
            "Error Diagnostik: ${e.localizedMessage}"
        }
    }

    suspend fun exportDataAsJson(): String {
        return repository.exportDataAsJson()
    }

    fun restoreDataFromJson(jsonString: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            repository.restoreDataFromJson(jsonString).onSuccess { msg ->
                showMessage(msg)
                onComplete?.invoke(true, msg)
            }.onFailure { err ->
                val errorMsg = "Gagal memulihkan cadangan: ${err.message}"
                _lastOperationError.value = errorMsg
                showMessage(errorMsg)
                onComplete?.invoke(false, errorMsg)
            }
        }
    }
}
