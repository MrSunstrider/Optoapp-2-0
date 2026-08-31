package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.drawer.DrawerFooter
import com.example.optoapp.ui.components.drawer.DrawerHeader
import com.example.optoapp.ui.components.drawer.DrawerNavEntry
import com.example.optoapp.ui.components.drawer.DrawerNavSection
import com.example.optoapp.ui.components.drawer.DrawerQuickAccess
import com.example.optoapp.ui.components.drawer.DrawerQuickAccessEntry
import com.example.optoapp.ui.components.drawer.DrawerSection
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.util.SyncErrorSanitizer
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderUi
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import kotlinx.coroutines.launch

private fun NavController.navigateDrawer(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun buildQuickAccessEntries(showCierreCaja: Boolean): List<DrawerQuickAccessEntry> {
    val entries = mutableListOf(
        DrawerQuickAccessEntry(
            route = Route.Pacientes.route,
            label = "Pacientes",
            icon = Icons.Default.Person,
            contentDescription = "Pacientes",
        ),
        DrawerQuickAccessEntry(
            route = Route.ServiciosExtra.route,
            label = "Servicios Extra",
            icon = Icons.Default.AddShoppingCart,
            contentDescription = "Servicios Extra",
        ),
    )
    if (showCierreCaja) {
        entries.add(
            DrawerQuickAccessEntry(
                route = Route.CierreCaja.route,
                label = "Cierre de Caja",
                icon = Icons.Default.AccountBalanceWallet,
                contentDescription = "Cierre de Caja",
                testTag = TestTags.NAV_DRAWER_CIERRE_CAJA,
            ),
        )
    }
    return entries
}

internal fun buildDrawerSections(
    showOperacionHoy: Boolean,
    showBiYReportes: Boolean,
    showConfiguracion: Boolean,
    showCierreCaja: Boolean,
    conflictCount: Int,
): List<DrawerNavSection> {
    val operacionEntries = buildList {
        if (showOperacionHoy) {
            add(
                DrawerNavEntry(
                    route = Route.OperacionHoy.route,
                    label = "Dashboard",
                    icon = Icons.Default.Today,
                    contentDescription = "Dashboard",
                ),
            )
        }
        add(
            DrawerNavEntry(
                route = Route.Agenda.route,
                label = "Agenda",
                icon = Icons.Default.CalendarMonth,
                contentDescription = "Agenda",
                testTag = TestTags.NAV_BOTTOM_AGENDA,
            ),
        )
    }

    val inventarioEntries = listOf(
        DrawerNavEntry(
            route = Route.Monturas.route,
            label = "Monturas",
            icon = Icons.Default.Inventory2,
            contentDescription = "Monturas",
        ),
        DrawerNavEntry(
            route = Route.InventarioFisico.route,
            label = "Conteo físico",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            contentDescription = "Conteo físico",
        ),
        DrawerNavEntry(
            route = Route.OrdenesCompra.route,
            label = "Pedidos a proveedor",
            icon = Icons.Default.LocalShipping,
            contentDescription = "Pedidos a proveedor",
        ),
        DrawerNavEntry(
            route = Route.Proveedores.route,
            label = "Proveedores",
            icon = Icons.Default.Business,
            contentDescription = "Proveedores",
        ),
    )

    val finanzasEntries = buildList {
        if (showCierreCaja) {
            add(
                DrawerNavEntry(
                    route = Route.CierreCaja.route,
                    label = "Cierre de Caja",
                    icon = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Cierre de Caja",
                ),
            )
        }
        if (showBiYReportes) {
            add(
                DrawerNavEntry(
                    route = Route.EstadisticasBI.route,
                    label = "Análisis Financiero",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Análisis Financiero",
                ),
            )
            add(
                DrawerNavEntry(
                    route = Route.Reportes.route,
                    label = "Reportes",
                    icon = Icons.Default.DateRange,
                    contentDescription = "Reportes",
                    testTag = TestTags.NAV_DRAWER_REPORTES,
                ),
            )
            add(
                DrawerNavEntry(
                    route = Route.CostosYGastos.route,
                    label = "Costos y Gastos",
                    icon = Icons.Default.AccountBalance,
                    contentDescription = "Costos y Gastos",
                    isSelected = { current ->
                        current == Route.CostosYGastos.route || current?.startsWith("${Route.CostosYGastos.route}/") == true
                    },
                ),
            )
        }
    }

    val sistemaEntries = buildList {
        if (showConfiguracion) {
            add(
                DrawerNavEntry(
                    route = Route.Configuracion.route,
                    label = "Configuración",
                    icon = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    testTag = TestTags.NAV_DRAWER_CONFIGURACION,
                ),
            )
        }
        if (conflictCount > 0) {
            add(
                DrawerNavEntry(
                    route = Route.Conflictos.route,
                    label = "Conflictos",
                    icon = Icons.Default.Warning,
                    contentDescription = "Conflictos",
                    badgeCount = conflictCount,
                    isDanger = true,
                ),
            )
        }
    }

    return buildList {
        add(DrawerNavSection(title = "OPERACIÓN", entries = operacionEntries))
        add(DrawerNavSection(title = "INVENTARIO", entries = inventarioEntries))
        if (finanzasEntries.isNotEmpty()) {
            add(DrawerNavSection(title = "FINANZAS", entries = finanzasEntries))
        }
        if (sistemaEntries.isNotEmpty()) {
            add(DrawerNavSection(title = "SISTEMA", entries = sistemaEntries))
        }
    }
}

@Composable
fun DrawerContent(
    currentRoute: String?,
    drawerState: DrawerState,
    navController: NavController,
    opticaHeader: OpticaHeaderUi,
    isOnline: Boolean,
    showCierreCaja: Boolean,
    showBiYReportes: Boolean,
    showOperacionHoy: Boolean,
    showConfiguracion: Boolean,
    syncState: SyncState,
    syncViewModel: SyncViewModel,
    authViewModel: AuthViewModel,
    parentNavController: NavController,
    onChangeOptica: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val conflictCount by syncViewModel.conflictCount.collectAsState()

    val quickAccessEntries = remember(showCierreCaja) { buildQuickAccessEntries(showCierreCaja) }
    val sections = remember(showOperacionHoy, showBiYReportes, showConfiguracion, showCierreCaja, conflictCount) {
        buildDrawerSections(showOperacionHoy, showBiYReportes, showConfiguracion, showCierreCaja, conflictCount)
    }

    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Success -> {
                android.widget.Toast.makeText(
                    context,
                    (syncState as SyncState.Success).message,
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                syncViewModel.clearSyncUiState()
            }
            is SyncState.Error -> {
                errorMessage = SyncErrorSanitizer.forUserMessage(
                    (syncState as SyncState.Error).message,
                )
                showErrorDialog = true
            }
            else -> {}
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                syncViewModel.clearSyncUiState()
            },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    syncViewModel.clearSyncUiState()
                }) { Text("Entendido") }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Error de Sincronización")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(errorMessage)
                }
            },
        )
    }

    LaunchedEffect(Unit) { syncViewModel.refreshConflicts() }

    Column(modifier = Modifier.fillMaxHeight()) {
        DrawerHeader(
            opticaHeader = opticaHeader,
            isOnline = isOnline,
            onChangeOptica = onChangeOptica,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            DrawerQuickAccess(
                entries = quickAccessEntries,
                currentRoute = currentRoute,
                onEntryClick = { entry ->
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer(entry.route)
                },
            )
            sections.forEach { section ->
                DrawerSection(
                    section = section,
                    currentRoute = currentRoute,
                    onEntryClick = { entry ->
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer(entry.route)
                    },
                )
            }
        }

        DrawerFooter(
            syncState = syncState,
            onSyncClick = {
                if (syncState !is SyncState.Loading) {
                    syncViewModel.performFullSync()
                }
            },
            onLogoutClick = {
                scope.launch {
                    drawerState.close()
                    authViewModel.logout()
                    parentNavController.navigate(Route.Login.route) {
                        popUpTo(parentNavController.graph.id) { inclusive = true }
                    }
                }
            },
        )
    }
}
