package com.example.optoapp.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.optoapp.ui.theme.OptoAppTheme

@Composable
fun OptoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun OptoFilterChipPreview() {
    OptoAppTheme {
        OptoFilterChip(selected = false, onClick = {}, label = "Filtro")
        OptoFilterChip(selected = true, onClick = {}, label = "Seleccionado")
    }
}
