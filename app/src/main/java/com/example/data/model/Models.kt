package com.example.data.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class IncomeType(val displayName: String) {
    GAJI_TETAP("Gaji Tetap"),
    BONUS("Bonus / Pemberian"),
    LAINNYA("Pendapatan Lainnya")
}

enum class IncomeStatus(val displayName: String) {
    DITERIMA("Diterima"),
    BELUM_DITERIMA("Belum Diterima")
}

enum class ExpenseCategory(val displayName: String, val isEssential: Boolean = false) {
    MAKAN_POKOK("Makan & Kebutuhan Pokok", true),
    KONTRIBUSI_KELUARGA("Kontribusi Keluarga", true),
    TRANSPORTASI("Transportasi", true),
    PULSA_INTERNET("Pulsa & Internet", true),
    TAGIHAN("Tagihan Wajib", true),
    KESEHATAN("Kesehatan", true),
    KEPERLUAN_PRIBADI("Keperluan Pribadi", false),
    PENDIDIKAN("Pendidikan & Pengembangan Diri", false),
    INVESTASI("Investasi", false),
    DANA_DARURAT("Dana Darurat", false),
    LAINNYA("Lainnya", false);

    companion object {
        fun fromDisplayName(name: String): ExpenseCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: LAINNYA
        }
    }
}

enum class PaymentMethod(val displayName: String) {
    TUNAI("Tunai"),
    TRANSFER_BANK("Transfer Bank"),
    E_WALLET("E-Wallet"),
    QRIS("QRIS"),
    LAINNYA("Lainnya")
}

enum class FundChangeType(val displayName: String) {
    SETORAN("Setoran"),
    PENARIKAN("Penarikan")
}

enum class AssetType(val displayName: String) {
    PASAR_UANG("Reksadana Pasar Uang"),
    OBLIGASI_SBN("Obligasi / SBN"),
    SAHAM("Saham"),
    EMAS("Emas Batangan"),
    LAINNYA("Aset Lainnya")
}

object CurrencyFormatter {
    private val rupiahLocale = Locale("id", "ID")
    private val rupiahFormat = NumberFormat.getCurrencyInstance(rupiahLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return rupiahFormat.format(amount).replace("Rp", "Rp ")
    }

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", rupiahLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatShortDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yy", rupiahLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatMonthYear(timeMillis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", rupiahLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatFullDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", rupiahLocale)
        return sdf.format(Date(timeMillis))
    }

    fun formatDayMonth(timeMillis: Long): String {
        val sdf = SimpleDateFormat("d MMMM", rupiahLocale)
        return sdf.format(Date(timeMillis))
    }
}

enum class BudgetPreset(
    val displayName: String,
    val subtitle: String,
    val description: String
) {
    DOMPET_PINTAR(
        "Dompet Pintar (45/15/20/5/10/5)",
        "Keluarga 45% • Tagihan 15% • Darurat 20% • Investasi 5% • Pribadi 10% • Fleksibel 5%",
        "Metode ramah pemula yang tinggal bersama keluarga. Mengamankan kebutuhan pokok dan membentuk dana darurat secara bertahap."
    ),
    RULE_50_30_20(
        "Aturan Populer 50/30/20",
        "Kebutuhan Pokok 50% • Keinginan 30% • Tabungan & Investasi 20%",
        "Metode standar global dari Senator Elizabeth Warren. Membagi penghasilan ke dalam Kebutuhan (50%), Keinginan (30%), dan Tabungan/Investasi (20%)."
    ),
    FIRE_AGGRESSIVE(
        "Prioritas Investasi / FIRE (35/15/10/30/5/5)",
        "Kebutuhan 35% • Tagihan 15% • Darurat 10% • Investasi 30% • Pribadi 5% • Fleksibel 5%",
        "Metode agresif bagi pejuang kemandirian finansial dan pensiun dini (Financial Independence, Retire Early)."
    ),
    CURRENT_PERCENTAGES(
        "Pertahankan Rasio Saat Ini",
        "Hitung ulang otomatis berdasarkan persentase yang sudah Anda sesuaikan",
        "Mempertahankan komposisi persentase yang sedang aktif dan memperbarui nominal Rupiah sesuai gaji baru."
    )
}

