package com.alvaronolasco.creditcardtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvaronolasco.creditcardtracker.ui.theme.ForestGreen
import com.alvaronolasco.creditcardtracker.ui.theme.SoftGray
import com.alvaronolasco.creditcardtracker.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
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
            selectedContainerColor = ForestGreen,
            selectedLabelColor = Color.White,
            labelColor = TextDark,
            containerColor = SoftGray
        ),
        border = null,
        trailingIcon = if (onDelete != null) {
            {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar categoría",
                    modifier = Modifier
                        .clickable(onClick = onDelete),
                    tint = if (selected) Color.White.copy(alpha = 0.8f) else TextDark.copy(alpha = 0.5f)
                )
            }
        } else null
    )
}
