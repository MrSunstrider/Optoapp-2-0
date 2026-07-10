package com.example.optoapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.util.DateUtils
import java.time.LocalDate

@Composable
fun FechaEntregaEditButton(
    fechaEntrega: LocalDate?,
    onFechaChanged: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { showDatePicker = true }
    ) {
        Text(
            text = "Entregado el día ${fechaEntrega?.let { DateUtils.formatLocalized(it) } ?: ""}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.Edit,
            contentDescription = "Editar fecha de entrega",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(14.dp).height(14.dp)
        )
    }

    if (showDatePicker) {
        OptoDatePickerDialog(
            initialDate = fechaEntrega ?: LocalDate.now(),
            onDateSelected = { date ->
                onFechaChanged(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
