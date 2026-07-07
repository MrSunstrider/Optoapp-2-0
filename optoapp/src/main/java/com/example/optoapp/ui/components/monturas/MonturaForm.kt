package com.example.optoapp.ui.components.monturas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.MonturaFormState

@Composable
fun MonturaEditForm(
    form: MonturaFormState,
    onUpdate: (MonturaFormState) -> Unit,
    error: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        OptoTextField(
            value = form.sku,
            onValueChange = { v -> onUpdate(form.copy(sku = v)) },
            label = "SKU * (Código único)",
            isError = error?.contains("SKU", ignoreCase = true) == true
        )
        OptoTextField(
            value = form.marca,
            onValueChange = { v -> onUpdate(form.copy(marca = v)) },
            label = "Marca / Fabricante *",
            isError = error?.contains("Marca", ignoreCase = true) == true
        )
        OptoTextField(
            value = form.modelo,
            onValueChange = { v -> onUpdate(form.copy(modelo = v)) },
            label = "Modelo / Nombre Producto *",
            isError = error?.contains("Modelo", ignoreCase = true) == true
        )
        OptoTextField(form.color, { v -> onUpdate(form.copy(color = v)) }, "Color / Variedad")
        OptoTextField(form.talla, { v -> onUpdate(form.copy(talla = v)) }, "Talla / Tamaño")
        DropdownField(label = "Tipo de Aro *", selected = form.tipoAro, options = listOf("Aro Completo", "Semi al aire", "Al aire")) { opt ->
            onUpdate(form.copy(tipoAro = opt))
        }
        DropdownField(label = "Material *", selected = form.materialMontura, options = listOf("Acetato", "Metal", "Carey", "TR-90", "Econ")) { opt ->
            onUpdate(form.copy(materialMontura = opt))
        }
        OptoTextField(form.costo, { v -> onUpdate(form.copy(costo = v)) }, "Costo unitario", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.precio, { v -> onUpdate(form.copy(precio = v)) }, "Precio venta", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.stockActual, { v -> onUpdate(form.copy(stockActual = v)) }, "Stock inicial", keyboardType = KeyboardType.Number)
        OptoTextField(form.stockMinimo, { v -> onUpdate(form.copy(stockMinimo = v)) }, "Alerta stock mínimo", keyboardType = KeyboardType.Number)

        OptoTextField(form.anchoMm, { v -> onUpdate(form.copy(anchoMm = v)) }, "Ancho (mm)", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.puenteMm, { v -> onUpdate(form.copy(puenteMm = v)) }, "Puente (mm)", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.alturaMm, { v -> onUpdate(form.copy(alturaMm = v)) }, "Altura (mm)", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.imagenUri, { v -> onUpdate(form.copy(imagenUri = v)) }, "Imagen (URI opcional)")

        var expandedCatalogo by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedCatalogo = !expandedCatalogo }
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Catálogo extendido",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (expandedCatalogo) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expandedCatalogo) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = expandedCatalogo) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Categoría",
                    selected = form.categoria,
                    options = listOf("SOL", "GRADUADA", "DEPORTIVA", "INFANTIL")
                ) { opt -> onUpdate(form.copy(categoria = opt)) }
                OptoTextField(form.coleccion, { v -> onUpdate(form.copy(coleccion = v)) }, "Colección")
                OptoTextField(form.temporada, { v -> onUpdate(form.copy(temporada = v)) }, "Temporada")
                DropdownField(
                    label = "Estado Comercial",
                    selected = form.estadoComercial,
                    options = listOf("ACTIVO", "DISCONTINUADO", "PROMOCION", "PENDIENTE")
                ) { opt -> onUpdate(form.copy(estadoComercial = opt)) }
                DropdownField(
                    label = "Género",
                    selected = form.genero,
                    options = listOf("MASCULINO", "FEMENINO", "UNISEX", "INFANTIL")
                ) { opt -> onUpdate(form.copy(genero = opt)) }
            }
        }

        var expandedProveedor by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedProveedor = !expandedProveedor }
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Proveedor (opcional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (expandedProveedor) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expandedProveedor) "Collapse" else "Expand"
            )
        }
        AnimatedVisibility(visible = expandedProveedor) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptoTextField(
                    value = form.costoProveedor,
                    onValueChange = { v -> onUpdate(form.copy(costoProveedor = v)) },
                    label = "Costo proveedor",
                    keyboardType = KeyboardType.Decimal
                )
                OptoTextField(
                    value = form.precioSugerido,
                    onValueChange = { v -> onUpdate(form.copy(precioSugerido = v)) },
                    label = "Precio sugerido",
                    keyboardType = KeyboardType.Decimal
                )
            }
        }

        Text("* Campos obligatorios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
