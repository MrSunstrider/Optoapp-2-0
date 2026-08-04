package com.example.optoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.AppRoles
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.theme.AlertRed
import com.example.optoapp.ui.theme.PositiveGreen
import com.example.optoapp.ui.theme.WarningAmber
import com.example.optoapp.util.DateUtils
import com.example.optoapp.util.FileShareUtils
import com.example.optoapp.util.InventarioMonturasPdfGenerator
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OperacionHoyViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperacionHoyScreen(
    navController: NavController,
    drawerState: DrawerState,
    viewModel: OperacionHoyViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")
    val canView = AppRoles.canViewOperacionHoy(opticaRol)
    val canExportInventario = AppRoles.canExportInventario(opticaRol)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var expanded by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = expanded, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallFloatingActionButton(
                            onClick = {
                                navController.navigate(Route.NuevoPaciente.route)
                                expanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Icon(Icons.Default.PersonAdd, "Nuevo Paciente")
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                navController.navigate(Route.NuevoServicio.route)
                                expanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Icon(Icons.Default.PostAdd, "Nuevo Servicio")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                FloatingActionButton(
                    onClick = { expanded = !expanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        if (expanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Acciones rápidas",
                    )
                }
            }
        },
        topBar = {
            OptoTopAppBar(
                title = "Dashboard",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
            )
        },
    ) { padding ->
        if (!canView) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Bloqueado", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("Acceso restringido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        
            Text(
                DateUtils.formatLocalized(uiState.fecha),
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

        
            Text(
                "Acciones rápidas",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickAction("Paciente", Icons.Default.PersonAdd, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) {
                    navController.navigate(Route.NuevoPaciente.route)
                }
                QuickAction("Servicio", Icons.Default.Handyman, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) {
                    navController.navigate(Route.NuevoServicio.route)
                }
                QuickAction("Caja", Icons.Default.PointOfSale, MaterialTheme.colorScheme.secondary, Modifier.weight(1f)) {
                    navController.navigate(Route.CierreCaja.route)
                }
            }

        
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardKpi(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Payments,
                        label = "Cobros hoy",
                        value = "s/. ${fmt(uiState.cobrosHoy)}",
                        color = PositiveGreen,
                    )
                    DashboardKpi(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        label = "Citas hoy",
                        value = "${uiState.citasHoy}",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = if (uiState.citasHoy > 0) {
                            { navController.navigate(Route.Agenda.route) }
                        } else {
                            null
                        },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardKpi(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        label = "Entregas pendientes",
                        value = "${uiState.entregasPendientes}",
                        color = if (uiState.entregasPendientes > 0) AlertRed else PositiveGreen,
                        highlight = uiState.entregasPendientes > 0,
                    )
                    DashboardKpi(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "Stock crítico",
                        value = "${uiState.stockCritico}",
                        color = if (uiState.stockCritico > 0) AlertRed else PositiveGreen,
                        highlight = uiState.stockCritico > 0,
                        onClick = if (uiState.stockCritico > 0) {
                            { navController.navigate(Route.Monturas.route) }
                        } else {
                            null
                        },
                    )
                }
            }

        
            if (uiState.alertas.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.08f)),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Alertas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarningAmber)
                        }
                        uiState.alertas.take(5).forEach { alerta ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = AlertRed, fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(alerta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PositiveGreen.copy(alpha = 0.06f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Completado", tint = PositiveGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Todo al día — sin alertas", fontSize = 13.sp, color = PositiveGreen)
                    }
                }
            }

        
            if (uiState.dispensacionesPendientes.isNotEmpty() || uiState.serviciosPendientes.isNotEmpty()) {
                Text(
                    "Pendientes de entrega",
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                uiState.dispensacionesPendientes.take(5).forEach { disp ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable {
                            navController.navigate(Route.InformacionFinanciera(disp.id).route)
                        },
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = "Inventario", tint = AlertRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OT ${disp.ot.ifBlank { "-" }}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    DateUtils.formatLocalized(disp.fecha) + if (disp.fecha.isBefore(uiState.fecha)) " · Atrasada" else "",
                                    fontSize = 11.sp,
                                    color = if (disp.fecha.isBefore(uiState.fecha)) AlertRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("s/. ${fmt(disp.montoTotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                uiState.serviciosPendientes.take(3).forEach { serv ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable {
                            navController.navigate(Route.EditarServicio(serv.id).route)
                        },
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Handyman, contentDescription = "Servicios", tint = AlertRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(serv.descripcion.ifBlank { "Servicio Extra" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Servicio · Pendiente", fontSize = 11.sp, color = AlertRed)
                            }
                            Text("s/. ${fmt(serv.montoTotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                val totalPendientes = uiState.dispensacionesPendientes.size + uiState.serviciosPendientes.size
                if (totalPendientes > 8) {
                    Text(
                        "... y ${totalPendientes - 8} más",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

        
            if (canExportInventario) {
                OutlinedButton(
                    onClick = {
                        val file = InventarioMonturasPdfGenerator.generate(context, uiState.monturas)
                        FileShareUtils.openPdf(context, file, "Abrir inventario PDF")
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar inventario PDF")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DashboardKpi(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = "Indicador", tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (highlight) {
                    Box(modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(4.dp), color = AlertRed.copy(alpha = 0.2f)) {
                        Text("!", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), fontSize = 9.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun fmt(value: Double): String = if (value == value.toLong().toDouble()) {
    String.format(Locale.getDefault(), "%,.0f", value)
} else {
    String.format(Locale.getDefault(), "%,.2f", value)
}
