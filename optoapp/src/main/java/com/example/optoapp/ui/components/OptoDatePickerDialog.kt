package com.example.optoapp.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptoDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    yearRange: IntRange = 1920..2080,
    dismissButton: (@Composable () -> Unit)? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(initialDate),
        yearRange = yearRange
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { mills ->
                    onDateSelected(DateUtils.pickerMillisToLocalDate(mills))
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = dismissButton
    ) {
        DatePicker(state = datePickerState)
    }
}
