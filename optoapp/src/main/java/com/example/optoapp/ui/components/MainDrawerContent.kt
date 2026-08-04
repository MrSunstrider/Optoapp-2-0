package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.util.SyncErrorSanitizer
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderUi
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import kotlinx.coroutines.launch

@Composable
fun MainDrawerContent(
    drawerState: DrawerState,
    navController: NavController,
    parentNavController: NavController,
    syncViewModel: SyncViewModel,
    authViewModel: AuthViewModel,
    opticaHeader: OpticaHeaderUi,
    currentRoute: String?,
    opticaRol: String,
    showCierreCaja: Boolean,
    showBiYReportes: Boolean,
    showOperacionHoy: Boolean,
) {
    val scope = rememberCoroutineScope()
    val syncState by syncViewModel.syncState.collectAsState()
    val isSilentSyncing by syncViewModel.isSilentSyncing.collectAsState()

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val initial = opticaHeader.nombreOptica.firstOrNull()?.uppercase() ?: "O"
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = opticaHeader.nombreOptica.ifBlank { "Sin óptica" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Plan Activo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                text = "GESTIÓN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            NavigationDrawerItem(
                label = { Text("Pacientes", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "pacientes",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.Pacientes.route) {
                        popUpTo("pacientes") { inclusive = true }
                    }
                },
                icon = { Icon(Icons.Default.Person, contentDescription = "Pacientes") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag(TestTags.NAV_DRAWER_MENU),
            )
            NavigationDrawerItem(
                label = { Text("Servicios Varios", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "servicios_extra",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.ServiciosExtra.route)
                },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Servicios Varios") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            if (showOperacionHoy) {
                NavigationDrawerItem(
                    label = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "operacion_hoy",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Route.OperacionHoy.route)
                    },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Dashboard") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            Text(
                text = "PROGRAMACIÓN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            NavigationDrawerItem(
                label = { Text("Agenda", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "agenda",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.Agenda.route)
                },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Agenda") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag(TestTags.NAV_BOTTOM_AGENDA),
            )
            NavigationDrawerItem(
                label = { Text("Inventario", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "monturas",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.Monturas.route)
                },
                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventario") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            NavigationDrawerItem(
                label = { Text("Órdenes de Compra", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "ordenes_compra",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.OrdenesCompra.route)
                },
                icon = { Icon(Icons.Default.Receipt, contentDescription = "Órdenes de Compra") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            NavigationDrawerItem(
                label = { Text("Inventario Físico", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "inventario_fisico",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.InventarioFisico.route)
                },
                icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventario Físico") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )

            if (showCierreCaja || showBiYReportes) {
                Text(
                    text = "FINANZAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                )
                if (showCierreCaja) {
                    NavigationDrawerItem(
                        label = { Text("Cierre de Caja", fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == "cierre_caja",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.CierreCaja.route)
                        },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Cierre de Caja") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
                if (showBiYReportes) {
                    NavigationDrawerItem(
                        label = { Text("Análisis Financiero", fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == "estadisticas_bi",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.EstadisticasBI.route)
                        },
                        icon = {
                            @Suppress("DEPRECATION")
                            Icon(Icons.Default.TrendingUp, contentDescription = "Análisis Financiero")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                    NavigationDrawerItem(
                        label = { Text("Reportes", fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == "reportes",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Reportes.route)
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Reportes") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }

            Text(
                text = "SISTEMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            NavigationDrawerItem(
                label = { Text("Configuración", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "configuracion",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Route.Configuracion.route)
                },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag(TestTags.NAV_DRAWER_CONFIGURACION),
            )

            val context = androidx.compose.ui.platform.LocalContext.current

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
                    is SyncState.Offline -> {
                        android.widget.Toast.makeText(context, "Sin conexión", android.widget.Toast.LENGTH_SHORT).show()
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

            NavigationDrawerItem(
                label = {
                    Text(
                        if (syncState is SyncState.Loading) "Sincronizando..." else "Sincronizar Cloud",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                selected = false,
                onClick = {
                    if (syncState !is SyncState.Loading) {
                        syncViewModel.performFullSync()
                    }
                },
                icon = {
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar")
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            NavigationDrawerItem(
                label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = {
                    scope.launch {
                        drawerState.close()
                        authViewModel.logout()
                        parentNavController.navigate(Route.Login.route) {
                            popUpTo(parentNavController.graph.id) { inclusive = true }
                        }
                    }
                },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
