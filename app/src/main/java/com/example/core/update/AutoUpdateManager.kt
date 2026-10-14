package com.example.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val releaseNotes: String
)

object AutoUpdateManager {

    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_GITHUB_REPO = "github_repo"

    fun getSavedRepo(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GITHUB_REPO, "") ?: ""
    }

    fun saveRepo(context: Context, repo: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GITHUB_REPO, repo.trim()).apply()
    }

    suspend fun checkForUpdate(context: Context? = null, customRepoOrUrl: String? = null): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val repoInput = customRepoOrUrl ?: context?.let { getSavedRepo(it) }.takeIf { !it.isNullOrBlank() } ?: ""
            val urlStr = if (repoInput.isNotBlank()) {
                if (repoInput.startsWith("http://") || repoInput.startsWith("https://")) {
                    repoInput
                } else {
                    "https://api.github.com/repos/$repoInput/releases/latest"
                }
            } else {
                "https://api.github.com/repos/owner/repo/releases/latest"
            }

            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "DompetPintar-App")
            }
            
            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                
                if (json.has("tag_name")) {
                    val tagName = json.getString("tag_name")
                    val body = json.optString("body", "Pembaruan rutin stabilitas & fitur baru dari GitHub.")
                    val assets = json.optJSONArray("assets")
                    var apkUrl = ""
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (apkUrl.isBlank()) {
                        apkUrl = json.optString("apkUrl", "")
                    }
                    
                    val remoteVersionName = tagName.removePrefix("v").removePrefix("V")
                    val remoteVersionCode = json.optInt("versionCode", remoteVersionName.replace(".", "").toIntOrNull() ?: 1)
                    
                    return@withContext UpdateInfo(
                        versionName = remoteVersionName,
                        versionCode = remoteVersionCode,
                        apkUrl = apkUrl,
                        releaseNotes = body
                    )
                } else if (json.has("versionName")) {
                    return@withContext UpdateInfo(
                        versionName = json.getString("versionName"),
                        versionCode = json.getInt("versionCode"),
                        apkUrl = json.getString("apkUrl"),
                        releaseNotes = json.optString("releaseNotes", "Pembaruan aplikasi dari GitHub")
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun downloadAndInstallApk(context: Context, apkUrl: String, onStatusChanged: (String) -> Unit) {
        try {
            onStatusChanged("Memulai mengunduh pembaruan APK dari GitHub...")
            val destination = File(context.cacheDir, "update.apk")
            if (destination.exists()) {
                destination.delete()
            }

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Pembaruan Dompet Pintar (GitHub)")
                .setDescription("Mengunduh versi terbaru...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor: Cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (statusIndex != -1) {
                                val status = cursor.getInt(statusIndex)
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    onStatusChanged("Unduhan selesai. Membuka installer...")
                                    installApk(context, destination)
                                } else {
                                    onStatusChanged("Unduhan gagal atau dibatalkan.")
                                }
                            }
                        }
                        cursor.close()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
            onStatusChanged("Unduhan berjalan di latar belakang...")

        } catch (e: Exception) {
            onStatusChanged("Gagal mengunduh: ${e.localizedMessage}")
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
