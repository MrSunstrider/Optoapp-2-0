package com.example.optoapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optoapp.BuildConfig
import com.example.optoapp.R
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

@Composable
fun DrawerContent(
    currentRoute: String?,
    drawerState: DrawerState,
    navController: NavController,
    opticaHeader: OpticaHeaderUi,
    showCierreCaja: Boolean,
    showBiYReportes: Boolean,
    showOperacionHoy: Boolean,
    showConfiguracion: Boolean,
    syncState: SyncState,
    syncViewModel: SyncViewModel,
    authViewModel: AuthViewModel,
    parentNavController: NavController,
) {
    val scope = rememberCoroutineScope()
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val conflictCount by syncViewModel.conflictCount.collectAsState()

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
                }) { Text(stringResource(R.string.drawer_sync_error_ok)) }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = stringResource(R.string.drawer_sync_error_cd), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.drawer_sync_error_title))
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(errorMessage)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_login),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(0.65f).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            )
            Text(
                text = opticaHeader.nombreOptica.ifBlank { stringResource(R.string.drawer_sin_optica) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.drawer_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Text(
            text = stringResource(R.string.drawer_gestion),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_pacientes), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "pacientes",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("pacientes")
            },
            icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.drawer_pacientes)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_servicios_extra), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "servicios_extra",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("servicios_extra")
            },
            icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = stringResource(R.string.drawer_servicios_extra)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        if (showOperacionHoy) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_dashboard), fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "operacion_hoy",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("operacion_hoy")
                },
                icon = { Icon(Icons.Default.Today, contentDescription = stringResource(R.string.drawer_dashboard)) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        Text(
            text = stringResource(R.string.drawer_programacion),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_agenda), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "agenda",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("agenda")
            },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.drawer_agenda)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        Text(
            text = stringResource(R.string.drawer_inventario_optico),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_monturas), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "monturas",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("monturas")
            },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = stringResource(R.string.drawer_monturas)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_conteo_fisico), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "inventario_fisico",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("inventario_fisico")
            },
            icon = { Icon(Icons.Default.FactCheck, contentDescription = stringResource(R.string.drawer_conteo_fisico)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_pedidos_proveedor), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "ordenes_compra",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("ordenes_compra")
            },
            icon = { Icon(Icons.Default.LocalShipping, contentDescription = stringResource(R.string.drawer_pedidos_proveedor)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_proveedores), fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "proveedores",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("proveedores")
            },
            icon = { Icon(Icons.Default.Business, contentDescription = stringResource(R.string.drawer_proveedores)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        if (showCierreCaja || showBiYReportes) {
            Text(
                text = stringResource(R.string.drawer_finanzas),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            if (showCierreCaja) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_cierre_caja), fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "cierre_caja",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("cierre_caja")
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = stringResource(R.string.drawer_cierre_caja)) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            if (showBiYReportes) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_analisis_financiero), fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "estadisticas_bi",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("estadisticas_bi")
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = stringResource(R.string.drawer_analisis_financiero)) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_reportes), fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "reportes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("reportes")
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.drawer_reportes)) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_costos_gastos), fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == Route.CostosYGastos.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer(Route.CostosYGastos.createRoute())
                    },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = stringResource(R.string.drawer_costos_gastos)) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }

        LaunchedEffect(Unit) { syncViewModel.refreshConflicts() }

        Text(
            text = stringResource(R.string.drawer_sistema),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
        )
        if (showConfiguracion) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_configuracion), fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "configuracion",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("configuracion")
                },
                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.drawer_configuracion)) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        if (conflictCount > 0) {
            NavigationDrawerItem(
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.drawer_conflictos), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(
                                conflictCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                },
                selected = currentRoute == "conflictos",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("conflictos")
                },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = stringResource(R.string.drawer_conflictos),
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        NavigationDrawerItem(
            label = {
                Text(
                    if (syncState is SyncState.Loading) stringResource(R.string.drawer_sincronizando) else stringResource(R.string.drawer_sincronizar_cloud),
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
                    Icon(Icons.Default.CloudSync, contentDescription = stringResource(R.string.drawer_sincronizar_cd))
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_cerrar_sesion), color = MaterialTheme.colorScheme.error) },
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
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.drawer_salir_cd)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
