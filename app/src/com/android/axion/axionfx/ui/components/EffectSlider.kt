package com.android.axion.axionfx.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.android.axion.compose.preferences.LocalPreferencePosition
import com.android.axion.compose.preferences.PreferencePosition
import com.android.axion.compose.preferences.EditableSliderPreference

@Composable
fun EffectSlider(
    title: String,
    summary: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unit: String = "",
    position: PreferencePosition = LocalPreferencePosition.current,
    onReset: (() -> Unit)? = null,
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val displayValue = if (unit.isNotEmpty()) {
        "${value.toInt()} $unit"
    } else {
        value.toInt().toString()
    }

    EditableSliderPreference(
        title = title,
        summary = summary,
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = {},
        valueRange = valueRange,
        displayValue = displayValue,
        modifier = modifier,
        enabled = enabled,
        position = position,
        onReset = onReset,
        onValueClick = { showEditDialog = true },
    )

    if (showEditDialog) {
        EditValueDialog(
            title = title,
            initialValue = value,
            valueRange = valueRange,
            unit = unit,
            onDismiss = { showEditDialog = false },
            onConfirm = {
                onValueChange(it)
                showEditDialog = false
            },
        )
    }
}

@Composable
private fun EditValueDialog(
    title: String,
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue.toInt().toString()) }
    val parsed = text.toFloatOrNull()
    val isValid = parsed != null && parsed in valueRange

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $title") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    Text(
                        "Value (${valueRange.start.toInt()}–${valueRange.endInclusive.toInt()}${
                            if (unit.isNotEmpty()) " $unit" else ""
                        })"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !isValid,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = isValid) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
