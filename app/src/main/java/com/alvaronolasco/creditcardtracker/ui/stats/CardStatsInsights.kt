package com.alvaronolasco.creditcardtracker.ui.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen

enum class InsightType {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    TIP
}

data class Insight(
    val type: InsightType,
    val message: String,
    val icon: ImageVector
)

object CardStatsInsights {

    fun generateInsights(
        periods: List<PeriodStats>,
        selectedIndex: Int,
        cardName: String
    ): List<Insight> {
        if (periods.isEmpty() || selectedIndex !in periods.indices) return emptyList()

        val insights = mutableListOf<Insight>()
        val selectedPeriod = periods[selectedIndex]
        val previousPeriod = periods.getOrNull(selectedIndex - 1)

        // 1. Trend vs previous month
        if (previousPeriod != null && previousPeriod.totalExpenses > 0) {
            val change = ((selectedPeriod.totalExpenses - previousPeriod.totalExpenses) / previousPeriod.totalExpenses * 100).toFloat()
            val absChange = kotlin.math.abs(change)
            if (absChange > 5) { // Only show if change is significant (>5%)
                val isDown = change < 0
                insights.add(
                    Insight(
                        type = if (isDown) InsightType.POSITIVE else InsightType.NEGATIVE,
                        message = if (isDown) {
                            "Tu gasto bajó un ${String.format("%.1f", absChange)}% respecto al mes anterior. ¡Buen trabajo!"
                        } else {
                            "Tu gasto subió un ${String.format("%.1f", absChange)}% respecto al mes anterior."
                        },
                        icon = if (isDown) Icons.Default.TrendingDown else Icons.Default.TrendingUp
                    )
                )
            }
        }

        // 2. Dominant category
        if (selectedPeriod.categoryBreakdown.isNotEmpty()) {
            val topCategory = selectedPeriod.categoryBreakdown.first()
            if (topCategory.percentage > 0.3f) {
                insights.add(
                    Insight(
                        type = InsightType.NEUTRAL,
                        message = "${topCategory.categoryName} es tu categoría más frecuente este período (${String.format("%.0f", topCategory.percentage * 100)}% del total).",
                        icon = Icons.Default.Category
                    )
                )
            }
        }

        // 3. Credit utilization warning
        if (selectedPeriod.creditUtilizationPercent > 0) {
            when {
                selectedPeriod.creditUtilizationPercent > 0.8f -> {
                    insights.add(
                        Insight(
                            type = InsightType.NEGATIVE,
                            message = "Has usado el ${String.format("%.0f", selectedPeriod.creditUtilizationPercent * 100)}% de tu límite de crédito. Considera hacer un pago pronto.",
                            icon = Icons.Default.Warning
                        )
                    )
                }
                selectedPeriod.creditUtilizationPercent < 0.2f -> {
                    insights.add(
                        Insight(
                            type = InsightType.POSITIVE,
                            message = "Excelente control: solo has usado el ${String.format("%.0f", selectedPeriod.creditUtilizationPercent * 100)}% de tu límite de crédito.",
                            icon = Icons.Default.Verified
                        )
                    )
                }
            }
        }

        // 4. Payment status
        if (selectedPeriod.totalPaymentsAmount >= selectedPeriod.totalExpenses && selectedPeriod.totalExpenses > 0) {
            insights.add(
                Insight(
                    type = InsightType.POSITIVE,
                    message = "¡Pagaste al 100% tus gastos de este período! Mantén el buen hábito.",
                    icon = Icons.Default.CheckCircle
                )
            )
        } else if (selectedPeriod.totalPaymentsAmount > 0) {
            val remaining = selectedPeriod.totalExpenses - selectedPeriod.totalPaymentsAmount
            insights.add(
                Insight(
                    type = InsightType.TIP,
                    message = "Tienes un pendiente de ${String.format("%,.0f", remaining)} por pagar en este período.",
                    icon = Icons.Default.Info
                )
            )
        }

        // 5. Average transaction
        if (selectedPeriod.avgTransactionAmount > 0) {
            insights.add(
                Insight(
                    type = InsightType.NEUTRAL,
                    message = "Gastas en promedio ${String.format("%,.0f", selectedPeriod.avgTransactionAmount)} por transacción en este período.",
                    icon = Icons.Default.Calculate
                )
            )
        }

        // 6. Pattern: peak month
        if (periods.size > 1) {
            val maxExpensePeriod = periods.maxByOrNull { it.totalExpenses }
            if (maxExpensePeriod == selectedPeriod && selectedPeriod.totalExpenses > 0) {
                insights.add(
                    Insight(
                        type = InsightType.NEUTRAL,
                        message = "Este es tu período con más gasto en los últimos ${periods.size} meses.",
                        icon = Icons.Default.EmojiEvents
                    )
                )
            }
        }

        // 7. Low activity tip
        if (selectedPeriod.expensesCount == 0) {
            insights.add(
                Insight(
                    type = InsightType.TIP,
                    message = "No tienes gastos registrados en este período. ¿Todo bien con $cardName?",
                    icon = Icons.Default.HelpOutline
                )
            )
        }

        return insights.take(5) // Max 5 insights to avoid overwhelming the user
    }

    fun getInsightColor(type: InsightType): Color = when (type) {
        InsightType.POSITIVE -> ForestGreen
        InsightType.NEGATIVE -> Color(0xFFE53935)
        InsightType.NEUTRAL -> Color(0xFF2196F3)
        InsightType.TIP -> Color(0xFFFFA000)
    }
}
