package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.BuildConfig
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import java.util.Calendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PaydayMode
import com.example.data.local.ProfileEntity
import com.example.data.model.CurrencyFormatter
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryEmerald
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    profile: ProfileEntity,
    onUpdateProfile: (ProfileEntity) -> Unit,
    onClearAllData: () -> Unit,
    onExportJson: suspend () -> String,
    onRestoreJson: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var livingSituation by remember(profile.livingSituation) { mutableStateOf(profile.livingSituation) }
    var monthlySalaryText by remember(profile.monthlySalary) { mutableStateOf(profile.monthlySalary.toString()) }
    var selectedPaydayMode by remember(profile.paydayMode) { mutableStateOf(profile.paydayMode) }
    var paydayDayOfMonthText by remember(profile.paydayDayOfMonth) { mutableStateOf(profile.paydayDayOfMonth.toString()) }
    var targetMonthsText by remember(profile.emergencyTargetMonths) { mutableStateOf(profile.emergencyTargetMonths.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("") }
    var availableUpdateInfo by remember { mutableStateOf<com.example.core.update.UpdateInfo?>(null) }
    var showUpdateAvailableDialog by remember { mutableStateOf(false) }
    var showUpToDateDialog by remember { mutableStateOf(false) }
    var githubRepoInput by remember { mutableStateOf(com.example.core.update.AutoUpdateManager.getSavedRepo(context)) }

    val currentMaxDays = remember { Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pengaturan & Akun",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Kelola preferensi finansial, cadangan data, dan diagnostik aplikasi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile & Gaji Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Profil Finansial & Kalender", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Pengaturan dasar perhitungan alokasi & siklus gajian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = livingSituation,
                        onValueChange = { livingSituation = it },
                        label = { Text("Situasi Tempat Tinggal") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = monthlySalaryText,
                        onValueChange = { monthlySalaryText = it },
                        label = { Text("Gaji Tetap Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section: Pilihan Tanggal Gajian Bulanan
                    Text(
                        text = "Jadwal & Siklus Tanggal Gajian",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dasar otomatis perhitungan berapa hari lagi menuju gajian & batas belanja harian",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pilihan: Awal Bulan / Akhir Bulan (Otomatis) / Tanggal Spesifik
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaydayOptionCard(
                            title = "Awal Bulan",
                            subtitle = "Tgl 1",
                            badge = null,
                            isSelected = selectedPaydayMode == PaydayMode.START_OF_MONTH,
                            onClick = { selectedPaydayMode = PaydayMode.START_OF_MONTH },
                            modifier = Modifier.weight(1f)
                        )

                        PaydayOptionCard(
                            title = "Akhir Bulan",
                            subtitle = "Otomatis",
                            badge = "Rekomendasi",
                            isSelected = selectedPaydayMode == PaydayMode.END_OF_MONTH,
                            onClick = { selectedPaydayMode = PaydayMode.END_OF_MONTH },
                            modifier = Modifier.weight(1.2f)
                        )

                        PaydayOptionCard(
                            title = "Spesifik",
                            subtitle = "Tgl 1-31",
                            badge = null,
                            isSelected = selectedPaydayMode == PaydayMode.SPECIFIC_DAY,
                            onClick = { selectedPaydayMode = PaydayMode.SPECIFIC_DAY },
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    if (selectedPaydayMode == PaydayMode.SPECIFIC_DAY) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = paydayDayOfMonthText,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }.take(2)
                                paydayDayOfMonthText = digitsOnly
                            },
                            label = { Text("Pilih Tanggal Tetap (1 - 31)") },
                            placeholder = { Text("Contoh: 25") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryEmerald)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val previewDay = when (selectedPaydayMode) {
                        PaydayMode.START_OF_MONTH -> 1
                        PaydayMode.END_OF_MONTH -> currentMaxDays
                        else -> paydayDayOfMonthText.toIntOrNull()?.coerceIn(1, 31) ?: profile.paydayDayOfMonth
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryEmerald.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (selectedPaydayMode == PaydayMode.END_OF_MONTH) Icons.Default.Star else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = PrimaryEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when (selectedPaydayMode) {
                                        PaydayMode.END_OF_MONTH -> "Siklus Otomatis: Akhir Bulan (Tgl $currentMaxDays)"
                                        PaydayMode.START_OF_MONTH -> "Siklus Aktif: Awal Bulan (Setiap Tgl 1)"
                                        else -> "Siklus Aktif: Setiap tanggal $previewDay"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = when (selectedPaydayMode) {
                                        PaydayMode.END_OF_MONTH -> "Otomatis menyesuaikan jumlah hari tiap bulan (28/29 Feb, 30 atau 31 hari) tanpa perlu diubah manual."
                                        PaydayMode.START_OF_MONTH -> "Gajian selalu dihitung pada hari pertama tiap bulan baru."
                                        else -> "Gajian dihitung tetap pada tanggal $previewDay setiap bulannya."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = targetMonthsText,
                        onValueChange = { targetMonthsText = it },
                        label = { Text("Target Dana Darurat (Jumlah Bulan)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val parsedSalary = monthlySalaryText.toLongOrNull() ?: profile.monthlySalary
                            val parsedPayday = (paydayDayOfMonthText.toIntOrNull() ?: profile.paydayDayOfMonth).coerceIn(1, 31)
                            val parsedMonths = targetMonthsText.toIntOrNull() ?: profile.emergencyTargetMonths
                            onUpdateProfile(
                                profile.copy(
                                    livingSituation = livingSituation.trim(),
                                    monthlySalary = parsedSalary,
                                    emergencyTargetMonths = parsedMonths,
                                    paydayMode = selectedPaydayMode,
                                    paydayDayOfMonth = parsedPayday
                                )
                            )
                            onShowMessage("Profil finansial & siklus gajian berhasil disimpan!")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Perubahan Profil", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Diagnostik & Pemecahan Masalah
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDiagnostics)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Diagnostik & Log Sistem", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Pantau kesehatan database, histori log, dan deteksi error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Data Management (Export & Reset)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Manajemen & Cadangan Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val jsonStr = onExportJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Backup Dompet Pintar", jsonStr))

                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Backup Dompet Pintar")
                                        putExtra(Intent.EXTRA_TEXT, jsonStr)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan Cadangan Data"))
                                } catch (e: Exception) {
                                    // fallback if chooser fails
                                }
                                onShowMessage("Data berhasil diekspor & disalin ke clipboard!")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor & Bagikan Cadangan (JSON)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pulihkan Data dari Cadangan")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Semua Data Aplikasi")
                    }
                }
            }
        }

        // Auto Update Card (GitHub OTA)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pembaruan Otomatis via GitHub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Versi Terpasang: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = { githubRepoInput = it },
                        label = { Text("Repository GitHub (owner/repo)") },
                        placeholder = { Text("contoh: username/dompet-pintar") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (updateStatusText.isNotBlank()) {
                        Text(
                            text = updateStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            com.example.core.update.AutoUpdateManager.saveRepo(context, githubRepoInput)
                            isCheckingUpdate = true
                            updateStatusText = "Memeriksa rilis terbaru di GitHub..."
                            scope.launch {
                                val info = com.example.core.update.AutoUpdateManager.checkForUpdate(context, githubRepoInput)
                                isCheckingUpdate = false
                                if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                                    availableUpdateInfo = info
                                    updateStatusText = "Versi baru ${info.versionName} tersedia di GitHub!"
                                    showUpdateAvailableDialog = true
                                } else if (info != null) {
                                    updateStatusText = "Aplikasi Anda (${BuildConfig.VERSION_NAME}) sudah menggunakan versi terbaru."
                                    showUpToDateDialog = true
                                } else {
                                    updateStatusText = "Gagal memeriksa GitHub. Periksa kembali nama repository (owner/repo)."
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCheckingUpdate) "Memeriksa GitHub..." else "Periksa Pembaruan dari GitHub")
                    }
                }
            }
        }

        // App Info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dompet Pintar v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aplikasi Pengelola Keuangan Pribadi & Alokasi Bonus Cerdas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AccentRose) },
            title = { Text("Reset Seluruh Data?") },
            text = { Text("Tindakan ini akan menghapus semua transaksi, pos anggaran, dana darurat, dan investasi. Data yang sudah dihapus tidak dapat dipulihkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Batal")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = { Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Pulihkan Data dari Cadangan") },
            text = {
                Column {
                    Text(
                        text = "Tempel teks JSON cadangan yang telah Anda ekspor sebelumnya. Data saat ini akan digantikan dengan data cadangan secara atomik.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("Tempel JSON cadangan di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            onRestoreJson(restoreJsonInput.trim())
                            showRestoreDialog = false
                            restoreJsonInput = ""
                        }
                    },
                    enabled = restoreJsonInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Pulihkan Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreDialog = false
                        restoreJsonInput = ""
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showUpdateAvailableDialog && availableUpdateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateAvailableDialog = false },
            icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Pembaruan Tersedia (${availableUpdateInfo?.versionName})") },
            text = {
                Column {
                    Text(
                        text = "Versi baru aplikasi (${availableUpdateInfo?.versionName}) siap diunduh dan diinstal secara otomatis.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Catatan Rilis:\n${availableUpdateInfo?.releaseNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateAvailableDialog = false
                        availableUpdateInfo?.let { info ->
                            com.example.core.update.AutoUpdateManager.downloadAndInstallApk(context, info.apkUrl) { status ->
                                updateStatusText = status
                                onShowMessage(status)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Unduh & Instal")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showUpdateAvailableDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Nanti Saja")
                }
            }
        )
    }

    if (showUpToDateDialog) {
        AlertDialog(
            onDismissRequest = { showUpToDateDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Aplikasi Terkini") },
            text = { Text("Aplikasi Dompet Pintar Anda sudah menggunakan versi paling mutakhir (${BuildConfig.VERSION_NAME}).") },
            confirmButton = {
                Button(
                    onClick = { showUpToDateDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PaydayOptionCard(
    title: String,
    subtitle: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 9.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(13.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
