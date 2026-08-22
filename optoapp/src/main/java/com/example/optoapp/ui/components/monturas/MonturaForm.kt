package com.example.optoapp.ui.components.monturas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.optoapp.domain.inventario.InventarioItemKind
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.MonturaFormState

@Composable
fun MonturaEditForm(
    form: MonturaFormState,
    onUpdate: (MonturaFormState) -> Unit,
    error: String?,
) {
    val esAccesorio = form.tipoItem.equals(InventarioItemKind.ACCESORIO, ignoreCase = true)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Text("Tipo de ítem *", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !esAccesorio,
                onClick = {
                    onUpdate(
                        form.copy(
                            tipoItem = InventarioItemKind.MONTURA,
                            categoria = form.categoria.takeUnless { InventarioItemKind.isAccesorio(it) }.orEmpty(),
                        ),
                    )
                },
                label = { Text("Montura") },
            )
            FilterChip(
                selected = esAccesorio,
                onClick = {
                    onUpdate(
                        form.copy(
                            tipoItem = InventarioItemKind.ACCESORIO,
                            tipoAro = "",
                            materialMontura = "",
                            anchoMm = "",
                            puenteMm = "",
                            alturaMm = "",
                            categoria = "",
                        ),
                    )
                },
                label = { Text("Accesorio") },
            )
        }
        if (esAccesorio) {
            Text(
                "Líquidos, estuches, paños y otros ítems con stock (sin aro ni material).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OptoTextField(
            value = form.sku,
            onValueChange = { v -> onUpdate(form.copy(sku = v)) },
            label = if (esAccesorio) "SKU * (código)" else "SKU * (código de montura)",
            isError = error?.contains("SKU", ignoreCase = true) == true,
        )
        OptoTextField(
            value = form.marca,
            onValueChange = { v -> onUpdate(form.copy(marca = v)) },
            label = "Marca *",
            isError = error?.contains("Marca", ignoreCase = true) == true,
        )
        OptoTextField(
            value = form.modelo,
            onValueChange = { v -> onUpdate(form.copy(modelo = v)) },
            label = if (esAccesorio) "Nombre / descripción *" else "Modelo *",
            isError = error?.contains("Modelo", ignoreCase = true) == true,
        )
        OptoTextField(
            form.color,
            { v -> onUpdate(form.copy(color = v)) },
            if (esAccesorio) "Variedad / presentación" else "Color",
        )
        if (!esAccesorio) {
            OptoTextField(form.talla, { v -> onUpdate(form.copy(talla = v)) }, "Talla (calibre)")
            DropdownField(
                label = "Tipo de aro *",
                selected = form.tipoAro,
                options = listOf("Aro Completo", "Semi al aire", "Al aire"),
            ) { opt ->
                onUpdate(form.copy(tipoAro = opt))
            }
            DropdownField(
                label = "Material *",
                selected = form.materialMontura,
                options = listOf("Acetato", "Metal", "Carey", "TR-90", "Econ"),
            ) { opt ->
                onUpdate(form.copy(materialMontura = opt))
            }
        }

        OptoTextField(form.costo, { v -> onUpdate(form.copy(costo = v)) }, "Costo unitario", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.precio, { v -> onUpdate(form.copy(precio = v)) }, "Precio de venta", keyboardType = KeyboardType.Decimal)
        OptoTextField(form.stockActual, { v -> onUpdate(form.copy(stockActual = v)) }, "Stock inicial (unidades)", keyboardType = KeyboardType.Number)
        OptoTextField(form.stockMinimo, { v -> onUpdate(form.copy(stockMinimo = v)) }, "Stock mínimo para alerta", keyboardType = KeyboardType.Number)

        Text("* Campos obligatorios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
