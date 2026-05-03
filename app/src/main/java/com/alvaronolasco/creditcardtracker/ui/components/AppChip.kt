package com.alvaronolasco.creditcardtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val chipSelectedContainer = if (isDark) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primary
    val chipSelectedLabel = Color.White
    val chipLabel = MaterialTheme.colorScheme.onSurfaceVariant
    val chipContainer = MaterialTheme.colorScheme.surfaceVariant
    val chipTrailingTint = if (selected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = modifier,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = chipSelectedContainer,
            selectedLabelColor = chipSelectedLabel,
            labelColor = chipLabel,
            containerColor = chipContainer
        ),
        border = null,
        trailingIcon = if (onDelete != null) {
            {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar categoría",
                    modifier = Modifier
                        .clickable(onClick = onDelete),
                    tint = chipTrailingTint
                )
            }
        } else null
    )
}
