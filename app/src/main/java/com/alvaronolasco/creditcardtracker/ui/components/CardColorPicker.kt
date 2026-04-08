package com.alvaronolasco.creditcardtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.alvaronolasco.creditcardtracker.ui.theme.*

@Composable
fun CardColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val predefinedColors = listOf(
        CardBlue,
        CardGreen,
        CardRed,
        CardYellow,
        CardPurple,
        CardOrange,
        CardDark
    )
    
    var showColorPicker by remember { mutableStateOf(false) }
    var tempColor by remember { mutableStateOf(selectedColor) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Predefined color palette
        predefinedColors.forEach { color ->
            val isSelected = selectedColor == color
            ColorCircle(
                color = color,
                isSelected = isSelected,
                onClick = { onColorSelected(color) }
            )
        }

        // Custom color picker button
        val isCustomSelected = !predefinedColors.contains(selectedColor)
        CustomColorButton(
            isSelected = isCustomSelected,
            currentColor = selectedColor,
            onClick = { showColorPicker = true }
        )
    }

    // Native Color Picker Dialog
    if (showColorPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Color Personalizado") },
            text = {
                ColorPickerDialogContent(
                    initialColor = selectedColor,
                    onColorChanged = { tempColor = it }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onColorSelected(tempColor)
                        showColorPicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color = color)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, ForestGreen, CircleShape)
                } else Modifier
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun CustomColorButton(
    isSelected: Boolean,
    currentColor: Color,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color = currentColor)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, ForestGreen, CircleShape)
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Add,
            contentDescription = "Color personalizado",
            tint = if (isSelected) ForestGreen else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ColorPickerDialogContent(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }

    // Initialize from current color
    androidx.compose.runtime.LaunchedEffect(initialColor) {
        val hsv = colorToHSV(initialColor)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val currentColor = Color.hsv(hue, saturation, value)

    // Update parent when color changes
    androidx.compose.runtime.LaunchedEffect(hue, saturation, value) {
        onColorChanged(currentColor)
    }

    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Color preview
        androidx.compose.material3.Surface(
            modifier = Modifier.size(200.dp),
            color = currentColor,
            shape = MaterialTheme.shapes.medium
        ) {}

        Spacer(modifier = Modifier.size(16.dp))

        // Hue slider
        Text("Tono (Hue)", style = MaterialTheme.typography.bodySmall)
        HSVSlider(
            value = hue,
            onValueChange = { hue = it },
            range = 0f..360f,
            color = Color.hsv(hue, 1f, 1f)
        )

        Spacer(modifier = Modifier.size(8.dp))

        // Saturation slider
        Text("Saturación", style = MaterialTheme.typography.bodySmall)
        HSVSlider(
            value = saturation,
            onValueChange = { saturation = it },
            range = 0f..1f,
            color = Color.hsv(hue, saturation, value)
        )

        Spacer(modifier = Modifier.size(8.dp))

        // Value (brightness) slider
        Text("Brillo", style = MaterialTheme.typography.bodySmall)
        HSVSlider(
            value = value,
            onValueChange = { value = it },
            range = 0f..1f,
            color = Color.hsv(hue, saturation, value)
        )

        Spacer(modifier = Modifier.size(8.dp))

        // Hex display
        Text(
            "Hex: ${currentColor.toArgb().toHexString()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HSVSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    color: Color
) {
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        colors = androidx.compose.material3.SliderDefaults.colors(
            activeTrackColor = color,
            inactiveTrackColor = color.copy(alpha = 0.3f)
        )
    )
}

private fun colorToHSV(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

private fun Int.toHexString(): String {
    return "#${this.toString(16).uppercase().padStart(8, '0')}"
}
