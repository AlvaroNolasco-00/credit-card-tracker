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
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

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
                DeveloperMessageCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            item {
                DonationTiersSection(
                    modifier = Modifier.padding(horizontal = 16.dp),
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
    onTierClick: (String) -> Unit
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Elige una contribución",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF003087).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = Color(0xFF003087),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "PayPal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003087)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        DonationTierCard(
            icon = Icons.Default.Coffee,
            label = "Un café",
            amount = "$2 USD · vía PayPal",
            iconBgColor = SoftLime.copy(alpha = 0.4f),
            iconTint = ForestGreen,
            onClick = { onTierClick(DONATION_URL_COFFEE) }
        )
        Spacer(Modifier.height(10.dp))
        DonationTierCard(
            icon = Icons.Default.Restaurant,
            label = "Un almuerzo",
            amount = "$5 USD · vía PayPal",
            iconBgColor = MintGreen.copy(alpha = 0.5f),
            iconTint = ForestGreen,
            onClick = { onTierClick(DONATION_URL_LUNCH) }
        )
        Spacer(Modifier.height(10.dp))
        DonationTierCard(
            icon = Icons.Default.Wifi,
            label = "Un día de internet",
            amount = "$10 USD · vía PayPal",
            containerColor = ForestGreen,
            iconBgColor = Color.White.copy(alpha = 0.2f),
            iconTint = Color.White,
            labelColor = Color.White,
            amountColor = Color.White.copy(alpha = 0.8f),
            trailingTint = Color.White.copy(alpha = 0.8f),
            onClick = { onTierClick(DONATION_URL_INTERNET) }
        )
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
        OutlinedButton(
            onClick = onShareClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ForestGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.5f))
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
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.alvaronolasco.creditcardtracker"
private const val SHARE_TEXT = "Lleva el control de tus tarjetas de crédito con esta app: $PLAY_STORE_URL"
