package com.alvaronolasco.creditcardtracker.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alvaronolasco.creditcardtracker.ui.components.AppButton
import com.alvaronolasco.creditcardtracker.ui.components.AppOutlinedButton
import com.alvaronolasco.creditcardtracker.ui.components.AppTopBar
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen
import com.alvaronolasco.creditcardtracker.ui.theme.MintGreen

@Composable
fun AuthLandingScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MintGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Guarda tus datos en la nube",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Crea una cuenta para sincronizar tus tarjetas y gastos entre dispositivos. Tus datos actuales se vincularán automáticamente.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            AppButton(
                text = "Crear cuenta",
                onClick = onRegister
            )

            Spacer(Modifier.height(12.dp))

            AppOutlinedButton(
                text = "Ya tengo cuenta",
                onClick = onLogin
            )
        }
    }
}
