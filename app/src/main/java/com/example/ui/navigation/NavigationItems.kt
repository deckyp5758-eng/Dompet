package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainDestination(val title: String, val icon: ImageVector) {
    BERANDA("Beranda", Icons.Default.Home),
    TRANSAKSI("Transaksi", Icons.Default.ReceiptLong),
    ANGGARAN("Anggaran", Icons.Default.AccountBalanceWallet),
    BONUS("Bonus", Icons.Default.CardGiftcard),
    DANA_DARURAT("Darurat", Icons.Default.Shield),
    INVESTASI("Investasi", Icons.Default.TrendingUp),
    LAPORAN("Laporan", Icons.Default.Assessment),
    PENGATURAN("Pengaturan", Icons.Default.Settings)
}
