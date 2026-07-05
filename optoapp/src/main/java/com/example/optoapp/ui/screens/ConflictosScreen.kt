package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.domain.sync.EntitySnapshotSerializer
import com.example.optoapp.domain.sync.MergeInput
import com.example.optoapp.domain.sync.ThreeWayMerge
import com.example.optoapp.viewmodel.SyncViewModel
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard

private val TYPE_LABELS = mapOf(
    "paciente" to "Paciente",
    "evaluacion" to "Evaluación",
    "dispensacion" to "Dispensación",
    "servicio_extra" to "Servicio Varios",
    "pago" to "Pago",
    "montura" to "Montura",
    "montura_movimiento" to "Movimiento de montura",
    "proveedor" to "Proveedor",
    "categoria_montura" to "Categoría de montura",
    "orden_compra" to "Orden de compra",
    "orden_compra_item" to "Ítem de orden de compra",
    "inventario_fisico" to "Inventario físico",
    "inventario_fisico_detalle" to "Detalle de inventario físico",
    "dispensacion_item" to "Ítem de dispensación",
    "arqueo_caja" to "Arqueo de caja"
)

private fun entityTypeLabel(type: String): String = TYPE_LABELS[type] ?: type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictosScreen(
    navController: NavController,
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val conflicts by syncViewModel.conflicts.collectAsState()

    LaunchedEffect(Unit) {
        syncViewModel.refreshConflicts()
    }

    Scaffold(
        topBar = {
            OptoTopAppBar(
                title = "Conflictos de sincronización",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (conflicts.isNotEmpty()) {
                        TextButton(onClick = {
                            syncViewModel.resolveKeepMineAll()
                        }) {
                            Text("Usar el mío para todos")
                        }
                        TextButton(onClick = {
                            syncViewModel.acceptAllCloud()
                        }) {
                            Text("Usar nube para todos")
                        }
                        TextButton(onClick = {
                            conflicts.forEach { syncViewModel.dismissConflict(it) }
                        }) {
                            Text("Descartar todos")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (conflicts.isEmpty()) {
            // Sin conflictos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay conflictos",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Todos los datos están sincronizados correctamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "${conflicts.size} conflicto(s) detectado(s)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Dos dispositivos modificaron los mismos datos sin conexión. " +
                            "Elegí qué versión conservar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(conflicts, key = { it.entityId }) { conflict ->
                    ConflictCard(
                        conflict = conflict,
                        onKeepMine = { syncViewModel.resolveKeepMine(conflict) },
                        onAcceptTheirs = { syncViewModel.resolveAcceptTheirs(conflict) },
                        onDismiss = { syncViewModel.dismissConflict(conflict) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(
    conflict: ConflictRecord,
    onKeepMine: () -> Unit,
    onAcceptTheirs: () -> Unit,
    onDismiss: () -> Unit
) {
    OptoCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entityTypeLabel(conflict.entityType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = conflict.entityId.take(8) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FR-12: Field-level conflict details when snapshot data is available
            val hasSnapshotData = EntitySnapshotSerializer.hasSnapshotData(conflict.baseSnapshot)
            if (hasSnapshotData) {
                // Compute merge result for field-level diff display
                val mergeResult = remember(conflict.baseSnapshot, conflict.localData, conflict.remoteData) {
                    runCatching {
                        ThreeWayMerge.merge(
                            MergeInput(
                                baseJson = EntitySnapshotSerializer.parseSnapshot(conflict.baseSnapshot),
                                localJson = EntitySnapshotSerializer.parseSnapshot(conflict.localData),
                                remoteJson = EntitySnapshotSerializer.parseSnapshot(conflict.remoteData)
                            )
                        )
                    }.getOrNull()
                }

                if (mergeResult != null) {
                    // Show conflicted fields
                    if (mergeResult.conflictedFields.isNotEmpty()) {
                        Text(
                            "Campos en conflicto:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        mergeResult.conflictedFields.forEach { field ->
                            val localVal = EntitySnapshotSerializer
                                .parseSnapshot(conflict.localData)[field]?.toString() ?: "—"
                            val remoteVal = EntitySnapshotSerializer
                                .parseSnapshot(conflict.remoteData)[field]?.toString() ?: "—"
                            Text(
                                text = "$field: local=$localVal vs nube=$remoteVal",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Show auto-merged count
                    if (mergeResult.autoMergedFields.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${mergeResult.autoMergedFields.size} campo(s) auto-mergeado(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Fallback if merge computation fails
                    Text(
                        "Datos de conflicto disponibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Timestamp-based display (original behavior)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Versión local", style = MaterialTheme.typography.labelSmall)
                        Text(
                            conflict.localSnapshot.take(19),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Versión nube", style = MaterialTheme.typography.labelSmall)
                        Text(
                            conflict.remoteSnapshot.take(19),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onKeepMine,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Usar el mío", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAcceptTheirs,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Usar nube", maxLines = 1)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Descartar")
                }
            }
        }
    }
}
