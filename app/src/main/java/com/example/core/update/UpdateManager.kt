package com.example.core.update

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.example.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val jsonUrl: String = "https://raw.githubusercontent.com/owner/repo/main/version.json"
) {
    private var checkJob: Job? = null

    data class RemoteVersionInfo(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String,
        val releaseNotes: String
    )

    fun startPeriodicCheck(intervalMillis: Long = 4 * 60 * 60 * 1000L) { // default 4 hours
        stopPeriodicCheck()
        checkJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                checkForUpdateAndNotify()
                delay(intervalMillis)
            }
        }
    }

    fun stopPeriodicCheck() {
        checkJob?.cancel()
        checkJob = null
    }

    suspend fun checkForUpdateAndNotify() {
        val remoteInfo = fetchRemoteVersion() ?: return
        if (isUpdateAvailable(BuildConfig.VERSION_NAME, remoteInfo.versionName)) {
            withContext(Dispatchers.Main) {
                val result = snackbarHostState.showSnackbar(
                    message = "Pembaruan versi ${remoteInfo.versionName} tersedia di GitHub",
                    actionLabel = "Unduh",
                    withDismissAction = true
                )
                if (result == SnackbarResult.ActionPerformed) {
                    AutoUpdateManager.downloadAndInstallApk(context, remoteInfo.apkUrl) { status ->
                        coroutineScope.launch(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(status)
                        }
                    }
                }
            }
        }
    }

    suspend fun fetchRemoteVersion(): RemoteVersionInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(jsonUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "AppUpdateManager")
            }
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val versionName = json.optString("versionName", "")
                val versionCode = json.optInt("versionCode", 0)
                val apkUrl = json.optString("apkUrl", "")
                val releaseNotes = json.optString("releaseNotes", "")
                if (versionName.isNotBlank() && apkUrl.isNotBlank()) {
                    return@withContext RemoteVersionInfo(versionName, versionCode, apkUrl, releaseNotes)
                }
            }
        } catch (_: Exception) {}
        
        // Fallback to GitHub Releases API if raw json fails or if configured via AutoUpdateManager repo
        try {
            val savedRepo = AutoUpdateManager.getSavedRepo(context)
            val repo = if (savedRepo.isNotBlank()) savedRepo else "owner/repo"
            val apiInfo = AutoUpdateManager.checkForUpdate(context, repo)
            if (apiInfo != null) {
                return@withContext RemoteVersionInfo(
                    versionName = apiInfo.versionName,
                    versionCode = apiInfo.versionCode,
                    apkUrl = apiInfo.apkUrl,
                    releaseNotes = apiInfo.releaseNotes
                )
            }
        } catch (_: Exception) {}

        null
    }

    fun isUpdateAvailable(currentVersion: String, remoteVersion: String): Boolean {
        try {
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = remoteVersion.trim().removePrefix("v").removePrefix("V")
            
            val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(currentParts.size, remoteParts.size)
            for (i in 0 until maxLength) {
                val c = currentParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
