package com.alvaronolasco.creditcardtracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import com.alvaronolasco.creditcardtracker.ui.theme.*

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        textStyle = textStyle.copy(fontWeight = FontWeight.Medium),
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        shape = CircleShape,
        trailingIcon = trailingIcon,
        supportingText = if (isError && errorText != null) {
            { Text(errorText, color = MaterialTheme.colorScheme.error) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else ForestGreen,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                   else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = if (isError) MaterialTheme.colorScheme.error else ForestGreen,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    )
}
