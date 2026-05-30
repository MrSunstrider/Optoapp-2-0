package com.example.optoapp.ui.components.virtualtryon

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.optoapp.data.Montura

/**
 * Bottom sheet for selecting a montura from a provided list.
 *
 * @param monturas Full list of available monturas
 * @param isLoading Whether monturas are still loading
 * @param onMonturaSelected Callback when a montura is chosen
 * @param onDismiss Callback when the sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonturaSelectorBottomSheet(
    monturas: List<Montura>,
    isLoading: Boolean,
    onMonturaSelected: (Montura) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterTipoAro by remember { mutableStateOf("") }

    // Apply filters
    val filteredMonturas = monturas.filter { m ->
        val matchesSearch = searchQuery.isBlank() ||
            m.marca.contains(searchQuery, ignoreCase = true) ||
            m.modelo.contains(searchQuery, ignoreCase = true)
        val matchesFilter = filterTipoAro.isBlank() || m.tipoAro == filterTipoAro
        matchesSearch && matchesFilter
    }

    // Available tipoAro values for filter
    val tipoAroOptions = monturas.map { it.tipoAro }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Seleccionar Montura",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar montura...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // TipoAro filter chips
            if (tipoAroOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTipoAro.isBlank(),
                        onClick = { filterTipoAro = "" },
                        label = { Text("Todos") }
                    )
                    tipoAroOptions.take(5).forEach { tipo ->
                        FilterChip(
                            selected = filterTipoAro == tipo,
                            onClick = { filterTipoAro = tipo },
                            label = { Text(tipo) }
                        )
                    }
                }
            }

            // Montura list
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredMonturas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (monturas.isEmpty()) "No hay monturas disponibles"
                               else "Sin resultados para la búsqueda",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredMonturas, key = { it.id }) { montura ->
                        MonturaListItem(
                            montura = montura,
                            onClick = {
                                onMonturaSelected(montura)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single item in the montura selection list.
 */
@Composable
private fun MonturaListItem(
    montura: Montura,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val thumbnail = montura.imagenUri?.let { uriStr ->
                        try {
                            val uri = Uri.parse(uriStr)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = "${montura.marca} ${montura.modelo}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${montura.marca} ${montura.modelo}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(montura.color)
                        if (montura.tipoAro.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(montura.tipoAro)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (montura.anchoMm != null || montura.puenteMm != null) {
                    Text(
                        text = buildString {
                            montura.anchoMm?.let { append("${it.toInt()}□") }
                            montura.puenteMm?.let { append(" ${it.toInt()}") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
