package com.example.optoapp.ui.components.monturas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
        DropdownField(label = "Material *", selected = form.materialMontura, options = listOf("Acetato", "Metal", "Carey", "Econ")) { opt ->
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

        Text("* Campos obligatorios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
