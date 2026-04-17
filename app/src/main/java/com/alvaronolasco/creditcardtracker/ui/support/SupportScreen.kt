package com.alvaronolasco.creditcardtracker.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen
import com.alvaronolasco.creditcardtracker.ui.theme.MintGreen
import com.alvaronolasco.creditcardtracker.ui.theme.SoftLime

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    onOnboardingClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.Wompi) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Apoya al desarrollador",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SupportHeader() }
            item {
                AppTourCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    onClick = onOnboardingClick
                )
            }
            item {
                DeveloperMessageCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            item {
                DonationTiersSection(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodChange = { selectedPaymentMethod = it },
                    onTierClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
            item {
                OtherSupportSection(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    onRateClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                        )
                    },
                    onShareClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir app"))
                    }
                )
            }
        }
    }
}

@Composable
private fun SupportHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MintGreen)
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Gracias por usar Credit Card Tracker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ForestGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun AppTourCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SoftLime.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ver tour de la app",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Repasa todas las funciones disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DeveloperMessageCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Un mensaje del desarrollador",
                style = MaterialTheme.typography.labelLarge,
                color = ForestGreen,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Hola! Soy un desarrollador independiente y construí esta app con mucho esfuerzo para ayudarte a tener el control de tus finanzas.\n\nSi te ha sido útil, considera invitarme un café — cada contribución ayuda a mantener y mejorar la app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun DonationTiersSection(
    modifier: Modifier = Modifier,
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onTierClick: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = "Elige una contribución",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        
        // Payment method selector
        Row(modifier = Modifier.fillMaxWidth()) {
            PaymentMethodChip(
                label = "💳 Tarjeta",
                sublabel = "Wompi",
                isSelected = selectedPaymentMethod == PaymentMethod.Wompi,
                onClick = { onPaymentMethodChange(PaymentMethod.Wompi) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            PaymentMethodChip(
                label = "🅿️ PayPal",
                sublabel = "Cuenta",
                isSelected = selectedPaymentMethod == PaymentMethod.PayPal,
                onClick = { onPaymentMethodChange(PaymentMethod.PayPal) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        val isWompi = selectedPaymentMethod == PaymentMethod.Wompi
        val coffeeUrl = if (isWompi) WOMPI_URL_COFFEE else DONATION_URL_COFFEE
        val lunchUrl = if (isWompi) WOMPI_URL_LUNCH else DONATION_URL_LUNCH
        val internetUrl = if (isWompi) WOMPI_URL_INTERNET else DONATION_URL_INTERNET
        
        val viaText = if (isWompi) "vía Wompi" else "vía PayPal"
        val coffeeAmount = if (isWompi) "$3 USD" else "$2 USD"
        val lunchAmount = if (isWompi) "$5 USD" else "$5 USD"
        val internetAmount = if (isWompi) "$10 USD" else "$10 USD"
        
        DonationTierCard(
            icon = Icons.Default.Coffee,
            label = "Un café",
            amount = "$coffeeAmount · $viaText",
            iconBgColor = SoftLime.copy(alpha = 0.4f),
            iconTint = ForestGreen,
            onClick = { onTierClick(coffeeUrl) }
        )
        Spacer(Modifier.height(10.dp))
        DonationTierCard(
            icon = Icons.Default.Restaurant,
            label = "Un almuerzo",
            amount = "$lunchAmount · $viaText",
            iconBgColor = MintGreen.copy(alpha = 0.5f),
            iconTint = ForestGreen,
            onClick = { onTierClick(lunchUrl) }
        )
        Spacer(Modifier.height(10.dp))
        DonationTierCard(
            icon = Icons.Default.Wifi,
            label = "Un día de internet",
            amount = "$internetAmount · $viaText",
            containerColor = ForestGreen,
            iconBgColor = Color.White.copy(alpha = 0.2f),
            iconTint = Color.White,
            labelColor = Color.White,
            amountColor = Color.White.copy(alpha = 0.8f),
            trailingTint = Color.White.copy(alpha = 0.8f),
            onClick = { onTierClick(internetUrl) }
        )
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    sublabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) ForestGreen else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) ForestGreen else MaterialTheme.colorScheme.outlineVariant
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DonationTierCard(
    icon: ImageVector,
    label: String,
    amount: String,
    onClick: () -> Unit,
    iconBgColor: Color = SoftLime.copy(alpha = 0.3f),
    iconTint: Color = ForestGreen,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    amountColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (containerColor == MaterialTheme.colorScheme.surface)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(amount, style = MaterialTheme.typography.bodySmall, color = amountColor)
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = trailingTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OtherSupportSection(
    modifier: Modifier = Modifier,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = "Otras formas de apoyar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MintGreen,
                contentColor = ForestGreen
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Calificar en Play Store", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onShareClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreen,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Compartir la app", fontWeight = FontWeight.Bold)
        }
    }
}

private const val DONATION_URL_COFFEE = "https://paypal.me/alvarocojonudo/2"
private const val DONATION_URL_LUNCH = "https://paypal.me/alvarocojonudo/5"
private const val DONATION_URL_INTERNET = "https://paypal.me/alvarocojonudo/10"

private const val WOMPI_URL_COFFEE = "https://s.wompi.sv/1238844zcw"
private const val WOMPI_URL_LUNCH = "https://s.wompi.sv/1238847Hjp"
private const val WOMPI_URL_INTERNET = "https://s.wompi.sv/1238848d5C"

private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.alvaronolasco.creditcardtracker"
private const val SHARE_TEXT = "Lleva el control de tus tarjetas de crédito con esta app: $PLAY_STORE_URL"

enum class PaymentMethod {
    Wompi, PayPal
}
