package com.example.optoapp.ui.components.monturas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import java.util.Locale

@Composable
fun MonturaListSection(
    porReponer: List<Montura>,
    restantes: List<Montura>,
    onEdit: (Montura) -> Unit,
    onDelete: (Montura) -> Unit,
    onEntrada: (Montura) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (porReponer.isNotEmpty()) {
            item {
                Text(
                    "Por reponer",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(porReponer, key = { "low-${it.id}" }) { m ->
                MonturaItem(
                    montura = m,
                    onEdit = { onEdit(m) },
                    onDelete = { onDelete(m) },
                    onEntrada = { onEntrada(m) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Todos los productos",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        items(restantes, key = { it.id }) { m ->
            MonturaItem(
                montura = m,
                onEdit = { onEdit(m) },
                onDelete = { onDelete(m) },
                onEntrada = { onEntrada(m) }
            )
        }
    }
}

@Composable
fun MonturaItem(
    montura: Montura,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEntrada: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${montura.marca} ${montura.modelo}", fontWeight = FontWeight.Bold)
                    Text("SKU: ${montura.sku}")
                }
                Row {
                    IconButton(onClick = onEntrada) { Icon(Icons.Default.Add, contentDescription = "Entrada +1") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
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
            }
        )
    }
}
