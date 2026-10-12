package com.example.core.logger

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val id: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale("id", "ID")).format(Date(timestamp))

    val fullFormattedMessage: String
        get() = "[$formattedTime] [${level.name}] [$tag] $message" +
                if (!stackTrace.isNullOrBlank()) "\n$stackTrace" else ""
}

/**
 * Pusat Pencatatan & Diagnostik Aplikasi Dompet Pintar.
 * Berfungsi untuk:
 * 1. Merekam semua proses penting, query, dan peringatan bisnis.
 * 2. Menangkap error atau kegagalan sistem seketika untuk diagnosis cepat.
 * 3. Menyediakan data log yang dapat dibaca dan disalin langsung dari dalam aplikasi.
 */
object AppLogger {
    private const val MAX_LOGS = 250
    private const val PREFS_NAME = "dompet_pintar_diagnostics"
    private const val KEY_LAST_CRASH = "last_unhandled_crash"
    private const val KEY_LAST_CRASH_TIME = "last_unhandled_crash_time"

    private val idCounter = AtomicLong(1L)
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _errorCount = MutableStateFlow(0)
    val errorCount: StateFlow<Int> = _errorCount.asStateFlow()

    private val _lastCrashReport = MutableStateFlow<String?>(null)
    val lastCrashReport: StateFlow<String?> = _lastCrashReport.asStateFlow()

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPersistedCrashReport()
        info("AppLogger", "Sistem log & diagnostik Dompet Pintar berhasil diinisialisasi")
    }

    private fun loadPersistedCrashReport() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val crash = prefs.getString(KEY_LAST_CRASH, null)
            val crashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0L)
            if (!crash.isNullOrBlank()) {
                val timeStr = if (crashTime > 0) {
                    SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date(crashTime))
                } else "Waktu tidak diketahui"
                val report = "Crash Terakhir ($timeStr):\n$crash"
                _lastCrashReport.value = report
                error("UncaughtCrash", "Laporan kegagalan fatal sebelumnya terdeteksi:\n$report")
            }
        } catch (e: Exception) {
            Log.e("AppLogger", "Gagal membaca crash report sebelumnya", e)
        }
    }

    fun recordCrash(thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceStr = sw.toString()
        val errorMsg = "Fatal Crash di thread '${thread.name}': ${throwable.localizedMessage}\n$stackTraceStr"

        error("CRASH", errorMsg, throwable)

        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LAST_CRASH, errorMsg)
                .putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
                .commit()
        } catch (e: Exception) {
            Log.e("AppLogger", "Gagal menyimpan crash report", e)
        }
    }

    fun clearLastCrashReport() {
        _lastCrashReport.value = null
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_LAST_CRASH).remove(KEY_LAST_CRASH_TIME).apply()
        } catch (_: Exception) {}
    }

    fun debug(tag: String, message: String) {
        appendEntry(LogLevel.DEBUG, tag, message, null)
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            println("[$tag] [DEBUG] $message")
        }
    }

    fun info(tag: String, message: String) {
        appendEntry(LogLevel.INFO, tag, message, null)
        try {
            Log.i(tag, message)
        } catch (_: Throwable) {
            println("[$tag] [INFO] $message")
        }
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let { extractStackTrace(it) }
        appendEntry(LogLevel.WARN, tag, message, stackTrace)
        try {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        } catch (_: Throwable) {
            println("[$tag] [WARN] $message ${throwable?.message ?: ""}")
        }
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let { extractStackTrace(it) }
        appendEntry(LogLevel.ERROR, tag, message, stackTrace)
        _errorCount.value = _errorCount.value + 1
        try {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } catch (_: Throwable) {
            println("[$tag] [ERROR] $message ${throwable?.message ?: ""}")
        }
    }

    private fun appendEntry(level: LogLevel, tag: String, message: String, stackTrace: String?) {
        val entry = LogEntry(
            id = idCounter.getAndIncrement(),
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )

        val current = _logs.value.toMutableList()
        current.add(0, entry) // Newest first
        if (current.size > MAX_LOGS) {
            _logs.value = current.take(MAX_LOGS)
        } else {
            _logs.value = current
        }
    }

    private fun extractStackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    fun clearLogs() {
        _logs.value = emptyList()
        _errorCount.value = 0
        info("AppLogger", "Riwayat log dibersihkan oleh pengguna")
    }

    fun exportLogsAsText(): String {
        val sb = StringBuilder()
        sb.append("=== LAPORAN LOG & DIAGNOSTIK DOMPET PINTAR ===\n")
        sb.append("Dibuat pada: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Jumlah Error: ${_errorCount.value}\n")
        sb.append("==============================================\n\n")

        _lastCrashReport.value?.let {
            sb.append("--- LAPORAN CRASH TERAKHIR ---\n")
            sb.append(it)
            sb.append("\n------------------------------\n\n")
        }

        _logs.value.forEach { entry ->
            sb.append(entry.fullFormattedMessage).append("\n")
        }
        return sb.toString()
    }
}
