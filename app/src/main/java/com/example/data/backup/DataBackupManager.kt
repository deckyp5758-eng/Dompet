package com.example.data.backup

import com.example.core.logger.AppLogger
import com.example.core.result.AppResult
import com.example.core.result.runCatchingApp
import com.example.data.local.FinanceDao
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manajer ekspor dan verifikasi integritas data aplikasi.
 */
object DataBackupManager {

    suspend fun exportDataAsJson(dao: FinanceDao): AppResult<String> {
        return runCatchingApp("DataBackupExport") {
            val root = JSONObject()
            val profile = dao.getProfileSync() ?: com.example.data.local.ProfileEntity()
            root.put("profile", JSONObject().apply {
                put("monthlySalary", profile.monthlySalary)
                put("livingSituation", profile.livingSituation)
                put("financialPriority", profile.financialPriority)
                put("emergencyTargetMonths", profile.emergencyTargetMonths)
            })

            val budgetCategories = dao.getBudgetCategoriesSync()
            val catArray = JSONArray()
            budgetCategories.forEach { cat ->
                catArray.put(JSONObject().apply {
                    put("name", cat.name)
                    put("percentage", cat.percentage)
                    put("plannedAmount", cat.plannedAmount)
                    put("orderIndex", cat.orderIndex)
                })
            }
            root.put("budgetCategories", catArray)
            root.put("exportedAt", System.currentTimeMillis())

            val jsonString = root.toString(2)
            AppLogger.info("DataBackupManager", "Berhasil mengekspor data profil & anggaran (${jsonString.length} bytes)")
            jsonString
        }.onFailure { error ->
            AppLogger.error("DataBackupManager", "Gagal mengekspor data ke JSON: ${error.message}", error.throwable)
        }
    }
}
