package com.example.optoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun OptoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    maxLength: Int? = null,
    showCharCount: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val limitedValue = if (maxLength != null && newValue.length > maxLength) {
                newValue.substring(0, maxLength)
            } else {
                newValue
            }
            onValueChange(limitedValue)
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        supportingText = when {
            showCharCount && maxLength != null -> {
                { Text("${value.length}/$maxLength") }
            }
            isError -> {
                { Text(supportingText ?: "Error de entrada") }
            }
            supportingText != null -> {
                { Text(supportingText) }
            }
            else -> null
        },
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}
