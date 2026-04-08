package com.alvaronolasco.creditcardtracker.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.alvaronolasco.creditcardtracker.ui.theme.MintGreen
import com.alvaronolasco.creditcardtracker.ui.theme.SoftLime

data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val iconBackgroundColor: Color,
    val isNamePage: Boolean = false
)

val onboardingPages = listOf(
    OnboardingPageData(
        icon = Icons.Default.CreditCard,
        title = "Tus tarjetas, organizadas",
        description = "Agrega todas tus tarjetas de crédito con colores personalizados. Visualiza límites, fechas de corte y fechas de pago en un solo lugar.",
        iconBackgroundColor = MintGreen
    ),
    OnboardingPageData(
        icon = Icons.Default.Receipt,
        title = "Registra cada gasto",
        description = "Lleva el control de tus gastos por categoría. Soporta meses sin intereses (MSI) y te muestra exactamente cuánto debes en cada periodo.",
        iconBackgroundColor = SoftLime.copy(alpha = 0.4f)
    ),
    OnboardingPageData(
        icon = Icons.Default.CameraAlt,
        title = "Escanea tus tickets",
        description = "Usa la cámara para escanear recibos y extraer montos automáticamente. Registrar gastos nunca fue tan rápido.",
        iconBackgroundColor = MintGreen
    ),
    OnboardingPageData(
        icon = Icons.Default.Notifications,
        title = "Nunca olvides un pago",
        description = "Recibe notificaciones antes de tus fechas de corte y pago. Configura recordatorios personalizados para cada tarjeta.",
        iconBackgroundColor = SoftLime.copy(alpha = 0.4f)
    ),
    OnboardingPageData(
        icon = Icons.Default.PieChart,
        title = "Planifica tu presupuesto",
        description = "Establece presupuestos mensuales por categoría y monitorea tu progreso. Toma el control de tus finanzas.",
        iconBackgroundColor = MintGreen
    ),
    OnboardingPageData(
        icon = Icons.Default.AccountBalanceWallet,
        title = "Ingresos y widget",
        description = "Configura tus fuentes de ingreso y ve la relación ingreso-gasto. Agrega el widget a tu pantalla de inicio para consultar saldos al instante.",
        iconBackgroundColor = SoftLime.copy(alpha = 0.4f)
    ),
    OnboardingPageData(
        icon = Icons.Default.Person,
        title = "¿Cómo te llamas?",
        description = "Tu nombre solo se guarda en este dispositivo para personalizar tu experiencia.",
        iconBackgroundColor = MintGreen,
        isNamePage = true
    )
)
