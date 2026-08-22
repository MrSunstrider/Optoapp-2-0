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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura
import com.example.optoapp.domain.inventario.InventarioItemKind
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
    canEdit: Boolean = true,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.sm)) {
        if (porReponer.isNotEmpty()) {
            item {
                Text(
                    "Reposición urgente",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs),
                )
            }
            items(porReponer, key = { "low-${it.id}" }) { m ->
                MonturaItem(
                    montura = m,
                    canEdit = canEdit,
                    onEdit = { onEdit(m) },
                    onDelete = { onDelete(m) },
                    onEntrada = { onEntrada(m) },
                    onSalida = { onSalida(m) },
                )
            }
            item {
                Spacer(modifier = Modifier.height(OptoTokens.spacing.xs))
                Text(
                    "Catálogo de monturas",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = OptoTokens.spacing.xs),
                )
            }
        }
        items(restantes, key = { it.id }) { m ->
            MonturaItem(
                montura = m,
                canEdit = canEdit,
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
    canEdit: Boolean = true,
) {
    var showDelete by remember { mutableStateOf(false) }
    val stockBajo = montura.stockActual <= montura.stockMinimo
    val esAccesorio = InventarioItemKind.isAccesorio(montura.categoria)
    val specs = if (esAccesorio) {
        montura.color.takeIf { it.isNotBlank() }.orEmpty()
    } else {
        listOfNotNull(
            montura.color.takeIf { it.isNotBlank() },
            montura.talla.takeIf { it.isNotBlank() }?.let { "Talla $it" },
            montura.tipoAro.takeIf { it.isNotBlank() },
            montura.materialMontura.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = OptoTokens.elevation.level1),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (stockBajo) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(OptoTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${montura.marca} ${montura.modelo}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "SKU ${montura.sku}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (esAccesorio) {
                        Text(
                            "Accesorio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            "${montura.stockActual}",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = if (stockBajo) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        disabledLabelColor = if (stockBajo) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    ),
                )
            }

            if (specs.isNotBlank()) {
                Text(
                    specs,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Venta s/. ${String.format(Locale.getDefault(), "%.2f", montura.precio)}" +
                        " · Costo s/. ${String.format(Locale.getDefault(), "%.2f", montura.costo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Mín. ${montura.stockMinimo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stockBajo) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (stockBajo) FontWeight.Bold else FontWeight.Normal,
                )
            }

            if (stockBajo) {
                Text(
                    "Reponer en vitrina / almacén",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            if (canEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(modifier = Modifier.size(48.dp), onClick = onSalida) {
                        Icon(Icons.Default.Remove, contentDescription = "Salida de stock −1")
                    }
                    IconButton(modifier = Modifier.size(48.dp), onClick = onEntrada) {
                        Icon(Icons.Default.Add, contentDescription = "Entrada de stock +1")
                    }
                    IconButton(modifier = Modifier.size(48.dp), onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar montura")
                    }
                    IconButton(modifier = Modifier.size(48.dp), onClick = { showDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar montura")
                    }
                }
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Eliminar montura") },
            text = {
                Text(
                    "¿Quitar ${montura.marca} ${montura.modelo} (SKU ${montura.sku}) del inventario? " +
                        "No afecta historial de ventas ya registradas.",
                )
            },
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
