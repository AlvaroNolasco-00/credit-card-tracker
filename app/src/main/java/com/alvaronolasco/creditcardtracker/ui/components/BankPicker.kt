package com.alvaronolasco.creditcardtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvaronolasco.creditcardtracker.data.entity.SupportedBank

private const val CUSTOM_BANK_ID = "__custom__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankPicker(
    selectedBankId: String?,
    customBankName: String,
    onBankSelected: (bankId: String?, displayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val displayValue = SupportedBank.fromId(selectedBankId)?.displayName
        ?: if (selectedBankId == null && customBankName.isNotEmpty()) customBankName
        else if (selectedBankId == null) "" else ""

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                label = { Text("Banco") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SupportedBank.entries.forEach { bank ->
                    DropdownMenuItem(
                        text = { Text(bank.displayName) },
                        onClick = {
                            onBankSelected(bank.id, bank.displayName)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Otro (personalizado)") },
                    onClick = {
                        onBankSelected(null, "")
                        expanded = false
                    }
                )
            }
        }

        if (selectedBankId == null) {
            AppTextField(
                value = customBankName,
                onValueChange = { onBankSelected(null, it) },
                label = "Nombre del banco",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
