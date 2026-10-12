package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExpenseEntity
import com.example.data.model.CurrencyFormatter
import kotlin.math.min

// Category Color Palette for Charts
val ChartColors = listOf(
    Color(0xFF0D6E48), // Emerald Green
    Color(0xFF0F766E), // Teal
    Color(0xFF2563EB), // Royal Blue
    Color(0xFFD97706), // Amber / Gold
    Color(0xFFDC2626), // Crimson Red
    Color(0xFF7C3AED), // Violet
    Color(0xFFDB2777), // Pink
    Color(0xFF475569)  // Slate Muted
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutExpenseChart(
    expenses: List<ExpenseEntity>,
    modifier: Modifier = Modifier
) {
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    val categoryBreakdown = remember(expenses, totalExpense) {
        if (totalExpense <= 0L) emptyList()
        else {
            expenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(6)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Komposisi Pengeluaran Bulan Ini",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(totalExpense),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data pengeluaran untuk ditampilkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            val strokeWidth = 18.dp.toPx()
                            val arcSize = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                            var startAngle = -90f
                            categoryBreakdown.forEachIndexed { index, (_, amount) ->
                                val sweep = (amount.toFloat() / totalExpense.toFloat()) * 360f
                                val color = ChartColors[index % ChartColors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${categoryBreakdown.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kategori",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryBreakdown.take(4).forEachIndexed { index, (cat, amount) ->
                            val color = ChartColors[index % ChartColors.size]
                            val pct = ((amount.toFloat() / totalExpense.toFloat()) * 100).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = color,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeComparisonBarChart(
    salaryReceived: Long,
    bonusReceived: Long,
    otherReceived: Long,
    modifier: Modifier = Modifier
) {
    val total = (salaryReceived + bonusReceived + otherReceived).coerceAtLeast(1L)
    val salaryPct = ((salaryReceived.toFloat() / total) * 100).toInt()
    val bonusPct = ((bonusReceived.toFloat() / total) * 100).toInt()
    val otherPct = (100 - salaryPct - bonusPct).coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Perbandingan Sumber Pendapatan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(salaryReceived + bonusReceived + otherReceived),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment horizontal bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                val totalWidth = size.width
                val cornerRadius = 8.dp.toPx()

                var currentX = 0f

                // Draw background track
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )

                if (salaryReceived > 0) {
                    val w = (salaryReceived.toFloat() / total) * totalWidth
                    drawRoundRect(
                        color = Color(0xFF0D6E48), // Emerald
                        topLeft = Offset(currentX, 0f),
                        size = Size(w, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )
                    currentX += w
                }

                if (bonusReceived > 0) {
                    val w = (bonusReceived.toFloat() / total) * totalWidth
                    drawRoundRect(
                        color = Color(0xFFD97706), // Amber
                        topLeft = Offset(currentX, 0f),
                        size = Size(w, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )
                    currentX += w
                }

                if (otherReceived > 0) {
                    val w = (otherReceived.toFloat() / total) * totalWidth
                    drawRoundRect(
                        color = Color(0xFF2563EB), // Blue
                        topLeft = Offset(currentX, 0f),
                        size = Size(w, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendPill(
                    color = Color(0xFF0D6E48),
                    label = "Gaji Tetap",
                    amount = salaryReceived,
                    pct = salaryPct
                )
                LegendPill(
                    color = Color(0xFFD97706),
                    label = "Bonus/Pemberian",
                    amount = bonusReceived,
                    pct = bonusPct
                )
                if (otherReceived > 0) {
                    LegendPill(
                        color = Color(0xFF2563EB),
                        label = "Lainnya",
                        amount = otherReceived,
                        pct = otherPct
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendPill(
    color: Color,
    label: String,
    amount: Long,
    pct: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = "$label ($pct%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.formatRupiah(amount),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
