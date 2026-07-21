package com.example.optoapp.ui.components.monturas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import com.example.optoapp.ui.theme.OptoTokens
import java.util.Locale

@Composable
fun MonturaListSection(
    porReponer: List<Montura>,
    restantes: List<Montura>,
    onEdit: (Montura) -> Unit,
    onDelete: (Montura) -> Unit,
    onEntrada: (Montura) -> Unit,
    onSalida: (Montura) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
        if (porReponer.isNotEmpty()) {
            item {
                Text(
                    "Por reponer",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs),
                )
            }
            items(porReponer, key = { "low-${it.id}" }) { m ->
                MonturaItem(
                    montura = m,
                    onEdit = { onEdit(m) },
                    onDelete = { onDelete(m) },
                    onEntrada = { onEntrada(m) },
                    onSalida = { onSalida(m) },
                )
            }
            item {
                Spacer(modifier = Modifier.height(OptoTokens.spacing.xs))
                Text(
                    "Todos los productos",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs),
                )
            }
        }
        items(restantes, key = { it.id }) { m ->
            MonturaItem(
                montura = m,
                onEdit = { onEdit(m) },
                onDelete = { onDelete(m) },
                onEntrada = { onEntrada(m) },
                onSalida = { onSalida(m) },
            )
        }
    }
}

@Composable
fun MonturaItem(
    montura: Montura,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEntrada: () -> Unit,
    onSalida: () -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = OptoTokens.elevation.level1),
    ) {
        Column(
            modifier = Modifier.padding(OptoTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.xs),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${montura.marca} ${montura.modelo}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("SKU: ${montura.sku}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    IconButton(modifier = Modifier.size(48.dp), onClick = onSalida) { Icon(Icons.Default.Remove, contentDescription = "Salida -1") }
                    IconButton(modifier = Modifier.size(48.dp), onClick = onEntrada) { Icon(Icons.Default.Add, contentDescription = "Entrada +1") }
                    IconButton(modifier = Modifier.size(48.dp), onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                    IconButton(modifier = Modifier.size(48.dp), onClick = { showDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                }
            }
            Text("Color/Talla: ${montura.color.ifBlank { "-" }} / ${montura.talla.ifBlank { "-" }}")
            Text("Costo: s/. ${String.format(Locale.getDefault(), "%.2f", montura.costo)}")
            Text("Precio: s/. ${String.format(Locale.getDefault(), "%.2f", montura.precio)}")
            Text("Stock: ${montura.stockActual}  |  Mínimo: ${montura.stockMinimo}")
            if (montura.stockActual <= montura.stockMinimo) {
                Text("Stock bajo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar ${montura.marca} ${montura.modelo}?") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDelete = false
                }) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDelete = false }) { Text("Cancelar") }
            },
        )
    }
}
