package com.example.optoapp.ui.components.paciente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.ui.components.OrderEstadoChip
import com.example.optoapp.ui.components.common.EmptyState
import com.example.optoapp.ui.theme.saldoColor
import com.example.optoapp.util.MoneyFormatter

@Composable
fun ServiciosExtraList(servicios: List<ServicioExtra>, onEdit: (String) -> Unit, aCuentaSumMap: Map<String, Double> = emptyMap()) {
    if (servicios.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.paciente_serv_vacio_titulo),
            subtitle = stringResource(R.string.paciente_serv_vacio_mensaje),
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(servicios, key = { it.id }) { serv ->
                val date = com.example.optoapp.util.DateUtils.formatLocalized(serv.fecha)
                val aCuenta = aCuentaSumMap[serv.id] ?: 0.0
                val saldo = serv.montoTotal - aCuenta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { onEdit(serv.id) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OrderEstadoChip(serv.estado, saldo = saldo, servicio = true)
                        }
                        Text(serv.descripcion, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(R.string.paciente_serv_saldo), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyFormatter.format(saldo), color = saldoColor(saldo), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
