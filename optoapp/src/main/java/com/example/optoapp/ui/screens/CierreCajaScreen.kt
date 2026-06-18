package com.example.optoapp.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.arqueo.ArqueoCaja
import com.example.optoapp.viewmodel.ArqueoCajaViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.BadgeColor
import com.example.optoapp.viewmodel.CierreCajaViewModel
import com.example.optoapp.util.ArqueoCajaPdfGenerator
import com.example.optoapp.util.DateUtils
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(
    navController: NavController,
    viewModel: CierreCajaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    arqueoVM: ArqueoCajaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val arqueoUiState by arqueoVM.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val opticaId by authViewModel.opticaId.collectAsState(initial = "")
    val canView = AppRoles.canViewCierreCaja(opticaRol)
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.localDateToPickerMillis(uiState.fecha)
    )

    LaunchedEffect(uiState.fecha, opticaId) {
        if (opticaId.isNotBlank()) {
            viewModel.observeArqueoForDate(uiState.fecha, opticaId)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setFecha(DateUtils.pickerMillisToLocalDate(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun exportPdf(arqueo: ArqueoCaja) {
        val pdfBytes = ArqueoCajaPdfGenerator.generate(arqueo, opticaId)
        val file = File(context.cacheDir, "arqueo_${arqueo.fecha}_${arqueo.opticaId}.pdf")
        file.writeBytes(pdfBytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Exportar Arqueo PDF"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            OptoTopAppBar(
                title = "Cierre de Caja",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (canView) {
                        uiState.arqueoForFecha?.let { arqueo ->
                            IconButton(onClick = { exportPdf(arqueo) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Cambiar Fecha")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            if (!canView) {
                OptoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Tu rol actual no tiene permiso para consultar cierre de caja.")
                    }
                }
                return@Column
            }

            Text(
                text = "Reporte del ${DateUtils.formatLocalized(uiState.fecha)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL VENTAS DEL DÍA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "s/. ${String.format(Locale.getDefault(), "%.2f", uiState.totalVentasHoy)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (uiState.saldoPendiente > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Saldo pendiente: s/. ${String.format(Locale.getDefault(), "%.2f", uiState.saldoPendiente)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totales = viewModel.getTotalesPorMetodo()
            val totalGeneral = totales.values.sum()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Efectivo", totales["Efectivo"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                ResumenCard("Móvil/Trans", (totales["Transferencia"] ?: 0.0) + (totales["Móvil"] ?: 0.0), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                ResumenCard("Tarjeta", totales["Tarjeta"] ?: 0.0, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL RECAUDADO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "s/. ${String.format(Locale.getDefault(), "%.2f", totalGeneral)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Desglose", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ventas de hoy", fontSize = 13.sp)
                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", uiState.ventasHoy)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cobros atrasados", fontSize = 13.sp)
                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", uiState.cobrosAtrasados)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total ingresado hoy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("s/. ${String.format(Locale.getDefault(), "%.2f", uiState.ventasHoy + uiState.cobrosAtrasados)}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ArqueoSection(
                arqueoFromCierre = uiState.arqueoForFecha,
                arqueoUiState = arqueoUiState,
                systemTotals = totales,
                fecha = uiState.fecha,
                opticaId = opticaId,
                onFondoCajaChange = arqueoVM::setFondoCaja,
                onEfectivoContadoChange = arqueoVM::setEfectivoContado,
                onTarjetaContadoChange = arqueoVM::setTarjetaContado,
                onTransferenciaContadoChange = arqueoVM::setTransferenciaContado,
                onMovilContadoChange = arqueoVM::setMovilContado,
                onCerrarDia = { arqueoVM.cerrarDia(uiState.fecha, opticaId, totales) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Detalle de Transacciones", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.pagos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay transacciones registradas este día", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.pagos.forEach { pago ->
                        TransactionItem(pago)
                    }
                }
            }
        }
    }
}

@Composable
fun ArqueoSection(
    arqueoFromCierre: ArqueoCaja?,
    arqueoUiState: com.example.optoapp.viewmodel.ArqueoCajaUiState,
    systemTotals: Map<String, Double>,
    fecha: java.time.LocalDate,
    opticaId: String,
    onFondoCajaChange: (Double) -> Unit,
    onEfectivoContadoChange: (Double) -> Unit,
    onTarjetaContadoChange: (Double) -> Unit,
    onTransferenciaContadoChange: (Double) -> Unit,
    onMovilContadoChange: (Double) -> Unit,
    onCerrarDia: () -> Unit
) {
    val isSellado = arqueoFromCierre?.sellado == true
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSellado)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Arqueo de Caja", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSellado) {
                        AssistChip(
                            onClick = {},
                            label = { Text("SELLADO") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Colapsar arqueo" else "Expandir arqueo",
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            if (isSellado) {
                val arqueo = arqueoFromCierre!!
                ReadOnlyField("Fondo de Caja", arqueo.fondoCaja)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Método", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                ArqueoReadOnlyRow("Efectivo", arqueo.efectivoContado, arqueo.efectivoCobrado, arqueo.diferenciaEfectivo)
                ArqueoReadOnlyRow("Tarjeta", arqueo.tarjetaContado, arqueo.tarjetaCobrado, arqueo.diferenciaTarjeta)
                ArqueoReadOnlyRow("Transferencia", arqueo.transferenciaContado, arqueo.transferenciaCobrado, arqueo.diferenciaTransferencia)
                ArqueoReadOnlyRow("Móvil", arqueo.movilContado, arqueo.movilCobrado, arqueo.diferenciaMovil)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ReadOnlyField("Diferencia Total", arqueo.diferenciaTotal)
                ReadOnlyField("Cerrado por", arqueo.cerradoPor)
            } else {
                ArqueoNumberField("Fondo de Caja", arqueoUiState.fondoCaja, onFondoCajaChange)
                ArqueoNumberField("Efectivo Contado", arqueoUiState.efectivoContado, onEfectivoContadoChange)
                ArqueoNumberField("Tarjeta Contado", arqueoUiState.tarjetaContado, onTarjetaContadoChange)
                ArqueoNumberField("Transferencia Contado", arqueoUiState.transferenciaContado, onTransferenciaContadoChange)
                ArqueoNumberField("Móvil Contado", arqueoUiState.movilContado, onMovilContadoChange)

                arqueoUiState.validationErrors.forEach { (field, error) ->
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onCerrarDia,
                    enabled = !arqueoUiState.isSellado,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cerrar Día")
                }
            }

                } // end Column inside AnimatedVisibility
            }   // end AnimatedVisibility
        }
    }
}

@Composable
private fun ArqueoNumberField(label: String, value: Double, onValueChange: (Double) -> Unit) {
    var textValue by remember(value) { mutableStateOf(if (value == 0.0) "" else value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
            val parsed = newText.toDoubleOrNull()
            if (parsed != null) onValueChange(parsed)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReadOnlyField(label: String, value: Any) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when (value) {
                is Double -> "s/. ${String.format(Locale.getDefault(), "%.2f", value)}"
                else -> value.toString()
            },
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ArqueoReadOnlyRow(
    method: String,
    contado: Double,
    cobrado: Double,
    diferencia: Double
) {
    val cobradoAmount = cobrado
    val badgeColor = when {
        diferencia == 0.0 -> BadgeColor.GREEN
        cobradoAmount == 0.0 -> BadgeColor.RED
        else -> {
            val ratio = kotlin.math.abs(diferencia) / cobradoAmount
            if (ratio <= 0.05) BadgeColor.YELLOW else BadgeColor.RED
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(method, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("Cont: ${"%.0f".format(contado)}", fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("Cob: ${"%.0f".format(cobrado)}", fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(
            "Dif: ${"%.0f".format(diferencia)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when (badgeColor) {
                BadgeColor.GREEN -> Color(0xFF2E7D32)
                BadgeColor.YELLOW -> Color(0xFFF57F17)
                BadgeColor.RED -> Color(0xFFC62828)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ResumenCard(label: String, monto: Double, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                "s/. ${String.format(Locale.getDefault(), "%.0f", monto)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionItem(pago: com.example.optoapp.data.Pago) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pago.dispensacionId != null) "Dispensación" else "Servicio Extra",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(pago.metodoPago, fontWeight = FontWeight.Bold)
                if (pago.nota.isNotEmpty()) {
                    Text(pago.nota, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "s/. ${String.format(Locale.getDefault(), "%.2f", pago.monto)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    pago.monto < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
        }
    }
}
