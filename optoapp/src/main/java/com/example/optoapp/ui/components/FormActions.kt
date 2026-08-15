package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.optoapp.R
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.ui.theme.OptoAppTheme

@Composable
fun FormActions(
    onSave: () -> Unit,
    onCancel: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    saveLoading: Boolean = false,
    saveText: String = stringResource(R.string.common_save),
    cancelText: String = stringResource(R.string.common_cancel),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCancel != null) {
            OptoButton(
                text = cancelText,
                onClick = onCancel,
                variant = OptoButtonVariant.Outlined,
                enabled = cancelEnabled,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(OptoTokens.spacing.sm))
        }
        OptoButton(
            text = saveText,
            onClick = onSave,
            variant = OptoButtonVariant.Filled,
            enabled = saveEnabled,
            loading = saveLoading,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FormActionsPreview() {
    OptoAppTheme { FormActions(onSave = {}, onCancel = {}) }
}

@Preview(showBackground = true)
@Composable
private fun FormActionsLoadingPreview() {
    OptoAppTheme { FormActions(onSave = {}, saveLoading = true) }
}
