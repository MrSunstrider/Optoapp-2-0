package com.example.optoapp.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
class MonthSelectorState(selected: YearMonth, val currentMonth: YearMonth) {
    val selected: YearMonth = minOf(selected, currentMonth)

    val canGoNext: Boolean get() = selected.isBefore(currentMonth)

    fun previous(): MonthSelectorState = MonthSelectorState(selected.minusMonths(1), currentMonth)

    fun next(): MonthSelectorState = MonthSelectorState(selected.plusMonths(1), currentMonth)
}

private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-PE"))

@Composable
fun MonthSelector(
    state: MonthSelectorState,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onMonthSelected(state.previous().selected) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.month_selector_previous),
                )
            }
            Text(
                text = state.selected.format(monthLabelFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = { if (state.canGoNext) onMonthSelected(state.next().selected) },
                enabled = state.canGoNext,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.month_selector_next),
                )
            }
        }
    }
}
